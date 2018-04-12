package com.ubtrobot.master.transport.connection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.ubtrobot.master.concurrent.MasterEventLoopGroup;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.IncomingHandlerAdapter;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-10-25.
 */

public class MasterSideConnectionPool {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterSideConnectionPool");

    private static volatile Context sApplicationContext;

    @SuppressLint("StaticFieldLeak")
    private static MasterSideConnectionPool sPool;

    private final MasterEventLoopGroup mMasterEventLoopGroup = new MasterEventLoopGroup();
    private volatile ConnectionInitializer mInitializer;

    private final HashMap<IBinder, MasterSideConnection> mBinderConnectionMap = new HashMap<>();
    private final HashMap<String, MasterSideConnection> mIdConnectionMap = new HashMap<>();
    private final ReentrantReadWriteLock mConnectionsLock = new ReentrantReadWriteLock();

    private final MasterSideBinder mMasterSideBinder = new MasterSideBinder();

    public static void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "MasterSideConnectionPool initialized with null context."
            );
        }

        if (sApplicationContext == null) {
            synchronized (MasterSideConnectionPool.class) {
                if (sApplicationContext == null) {
                    sApplicationContext = context.getApplicationContext();
                    sPool = new MasterSideConnectionPool();
                }
            }
        }
    }

    private MasterSideConnectionPool() {
    }

    public static MasterSideConnectionPool get() {
        if (sApplicationContext == null) {
            synchronized (MasterSideConnectionPool.class) {
                if (sApplicationContext == null) {
                    throw new IllegalStateException(
                            "MasterSideConnectionPool MUST be initialized first.");
                }
            }
        }

        return sPool;
    }

    public MasterEventLoopGroup masterEventLoopGroup() {
        return mMasterEventLoopGroup;
    }

    public void setConnectionInitializer(ConnectionInitializer initializer) {
        mInitializer = initializer;
    }

    private MasterSideConnection getConnection(IBinder connectMasterSideBinder) {
        mConnectionsLock.readLock().lock();
        try {
            return mBinderConnectionMap.get(connectMasterSideBinder);
        } finally {
            mConnectionsLock.readLock().unlock();
        }
    }

    /**
     * 分配连接
     *
     * @param connectMasterSideBinder 连接端的 Binder
     * @param packageName             连接端的包名
     * @param connectSideNameForUid   连接端的 uid
     * @param connectSidePid          连接端的 pid
     * @return 分配的连接。如果 connectMasterSideBinder 已经分配过，则分配失败，返回 null
     */
    public MasterSideConnection allocateConnection(
            IBinder connectMasterSideBinder,
            String packageName,
            String connectSideNameForUid,
            int connectSidePid) {
        mConnectionsLock.writeLock().lock();
        try {
            MasterSideConnection connection = mBinderConnectionMap.get(connectMasterSideBinder);
            if (connection != null) {
                return null;
            }

            connection = createConnection(
                    connectMasterSideBinder, connectSideNameForUid, connectSidePid);

            connection.attributes().put(ConnectionConstants.ATTR_KEY_PACKAGE, packageName);
            mBinderConnectionMap.put(connectMasterSideBinder, connection);
            mIdConnectionMap.put(connection.id().asText(), connection);

            if (mInitializer != null) {
                mInitializer.onConnectionInitialize(connection);
            }

            final MasterSideConnection finalConnection = connection;
            connection.connect(new OutgoingCallback() {
                @Override
                public void onSuccess() {
                    LOGGER.i("New connection established. connectionId=%s",
                            finalConnection.id().asText());
                }

                @Override
                public void onFailure(Exception e) {
                    removeConnection(finalConnection.id().asText());

                    LOGGER.e("New connection established failed. connectionId=%s",
                            finalConnection.id().asText());
                }
            });

            return connection;
        } finally {
            mConnectionsLock.writeLock().unlock();
        }
    }

    private MasterSideConnection createConnection(
            IBinder connectMasterSideBinder, String connectSideNameForUid, int connectSidePid) {
        final MasterSideConnection connection;
        connection = new MasterSideConnection(
                new DefaultConnectionId(connectSideNameForUid, connectSidePid),
                mMasterEventLoopGroup.next(),
                allocateMasterSideBinder(),
                connectMasterSideBinder
        );

        connection.pipeline().add(new IncomingHandlerAdapter() {
            @Override
            public void onDisconnected(HandlerContext context) {
                removeConnection(connection.id().asText());
                context.onDisconnected();
            }
        });

        return connection;
    }

    private IBinder allocateMasterSideBinder() {
        // TODO 多个 MasterSideBinder 调度
        return mMasterSideBinder;
    }

    private void removeConnection(String connectionId) {
        mConnectionsLock.writeLock().lock();
        try {
            MasterSideConnection connection = mIdConnectionMap.remove(connectionId);
            if (connection == null) {
                return;
            }

            Iterator<Map.Entry<IBinder, MasterSideConnection>> iterator
                    = mBinderConnectionMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<IBinder, MasterSideConnection> entry = iterator.next();
                if (entry.getValue().id().asText().equals(connection.id().asText())) {
                    iterator.remove();
                    return;
                }
            }
        } finally {
            mConnectionsLock.writeLock().unlock();
        }
    }

    public Connection getConnection(String connectionId) {
        mConnectionsLock.readLock().lock();
        try {
            return mIdConnectionMap.get(connectionId);
        } finally {
            mConnectionsLock.readLock().unlock();
        }
    }

    private final class MasterSideBinder extends Binder {

        @Override
        protected boolean onTransact(
                int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            IBinder binder = data.readStrongBinder();
            if (binder == null) {
                LOGGER.e("Illegal binder transact data. No binder in the data parcel.");
                return false;
            }

            MasterSideConnection connection = getConnection(binder);
            if (connection == null) {
                if (ConnectionConstants.TRANS_CODE_DISCONNECT == code) {
                    LOGGER.d("No connection found associate with the binder." +
                            " Maybe already disconnected. ");
                } else {
                    LOGGER.e("No connection found associate with the binder.");
                }

                return false;
            }

            if (ConnectionConstants.TRANS_CODE_WRITE == code) {
                return beWritten(connection, data);
            }

            if (ConnectionConstants.TRANS_CODE_DISCONNECT == code) {
                beDisconnected(connection);
                return true;
            }

            LOGGER.e("Unexpected transact code. code=%s", code);
            return false;
        }

        private boolean beWritten(Connection connection, Parcel data) {
            ParcelMessage parcelMessage = data.readParcelable(ParcelMessage.class.getClassLoader());
            if (parcelMessage == null) {
                LOGGER.e("Illegal binder transact data. No content in the data parcel.");
                return false;
            }

            connection.pipeline().onRead(parcelMessage);
            return true;
        }

        private void beDisconnected(final Connection connection) {
            connection.disconnect(new OutgoingCallback() {
                @Override
                public void onSuccess() {
                    LOGGER.i("Disconnected by the connect-master side.");
                    // 会在 IncomingHandlerAdapter 中 removeConnection
                }

                @Override
                public void onFailure(Exception e) {
                    LOGGER.i(e, "Disconnected by the connect-master side.");
                    removeConnection(connection.id().asText());
                }
            });
        }
    }
}