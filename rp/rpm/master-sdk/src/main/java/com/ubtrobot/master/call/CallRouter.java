package com.ubtrobot.master.call;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.component.ComponentInfoSource;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.IntentExtras;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-12-1.
 */

public class CallRouter {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("CallRouter");

    private final Context mContext;
    private final ComponentInfoSource mSource;

    private final HashMap<String, Route> mRoutes = new HashMap<>();
    private final ReentrantReadWriteLock mRoutesLock = new ReentrantReadWriteLock();

    public CallRouter(Context context, ComponentInfoSource source) {
        mContext = context;
        mSource = source;
    }

    public void addRoute(Class<? extends AbstractCallComponent> clazz, Route route) {
        mRoutesLock.writeLock().lock();
        try {
            if (mRoutes.put(clazz.getName(), route) != null) {
                LOGGER.w("Repeatedly invoke CallRouter.addRoute by " + clazz.getName());
            }
        } finally {
            mRoutesLock.writeLock().unlock();
        }

    }

    public void removeRoute(Route route) {
        mRoutesLock.writeLock().lock();
        try {
            Iterator<Map.Entry<String, Route>> iterator = mRoutes.entrySet().iterator();
            while (iterator.hasNext()) {
                Route aRoute = iterator.next().getValue();
                if (aRoute == route) {
                    iterator.remove();
                }
            }
        } finally {
            mRoutesLock.writeLock().unlock();
        }
    }

    private Route getRouteLocked(String className) {
        mRoutesLock.readLock().lock();
        try {
            return mRoutes.get(className);
        } finally {
            mRoutesLock.readLock().unlock();
        }
    }

    public void route(ParcelRequest request, OutgoingCallback callback) {
        ComponentInfo componentInfo;

        String responderType = request.getContext().getResponderType();
        String responder = request.getContext().getResponder();

        if (ParcelRequestContext.RESPONDER_TYPE_SKILLS.equals(responderType)) {
            componentInfo = mSource.getSkillInfo(responder);

            if (componentInfo == null) {
                callback.onFailure(new IllegalStateException(
                        ("Skill NOT found. name=" + responder)));
                return;
            }
        } else if (ParcelRequestContext.RESPONDER_TYPE_SERVICE.equals(responderType)) {
            componentInfo = mSource.getServiceInfo(responder);

            if (componentInfo == null) {
                callback.onFailure(new IllegalStateException(
                        ("Service NOT found. name=" + responder)));
                return;
            }
        } else {
            callback.onFailure(new IllegalStateException("Illegal request processorType. " +
                    "processorType=" + responderType));
            return;
        }

        Route route = getRouteLocked(componentInfo.getClassName());
        if (route != null) {
            route.onRoute(request);
            return;
        }

        // 对应的 Android Service 没有启动，把 Request 投递过去，route 将会被重新调用
        if (mContext.startService(new Intent().
                setComponent(new ComponentName(
                        componentInfo.getPackageName(), componentInfo.getClassName()
                )).
                putExtra(IntentExtras.KEY_REQUEST, request)) != null) {
            callback.onSuccess();
            return;
        }

        callback.onFailure(new IllegalStateException("Can NOT start service. class=" +
                componentInfo.getClassName()));
    }

    public interface Route {

        void onRoute(ParcelRequest request);
    }
}
