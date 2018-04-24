package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.call.CallDestinations;
import com.ubtrobot.master.call.ComponentInfoPool;
import com.ubtrobot.master.competition.CompetitionSessionManager;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.event.EventDispatcher;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.service.ServiceManager;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillManager;
import com.ubtrobot.master.transport.connection.ConnectionConstants;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.IncomingHandlerAdapter;
import com.ubtrobot.ulog.Logger;

import java.util.List;

/**
 * Created by column on 10/09/2017.
 */

public class ConnectionHandler extends IncomingHandlerAdapter {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("ConnectionHandler");

    private final EventDispatcher mInternalEventDispatcher;
    private final EventDispatcher mSdkUserEventDispatcher;
    private final ComponentInfoPool mPool;
    private final CallDestinations mCallDestinations;

    private final ServiceManager mServiceManager;
    private final SkillManager mSkillManager;
    private final CompetitionSessionManager mSessionManager;

    public ConnectionHandler(
            EventDispatcher internalEventDispatcher,
            EventDispatcher sdkUserEventDispatcher,
            ComponentInfoPool pool,
            CallDestinations callDestinations,
            SkillManager skillManager,
            ServiceManager serviceManager,
            CompetitionSessionManager sessionManager) {
        mInternalEventDispatcher = internalEventDispatcher;
        mSdkUserEventDispatcher = sdkUserEventDispatcher;
        mPool = pool;
        mCallDestinations = callDestinations;

        mServiceManager = serviceManager;
        mSkillManager = skillManager;
        mSessionManager = sessionManager;
    }

    @Override
    public void onConnected(HandlerContext context) {
        String packageName = (String) context.connection().attributes().
                get(ConnectionConstants.ATTR_KEY_PACKAGE);
        if (packageName == null) {
            throw new AssertionError("packageName != null");
        }

        List<? extends ComponentInfo> componentInfos = mPool.getComponentInfos(packageName);
        for (ComponentInfo componentInfo : componentInfos) {
            if (componentInfo instanceof SkillInfo) {
                mCallDestinations.addSkillCallDestinations(
                        (SkillInfo) componentInfo, context.connection());
                continue;
            }

            if (componentInfo instanceof ServiceInfo) {
                mCallDestinations.addServiceCallDestinations(
                        (ServiceInfo) componentInfo, context.connection());
                continue;
            }

            throw new AssertionError(
                    "componentInfo instanceof SkillInfo || componentInfo instanceof ServiceInfo");
        }

        context.onConnected();
    }

    @Override
    public void onDisconnected(HandlerContext context) {
        LOGGER.i("Connection disconnected. Unsubscribe all the event subscribed by the connection." +
                " connectionId=%s", context.connection().id().asText());
        mInternalEventDispatcher.unsubscribe(context.connection());
        mSdkUserEventDispatcher.unsubscribe(context.connection());

        LOGGER.i("Connection disconnected. Remove all calls registered by the connection." +
                " connectionId=%s", context.connection().id().asText());
        mCallDestinations.removeCallDestinations(context.connection());

        mSkillManager.stopConnectionSkills(context.connection());
        mServiceManager.removeConnectionServicesStates(context.connection());
        mSessionManager.deactivateSessions(context, context.connection());

        context.onDisconnected();
    }
}