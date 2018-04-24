package com.ubtrobot.master.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.competition.CompetingItem;
import com.ubtrobot.master.competition.CompetingItemDetail;
import com.ubtrobot.master.competition.CompetingItemDetailList;
import com.ubtrobot.master.competition.CompetitionSessionInfo;
import com.ubtrobot.master.component.ComponentInfoSource;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IntentExtras;
import com.ubtrobot.master.transport.message.MasterCallPaths;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 24/12/2017.
 */

public class ServiceLifecycle {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("ServiceLifecycle");

    private final Context mContext;
    private final ComponentInfoSource mSource;
    private final CallMasterCallable mCallable;
    private final EventLoop mCallProcessLoop;

    private final HashMap<String, ServiceLifecycle.LivingService> mLivingServices = new HashMap<>();
    private final ReentrantReadWriteLock mLivingServicesLock = new ReentrantReadWriteLock();

    public ServiceLifecycle(
            Context context,
            ComponentInfoSource source,
            CallMasterCallable callable,
            EventLoop callProcessLoop) {
        mContext = context;
        mSource = source;
        mCallable = callable;
        mCallProcessLoop = callProcessLoop;
    }

    String onServiceCreate(Class<? extends MasterService> clazz, MasterService service) {
        mLivingServicesLock.writeLock().lock();
        try {
            ServiceInfo serviceInfo = mSource.getServiceInfo(clazz);
            if (serviceInfo == null) {
                throw new IllegalStateException("Service NOT configured in the xml file.");
            }

            if (mLivingServices.put(clazz.getName(),
                    new ServiceLifecycle.LivingService(clazz, serviceInfo.getName(), service)) != null) {
                throw new IllegalStateException("Skill lifecycle error. onCreate has been invoked. " +
                        "skillClass=" + clazz.getName());
            }

            return serviceInfo.getName();
        } finally {
            mLivingServicesLock.writeLock().unlock();
        }
    }

    void addState(Class<? extends MasterService> clazz, final String state) {
        mLivingServicesLock.writeLock().lock();
        try {
            final LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                throw new IllegalStateException("MasterService did NOT created. serviceClass=" +
                        clazz.getName());
            }

            if (!livingService.onAddState(state)) {
                LOGGER.w("State already added. state=" + state);
                return;
            }

            Bundle paramBundle = new Bundle();
            paramBundle.putString(ParamBundleConstants.KEY_SERVICE_NAME, livingService.serviceName);
            paramBundle.putString(ParamBundleConstants.KEY_STATE, state);

            mCallable.call(
                    MasterCallPaths.PATH_ADD_SERVICE_STATE,
                    ParcelableParam.create(paramBundle),
                    new ResponseCallback() {
                        @Override
                        public void onResponse(Request req, Response res) {
                            // Ignore
                        }

                        @Override
                        public void onFailure(Request req, CallException e) {
                            // 如果是 Master Crash 的情况，在 Master 重启后会自动同步状态
                            LOGGER.e(e, "Add state for service failed. serviceClass=" +
                                    livingService.getClass().getName() + ", state=" + state);
                        }
                    }
            );
        } finally {
            mLivingServicesLock.writeLock().unlock();
        }
    }

    void removeState(Class<? extends MasterService> clazz, final String state) {
        mLivingServicesLock.writeLock().lock();
        try {
            final LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                throw new IllegalStateException(
                        "MasterService did NOT created. serviceClass=" + clazz.getName());
            }

            if (!livingService.onRemoveState(state)) {
                LOGGER.w("State NOT added or already removed. state=" + state);
                return;
            }

            Bundle paramBundle = new Bundle();
            paramBundle.putString(ParamBundleConstants.KEY_SERVICE_NAME, livingService.serviceName);
            paramBundle.putString(ParamBundleConstants.KEY_STATE, state);

            mCallable.call(
                    MasterCallPaths.PATH_REMOVE_SERVICE_STATE,
                    ParcelableParam.create(paramBundle),
                    new ResponseCallback() {
                        @Override
                        public void onResponse(Request req, Response res) {
                            // Ignore
                        }

                        @Override
                        public void onFailure(Request req, CallException e) {
                            // 如果是 Master Crash 的情况，在 Master 重启后会自动同步状态
                            LOGGER.e(e, "Remove state for service failed. serviceClass=" +
                                    livingService.getClass().getName() + ", state=" + state);
                        }
                    }
            );
        } finally {
            mLivingServicesLock.writeLock().unlock();
        }
    }

    List<String> getStates(Class<? extends MasterService> clazz) {
        mLivingServicesLock.readLock().lock();
        try {
            LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                throw new IllegalStateException(
                        "MasterService did NOT created. serviceClass=" + clazz.getName());
            }

            LinkedList<String> states = new LinkedList<>();
            Long previous = null;
            for (Map.Entry<String, Long> entry : livingService.states.entrySet()) {
                if (previous == null) {
                    previous = entry.getValue();
                    states.add(entry.getKey());
                    continue;
                }

                if (previous < entry.getValue()) {
                    states.add(entry.getKey());
                } else {
                    states.add(0, entry.getKey());
                }

                previous = entry.getValue();
            }

            return states;
        } finally {
            mLivingServicesLock.readLock().unlock();
        }
    }

    boolean didAddState(Class<? extends MasterService> clazz, String state) {
        mLivingServicesLock.readLock().lock();
        try {
            LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                throw new IllegalStateException(
                        "MasterService did NOT created. serviceClass=" + clazz.getName());
            }

            return livingService.states.containsKey(state);
        } finally {
            mLivingServicesLock.readLock().unlock();
        }
    }

    public MasterService getLivingService(Class<? extends MasterService> clazz) {
        mLivingServicesLock.readLock().lock();
        try {
            LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                return null;
            }

            return livingService.getService();
        } finally {
            mLivingServicesLock.readLock().unlock();
        }
    }

    public void getCompetingItems(
            final EventLoop eventLoop,
            ParcelRequest request,
            CompetingSessionCallback<CompetingItemDetailList> callback) {
        doCompetingOperation(
                eventLoop,
                request,
                callback,
                new CompetingSessionOperation<Void, CompetingItemDetailList>() {
                    @Override
                    public Pair<String, Void>
                    resolveRequestParam(Request request) throws CallException {
                        try {
                            String service = ParcelableParam.from(request.getParam(), Bundle.class).
                                    getParcelable().getString(ParamBundleConstants.KEY_SERVICE_NAME);
                            if (TextUtils.isEmpty(service)) {
                                throw new CallException(CallGlobalCode.BAD_REQUEST,
                                        "Illegal argument. service name is null.");
                            }

                            return new Pair<>(service, null);
                        } catch (ParcelableParam.InvalidParcelableParamException e) {
                            throw new CallException(CallGlobalCode.BAD_REQUEST, "Illegal argument.");
                        }
                    }

                    @Override
                    public CompetingItemDetailList
                    onOperate(LivingService livingService, Void input) throws CallException {
                        List<CompetingItemDetail> items = livingService.getService().getCompetingItems();
                        if (items == null) {
                            return new CompetingItemDetailList();
                        }

                        HashSet<String> existed = new HashSet<>();
                        boolean itemsLegal = true;
                        String serviceName = livingService.getService().getName();

                        for (CompetingItemDetail item : items) {
                            if (!existed.add(item.getItemId())) {
                                itemsLegal = false;
                                LOGGER.e("Duplicate competing item. itemId=%s", item.getItemId());
                                continue;
                            }

                            if (!serviceName.equals(item.getService())) {
                                LOGGER.e("Illegal competing item. Unexpected service name. " +
                                        "service=%s, should be %s", item.getService(), serviceName);
                                itemsLegal = false;
                                continue;
                            }

                            if (item.getCallPathList() == null || item.getCallPathList().isEmpty()) {
                                LOGGER.e("Illegal competing item. Empty call path list. item=%s" +
                                        item);
                                itemsLegal = false;
                            }
                        }

                        if (itemsLegal) {
                            return new CompetingItemDetailList(items);
                        }

                        throw new CallException(CallGlobalCode.INTERNAL_ERROR,
                                "Service internal error. return illegal competing items.");
                    }
                },
                false
        );
    }

    private <I, O> void doCompetingOperation(
            final EventLoop eventLoop,
            ParcelRequest request,
            final CompetingSessionCallback<O> callback,
            final CompetingSessionOperation<I, O> operation,
            boolean ignoreIfNotCreate) {
        String service;
        final I input;
        try {
            Pair<String, I> requestParam = operation.resolveRequestParam(request);
            service = requestParam.first;
            input = requestParam.second;
        } catch (CallException e) {
            callbackFailure(eventLoop, callback, e);
            return;
        }

        ServiceInfo serviceInfo = mSource.getServiceInfo(service);
        if (serviceInfo == null) {
            callbackFailure(eventLoop, callback, new CallException(CallGlobalCode.BAD_REQUEST,
                    service + " service NOT found."));
            return;
        }

        final LivingService livingService;
        mLivingServicesLock.readLock().lock();
        try {
            livingService = mLivingServices.get(serviceInfo.getClassName());
        } finally {
            mLivingServicesLock.readLock().unlock();
        }

        // 对应的 Android Service 已经启动
        if (livingService != null) {
            mCallProcessLoop.post(new Runnable() {
                @Override
                public void run() {
                    mLivingServicesLock.writeLock().lock();
                    try {
                        O data = operation.onOperate(livingService, input);
                        callbackSuccess(eventLoop, callback, data);
                    } catch (CallException e) {
                        callbackFailure(eventLoop, callback, e);
                    } finally {
                        mLivingServicesLock.writeLock().unlock();
                    }
                }
            });

            return;
        }

        if (ignoreIfNotCreate) {
            callbackSuccess(eventLoop, callback, null);
            return;
        }

        // 对应的 Android Service 没有启动，把 Request 投递过去，doCompetingOperation 将会被重新调用
        if (mContext.startService(new Intent().
                setComponent(new ComponentName(
                        serviceInfo.getPackageName(), serviceInfo.getClassName()
                )).
                putExtra(IntentExtras.KEY_REQUEST, request)) != null) {
            callbackRetrySoon(eventLoop, callback);
            return;
        }

        callbackFailure(eventLoop, callback, new CallException(
                CallGlobalCode.INTERNAL_ERROR,
                "Can NOT start android service. serviceClass=" + serviceInfo.getClassName()));
    }

    private <T> void callbackSuccess(
            EventLoop eventLoop,
            final CompetingSessionCallback<T> callback,
            final T data) {
        eventLoop.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(data);
            }
        });
    }

    private void callbackRetrySoon(EventLoop eventLoop, final CompetingSessionCallback callback) {
        eventLoop.post(new Runnable() {
            @Override
            public void run() {
                callback.onRetrySoon();
            }
        });
    }

    private void callbackFailure(
            EventLoop eventLoop, final CompetingSessionCallback callback, final CallException e) {
        eventLoop.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
            }
        });
    }

    public void notifyCompetingSessionActivate(
            final EventLoop eventLoop,
            ParcelRequest request,
            CompetingSessionCallback<Void> callback) {
        doCompetingOperation(
                eventLoop,
                request,
                callback,
                new CompetingSessionOperation<CompetitionSessionInfo, Void>() {
                    @Override
                    public Pair<String, CompetitionSessionInfo>
                    resolveRequestParam(Request request) throws CallException {
                        try {
                            CompetitionSessionInfo sessionInfo = ParcelableParam.from(
                                    request.getParam(), CompetitionSessionInfo.class).getParcelable();
                            Map<String, Set<CompetingItem>> items = sessionInfo.getCompetingItemMap();
                            if (items.size() != 1) {
                                throw new CallException(CallGlobalCode.BAD_REQUEST,
                                        "Illegal argument.");
                            }

                            Pair<String, CompetitionSessionInfo> ret = null;
                            for (Map.Entry<String, Set<CompetingItem>> entry :
                                    sessionInfo.getCompetingItemMap().entrySet()) {
                                ret = new Pair<>(entry.getKey(), sessionInfo);
                            }

                            return ret;
                        } catch (ParcelableParam.InvalidParcelableParamException e) {
                            throw new CallException(CallGlobalCode.BAD_REQUEST, "Illegal argument.");
                        }
                    }

                    @Override
                    public Void onOperate(
                            LivingService livingService,
                            CompetitionSessionInfo sessionInfo) {
                        livingService.onCompetitionSessionActivate(sessionInfo);
                        return null;
                    }
                },
                false
        );
    }

    public CompetitionSessionInfo getActiveCompetitionSession(
            Class<? extends MasterService> clazz,
            String sessionId) {
        mLivingServicesLock.readLock().lock();
        try {
            LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                return null;
            }

            return livingService.getActiveCompetitionSessionInfo(sessionId);
        } finally {
            mLivingServicesLock.readLock().unlock();
        }
    }

    public CompetitionSessionInfo getActiveCompetitionSessionByItemId(
            Class<? extends MasterService> clazz,
            String itemId) {
        mLivingServicesLock.writeLock().lock();
        try {
            LivingService livingService = mLivingServices.get(clazz.getName());
            if (livingService == null) {
                return null;
            }

            return livingService.getActiveCompetitionSessionInfoByItemId(itemId);
        } finally {
            mLivingServicesLock.writeLock().unlock();
        }
    }

    public void notifyCompetingSessionDeactivate(
            EventLoop eventLoop,
            ParcelRequest request,
            CompetingSessionCallback<Void> callback) {
        doCompetingOperation(
                eventLoop,
                request,
                callback,
                new CompetingSessionOperation<String, Void>() {
                    @Override
                    public Pair<String, String>
                    resolveRequestParam(Request request) throws CallException {
                        try {
                            Bundle bundle = ParcelableParam.from(
                                    request.getParam(), Bundle.class).getParcelable();
                            String service = bundle.getString(ParamBundleConstants.KEY_SERVICE_NAME);
                            String sessionId = bundle.getString(ParamBundleConstants.
                                    KEY_COMPETING_SESSION_ID);
                            return new Pair<>(service, sessionId);
                        } catch (ParcelableParam.InvalidParcelableParamException e) {
                            throw new CallException(CallGlobalCode.BAD_REQUEST, "Illegal argument.");
                        }
                    }

                    @Override
                    public Void onOperate(
                            LivingService livingService,
                            String sessionId) {
                        CompetitionSessionInfo sessionInfo = livingService.
                                getActiveCompetitionSessionInfo(sessionId);
                        if (sessionInfo != null) {
                            livingService.onCompetitionSessionDeactivate(sessionInfo);
                        }
                        return null;
                    }
                },
                true
        );
    }

    void onServiceDestroy(Class<? extends MasterService> clazz) {
        mLivingServicesLock.writeLock().lock();
        try {
            if (mLivingServices.remove(clazz.getName()) == null) {
                throw new IllegalStateException("Skill lifecycle error. onCreate was NOT invoked. " +
                        "skillClass=" + clazz.getName());
            }
        } finally {
            mLivingServicesLock.writeLock().unlock();
        }
    }

    static class LivingService {

        Class<? extends MasterService> serviceClass;
        String serviceName;
        MasterService service;
        HashMap<String, Long> states = new HashMap<>();
        HashMap<String, CompetitionSessionInfo> mSessions = new HashMap<>();
        HashMap<String, CompetitionSessionInfo> mSessionsByItemId = new HashMap<>();

        LivingService(
                Class<? extends MasterService> serviceClass,
                String serviceName,
                MasterService service) {
            this.serviceClass = serviceClass;
            this.serviceName = serviceName;
            this.service = service;
        }

        public MasterService getService() {
            return service;
        }

        boolean onAddState(String state) {
            if (states.containsKey(state)) {
                return false;
            }

            states.put(state, System.currentTimeMillis());
            return true;
        }

        boolean onRemoveState(String state) {
            return states.remove(state) != null;
        }

        void onCompetitionSessionActivate(CompetitionSessionInfo sessionInfo) {
            CompetitionSessionInfo previous = mSessions.put(sessionInfo.getSessionId(), sessionInfo);
            if (previous != null) {
                LOGGER.e("Already notify competing session. previous=%s, current=%s",
                        previous, sessionInfo);
            }

            for (CompetingItem item : sessionInfo.getCompetingItems()) {
                if (!serviceName.equals(item.getService())) {
                    LOGGER.e("New active competition session contains other service 's " +
                            "competing item. sessionInfo=%s, service=%s", sessionInfo, serviceName);
                    continue;
                }

                previous = mSessionsByItemId.put(item.getItemId(), sessionInfo);
                if (previous != null) {
                    LOGGER.e("The competing item already in a competition session. previous=%s," +
                            " current=%s", previous, sessionInfo);
                }
            }

            service.onCompetitionSessionActive(sessionInfo);
        }

        CompetitionSessionInfo getActiveCompetitionSessionInfo(String sessionId) {
            return mSessions.get(sessionId);
        }

        CompetitionSessionInfo getActiveCompetitionSessionInfoByItemId(String itemId) {
            return mSessionsByItemId.get(itemId);
        }

        void onCompetitionSessionDeactivate(CompetitionSessionInfo sessionInfo) {
            Iterator<Map.Entry<String, CompetitionSessionInfo>> iterator = mSessionsByItemId.
                    entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CompetitionSessionInfo> entry = iterator.next();
                if (entry.getValue().getSessionId().equals(sessionInfo.getSessionId())) {
                    iterator.remove();
                }
            }

            CompetitionSessionInfo previous = mSessions.remove(sessionInfo.getSessionId());
            if (previous == null) {
                LOGGER.e("Notify competing session error. NOT active or already deactivated. " +
                        "session=%s", sessionInfo);
                return;
            }

            service.onCompetitionSessionInactive(sessionInfo);
        }

        @Override
        public String toString() {
            return "LivingService{" +
                    "serviceClass=" + serviceClass +
                    ", states=" + states +
                    '}';
        }
    }

    private interface CompetingSessionOperation<I, O> {

        Pair<String, I> resolveRequestParam(Request request) throws CallException;

        O onOperate(LivingService livingService, I input) throws CallException;
    }

    public interface CompetingSessionCallback<T> {

        void onSuccess(T data);

        void onRetrySoon();

        void onFailure(CallException e);
    }
}