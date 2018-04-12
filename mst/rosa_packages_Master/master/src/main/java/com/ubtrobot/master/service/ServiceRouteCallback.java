package com.ubtrobot.master.service;

import android.content.Context;
import android.support.annotation.Nullable;

import com.ubtrobot.master.async.Callback;
import com.ubtrobot.master.call.CallRouteCallback;
import com.ubtrobot.master.competition.CompetitionSessionManager;
import com.ubtrobot.master.competition.SessionManageException;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.parcel.AbstractParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.message.Request;

/**
 * Created by column on 17-11-29.
 */

public class ServiceRouteCallback extends CallRouteCallback {

    private final CompetitionSessionManager mSessionManager;

    public ServiceRouteCallback(
            Context context,
            HandlerContext handlerContext,
            CompetitionSessionManager sessionManager) {
        super(context, handlerContext);
        mSessionManager = sessionManager;
    }

    @Override
    public void onRoute(
            final Request request,
            ComponentInfo destinationComponentInfo,
            @Nullable final Connection destinationConnection) {
        final ServiceInfo serviceInfo = (ServiceInfo) destinationComponentInfo;
        mSessionManager.checkRequestCompetingSession(
                (ParcelRequest) request,
                (ServiceInfo) destinationComponentInfo,
                destinationConnection,
                new Callback<Void, SessionManageException>() {
                    @Override
                    public void onSuccess(Void value) {
                        forwardRequest(request, destinationConnection, serviceInfo);
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        IPCResponder responder = new IPCResponder(handlerContext().connection(),
                                (AbstractParcelRequest) request);
                        responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, e.getMessage());
                    }
                }
        );
    }
}
