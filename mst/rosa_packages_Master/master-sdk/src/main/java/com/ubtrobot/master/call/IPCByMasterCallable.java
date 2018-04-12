package com.ubtrobot.master.call;

import com.ubtrobot.master.transport.message.AbstractIPCCallable;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.Request;

/**
 * Created by column on 17-9-26.
 */

public class IPCByMasterCallable extends AbstractIPCCallable {

    private final Connection mConnection;

    public IPCByMasterCallable(Connection connection) {
        super(connection.eventLoop());
        mConnection = connection;
    }

    public Connection connection() {
        return mConnection;
    }

    @Override
    protected void sendCallRequest(Request req, final OutgoingCallback callback) {
        mConnection.write(req, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}