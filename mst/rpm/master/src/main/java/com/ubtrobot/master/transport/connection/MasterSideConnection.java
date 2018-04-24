package com.ubtrobot.master.transport.connection;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.transport.connection.AbstractConnection;
import com.ubtrobot.transport.connection.ConnectionId;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-10-25.
 */

public class MasterSideConnection extends AbstractConnection {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterSideConnection");

    private final IBinder mMasterSideBinder;
    private final IBinder mConnectMasterSideBinder;

    private volatile boolean mConnected;
    private boolean mDisconnected;

    public MasterSideConnection(
            ConnectionId id, EventLoop eventLoop,
            IBinder masterSideBinder, IBinder connectMasterSideBinder) {
        super(id, eventLoop, true);

        mMasterSideBinder = masterSideBinder;
        mConnectMasterSideBinder = connectMasterSideBinder;
    }

    public IBinder masterSideBinder() {
        return mMasterSideBinder;
    }

    @Override
    public boolean isConnected() {
        return mConnected;
    }

    @Override
    protected Unsafe newUnsafe() {
        return new ConnectionUnsafe();
    }

    private class ConnectionUnsafe implements Unsafe, IBinder.DeathRecipient {

        @Override
        public void connect(OutgoingCallback callback) {
            if (mConnected) {
                callbackFailure(callback, new Exception("Already connected."));
                return;
            }

            if (mDisconnected) {
                callbackFailure(callback, new Exception("Already disconnected."));
                return;
            }

            mConnected = true;
            try {
                mConnectMasterSideBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                callbackFailure(callback, new Exception("Can NOT linkToDeath."));
            }

            callbackSuccess(callback);
            onConnectedAfterCallback();
        }

        @Override
        public void disconnect(OutgoingCallback callback) {
            if (mDisconnected) {
                callbackSuccess(callback);
                return;
            }

            if (!mConnected) {
                callbackFailure(callback, new Exception("Not connected."));
                return;
            }

            mConnectMasterSideBinder.unlinkToDeath(this, 0);
            try {
                mConnectMasterSideBinder.transact(ConnectionConstants.TRANS_CODE_DISCONNECT,
                        null, null, Binder.FLAG_ONEWAY);
            } catch (RemoteException e) {
                // Ignore
            }

            mConnected = false;
            mDisconnected = true;

            callbackSuccess(callback);
            onDisconnectedAfterCallback();
        }

        @Override
        public void write(Object message, OutgoingCallback callback) {
            if (!mConnected) {
                callbackFailure(callback, new Exception("Not connected."));
                return;
            }

            if (!(message instanceof ParcelMessage)) {
                throw new IllegalStateException("Unexpected message type. message class is " +
                        message.getClass().getName());
            }

            Parcel data = Parcel.obtain();
            data.writeParcelable((ParcelMessage) message, 0);

            try {
                mConnectMasterSideBinder.transact(ConnectionConstants.TRANS_CODE_WRITE, data,
                        null, Binder.FLAG_ONEWAY);
                callbackSuccess(callback);
            } catch (RemoteException e) {
                callbackFailure(callback,
                        new Exception("Write message failed. Remote binder maybe crashed.", e));
            } finally {
                data.recycle();
            }
        }

        @Override
        public void binderDied() {
            LOGGER.e("Remote binder maybe crashed. Connection is disconnected. connectionId=%s",
                    id().asText());

            eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    mConnected = false;
                    mDisconnected = true;
                    mConnectMasterSideBinder.unlinkToDeath(ConnectionUnsafe.this, 0);

                    pipeline().onDisconnected();
                }
            });
        }
    }
}
