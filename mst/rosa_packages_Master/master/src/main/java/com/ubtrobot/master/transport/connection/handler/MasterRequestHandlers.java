package com.ubtrobot.master.transport.connection.handler;

import android.os.Bundle;
import android.text.TextUtils;

import com.ubtrobot.master.async.Callback;
import com.ubtrobot.master.competition.ActivateParam;
import com.ubtrobot.master.competition.CompetitionSessionManager;
import com.ubtrobot.master.competition.SessionManageException;
import com.ubtrobot.master.component.ComponentBaseInfo;
import com.ubtrobot.master.event.EventDispatcher;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.ServiceManager;
import com.ubtrobot.master.skill.SkillInfoList;
import com.ubtrobot.master.skill.SkillManageException;
import com.ubtrobot.master.skill.SkillManager;
import com.ubtrobot.master.transport.connection.ConnectionConstants;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.MasterCallPaths;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.ULog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by column on 10/09/2017.
 */

public class MasterRequestHandlers {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterRequestHandlers");

    private static abstract class EventSubscribeBaseHandler extends MessageSplitter.RequestHandler {

        protected final EventDispatcher mInternalEventDispatcher;
        protected final EventDispatcher mSdkUserEventDispatcher;

        public EventSubscribeBaseHandler(
                EventDispatcher internalEventDispatcher,
                EventDispatcher sdkUserEventDispatcher) {
            mInternalEventDispatcher = internalEventDispatcher;
            mSdkUserEventDispatcher = sdkUserEventDispatcher;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            IPCResponder responder = new IPCResponder(context.connection(), request);
            Bundle param;
            try {
                param = ParcelableParam.from(request.getParam(), Bundle.class).getParcelable();
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST,
                        "Illegal argument. Argument event-actions is null or empty.");
                return;
            }

            boolean isInternal = param.getBoolean(ParamBundleConstants.KEY_IS_INTERNAL_EVENT);
            ArrayList<String> actions = param.getStringArrayList(ParamBundleConstants.KEY_EVENT_ACTIONS);
            if (actions == null || actions.isEmpty()) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST,
                        "Illegal argument. Argument event-actions is null or empty.");
                return;
            }

            process(context, actions, isInternal);
            responder.respondSuccess();

            context.onRead(request);
        }

        protected abstract void process(HandlerContext context, List<String> actions, boolean internal);
    }

    /**
     * @see MasterCallPaths 's PATH_SUBSCRIBE_EVENT
     */
    public static class SubscribeEventsHandler extends EventSubscribeBaseHandler {

        public SubscribeEventsHandler(
                EventDispatcher internalEventDispatcher,
                EventDispatcher sdkUserEventDispatcher) {
            super(internalEventDispatcher, sdkUserEventDispatcher);
        }

        @Override
        protected void process(HandlerContext context, List<String> actions, boolean internal) {
            if (internal) {
                mInternalEventDispatcher.subscribe(context.connection(), actions);
            } else {
                mSdkUserEventDispatcher.subscribe(context.connection(), actions);
            }
        }
    }

    /**
     * @see MasterCallPaths 's PATH_UNSUBSCRIBE_EVENT
     */
    public static class UnsubscribeEventsHandler extends EventSubscribeBaseHandler {

        public UnsubscribeEventsHandler(
                EventDispatcher internalEventDispatcher,
                EventDispatcher sdkUserEventDispatcher) {
            super(internalEventDispatcher, sdkUserEventDispatcher);
        }

        @Override
        protected void process(HandlerContext context, List<String> actions, boolean internal) {
            if (internal) {
                mInternalEventDispatcher.unsubscribe(context.connection(), actions);
            } else {
                mSdkUserEventDispatcher.unsubscribe(context.connection(), actions);
            }
        }
    }

    public static class SetSkillStateHandler extends MessageSplitter.RequestHandler {

        private final SkillManager mSkillManager;

        public SetSkillStateHandler(SkillManager skillManager) {
            mSkillManager = skillManager;
        }

        @Override
        public void onRead(final HandlerContext context, final ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);

            String packageName = (String) context.connection().attributes().
                    get(ConnectionConstants.ATTR_KEY_PACKAGE);
            if (packageName == null) {
                throw new AssertionError("packageName != null");
            }

            try {
                Bundle param = ParcelableParam.from(request.getParam(), Bundle.class).getParcelable();
                String name = param.getString(ParamBundleConstants.KEY_SKILL_NAME);
                final String state = param.getString(ParamBundleConstants.KEY_STATE, null);

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(state)) {
                    responder.respondFailure(CallGlobalCode.BAD_REQUEST,
                            "Illegal argument. skillName or state is empty.");
                    return;
                }

                final ComponentBaseInfo skillBaseInfo = new ComponentBaseInfo(name, packageName);
                mSkillManager.setSkillState(
                        context, skillBaseInfo, state,
                        new Callback<Void, SkillManageException>() {
                            @Override
                            public void onSuccess(Void data) {
                                responder.respondSuccess();
                                context.onRead(request);
                            }

                            @Override
                            public void onFailure(SkillManageException e) {
                                if (SkillManageException.CODE_SET_STATE_BEFORE_SKILL_RUNNING ==
                                        e.getCode()) {
                                    responder.respondFailure(CallGlobalCode.BAD_REQUEST,
                                            "Skill NOT found or NOT running when set state. " +
                                                    "skill=%s" + skillBaseInfo + ", state=" +
                                                    state
                                    );
                                } else {
                                    LOGGER.e(e);
                                    responder.respondFailure(CallGlobalCode.INTERNAL_ERROR,
                                            "Internal error. ");
                                }
                            }
                        }
                );
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                LOGGER.e(e);
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal argument.");
            }
        }
    }

    public static class StopSkillHandler extends MessageSplitter.RequestHandler {

        private final SkillManager mSkillManager;

        public StopSkillHandler(SkillManager skillManager) {
            mSkillManager = skillManager;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);

            String packageName = (String) context.connection().attributes().
                    get(ConnectionConstants.ATTR_KEY_PACKAGE);
            if (packageName == null) {
                throw new AssertionError("packageName != null");
            }

            try {
                Bundle param = ParcelableParam.from(request.getParam(), Bundle.class).getParcelable();
                String skillName = param.getString(ParamBundleConstants.KEY_SKILL_NAME);
                if (TextUtils.isEmpty(skillName)) {
                    responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
                    return;
                }

                mSkillManager.stopSkill(
                        context,
                        new ComponentBaseInfo(skillName, packageName),
                        new Callback<Void, SkillManageException>() {
                            @Override
                            public void onSuccess(Void data) {
                                responder.respondSuccess();
                            }

                            @Override
                            public void onFailure(SkillManageException e) {
                                ULog.e(e);
                                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "Internal error.");
                            }
                        });
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                LOGGER.e(e);
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
            }
        }
    }

    public static class GetStartedSkillsHandler extends MessageSplitter.RequestHandler {

        private final SkillManager mSkillManager;

        public GetStartedSkillsHandler(SkillManager skillManager) {
            mSkillManager = skillManager;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);
            responder.respondSuccess(ParcelableParam.create(
                    new SkillInfoList(mSkillManager.getStartedSkills())));
        }
    }

    public static class OperateServiceStateHandler extends MessageSplitter.RequestHandler {

        private final ServiceManager mServiceManager;
        private final boolean mAddOrRemove;

        public OperateServiceStateHandler(ServiceManager serviceManager, boolean addOrRemove) {
            mServiceManager = serviceManager;
            mAddOrRemove = addOrRemove;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);

            String packageName = (String) context.connection().attributes().
                    get(ConnectionConstants.ATTR_KEY_PACKAGE);
            if (packageName == null) {
                throw new AssertionError("packageName != null");
            }

            try {
                Bundle param = ParcelableParam.from(request.getParam(), Bundle.class).getParcelable();
                String name = param.getString(ParamBundleConstants.KEY_SERVICE_NAME);
                final String state = param.getString(ParamBundleConstants.KEY_STATE, null);

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(state)) {
                    responder.respondFailure(CallGlobalCode.BAD_REQUEST,
                            "Illegal argument. skillName or state is empty.");
                    return;
                }

                final ComponentBaseInfo serviceBaseInfo = new ComponentBaseInfo(name, packageName);
                if (mAddOrRemove) {
                    mServiceManager.addServiceState(serviceBaseInfo, state);
                } else {
                    mServiceManager.removeServiceState(serviceBaseInfo, state);
                }

                responder.respondSuccess();
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                LOGGER.e(e);
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal argument.");
            }
        }
    }

    public static class QueryServiceStateHandler extends MessageSplitter.RequestHandler {

        private final ServiceManager mServiceManager;

        public QueryServiceStateHandler(ServiceManager serviceManager) {
            mServiceManager = serviceManager;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);

            try {
                Bundle param = ParcelableParam.from(request.getParam(), Bundle.class).getParcelable();
                String name = param.getString(ParamBundleConstants.KEY_SERVICE_NAME);
                String state = param.getString(ParamBundleConstants.KEY_STATE, null);

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(state)) {
                    responder.respondFailure(CallGlobalCode.BAD_REQUEST,
                            "Illegal argument. skillName or state is empty.");
                    return;
                }

                Bundle bundle = new Bundle();
                if (mServiceManager.didServiceAddState(name, state)) {
                    bundle.putString(ParamBundleConstants.KEY_STATE, state);
                }

                responder.respondSuccess(ParcelableParam.create(bundle));
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                LOGGER.e(e);
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal argument.");
            }
        }
    }

    public static class ActivateCompetingSessionHandler extends MessageSplitter.RequestHandler {

        private final CompetitionSessionManager mSessionManager;

        public ActivateCompetingSessionHandler(CompetitionSessionManager sessionManager) {
            mSessionManager = sessionManager;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);
            try {
                ActivateParam activateParam = ParcelableParam.from(request.getParam(),
                        ActivateParam.class).getParcelable();
                if (activateParam.getSessionInfo().getCompetingItems().isEmpty()) {
                    responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
                    return;
                }

                Callback<String, SessionManageException>
                        callback = new Callback<String, SessionManageException>() {
                    @Override
                    public void onSuccess(String sessionId) {
                        Bundle bundle = new Bundle();
                        bundle.putString(ParamBundleConstants.KEY_COMPETING_SESSION_ID, sessionId);
                        responder.respondSuccess(ParcelableParam.create(bundle));
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        LOGGER.e(e);
                        switch (e.getCode()) {
                            case SessionManageException.CODE_COMPETING_ITEM_NOT_FOUND:
                                responder.respondFailure(CallGlobalCode.BAD_REQUEST, e.getMessage());
                                break;
                            case SessionManageException.CODE_FORBIDDEN_TO_INTERRUPT:
                                responder.respondFailure(CallGlobalCode.FORBIDDEN, e.getMessage());
                                break;
                            case SessionManageException.CODE_SERVICE_INTERNAL_ERROR:
                                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR,
                                        e.getMessage());
                                break;
                            default:
                                throw new AssertionError("Impossible here.");
                        }
                    }
                };

                mSessionManager.activateSession(
                        context,
                        request.getContext(),
                        activateParam.getSessionInfo(),
                        activateParam.getOption(),
                        callback
                );
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
            }
        }
    }

    public static class DeactivateCompetingSessionHandler extends MessageSplitter.RequestHandler {

        private final CompetitionSessionManager mSessionManager;

        public DeactivateCompetingSessionHandler(CompetitionSessionManager sessionManager) {
            mSessionManager = sessionManager;
        }

        @Override
        public void onRead(HandlerContext context, ParcelRequest request) {
            final IPCResponder responder = new IPCResponder(context.connection(), request);
            try {
                String sessionId = ParcelableParam.from(request.getParam(), Bundle.class).
                        getParcelable().getString(ParamBundleConstants.KEY_COMPETING_SESSION_ID);
                if (TextUtils.isEmpty(sessionId)) {
                    responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
                    return;
                }

                Callback<Void, SessionManageException>
                        callback = new Callback<Void, SessionManageException>() {
                    @Override
                    public void onSuccess(Void value) {
                        responder.respondSuccess();
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        throw new AssertionError("Impossible here.");
                    }
                };

                mSessionManager.deactivateSession(
                        context,
                        request.getContext(),
                        sessionId,
                        callback
                );
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
            }
        }
    }
}