package com.ubtrobot.master.call;

import android.content.Context;

import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.parcel.AbstractParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelImplicitRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.Request;

/**
 * Created by column on 17-11-29.
 */

public abstract class CallRouteCallback implements CallRouter.RouteCallback<Request> {

    private final Context mContext;
    private final HandlerContext mHandlerContext;

    public CallRouteCallback(Context context, HandlerContext handlerContext) {
        mContext = context;
        mHandlerContext = handlerContext;
    }

    protected HandlerContext handlerContext() {
        return mHandlerContext;
    }

    protected void forwardRequest(
            final Request request,
            Connection destinationConnection,
            ComponentInfo destinationComponentInfo) {
        ParcelRequest parcelRequest;
        if (request instanceof ParcelImplicitRequest) {
            parcelRequest = ((ParcelImplicitRequest) request).toExplicitRequest();
        } else {
            parcelRequest = (ParcelRequest) request;
        }
        // 收到 Response 后能找到发送 Request 的 Connection
        parcelRequest.changeConnectionId(mHandlerContext.connection().id().asText());

        CallForwarder forwarder = new CallForwarder(
                mContext, destinationConnection, destinationComponentInfo);
        forwarder.forward(parcelRequest, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                mHandlerContext.onRead(request);
            }

            @Override
            public void onFailure(Exception e) {
                IPCResponder responder = new IPCResponder(
                        mHandlerContext.connection(), (AbstractParcelRequest) request);
                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, e.getMessage());
            }
        });
    }

    @Override
    public void onNotFound(Request request) {
        IPCResponder responder = new IPCResponder(
                mHandlerContext.connection(), (AbstractParcelRequest) request);
        responder.respondFailure(CallGlobalCode.NOT_FOUND,
                "Call NOT found. callPath=" + request.getPath());
    }

    @Override
    public void onConflict(Request request) {
        IPCResponder responder = new IPCResponder(
                mHandlerContext.connection(), (AbstractParcelRequest) request);
        responder.respondFailure(CallGlobalCode.CONFLICT,
                "Conflict");
    }
}
