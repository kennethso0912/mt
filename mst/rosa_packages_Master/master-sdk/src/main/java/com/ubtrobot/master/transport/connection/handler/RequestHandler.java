package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-8-31.
 */

public class RequestHandler extends MessageSplitter.RequestHandler {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("RequestHandler");

    private final CallRouter mCallRouter;
    private final FromMasterRequestHandler mMasterRequestHandler;

    public RequestHandler(CallRouter callRouter, FromMasterRequestHandler handler) {
        mCallRouter = callRouter;
        mMasterRequestHandler = handler;
    }

    @Override
    public void onRead(final HandlerContext context, final ParcelRequest request) {
        if (ParcelRequestContext.REQUESTER_TYPE_MASTER.
                equals(request.getContext().getRequesterType())) {
            mMasterRequestHandler.onRead(context, request);
            return;
        }

        mCallRouter.route(request, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                context.onRead(request);
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Route call failed. request=%s", request);
            }
        });
    }
}