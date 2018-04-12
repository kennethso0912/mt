package com.ubtrobot.master.transport.connection;

import android.content.Context;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.ConnectionId;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.IncomingHandlerAdapter;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.connection.Pipeline;
import com.ubtrobot.ulog.Logger;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by column on 17-8-30.
 */

public class AutoReconnectConnection implements Connection {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("AutoReconnectConnection");

    private final Context mContext;
    private final ConnectionInitializer mConnectionInitializer;

    private final EventLoop mEventLoop;
    private volatile Connection mRealConnection;
    private final ReconnectHandler mReconnectHandler = new ReconnectHandler();

    private boolean mConnecting;
    private final ArrayList<Runnable> mTasksAfterConnectFinish = new ArrayList<>();

    public AutoReconnectConnection(
            Context context, EventLoop eventLoop, ConnectionInitializer initializer) {
        mContext = context.getApplicationContext();
        mEventLoop = eventLoop;
        mConnectionInitializer = initializer;
    }

    private Connection newRealConnectionInLoop() {
        final Connection connection = new ConnectMasterSideConnection(
                new DefaultConnectionId(mContext.getPackageName()), mEventLoop, mContext);
        connection.pipeline().add(mReconnectHandler);
        mConnectionInitializer.onConnectionInitialize(connection);
        return connection;
    }

    @Override
    public void connect(final OutgoingCallback callback) {
        if (eventLoop().inEventLoop()) {
            connectInLoop(callback);
        } else {
            eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    connectInLoop(callback);
                }
            });
        }
    }

    private void connectInLoop(final OutgoingCallback callback) {
        if (isConnected()) {
            callback.onSuccess();
            return;
        }

        doConnectInLoop(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(new Exception("Can NOT connect to the master."));
                }
            }
        });
    }

    @Override
    public void disconnect(OutgoingCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(final Object message, final OutgoingCallback callback) {
        if (eventLoop().inEventLoop()) {
            writeInLoop(message, callback);
        } else {
            eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    writeInLoop(message, callback);
                }
            });
        }
    }

    private void writeInLoop(final Object message, final OutgoingCallback callback) {
        if (isConnected()) {
            mRealConnection.write(message, callback);
            return;
        }

        doConnectInLoop(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    mRealConnection.write(message, callback);
                } else {
                    callback.onFailure(new Exception("Can NOT connect to the master."));
                }
            }
        });
    }

    private void doConnectInLoop(Runnable taskAfterBindFinish) {
        mTasksAfterConnectFinish.add(taskAfterBindFinish);

        if (mConnecting) {
            return;
        }

        mConnecting = true;
        mRealConnection = newRealConnectionInLoop();
        mRealConnection.connect(new OutgoingCallback() {
            @Override
            public void onSuccess() {
                connectFinish();
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Connect to master failed.");
                connectFinish();
            }

            private void connectFinish() {
                mConnecting = false;

                for (Runnable task : mTasksAfterConnectFinish) {
                    task.run();
                }
                mTasksAfterConnectFinish.clear();
            }
        });
    }

    @Override
    public ConnectionId id() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventLoop eventLoop() {
        return mEventLoop;
    }

    @Override
    public Pipeline pipeline() {
        throw new UnsupportedOperationException();
    }

    /**
     * 向 pipeline 中插入被 Master startService 传递的消息，伪造通过 connection 收到了消息
     *
     * @param message
     */
    public void onRead(final Object message, final OutgoingCallback callback) {
        if (eventLoop().inEventLoop()) {
            onReadInLoop(message, callback);
        } else {
            eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    onReadInLoop(message, callback);
                }
            });
        }
    }

    private void onReadInLoop(final Object message, final OutgoingCallback callback) {
        if (isConnected()) {
            mRealConnection.pipeline().onRead(message);
            eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess();
                }
            });

            return;
        }

        doConnectInLoop(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    mRealConnection.pipeline().onRead(message);
                } else {
                    callback.onFailure(new Exception("Can NOT connect to the master."));
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        return mRealConnection != null && mRealConnection.isConnected();
    }

    @Override
    public Map<String, Object> attributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Unsafe unsafe() {
        throw new UnsupportedOperationException();
    }

    private class ReconnectHandler extends IncomingHandlerAdapter {

        @Override
        public void onDisconnected(HandlerContext context) {
            LOGGER.i("Try reconnect master.");
            doConnectInLoop(new Runnable() {
                @Override
                public void run() {
                    if (isConnected()) {
                        LOGGER.i("Reconnect master success.");
                    } else {
                        LOGGER.e("Reconnect master failed.");
                    }
                }
            });

            context.onDisconnected();
        }
    }
}