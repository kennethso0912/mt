package com.ubtrobot.master.transport.connection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.BundleCompatWrapper;
import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.master.transport.version.Version;
import com.ubtrobot.transport.connection.AbstractConnection;
import com.ubtrobot.transport.connection.ConnectionId;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-10-25.
 */

public class ConnectMasterSideConnection extends AbstractConnection {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("ConnectMasterSideConnection");

    private final Context mContext;

    private static final String MASTER_PACKAGE_NAME = "com.ubtrobot.master";
    private static final String AUTHORITY = "content://com.ubtrobot.provider.master";

    protected ConnectMasterSideConnection(
            ConnectionId id, EventLoop eventLoop,
            Context context) {
        super(id, eventLoop);
        mContext = context;
    }

    @Override
    public boolean isConnected() {
        return ((ConnectionUnsafe) unsafe()).isConnected();
    }

    @Override
    protected Unsafe newUnsafe() {
        return new ConnectionUnsafe();
    }

    private class ConnectionUnsafe extends Binder implements Unsafe, IBinder.DeathRecipient {

        private IBinder mMasterSideBinder;
        private OutgoingCallback mCallback;

        private volatile boolean mDisconnected;

        @Override
        public void connect(OutgoingCallback callback) {
            if (mMasterSideBinder != null) {
                callbackFailure(callback, new Exception("Already connected."));
                return;
            }

            if (mCallback != null) {
                callbackFailure(callback, new Exception("Connecting."));
                return;
            }

            mCallback = callback;
            doConnect(callback);
        }

        private void doConnect(OutgoingCallback callback) {
            Bundle args = new Bundle();
            args.putString(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_VERSION, Version.LATEST);
            args.putString(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_PACKAGE, mContext.getPackageName());
            BundleCompatWrapper.putBinder(args, ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_BINDER, this);

            Bundle ret;
            try {
                ret = mContext.getContentResolver().call(Uri.parse(AUTHORITY),
                        ConnectionConstants.PRVDR_CALL_MTHD_CONNECT, null, args);
            } catch (Exception e) { // Master Provider 内部实现出错的异常均可能出现
                if (!isMasterInstalled()) {
                    callbackFailure(callback, new IllegalStateException(
                            "Can NOT connect to master. Master NOT installed.", e));
                    return;
                }

                if (e instanceof SecurityException) {
                    callbackFailure(callback, new IllegalStateException(
                            "Can NOT connect to master. Permission Denial. " +
                                    "Your APK 's signature MUST keep same with the Master apk.", e));
                    return;
                }

                callbackFailure(callback, new IllegalStateException(
                        "Can NOT connect to master. Master internal error.", e));
                return;
            }

            if (ret == null) {
                callbackFailure(callback, new Exception("empty call result from master."));
                return;
            }

            int code = ret.getInt(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_CODE, -1);
            if (code == -1) {
                callbackFailure(callback,
                        new Exception("Unexpected call result from master. No code."));
                return;
            }

            if (code != ConnectionConstants.CODE_SUCCESS) {
                String message = ret.getString(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_ERR_MSG);
                callbackFailure(callback,
                        new Exception("Unexpected call result from master. code=%s"
                                + code + ", message=" + message));
                return;
            }

            mMasterSideBinder = BundleCompatWrapper.getBinder(
                    ret, ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_BINDER);
            if (mMasterSideBinder == null) {
                callbackFailure(callback,
                        new Exception("Unexpected call result from master. No binder."));
                return;
            }

            try {
                mMasterSideBinder.linkToDeath(this, 0);
                callbackSuccess(callback);
                onConnectedAfterCallback();
            } catch (RemoteException e) {
                mMasterSideBinder = null;
                callbackFailure(callback, new Exception("linkToDeath to master failed.", e));
            }
        }

        private boolean isMasterInstalled() {
            try {
                return mContext.getPackageManager().getPackageInfo(MASTER_PACKAGE_NAME, 0) != null;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        public boolean isConnected() {
            return mMasterSideBinder != null && !mDisconnected;
        }

        @Override
        public void disconnect(OutgoingCallback callback) {
            if (mDisconnected) {
                callbackSuccess(callback);
                return;
            }

            if (mMasterSideBinder == null) {
                callbackFailure(callback, new Exception("Not connected."));
                return;
            }

            mDisconnected = true;
            mMasterSideBinder.unlinkToDeath(this, 0);

            Parcel data = Parcel.obtain();
            try {
                data.writeStrongBinder(this);
                mMasterSideBinder.transact(ConnectionConstants.TRANS_CODE_DISCONNECT, data, null, 0);
            } catch (RemoteException e) {
                // Ignore
            } finally {
                data.recycle();
            }

            callbackSuccess(callback);
            onDisconnectedAfterCallback();
        }

        @Override
        public void write(Object message, OutgoingCallback callback) {
            if (mMasterSideBinder == null) {
                callbackFailure(callback, new Exception("Write message before connected."));
                return;
            }

            if (mDisconnected) {
                callbackFailure(callback, new Exception("Disconnected."));
                return;
            }

            if (message instanceof ParcelMessage) {
                Parcel data = Parcel.obtain();
                data.writeStrongBinder(this);
                data.writeParcelable((ParcelMessage) message, 0);

                try {
                    mMasterSideBinder.transact(
                            ConnectionConstants.TRANS_CODE_WRITE, data, null, 0);
                    callbackSuccess(callback);
                } catch (RemoteException e) {
                    callbackFailure(callback,
                            new Exception("Write message failed. Remote binder maybe crashed.", e));
                } finally {
                    data.recycle();
                }

                return;
            }

            throw new IllegalStateException("Unexpected message type. message class is " +
                    message.getClass().getName());
        }

        @Override
        protected boolean onTransact(
                int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (ConnectionConstants.TRANS_CODE_WRITE == code) {
                ParcelMessage parcelMessage = data.readParcelable(ParcelMessage.class.getClassLoader());
                if (parcelMessage == null) {
                    LOGGER.e("Illegal binder transact data. No content in the data parcel.");
                    return false;
                }

                pipeline().onRead(parcelMessage);
                return true;
            }

            if (ConnectionConstants.TRANS_CODE_DISCONNECT == code) {
                disconnect(new OutgoingCallback() {
                    @Override
                    public void onSuccess() {
                        LOGGER.i("Disconnected by the master side.");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        throw new IllegalStateException(e);
                    }
                });

                return true;
            }

            return false;
        }

        @Override
        public void binderDied() {
            LOGGER.e("Master maybe crashed. Connection is disconnected. connectionId=%s",
                    id().asText());

            eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    mDisconnected = true;
                    mMasterSideBinder.unlinkToDeath(ConnectionUnsafe.this, 0);

                    pipeline().onDisconnected();
                }
            });
        }
    }
}
