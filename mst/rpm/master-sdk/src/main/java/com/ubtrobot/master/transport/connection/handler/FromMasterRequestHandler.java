package com.ubtrobot.master.transport.connection.handler;

import android.os.Parcelable;

import com.ubtrobot.master.competition.CompetingItemDetailList;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.ServiceLifecycle;
import com.ubtrobot.master.skill.SkillLifecycle;
import com.ubtrobot.master.transport.message.FromMasterPaths;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;

/**
 * Created by column on 17-11-30.
 */

public class FromMasterRequestHandler extends MessageSplitter.RequestHandler {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("FromMasterRequestHandler");

    private final HashMap<String, MessageSplitter.RequestHandler> mHandlers = new HashMap<>();

    private final SkillLifecycle mSkillLifecycle;
    private final ServiceLifecycle mServiceLifecycle;

    public FromMasterRequestHandler(
            SkillLifecycle skillLifecycle, ServiceLifecycle serviceLifecycle) {
        mHandlers.put(FromMasterPaths.PATH_START_SKILL, new StartSkillHandler());
        mHandlers.put(FromMasterPaths.PATH_STOP_SKILL, new StopSkillHandler());

        mHandlers.put(FromMasterPaths.PATH_SYNC_COMPONENT_LIFETCYCLE,
                new SyncComponentLifecycleHandler());

        mHandlers.put(FromMasterPaths.PATH_GET_COMPETING_ITEMS, new GetCompetingItemsHandler());
        mHandlers.put(FromMasterPaths.PATH_ACTIVATE_COMPETING_SESSION,
                new NotifyCompetingSessionActivateHandler());
        mHandlers.put(FromMasterPaths.PATH_DEACTIVATE_COMPETING_SESSION,
                new NotifyCompetingSessionDeactivateHandler());

        mSkillLifecycle = skillLifecycle;
        mServiceLifecycle = serviceLifecycle;
    }

    @Override
    public void onRead(HandlerContext context, ParcelRequest request) {
        MessageSplitter.RequestHandler handler = mHandlers.get(request.getPath());
        if (handler == null) {
            LOGGER.e("Unexpected request from the master. requestPath=%s", request.getPath());
            return;
        }

        handler.onRead(context, request);
    }

    private class StartSkillHandler extends MessageSplitter.RequestHandler {

        @Override
        public void onRead(final HandlerContext context, final ParcelRequest request) {
            mSkillLifecycle.notifySkillStart(context.eventLoop(), request,
                    new CommonSkillNotifyCallback(context.connection(), request));
        }
    }

    private class StopSkillHandler extends MessageSplitter.RequestHandler {

        @Override
        public void onRead(final HandlerContext context, final ParcelRequest request) {
            mSkillLifecycle.notifySkillStop(context.eventLoop(), request,
                    new CommonSkillNotifyCallback(context.connection(), request));
        }
    }

    private static class CommonSkillNotifyCallback implements SkillLifecycle.SkillNotifyCallback {

        private final Connection mConnection;
        private final ParcelRequest mRequest;

        public CommonSkillNotifyCallback(Connection connection, ParcelRequest request) {
            mConnection = connection;
            mRequest = request;
        }

        @Override
        public void onSuccess() {
            IPCResponder responder = new IPCResponder(mConnection, mRequest);
            responder.respondSuccess();
        }

        @Override
        public void onRetrySoon() {
            // Nothing
            // 会重新回调 XxxSkillHandler.onRead
        }

        @Override
        public void onFailure(CallException e) {
            IPCResponder responder = new IPCResponder(mConnection, mRequest);
            responder.respondFailure(e.getCode(), e.getMessage());
        }
    }

    private class SyncComponentLifecycleHandler extends MessageSplitter.RequestHandler {

        @Override
        public void onRead(HandlerContext context, ParcelRequest message) {
            //
        }
    }

    private class GetCompetingItemsHandler extends MessageSplitter.RequestHandler {

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            mServiceLifecycle.getCompetingItems(
                    context.eventLoop(),
                    request,
                    new CommonCompetingSessionCallback<CompetingItemDetailList>(
                            context.connection(), request
                    )
            );
        }
    }

    private class NotifyCompetingSessionActivateHandler extends MessageSplitter.RequestHandler {

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            mServiceLifecycle.notifyCompetingSessionActivate(context.eventLoop(), request,
                    new CommonCompetingSessionCallback<Void>(context.connection(), request));
        }
    }

    private class NotifyCompetingSessionDeactivateHandler extends MessageSplitter.RequestHandler {

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            mServiceLifecycle.notifyCompetingSessionDeactivate(context.eventLoop(), request,
                    new CommonCompetingSessionCallback<Void>(context.connection(), request));
        }
    }

    private static class CommonCompetingSessionCallback<T>
            implements ServiceLifecycle.CompetingSessionCallback<T> {

        private final Connection mConnection;
        private final ParcelRequest mRequest;

        CommonCompetingSessionCallback(Connection connection, ParcelRequest request) {
            mConnection = connection;
            mRequest = request;
        }

        @Override
        public void onSuccess(T data) {
            IPCResponder responder = new IPCResponder(mConnection, mRequest);
            if (data == null) {
                responder.respondSuccess();
            } else {
                if (data instanceof Parcelable) {
                    responder.respondSuccess(ParcelableParam.create((Parcelable) data));
                    return;
                }

                throw new IllegalStateException("Should be onSuccess(<? extends Parcelable>)");
            }
        }

        @Override
        public void onRetrySoon() {
            // Nothing
            // 会重新回调 *Competing*Handler.onRead
        }

        @Override
        public void onFailure(CallException e) {
            IPCResponder responder = new IPCResponder(mConnection, mRequest);
            responder.respondFailure(e.getCode(), e.getMessage());
        }
    }
}