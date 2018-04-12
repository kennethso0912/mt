package com.ubtrobot.master.competition;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.async.AsyncLock;
import com.ubtrobot.master.async.AsyncTask;
import com.ubtrobot.master.async.AsyncTaskCallback;
import com.ubtrobot.master.async.Callback;
import com.ubtrobot.master.async.ParallelFlow;
import com.ubtrobot.master.async.SeriesFlow;
import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.call.IPCFromMasterCallable;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.event.InternalEventDispatcher;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.transport.connection.ConnectionConstants;
import com.ubtrobot.master.transport.message.FromMasterPaths;
import com.ubtrobot.master.transport.message.MasterEvents;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO 处理多服务的情况
public class CompetitionSessionManager {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("CompetitionSessionManager");

    private final CallRouter mCallRouter;
    private final IPCFromMasterCallable mCallable;
    private final InternalEventDispatcher mEventDispatcher;

    private final Map<String, ServiceCompetingInfo> mCompetingInfos = new HashMap<>();
    private final AsyncLock mCompetingInfosLock = new AsyncLock();

    private final Map<String, SessionEnv> mActiveSessions = new HashMap<>();
    // mSessionStack.get(0) 为栈顶
    private final LinkedList<SessionEnv> mSessionStack = new LinkedList<>();
    private final Map<String, SessionEnv> mSessionMap = new HashMap<>();
    private final AsyncLock mSessionLock = new AsyncLock();

    public CompetitionSessionManager(
            CallRouter callRouter,
            IPCFromMasterCallable callable,
            InternalEventDispatcher dispatcher) {
        mCallRouter = callRouter;
        mCallable = callable;
        mEventDispatcher = dispatcher;
    }

    /**
     * 激活竞争会话
     *
     * @param handlerContext 请求激活的连接处理上下文
     * @param requestContext 请求激活的请求上下文
     * @param sessionInfo    竞争会话信息
     * @param option         激活竞争会话选项
     * @param callback       激活回调。callback.onFailure(SessionManageException e) 具体异常原因详见：
     * @see SessionManageException#CODE_COMPETING_ITEM_NOT_FOUND
     * @see SessionManageException#CODE_FORBIDDEN_TO_INTERRUPT
     * @see SessionManageException#CODE_SERVICE_INTERNAL_ERROR
     */
    public void activateSession(
            final HandlerContext handlerContext,
            ParcelRequestContext requestContext,
            final CompetitionSessionInfo sessionInfo,
            final ActivateOption option,
            final Callback<String, SessionManageException> callback) {
        if (sessionInfo.getCompetingItems().isEmpty()) {
            throw new AssertionError("sessionInfo.getCompetingItems() is NOT empty.");
        }

        LOGGER.i("Start to activate session. sessionInfo=%s, requestContext=%s, connId=%s",
                sessionInfo, requestContext, handlerContext.connection().id().asText());

        final Collection<String> lockKeys = lockKeys(sessionInfo.getCompetingItems());
        new SeriesFlow<String, SessionManageException>(
        ).add(
                newAcquireLockTask(mCompetingInfosLock, lockKeys)
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                final Map<String, Set<CompetingItem>> notResolved = new HashMap<>();
                final LinkedList<CompetingItem> itemNotExisted = new LinkedList<>();

                synchronized (mCompetingInfosLock) {
                    for (Map.Entry<String, Set<CompetingItem>> entry :
                            sessionInfo.getCompetingItemMap().entrySet()) {
                        ServiceCompetingInfo competingInfo = mCompetingInfos.get(entry.getKey());
                        if (competingInfo == null) {
                            notResolved.put(entry.getKey(), entry.getValue());
                            continue;
                        }

                        for (CompetingItem item : entry.getValue()) {
                            if (!competingInfo.itemSet.contains(item)) {
                                itemNotExisted.add(item);
                            }
                        }
                    }
                }

                final String lockToken = (String) arguments[0];
                if (!itemNotExisted.isEmpty()) {
                    mCompetingInfosLock.release(lockToken);
                    callback.onFailure(new SessionManageException(
                            SessionManageException.CODE_COMPETING_ITEM_NOT_FOUND,
                            "Competing items NOT found. items=" + itemNotExisted.toString()
                    ));

                    LOGGER.e("Activate session failed due to competing item NOT found. " +
                            "itemNotExisted=%s", itemNotExisted);
                    return;
                }

                if (notResolved.isEmpty()) {
                    mCompetingInfosLock.release(lockToken);
                    callback.onSuccess();
                    return;
                }

                Callback<Map<String, ServiceCompetingInfo>, SessionManageException> mapCallback
                        = new Callback<Map<String, ServiceCompetingInfo>, SessionManageException>() {
                    @Override
                    public void onSuccess(Map<String, ServiceCompetingInfo> competingInfos) {
                        for (Map.Entry<String, Set<CompetingItem>> entry : notResolved.entrySet()) {
                            ServiceCompetingInfo competingInfo = competingInfos.get(entry.getKey());
                            if (competingInfo == null) {
                                throw new AssertionError("Should NOT be null.");
                            }

                            for (CompetingItem item : entry.getValue()) {
                                if (!competingInfo.itemSet.contains(item)) {
                                    itemNotExisted.add(item);
                                }
                            }
                        }

                        mCompetingInfosLock.release(lockToken);

                        if (itemNotExisted.isEmpty()) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure(new SessionManageException(
                                    SessionManageException.CODE_COMPETING_ITEM_NOT_FOUND,
                                    "Competing items NOT found. items=" + itemNotExisted.toString()
                            ));

                            LOGGER.e("Activate session failed due to competing item NOT found. " +
                                    "itemNotExisted=%s", itemNotExisted);
                        }
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        mCompetingInfosLock.release(lockToken);
                        callback.onFailure(e);
                    }
                };

                getServicesCompetingItemsLocked(handlerContext.eventLoop(), notResolved.keySet(),
                        mapCallback);
            }
        }).add(
                newAcquireLockTask(mSessionLock, lockKeys)
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                final String lockToken = (String) arguments[0];
                HashMap<String, SessionEnv> interruptMap = new HashMap<>();

                synchronized (mSessionLock) {
                    for (SessionEnv sessionEnv : mActiveSessions.values()) {
                        if (!hasCompeting(sessionEnv.sessionInfo, sessionInfo)) {
                            continue;
                        }

                        if (resolveInterrupt(option, sessionEnv.option)) {
                            interruptMap.put(sessionEnv.sessionId, sessionEnv);
                            continue;
                        }

                        mSessionLock.release(lockToken);
                        callback.onFailure(new SessionManageException(
                                SessionManageException.CODE_FORBIDDEN_TO_INTERRUPT,
                                "Forbidden to interrupt current sessions."
                        ));

                        LOGGER.e("Forbidden to interrupt some active session. activeSession=%s",
                                sessionEnv);
                        return;
                    }
                }

                if (interruptMap.isEmpty()) {
                    callback.onSuccess(lockToken);
                    return;
                }

                interruptSessionsLocked(handlerContext.eventLoop(), interruptMap, new Callback<
                        Void, SessionManageException>() {
                    @Override
                    public void onSuccess(Void value) {
                        callback.onSuccess(lockToken);
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        throw new AssertionError("Impossible here.");
                    }
                });
            }
        }).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                final String lockToken = (String) arguments[0];
                Callback<SessionEnv, SessionManageException>
                        activateCallback = new Callback<SessionEnv, SessionManageException>() {
                    @Override
                    public void onSuccess(SessionEnv sessionEnv) {
                        resumeInterruptionLocked();
                        mSessionLock.release(lockToken);

                        callback.onSuccess(sessionEnv.sessionInfo.getSessionId());
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        resumeInterruptionLocked();
                        mSessionLock.release(lockToken);

                        callback.onFailure(e);
                    }
                };

                doActivateSessionLocked(handlerContext, sessionInfo, option, activateCallback);
            }
        }).onSuccess(new SeriesFlow.SuccessCallback<String>() {
            @Override
            public void onSuccess(String sessionId) {
                callback.onSuccess(sessionId);
            }
        }).onFailure(new SeriesFlow.FailureCallback<SessionManageException>() {
            @Override
            public void onFailure(SessionManageException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    private Collection<String> lockKeys(List<CompetingItem> items) {
        LinkedList<String> lockKeys = new LinkedList<>();
        for (CompetingItem item : items) {
            lockKeys.add(item.getService() + "|" + item.getItemId());
        }

        return lockKeys;
    }

    private AsyncTask<SessionManageException> newAcquireLockTask(
            final AsyncLock lock, final Collection<String> lockKeys) {
        return new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback, Object... arguments) {
                lock.acquire(new AsyncLock.AcquireCallback() {
                    @Override
                    public void onAcquired(String token) {
                        callback.onSuccess(token);
                    }
                }, lockKeys);
            }
        };
    }

    private void getServicesCompetingItemsLocked(
            final EventLoop requestEventLoop,
            Collection<String> services,
            final Callback<Map<String, ServiceCompetingInfo>, SessionManageException> callback) {
        ParallelFlow<SessionManageException> flow = new ParallelFlow<>();
        for (final String service : services) {
            flow.add(new AsyncTask<SessionManageException>() {
                @Override
                public void execute(
                        final AsyncTaskCallback<SessionManageException> callback,
                        Object... arguments) {
                    getServiceCompetingItemsLocked(requestEventLoop, service, new Callback<
                            ServiceCompetingInfo, SessionManageException>() {
                        @Override
                        public void onSuccess(ServiceCompetingInfo competingInfo) {
                            callback.onSuccess(competingInfo);
                        }

                        @Override
                        public void onFailure(SessionManageException e) {
                            callback.onFailure(e);

                            LOGGER.e(e, "Get service 's competing items failed. service=%s", service);
                        }
                    });
                }
            });
        }

        flow.onComplete(new ParallelFlow.CompleteCallback<SessionManageException>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onComplete(ParallelFlow.Results<SessionManageException> results) {
                if (!results.isAllSuccess()) {
                    callback.onFailure(results.get(results.failure().get(0)).getException());
                    return;
                }

                HashMap<String, ServiceCompetingInfo> competingInfos = new HashMap<>();
                for (ParallelFlow.Result<SessionManageException> result : results) {
                    ServiceCompetingInfo competingInfo = (ServiceCompetingInfo) result.getValues().
                            get(0);
                    competingInfos.put(competingInfo.serviceInfo.getName(), competingInfo);
                }
                callback.onSuccess(competingInfos);
            }
        }).start();
    }

    private void getServiceCompetingItemsLocked(
            final EventLoop requestEventLoop,
            final String service,
            final Callback<ServiceCompetingInfo, SessionManageException> callback) {
        new SeriesFlow<ServiceCompetingInfo, SessionManageException>(
        ).add(
                newRouteTask(requestEventLoop, service)
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                ServiceInfo serviceInfo = (ServiceInfo) arguments[0];
                Connection connection = (Connection) arguments[1];

                getServiceCompetingItemsLocked(serviceInfo, connection, new Callback<
                        ServiceCompetingInfo, SessionManageException>() {
                    @Override
                    public void onSuccess(ServiceCompetingInfo competingInfo) {
                        callback.onSuccess(competingInfo);
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        callback.onFailure(e);
                    }
                });
            }
        }).onSuccess(new SeriesFlow.SuccessCallback<ServiceCompetingInfo>() {
            @Override
            public void onSuccess(ServiceCompetingInfo competingInfo) {
                LOGGER.i("Get service competing info success. service=%s, competingInfo=%s",
                        service, competingInfo);
                callback.onSuccess(competingInfo);
            }
        }).onFailure(new SeriesFlow.FailureCallback<SessionManageException>() {
            @Override
            public void onFailure(SessionManageException e) {
                LOGGER.i(e, "Get service competing info failure. service=%s", service);
                callback.onFailure(e);
            }
        }).start();
    }

    @NonNull
    private AsyncTask<SessionManageException> newRouteTask(
            final EventLoop requestEventLoop,
            final String service) {
        return new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                final CallRouter.RouteCallback<String> routeCallback
                        = new CallRouter.RouteCallback<String>() {
                    @Override
                    public void onRoute(
                            String service,
                            ComponentInfo destinationComponentInfo,
                            @Nullable Connection destinationConnection) {
                        callback.onSuccess(destinationComponentInfo,
                                destinationConnection);
                    }

                    @Override
                    public void onNotFound(String service) {
                        callback.onFailure(new SessionManageException(
                                SessionManageException.CODE_COMPETING_ITEM_NOT_FOUND,
                                "Competing item NOT found due to service NOT found. service=" +
                                        service
                        ));
                    }

                    @Override
                    public void onConflict(String service) {
                        callback.onFailure(new SessionManageException(
                                SessionManageException.CODE_SERVICE_INTERNAL_ERROR,
                                "Service internal error due to service conflict. service=" +
                                        service
                        ));
                    }
                };

                mCallRouter.routeService(requestEventLoop, service, routeCallback);
            }
        };
    }

    private void getServiceCompetingItemsLocked(
            final ServiceInfo serviceInfo,
            @Nullable Connection destinationConnection,
            final Callback<ServiceCompetingInfo, SessionManageException> callback) {
        ResponseCallback responseCallback = new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                List<CompetingItemDetail> detailList;
                try {
                    detailList = ParcelableParam.from(res.getParam(),
                            CompetingItemDetailList.class).getParcelable().getCompetingItemList();
                } catch (ParcelableParam.InvalidParcelableParamException e) {
                    callback.onFailure(new SessionManageException(
                            SessionManageException.CODE_SERVICE_INTERNAL_ERROR,
                            "Service internal error.",
                            e
                    ));
                    return;
                }

                synchronized (mCompetingInfosLock) {
                    ServiceCompetingInfo competingInfo = new ServiceCompetingInfo(serviceInfo);
                    mCompetingInfos.put(serviceInfo.getName(), competingInfo);

                    for (CompetingItemDetail detail : detailList) {
                        competingInfo.itemSet.add(new CompetingItem(serviceInfo.getName(),
                                detail.getItemId()));
                        competingInfo.details.put(detail.getItemId(), detail);
                        competingInfo.itemPathSet.addAll(detail.getCallPathList());
                    }

                    callback.onSuccess(competingInfo);
                }
            }

            @Override
            public void onFailure(Request req, CallException e) {
                callback.onFailure(new SessionManageException(
                        SessionManageException.CODE_SERVICE_INTERNAL_ERROR,
                        "Service internal error.",
                        e
                ));
            }
        };

        Bundle bundle = new Bundle();
        bundle.putString(ParamBundleConstants.KEY_SERVICE_NAME, serviceInfo.getName());
        mCallable.call(destinationConnection, serviceInfo, FromMasterPaths.PATH_GET_COMPETING_ITEMS,
                ParcelableParam.create(bundle), responseCallback);
    }

    private boolean hasCompeting(CompetitionSessionInfo lhs, CompetitionSessionInfo rhs) {
        for (Map.Entry<String, Set<CompetingItem>> entry : lhs.getCompetingItemMap().entrySet()) {
            Set<CompetingItem> itemSet = rhs.getServiceCompetingItems(entry.getKey());
            if (itemSet == null) {
                continue;
            }

            for (CompetingItem item : entry.getValue()) {
                if (itemSet.contains(item)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean resolveInterrupt(
            ActivateOption interrupting,
            ActivateOption interrupted) {
        return interrupting.getPriority() >= interrupted.getPriority();
    }

    private String generateSessionId() {
        return System.nanoTime() + "";
    }

    private void interruptSessionsLocked(
            final EventLoop requestEventLoop,
            final Map<String, SessionEnv> sessionEnvMap,
            final Callback<Void, SessionManageException> callback) {
        ParallelFlow<SessionManageException> flow = new ParallelFlow<>();
        for (final SessionEnv sessionEnv : sessionEnvMap.values()) {
            flow.add(new AsyncTask<SessionManageException>() {
                @Override
                public void execute(
                        final AsyncTaskCallback<SessionManageException> callback,
                        Object... arguments) {
                    doDeactivateSessionLocked(requestEventLoop, sessionEnv, new Callback<
                            Void, SessionManageException>() {
                        @Override
                        public void onSuccess(Void value) {
                            callback.onSuccess();
                        }

                        @Override
                        public void onFailure(SessionManageException e) {
                            throw new AssertionError("Impossible here.");
                        }
                    });
                }
            });
        }

        flow.onComplete(new ParallelFlow.CompleteCallback<SessionManageException>() {
            @Override
            public void onComplete(ParallelFlow.Results<SessionManageException> results) {
                if (!results.isAllSuccess()) {
                    throw new AssertionError("Impossible here.");
                }

                synchronized (this) {
                    Iterator<SessionEnv> iterator = mSessionStack.iterator();
                    while (iterator.hasNext()) {
                        SessionEnv sessionEnv = iterator.next();
                        if (!sessionEnvMap.containsKey(sessionEnv.sessionId)) {
                            continue;
                        }

                        mEventDispatcher.publish(
                                MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_BEGAN +
                                        sessionEnv.sessionId
                        );

                        if (sessionEnv.option.isShouldResume()) {
                            sessionEnv.interrupted = true;
                        } else {
                            iterator.remove();
                            mSessionMap.remove(sessionEnv.sessionId);

                            Bundle bundle = new Bundle();
                            bundle.putBoolean(ParamBundleConstants.KEY_COMPETING_SESSION_RESUMED, false);
                            mEventDispatcher.publish(
                                    MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_ENDED +
                                            sessionEnv.sessionId,
                                    ParcelableParam.create(bundle)
                            );
                        }

                        mActiveSessions.remove(sessionEnv.sessionId);
                    }
                }

                callback.onSuccess(null);
            }
        }).start();
    }

    private void doDeactivateSessionLocked(
            final EventLoop requestEventLoop,
            final SessionEnv sessionEnv,
            final Callback<Void, SessionManageException> callback) {
        ParallelFlow<SessionManageException> flow = new ParallelFlow<>();
        for (final String service : sessionEnv.sessionInfo.getCompetingItemMap().keySet()) {
            flow.add(new AsyncTask<SessionManageException>() {
                @Override
                public void execute(
                        final AsyncTaskCallback<SessionManageException> callback,
                        Object... arguments) {
                    Callback<Void, SessionManageException> deactivateCallback
                            = new Callback<Void, SessionManageException>() {
                        @Override
                        public void onSuccess(Void value) {
                            callback.onSuccess();
                        }

                        @Override
                        public void onFailure(SessionManageException e) {
                            throw new AssertionError("Impossible here.");
                        }
                    };

                    doDeactivateSessionLocked(
                            requestEventLoop,
                            service,
                            sessionEnv.sessionInfo,
                            deactivateCallback
                    );
                }
            });
        }

        flow.onComplete(new ParallelFlow.CompleteCallback<SessionManageException>() {
            @Override
            public void onComplete(ParallelFlow.Results<SessionManageException> results) {
                if (!results.isAllSuccess()) {
                    throw new AssertionError("Impossible here.");
                }

                LOGGER.i("Deactivate session success. sessionEnv=%s", sessionEnv);
                callback.onSuccess(null);
            }
        }).start();
    }

    private void doDeactivateSessionLocked(
            EventLoop requestEventLoop,
            final String service,
            final CompetitionSessionInfo sessionInfo,
            final Callback<Void, SessionManageException> callback) {
        new SeriesFlow<Void, SessionManageException>().add(
                newRouteTask(requestEventLoop, service)
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback, Object... arguments) {
                ServiceInfo serviceInfo = (ServiceInfo) arguments[0];
                Connection connection = (Connection) arguments[1];

                doDeactivateSessionLocked(serviceInfo, connection, sessionInfo, new Callback<
                        Void, SessionManageException>() {
                    @Override
                    public void onSuccess(Void value) {
                        callback.onSuccess(value);
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        throw new AssertionError("Impossible here.");
                    }
                });
            }
        }).onSuccess(new SeriesFlow.SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                LOGGER.i("Deactivate service 's session success. service=%s," +
                        "sessionInfo=%s", service, sessionInfo);
                callback.onSuccess(value);
            }
        }).onFailure(new SeriesFlow.FailureCallback<SessionManageException>() {
            @Override
            public void onFailure(SessionManageException e) {
                LOGGER.e(e, "Notify service competition session inactive failure.");
                callback.onSuccess(null);
            }
        }).start();
    }

    private void doDeactivateSessionLocked(
            ServiceInfo serviceInfo,
            Connection connection,
            final CompetitionSessionInfo sessionInfo,
            final Callback<Void, SessionManageException> callback) {
        ResponseCallback responseCallback = new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Request req, CallException e) {
                LOGGER.e(e, "Notify service competition session inactive failure.");
                callback.onSuccess(null);
            }
        };

        Bundle bundle = new Bundle();
        bundle.putString(ParamBundleConstants.KEY_SERVICE_NAME, serviceInfo.getName());
        bundle.putString(ParamBundleConstants.KEY_COMPETING_SESSION_ID, sessionInfo.getSessionId());
        mCallable.call(
                connection,
                serviceInfo,
                FromMasterPaths.PATH_DEACTIVATE_COMPETING_SESSION,
                ParcelableParam.create(bundle),
                responseCallback
        );
    }

    private void doActivateSessionLocked(
            final HandlerContext handlerContext,
            final CompetitionSessionInfo sessionInfo,
            final ActivateOption option,
            final Callback<SessionEnv, SessionManageException> callback) {
        final String sessionId = generateSessionId();
        ParallelFlow<SessionManageException> flow = new ParallelFlow<>();
        for (final Map.Entry<String, Set<CompetingItem>> entry :
                sessionInfo.getCompetingItemMap().entrySet()) {
            flow.add(new AsyncTask<SessionManageException>() {
                @Override
                public void execute(
                        final AsyncTaskCallback<SessionManageException> callback,
                        Object... arguments) {
                    Callback<ServiceInfo, SessionManageException>
                            activateCallback = new Callback<ServiceInfo, SessionManageException>() {
                        @Override
                        public void onSuccess(ServiceInfo serviceInfo) {
                            callback.onSuccess(serviceInfo);
                        }

                        @Override
                        public void onFailure(SessionManageException e) {
                            callback.onFailure(e);
                        }
                    };

                    doActivateSessionLocked(
                            handlerContext.eventLoop(),
                            entry.getKey(),
                            new CompetitionSessionInfo.Builder().
                                    setSessionId(sessionId).
                                    addCompetingItemAll(entry.getValue()).
                                    build(),
                            activateCallback
                    );
                }
            });
        }

        flow.onComplete(new ParallelFlow.CompleteCallback<SessionManageException>() {
            @Override
            public void onComplete(ParallelFlow.Results<SessionManageException> results) {
                if (!results.isAllSuccess()) {
                    LOGGER.e("Activate session failure. sessionInfo=%s, activateOption=%s",
                            sessionInfo, option);
                    callback.onFailure(results.get(results.failure().get(0)).getException());
                    return;
                }

                SessionEnv sessionEnv = new SessionEnv(
                        new CompetitionSessionInfo.Builder(sessionInfo).
                                setSessionId(sessionId).
                                build(),
                        option,
                        null,
                        null,
                        handlerContext.connection().id().asText()
                );

                synchronized (mCompetingInfosLock) { // TODO 可能需要扩大异步锁的范围
                    for (ParallelFlow.Result<SessionManageException> result : results) {
                        ServiceInfo serviceInfo = (ServiceInfo) result.getValues().get(0);
                        sessionEnv.serviceInfos.add(serviceInfo);

                        ServiceCompetingInfo competingInfo = mCompetingInfos.get(
                                serviceInfo.getName());
                        if (competingInfo == null) {
                            throw new AssertionError("Service competing info should already " +
                                    "be cached.");
                        }

                        HashSet<String> pathSet = new HashSet<>();
                        sessionEnv.serviceItemPaths.put(serviceInfo.getName(), pathSet);

                        for (CompetingItemDetail detail : competingInfo.details.values()) {
                            pathSet.addAll(detail.getCallPathList());
                        }
                    }
                }

                synchronized (mSessionLock) {
                    mActiveSessions.put(sessionId, sessionEnv);
                    mSessionMap.put(sessionId, sessionEnv);
                    mSessionStack.add(0, sessionEnv);
                }

                LOGGER.i("Activate session success. sessionEnv=%s", sessionEnv);
                callback.onSuccess(sessionEnv);
            }
        }).start();
    }

    private void doActivateSessionLocked(
            EventLoop requestEventLoop,
            final String service,
            final CompetitionSessionInfo sessionInfo,
            final Callback<ServiceInfo, SessionManageException> callback) {
        new SeriesFlow<ServiceInfo, SessionManageException>(
        ).add(
                newRouteTask(requestEventLoop, service)
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                final ServiceInfo serviceInfo = (ServiceInfo) arguments[0];
                Connection connection = (Connection) arguments[1];

                doActivateSessionLocked(serviceInfo, connection, sessionInfo, new Callback<
                        Void, SessionManageException>() {
                    @Override
                    public void onSuccess(Void value) {
                        callback.onSuccess(serviceInfo);
                    }

                    @Override
                    public void onFailure(SessionManageException e) {
                        callback.onFailure(e);
                    }
                });
            }
        }).onSuccess(new SeriesFlow.SuccessCallback<ServiceInfo>() {
            @Override
            public void onSuccess(ServiceInfo serviceInfo) {
                LOGGER.i("Activate service 's session success. service=%s, sessionInfo=%s",
                        service, sessionInfo);
                callback.onSuccess(serviceInfo);
            }
        }).onFailure(new SeriesFlow.FailureCallback<SessionManageException>() {
            @Override
            public void onFailure(SessionManageException e) {
                LOGGER.e(e, "Activate service 's session failure. service=%s, sessionInfo=%s",
                        service, sessionInfo);
                callback.onFailure(e);
            }
        }).start();
    }

    private void doActivateSessionLocked(
            ServiceInfo serviceInfo,
            Connection connection,
            CompetitionSessionInfo sessionInfo,
            final Callback<Void, SessionManageException> callback) {
        ResponseCallback responseCallback = new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Request req, CallException e) {
                callback.onFailure(new SessionManageException(
                        SessionManageException.CODE_SERVICE_INTERNAL_ERROR,
                        "Service internal error.",
                        e
                ));
            }
        };

        mCallable.call(
                connection,
                serviceInfo,
                FromMasterPaths.PATH_ACTIVATE_COMPETING_SESSION,
                ParcelableParam.create(sessionInfo),
                responseCallback
        );
    }

    /**
     * 检查调用请求的竞争会话
     *
     * @param request               请求
     * @param serviceInfo           调用的服务的信息
     * @param destinationConnection 调用服务的目的连接
     * @param callback              检查回调。callback.onFailure(SessionManageException e) 具体异常原因详见：
     * @see SessionManageException#CODE_ACTIVE_SESSION_NOT_FOUND
     * @see SessionManageException#CODE_CALL_IN_INCORRECT_SESSION
     * @see SessionManageException#CODE_CALL_NOT_IN_SESSION
     */
    public void checkRequestCompetingSession(
            final ParcelRequest request,
            final ServiceInfo serviceInfo,
            @Nullable final Connection destinationConnection,
            final Callback<Void, SessionManageException> callback) {
        final String sessionId = request.getContext().getCompetingSession();
        if (sessionId != null) {
            checkSessionExistenceAndPathValid(request, serviceInfo, sessionId, callback);
        } else {
            checkRequestIfNeedInSession(request, serviceInfo, destinationConnection, callback);
        }
    }

    private void checkSessionExistenceAndPathValid(
            final ParcelRequest request,
            final ServiceInfo serviceInfo,
            final String sessionId,
            final Callback<Void, SessionManageException> callback) {
        // TODO 检查调用者身份

        mSessionLock.acquire(new AsyncLock.AcquireCallback() {
            @Override
            public void onAcquired(String lockToken) {
                synchronized (mSessionLock) {
                    SessionEnv sessionEnv = mActiveSessions.get(sessionId);
                    if (sessionEnv == null) {
                        mSessionLock.release(lockToken);
                        callback.onFailure(new SessionManageException(
                                SessionManageException.CODE_ACTIVE_SESSION_NOT_FOUND,
                                "Active competing session NOT found. sessionId=" + sessionId
                        ));

                        return;
                    }

                    Set<String> pathSet = sessionEnv.serviceItemPaths.get(serviceInfo.getName());
                    if (pathSet == null || !pathSet.contains(request.getPath())) {
                        mSessionLock.release(lockToken);
                        callback.onFailure(new SessionManageException(
                                SessionManageException.CODE_CALL_IN_INCORRECT_SESSION,
                                "Call in a incorrect session. sessionId=" + sessionId
                        ));

                        return;
                    }

                    mSessionLock.release(lockToken);
                    callback.onSuccess(null);
                }
            }
        });
    }

    private void checkRequestIfNeedInSession(
            final ParcelRequest request,
            final ServiceInfo serviceInfo,
            @Nullable final Connection destinationConnection,
            final Callback<Void, SessionManageException> callback) {
        new SeriesFlow<Void, SessionManageException>(
        ).add(
                newAcquireLockTask(mCompetingInfosLock, Collections.<String>emptyList())
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                final String lockToken = (String) arguments[0];

                synchronized (mCompetingInfosLock) {
                    ServiceCompetingInfo competingInfo = mCompetingInfos.get(serviceInfo.getName());
                    if (competingInfo != null) {
                        callback.onSuccess(lockToken, competingInfo.itemPathSet);
                        return;
                    }
                }

                getServiceCompetingItemsLocked(
                        serviceInfo,
                        destinationConnection,
                        new Callback<ServiceCompetingInfo, SessionManageException>() {
                            @Override
                            public void onSuccess(ServiceCompetingInfo competingInfo) {
                                callback.onSuccess(lockToken, competingInfo.itemPathSet);
                            }

                            @Override
                            public void onFailure(SessionManageException e) {
                                mCompetingInfosLock.release(lockToken);
                                callback.onFailure(e);
                            }
                        }
                );
            }
        }).add(new AsyncTask<SessionManageException>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(
                    AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                String lockToken = (String) arguments[0];
                mCompetingInfosLock.release(lockToken);

                HashSet<String> itemPathSet = (HashSet<String>) arguments[1];
                if (itemPathSet.contains(request.getPath())) {
                    callback.onFailure(new SessionManageException(
                            SessionManageException.CODE_CALL_NOT_IN_SESSION,
                            "Call NOT in a session. requestId=" + request.getId()
                    ));
                } else {
                    callback.onSuccess();
                }
            }
        }).onSuccess(new SeriesFlow.SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                callback.onSuccess(null);
            }
        }).onFailure(new SeriesFlow.FailureCallback<SessionManageException>() {
            @Override
            public void onFailure(SessionManageException e) {
                callback.onFailure(e);
            }
        }).start();
    }

    /**
     * 使竞争会话失效
     *
     * @param handlerContext 请求激活的连接处理上下文
     * @param requestContext 请求激活的请求上下文
     * @param sessionId      竞争会话 id
     * @param callback       使竞争会话失效的回调
     */
    public void deactivateSession(
            final HandlerContext handlerContext,
            ParcelRequestContext requestContext,
            final String sessionId,
            final Callback<Void, SessionManageException> callback) {
        mSessionLock.acquire(new AsyncLock.AcquireCallback() {
            @Override
            public void onAcquired(final String lockToken) {
                synchronized (mSessionLock) {
                    Iterator<SessionEnv> iterator = mSessionStack.iterator();
                    SessionEnv removed = null;
                    while (iterator.hasNext()) {
                        SessionEnv sessionEnv = iterator.next();
                        if (sessionEnv.sessionId.equals(sessionId)) {
                            iterator.remove();
                            removed = sessionEnv;
                            break;
                        }
                    }

                    if (removed == null) {
                        mSessionLock.release(lockToken);

                        LOGGER.w("Deactivate session success, but session NOT found. " +
                                "sessionId=%s", sessionId);
                        callback.onSuccess(null);
                        return;
                    }

                    mActiveSessions.remove(removed.sessionId);
                    mSessionMap.remove(removed.sessionId);

                    Callback<Void, SessionManageException> deactivateCallback
                            = new Callback<Void, SessionManageException>() {
                        @Override
                        public void onSuccess(Void value) {
                            resumeInterruptionLocked();
                            mSessionLock.release(lockToken);

                            callback.onSuccess(null);

                        }

                        @Override
                        public void onFailure(SessionManageException e) {
                            throw new AssertionError("Impossible here.");
                        }
                    };

                    doDeactivateSessionLocked(
                            handlerContext.eventLoop(),
                            removed,
                            deactivateCallback
                    );
                }
            }
        });
    }

    private void resumeInterruptionLocked() {
        synchronized (this) {
            HashSet<CompetingItem> usedSet = new HashSet<>();
            for (SessionEnv sessionEnv : mActiveSessions.values()) {
                usedSet.addAll(sessionEnv.sessionInfo.getCompetingItems());
            }

            for (SessionEnv sessionEnv : mSessionStack) {
                if (!sessionEnv.interrupted) {
                    continue;
                }

                boolean canResume = true;
                for (CompetingItem item : sessionEnv.sessionInfo.getCompetingItems()) {
                    if (usedSet.contains(item)) {
                        canResume = false;
                        break;
                    }
                }

                if (!canResume) {
                    continue;
                }

                sessionEnv.interrupted = false;
                mActiveSessions.put(sessionEnv.sessionId, sessionEnv);
                usedSet.addAll(sessionEnv.sessionInfo.getCompetingItems());

                Bundle bundle = new Bundle();
                bundle.putBoolean(ParamBundleConstants.KEY_COMPETING_SESSION_RESUMED, true);
                mEventDispatcher.publish(
                        MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_ENDED +
                                sessionEnv.sessionId,
                        ParcelableParam.create(bundle)
                );
            }
        }
    }

    public void deactivateSessions(
            final HandlerContext handlerContext,
            final Connection connection) {
        final String packageName = (String) connection.attributes().
                get(ConnectionConstants.ATTR_KEY_PACKAGE);
        if (packageName == null) {
            throw new AssertionError("packageName != null");
        }

        new ParallelFlow<SessionManageException>(
        ).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    AsyncTaskCallback<SessionManageException> callback, Object... arguments) {
                mCompetingInfosLock.acquire(new AsyncLock.AcquireCallback() {
                    @Override
                    public void onAcquired(String lockToken) {
                        synchronized (mCompetingInfosLock) {
                            Iterator<Map.Entry<String, ServiceCompetingInfo>> iterator =
                                    mCompetingInfos.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, ServiceCompetingInfo> entry = iterator.next();
                                if (entry.getValue().serviceInfo.getPackageName().
                                        equals(packageName)) {
                                    LOGGER.i("Remove competing info cache. competingInfo=%s",
                                            entry.getValue());
                                    iterator.remove();
                                }
                            }
                        }

                        mCompetingInfosLock.release(lockToken);
                    }
                });
            }
        }).add(new AsyncTask<SessionManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SessionManageException> callback,
                    Object... arguments) {
                mSessionLock.acquire(new AsyncLock.AcquireCallback() {
                    @Override
                    public void onAcquired(final String lockToken) {
                        synchronized (mSessionLock) {
                            deactivateSessionsLocked(
                                    handlerContext,
                                    connection.id().asText(),
                                    packageName,
                                    new Callback<Void, SessionManageException>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            resumeInterruptionLocked();
                                            mSessionLock.release(lockToken);

                                            callback.onSuccess();
                                        }

                                        @Override
                                        public void onFailure(SessionManageException e) {
                                            throw new AssertionError("Impossible here.");
                                        }
                                    }
                            );
                        }
                    }
                });
            }
        }).onComplete(new ParallelFlow.CompleteCallback<SessionManageException>() {
            @Override
            public void onComplete(ParallelFlow.Results<SessionManageException> results) {
                // Ignore
            }
        }).start();
    }

    private void deactivateSessionsLocked(
            final HandlerContext handlerContext,
            String connId,
            String packageName,
            final Callback<Void, SessionManageException> callback) {
        Iterator<SessionEnv> iterator = mSessionStack.iterator();
        LinkedList<Pair<SessionEnv, Boolean>> removed = new LinkedList<>();

        while (iterator.hasNext()) {
            SessionEnv sessionEnv = iterator.next();
            boolean remove = false;
            boolean isActivator = false;

            if (sessionEnv.activatorConnectionId.equals(connId)) {
                remove = true;
                isActivator = true;
            } else {
                for (ServiceInfo serviceInfo : sessionEnv.serviceInfos) {
                    if (serviceInfo.getPackageName().equals(packageName)) {
                        remove = true;
                        break;
                    }
                }
            }

            if (!remove) {
                continue;
            }

            iterator.remove();
            mActiveSessions.remove(sessionEnv.sessionId);
            mSessionMap.remove(sessionEnv.sessionId);

            removed.add(new Pair<>(sessionEnv, isActivator));
        }

        if (removed.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        ParallelFlow<SessionManageException> flow = new ParallelFlow<>();
        for (final Pair<SessionEnv, Boolean> pair : removed) {
            flow.add(new AsyncTask<SessionManageException>() {
                @Override
                public void execute(
                        final AsyncTaskCallback<SessionManageException> callback,
                        Object... arguments) {
                    doDeactivateSessionLocked(
                            handlerContext.eventLoop(),
                            pair.first,
                            new Callback<Void, SessionManageException>() {
                                @Override
                                public void onSuccess(Void value) {
                                    callback.onSuccess();

                                    if (!pair.second) {
                                        mEventDispatcher.publish(
                                                MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_BEGAN +
                                                        pair.first.sessionId
                                        );

                                        Bundle bundle = new Bundle();
                                        bundle.putBoolean(ParamBundleConstants.KEY_COMPETING_SESSION_RESUMED, false);
                                        mEventDispatcher.publish(
                                                MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_ENDED +
                                                        pair.first.sessionId,
                                                ParcelableParam.create(bundle)
                                        );
                                    }
                                }

                                @Override
                                public void onFailure(SessionManageException e) {
                                    throw new AssertionError("Impossible here.");
                                }
                            }
                    );
                }
            });
        }

        flow.onComplete(new ParallelFlow.CompleteCallback<SessionManageException>() {
            @Override
            public void onComplete(ParallelFlow.Results<SessionManageException> results) {
                if (results.isAllSuccess()) {
                    callback.onSuccess(null);
                    return;
                }

                throw new AssertionError("Impossible here.");
            }
        }).start();
    }

    private static class ServiceCompetingInfo {

        ServiceInfo serviceInfo;
        HashSet<CompetingItem> itemSet = new HashSet<>();
        HashMap<String, CompetingItemDetail> details = new HashMap<>();
        HashSet<String> itemPathSet = new HashSet<>();

        public ServiceCompetingInfo(ServiceInfo serviceInfo) {
            this.serviceInfo = serviceInfo;
        }

        @Override
        public String toString() {
            return "ServiceCompetingInfo{" +
                    "serviceInfo=" + serviceInfo +
                    ", itemSet=" + itemSet +
                    ", details=" + details +
                    ", itemPathSet=" + itemPathSet +
                    '}';
        }
    }

    private static class SessionEnv {

        String sessionId;
        CompetitionSessionInfo sessionInfo;
        ActivateOption option;
        String activatorType;
        String activator;
        String activatorConnectionId;
        HashMap<String, Set<String>> serviceItemPaths = new HashMap<>();
        LinkedList<ServiceInfo> serviceInfos = new LinkedList<>();
        boolean interrupted;

        SessionEnv(
                CompetitionSessionInfo sessionInfo,
                ActivateOption option,
                String activatorType,
                String activator,
                String activatorConnectionId) {
            sessionId = sessionInfo.getSessionId();
            this.sessionInfo = sessionInfo;
            this.option = option;
            this.activatorType = activatorType;
            this.activator = activator;
            this.activatorConnectionId = activatorConnectionId;
        }

        @Override
        public String toString() {
            return "SessionEnv{" +
                    "sessionId='" + sessionId + '\'' +
                    ", sessionInfo=" + sessionInfo +
                    ", option=" + option +
                    ", activatorType='" + activatorType + '\'' +
                    ", activator='" + activator + '\'' +
                    ", activatorConnectionId='" + activatorConnectionId + '\'' +
                    ", serviceItemPaths=" + serviceItemPaths +
                    ", serviceInfos=" + serviceInfos +
                    ", interrupted=" + interrupted +
                    '}';
        }
    }
}