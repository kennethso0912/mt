package com.ubtrobot.master.transport.connection.handler;

import android.text.TextUtils;

import com.ubtrobot.master.call.IPCFromMasterCallable;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.connection.MasterSideConnectionPool;
import com.ubtrobot.master.transport.message.parcel.ParcelResponse;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-8-31.
 */

public class CompositeResponseHandler extends com.ubtrobot.master.transport.connection.handler.ResponseHandler {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterResponseHandler");

    private final MasterSideConnectionPool mPool;

    public CompositeResponseHandler(IPCFromMasterCallable callable, MasterSideConnectionPool pool) {
        super(callable);
        mPool = pool;
    }

    @Override
    public void onRead(final HandlerContext context, final ParcelResponse response) {
        if (isResponseToMaster(response)) {
            super.onRead(context, response);
            return;
        }

        Connection requestConnection = mPool.getConnection(response.getRequestConnectionId());
        if (requestConnection == null) {
            LOGGER.e("Forward response failed. The connection of the request not found." +
                    " requestConnectionId=%s", response.getRequestConnectionId());
            return;
        }

        requestConnection.write(response, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                context.onRead(response);
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Forward response failed. requestConnectionId=%s, response=%s",
                        response.getRequestConnectionId(), response);
            }
        });
    }

    private boolean isResponseToMaster(ParcelResponse response) {
        return TextUtils.isEmpty(response.getRequestConnectionId());
    }
}