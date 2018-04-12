package com.ubtrobot.master.transport.connection.handler;

import android.content.Context;

import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.competition.CompetitionSessionManager;
import com.ubtrobot.master.policy.PolicyConstants;
import com.ubtrobot.master.service.ServiceRouteCallback;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.HandlerContext;

/**
 * Created by column on 17-11-28.
 */

public class ServiceRequestHandler extends MessageSplitter.RequestHandler {

    private final Context mContext;
    private final CompetitionSessionManager mSessionManager;
    private final CallRouter mCallRouter;

    public ServiceRequestHandler(
            Context context,
            CallRouter callRouter,
            CompetitionSessionManager sessionManager) {
        mContext = context;
        mSessionManager = sessionManager;
        mCallRouter = callRouter;
    }

    @Override
    public void onRead(final HandlerContext context, final ParcelRequest request) {
        if (PolicyConstants.POLICY_SERVICE_NAME.equals(request.getContext().getResponder())) {
            IPCResponder responder = new IPCResponder(context.connection(), request);
            responder.respondFailure(CallGlobalCode.FORBIDDEN, "Forbidden to call policy service.");
            return;
        }

        mCallRouter.routeService(context.eventLoop(), request,
                new ServiceRouteCallback(mContext, context, mSessionManager));
    }
}