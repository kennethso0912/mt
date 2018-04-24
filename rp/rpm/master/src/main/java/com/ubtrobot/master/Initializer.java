package com.ubtrobot.master;

import android.content.Context;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.call.CallDestinations;
import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.call.ComponentInfoPool;
import com.ubtrobot.master.call.IPCFromMasterCallable;
import com.ubtrobot.master.call.IPCToPolicyCallable;
import com.ubtrobot.master.competition.CompetitionSessionManager;
import com.ubtrobot.master.event.EventDispatcher;
import com.ubtrobot.master.event.InternalEventDispatcher;
import com.ubtrobot.master.event.StaticEventDispatcher;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.policy.ComponentPolicySource;
import com.ubtrobot.master.service.ServiceManager;
import com.ubtrobot.master.skill.SkillManager;
import com.ubtrobot.master.transport.connection.ConnectionInitializer;
import com.ubtrobot.master.transport.connection.MasterSideConnectionPool;
import com.ubtrobot.master.transport.connection.handler.CompositeResponseHandler;
import com.ubtrobot.master.transport.connection.handler.ConnectionHandler;
import com.ubtrobot.master.transport.connection.handler.EventHandler;
import com.ubtrobot.master.transport.connection.handler.ImplicitSkillRequestHandler;
import com.ubtrobot.master.transport.connection.handler.LoggingHandler;
import com.ubtrobot.master.transport.connection.handler.MasterRequestHandlers;
import com.ubtrobot.master.transport.connection.handler.MasterRequestSplitter;
import com.ubtrobot.master.transport.connection.handler.MessageCodec;
import com.ubtrobot.master.transport.connection.handler.MessageSplitter;
import com.ubtrobot.master.transport.connection.handler.ResponderTypeRequestSplitter;
import com.ubtrobot.master.transport.connection.handler.ServiceRequestHandler;
import com.ubtrobot.master.transport.connection.handler.SkillsRequestHandler;
import com.ubtrobot.master.transport.message.MasterCallPaths;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.ulog.ULog;
import com.ubtrobot.ulog.logger.android.AndroidLoggerFactory;

/**
 * Created by column on 17-10-26.
 */

class Initializer {

    private static final Initializer INITIALIZER = new Initializer();

    private boolean mInitialized;

    private InternalEventDispatcher mInternalEventDispatcher;
    private EventDispatcher mSdkUserEventDispatcher;
    private StaticEventDispatcher mStaticEventDispatcher;

    private ComponentInfoPool mComponentInfoPool;
    private CallDestinations mCallDestinations;
    private CallRouter mCallRouter;

    private CompetitionSessionManager mSessionManager;
    private ServiceManager mServiceManager;
    private SkillManager mSkillManager;

    private Initializer() {
    }

    public static Initializer get() {
        return INITIALIZER;
    }

    public synchronized void initialize(Context context) {
        if (mInitialized) {
            return;
        }

        setupLogger();

        final Context appContext = context.getApplicationContext();
        MasterSideConnectionPool.initialize(appContext);
        EventLoop policyLoop = MasterSideConnectionPool.get().masterEventLoopGroup().managerEventLoop();

        mInternalEventDispatcher = new InternalEventDispatcher();
        mSdkUserEventDispatcher = new EventDispatcher();
        mStaticEventDispatcher = new StaticEventDispatcher(appContext);

        mComponentInfoPool = new ComponentInfoPool(context, policyLoop);
        mCallDestinations = new CallDestinations();
        mCallRouter = new CallRouter(mComponentInfoPool, mCallDestinations);

        final IPCFromMasterCallable fromMasterCallable =
                new IPCFromMasterCallable(context, policyLoop);
        IPCToPolicyCallable toPolicyCallable =
                new IPCToPolicyCallable(mComponentInfoPool, mCallDestinations, fromMasterCallable);

        mSessionManager = new CompetitionSessionManager(
                mCallRouter,
                fromMasterCallable,
                mInternalEventDispatcher
        );
        mServiceManager = new ServiceManager();
        mSkillManager = new SkillManager(
                policyLoop,
                mServiceManager,
                new ComponentPolicySource(context, toPolicyCallable),
                fromMasterCallable,
                mInternalEventDispatcher
        );

        MasterSideConnectionPool.get().setConnectionInitializer(new ConnectionInitializer() {
            @Override
            public void onConnectionInitialize(Connection connection) {
                initializeConnection(appContext, connection, fromMasterCallable);
            }
        });

        mInitialized = true;
    }

    private void setupLogger() {
        AndroidLoggerFactory factory = new AndroidLoggerFactory();
        MasterLoggerFactory.setup(factory);
        ULog.setup("Master", factory);
    }

    private void initializeConnection(
            Context context,
            Connection connection,
            IPCFromMasterCallable fromMasterCallable) {
        connection.pipeline().add(new LoggingHandler());
        connection.pipeline().add(new MessageCodec());
        connection.pipeline().add(
                new ConnectionHandler(
                        mInternalEventDispatcher, mSdkUserEventDispatcher,
                        mComponentInfoPool, mCallDestinations,
                        mSkillManager, mServiceManager, mSessionManager
                )
        );

        connection.pipeline().add(new MessageSplitter(
                new EventHandler(mSdkUserEventDispatcher, mStaticEventDispatcher),
                createResponderTypeRequestSplitter(context),
                new ImplicitSkillRequestHandler(context, mCallRouter, mSkillManager),
                new CompositeResponseHandler(fromMasterCallable, MasterSideConnectionPool.get())
        ));
    }

    private ResponderTypeRequestSplitter createResponderTypeRequestSplitter(Context context) {
        return new ResponderTypeRequestSplitter().
                addHandler(
                        ParcelRequestContext.RESPONDER_TYPE_MASTER,
                        createMasterRequestSplitter()
                ).
                addHandler(
                        ParcelRequestContext.RESPONDER_TYPE_SKILLS,
                        new SkillsRequestHandler(context, mCallRouter, mSkillManager)
                ).
                addHandler(
                        ParcelRequestContext.RESPONDER_TYPE_SERVICE,
                        new ServiceRequestHandler(context, mCallRouter, mSessionManager)
                );
    }

    private MasterRequestSplitter createMasterRequestSplitter() {
        return new MasterRequestSplitter().
                addHandler(
                        MasterCallPaths.PATH_SUBSCRIBE_EVENT,
                        new MasterRequestHandlers.SubscribeEventsHandler(
                                mInternalEventDispatcher, mSdkUserEventDispatcher)
                ).
                addHandler(
                        MasterCallPaths.PATH_UNSUBSCRIBE_EVENT,
                        new MasterRequestHandlers.UnsubscribeEventsHandler(
                                mInternalEventDispatcher, mSdkUserEventDispatcher)
                ).
                addHandler(
                        MasterCallPaths.PATH_SET_SKILL_STATE,
                        new MasterRequestHandlers.SetSkillStateHandler(mSkillManager)
                ).
                addHandler(MasterCallPaths.PATH_STOP_SKILL,
                        new MasterRequestHandlers.StopSkillHandler(mSkillManager)
                ).
                addHandler(MasterCallPaths.PATH_GET_STARTED_SKILLS,
                        new MasterRequestHandlers.GetStartedSkillsHandler(mSkillManager)
                ).
                addHandler(MasterCallPaths.PATH_ADD_SERVICE_STATE,
                        new MasterRequestHandlers.OperateServiceStateHandler(mServiceManager, true)
                ).
                addHandler(MasterCallPaths.PATH_REMOVE_SERVICE_STATE,
                        new MasterRequestHandlers.OperateServiceStateHandler(mServiceManager, false)
                ).
                addHandler(MasterCallPaths.PATH_QUERY_SERVICE_STATE_DID_ADD,
                        new MasterRequestHandlers.QueryServiceStateHandler(mServiceManager)
                ).
                addHandler(MasterCallPaths.PATH_ACTIVATE_COMPETING_SESSION,
                        new MasterRequestHandlers.ActivateCompetingSessionHandler(mSessionManager)
                ).
                addHandler(MasterCallPaths.PATH_DEACTIVATE_COMPETING_SESSION,
                        new MasterRequestHandlers.DeactivateCompetingSessionHandler(mSessionManager)
                );
    }
}