package com.ubtrobot.master.skill;

import android.support.annotation.Nullable;
import android.util.Pair;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.async.AsyncTask;
import com.ubtrobot.master.async.AsyncTaskCallback;
import com.ubtrobot.master.async.Callback;
import com.ubtrobot.master.async.SeriesFlow;
import com.ubtrobot.master.async.TaskQueue;
import com.ubtrobot.master.call.IPCFromMasterCallable;
import com.ubtrobot.master.component.ComponentBaseInfo;
import com.ubtrobot.master.event.InternalEventDispatcher;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.policy.BlackWhiteList;
import com.ubtrobot.master.policy.ComponentPolicyList;
import com.ubtrobot.master.policy.ComponentPolicySource;
import com.ubtrobot.master.policy.MixedComponentBaseInfoList;
import com.ubtrobot.master.policy.PolicyException;
import com.ubtrobot.master.policy.ServicePolicy;
import com.ubtrobot.master.policy.SkillPolicy;
import com.ubtrobot.master.service.ServiceManager;
import com.ubtrobot.master.transport.connection.ConnectionConstants;
import com.ubtrobot.master.transport.message.FromMasterPaths;
import com.ubtrobot.master.transport.message.MasterEvents;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
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
 * Created by column on 03/12/2017.
 */

public class SkillManager {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("SkillManager");

    public static final String STATE_DEFAULT = ParamBundleConstants.VAL_SKILL_STATE_DEFAULT;

    private final EventLoop mEventLoop;
    private final TaskQueue mTaskQueue;

    private final ServiceManager mServiceManager;
    private final ServiceStateAddedListener mListener = new ServiceStateAddedListener();

    private final ComponentPolicySource mPolicySource;
    private final IPCFromMasterCallable mCallable;
    private final InternalEventDispatcher mEventDispatcher;

    private final HashSet<ComponentBaseInfo> mStartedSkills = new HashSet<>();
    private final HashMap<ComponentBaseInfo, SkillEnv> mSkillEnvMap = new HashMap<>();
    /**
     * mSkillsLock 锁使用注意：
     * <p>
     * 使用 mSkillsLock 的线程分为两类：
     * 1. mEventLoop 线程（startSkill、setSkillState、stopSkill、stopConnectionSkills 执行的线程）
     * 2. getStartedSkills 的调用者线程
     * <p>
     * 1 中包含的线程对 mStartedSkills、mSkillEnvMap 进行了读写操作，统一使用 WriteLock
     * 2 中包含的线程对 mStartedSkills、mSkillEnvMap 只进行了读操作，统一使用 ReadLock
     */
    private final ReentrantReadWriteLock mSkillsLock = new ReentrantReadWriteLock();

    public SkillManager(
            EventLoop eventLoop,
            ServiceManager serviceManager,
            ComponentPolicySource policySource,
            IPCFromMasterCallable callable,
            InternalEventDispatcher eventDispatcher) {
        // 确保 TaskQueue 中任务与 runInLoop(runnable) 都执行在同一个 EventLoop
        mEventLoop = eventLoop;
        mTaskQueue = new TaskQueue(mEventLoop);

        mServiceManager = serviceManager;
        mServiceManager.addOnServiceStateAddedListener(mListener); // TODO 移除

        mPolicySource = policySource;
        mCallable = callable;
        mEventDispatcher = eventDispatcher;
    }

    /**
     * 启动 Skill。已经启动的情况，回调成功
     *
     * @param handlerContext  请求启动 Skill 的连接的 HandlerContext
     * @param skill           请求启动的 Skill
     * @param skillConnection 请求启动的 Skill 的连接。如果 Skill 没有连接到 Master，则为 null
     * @param callback        请求启动回调
     */
    public void startSkill(
            final HandlerContext handlerContext,
            final SkillInfo skill,
            @Nullable final Connection skillConnection,
            final Callback<Void, SkillManageException> callback) {
        runInLoop(new Runnable() {
            @Override
            public void run() {
                // 在 addTask 之前调用 getOrCreateSkillEnv 的原因：
                // execStartSkillTaskInLoop 实际由 EventLoop 中多个 runnable 构成
                // runnable 之间可插入 skillConnection，便于前面的 start 任务能用上 skillConnection
                final SkillEnv skillEnv = getOrCreateSkillEnv(skill, skillConnection);

                mTaskQueue.addTask(new TaskQueue.Task() {
                    @Override
                    protected void execute() {
                        if (skillEnv.started) {
                            notifyComplete();

                            callbackSuccessInLoop(handlerContext, callback);
                            return;
                        }

                        execStartSkillTaskInLoop(handlerContext, skillEnv, this, callback);
                    }
                });
            }
        });
    }

    private void runInLoop(Runnable runnable) {
        if (mEventLoop.inEventLoop()) {
            runnable.run();
        } else {
            mEventLoop.post(runnable);
        }
    }

    private <T> T lockForEventLoop(Callable<T> callable) {
        mSkillsLock.writeLock().lock();
        try {
            return callable.call();
        } finally {
            mSkillsLock.writeLock().unlock();
        }
    }

    private void lockForEventLoop(Runnable runnable) {
        mSkillsLock.writeLock().lock();
        try {
            runnable.run();
        } finally {
            mSkillsLock.writeLock().unlock();
        }
    }

    private SkillEnv getOrCreateSkillEnv(final SkillInfo skillInfo, final Connection connection) {
        return lockForEventLoop(new Callable<SkillEnv>() {
            @Override
            public SkillEnv call() {
                ComponentBaseInfo skillBaseInfo = new ComponentBaseInfo(
                        skillInfo.getName(), skillInfo.getPackageName());
                SkillEnv skillEnv = mSkillEnvMap.get(skillBaseInfo);

                if (skillEnv == null) {
                    skillEnv = new SkillEnv(skillInfo, skillBaseInfo);
                    mSkillEnvMap.put(skillBaseInfo, skillEnv);
                }

                skillEnv.skillConnection = connection;
                return skillEnv;
            }
        });
    }

    private void callbackSuccessInLoop(
            HandlerContext context, final Callback<Void, SkillManageException> callback) {
        context.eventLoop().post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(null);
            }
        });
    }

    private void execStartSkillTaskInLoop(
            final HandlerContext handlerContext,
            final SkillEnv skillEnv,
            final TaskQueue.Task queueTask,
            final Callback<Void, SkillManageException> callback) {
        new SeriesFlow<Void, SkillManageException>().add(new AsyncTask<SkillManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SkillManageException> callback, Object... arguments) {
                final MixedComponentBaseInfoList baseInfoList = new MixedComponentBaseInfoList();
                baseInfoList.getSkillBaseInfoList().add(skillEnv.skillBaseInfo);
                lockForEventLoop(new Runnable() {
                    @Override
                    public void run() {
                        baseInfoList.getSkillBaseInfoList().addAll(mStartedSkills);
                    }
                });

                final Map<ComponentBaseInfo, Set<String>> statefulServices =
                        mServiceManager.getStatefulServices();
                baseInfoList.getServiceBaseInfoList().addAll(statefulServices.keySet());

                getComponentPolicies(
                        baseInfoList,
                        new Callback<ComponentPolicyList, SkillManageException>() {
                            @Override
                            public void onSuccess(ComponentPolicyList policies) {
                                callback.onSuccess(policies, statefulServices);
                            }

                            @Override
                            public void onFailure(SkillManageException e) {
                                callback.onFailure(e);
                            }
                        }
                );
            }
        }).add(new AsyncTask<SkillManageException>() {
            @Override
            public void execute(
                    AsyncTaskCallback<SkillManageException> callback, Object... arguments) {
                ComponentPolicyList policyList = (ComponentPolicyList) arguments[0];
                @SuppressWarnings("unchecked") Map<ComponentBaseInfo, Set<String>>
                        statefulServices = (Map<ComponentBaseInfo, Set<String>>) arguments[1];

                Pair<Boolean, BlackWhiteList<ComponentBaseInfo>> decision =
                        decideAllowStartAndShouldStop(
                                skillEnv.skillBaseInfo,
                                policyList,
                                statefulServices);
                if (decision.first) {
                    callback.onSuccess(decision.second);
                } else {
                    callback.onFailure(new SkillManageException(
                            SkillManageException.CODE_FORBIDDEN, "Forbidden start."));
                }
            }
        }).add(
                stoppingStartedSkillTask()
        ).add(new AsyncTask<SkillManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SkillManageException> callback, Object... arguments) {
                doStartSkillInLoop(skillEnv, new Callback<Void, SkillManageException>() {
                    @Override
                    public void onSuccess(Void data) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(SkillManageException e) {
                        callback.onFailure(e);
                    }
                });
            }
        }).onSuccess(new SeriesFlow.SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                lockForEventLoop(new Runnable() {
                    @Override
                    public void run() {
                        skillEnv.started = true;
                        mStartedSkills.add(skillEnv.skillBaseInfo);
                    }
                });

                queueTask.notifyComplete();

                callbackSuccessInLoop(handlerContext, callback);

                mEventLoop.post(new Runnable() {
                    @Override
                    public void run() {
                        mEventDispatcher.publish(
                                MasterEvents.ACTION_SKILL_STARTED,
                                ParcelableParam.create(skillEnv.skillInfo)
                        );
                    }
                });
            }
        }).onFailure(new SeriesFlow.FailureCallback<SkillManageException>() {
            @Override
            public void onFailure(SkillManageException e) {
                skillEnv.started = false;
                queueTask.notifyComplete();

                callbackFailureInLoop(handlerContext, callback, e);
            }
        }).start();
    }

    private void getComponentPolicies(
            MixedComponentBaseInfoList baseInfoList,
            final Callback<ComponentPolicyList, SkillManageException> callback) {
        mPolicySource.getComponentPolicies(baseInfoList,
                new Callback<ComponentPolicyList, PolicyException>() {
                    @Override
                    public void onSuccess(final ComponentPolicyList policies) {
                        runInLoop(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(policies);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final PolicyException e) {
                        runInLoop(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(new SkillManageException(
                                        SkillManageException.CODE_INTERNAL_ERROR,
                                        "Internal error for starting the skill " +
                                                "due to get policy failed.",
                                        e
                                ));
                            }
                        });
                    }
                }
        );
    }

    private Pair<Boolean, BlackWhiteList<ComponentBaseInfo>> decideAllowStartAndShouldStop(
            ComponentBaseInfo skillWillStart,
            ComponentPolicyList policyList,
            Map<ComponentBaseInfo, Set<String>> statefulServices) {
        BlackWhiteList<ComponentBaseInfo> shouldStop = null;

        for (SkillPolicy policy : policyList.getSkillPolicyList()) {
            if (policy.getSkill().equals(skillWillStart)) {
                shouldStop = policy.getWillStart();
                continue;
            }

            BlackWhiteList<ComponentBaseInfo> didStart = policy.getDidStart();
            boolean didStartContains = didStart.contains(skillWillStart);
            if ((didStart.isWhitelist() && !didStartContains) ||
                    (didStart.isBlacklist() && didStartContains)) {
                return new Pair<>(false, shouldStop);
            }

            SkillEnv startedSkillEnv = getStartedSkillEnv(policy.getSkill());
            if (STATE_DEFAULT.equals(startedSkillEnv.state)) {
                continue;
            }

            BlackWhiteList<ComponentBaseInfo> didSetState = policy.getDidSetState(startedSkillEnv.state);
            boolean didSetStateContains = didSetState.contains(skillWillStart);
            if ((didSetState.isWhitelist() && !didSetStateContains) ||
                    (didSetState.isBlacklist() && didSetStateContains)) {
                return new Pair<>(false, shouldStop);
            }
        }

        for (ServicePolicy servicePolicy : policyList.getServicePolicyList()) {
            Set<String> states = statefulServices.get(servicePolicy.getService());
            for (String state : states) {
                BlackWhiteList<ComponentBaseInfo> didAddState = servicePolicy.getDidAddState(state);
                boolean didAddStateContains = didAddState.contains(skillWillStart);
                if ((didAddState.isWhitelist() && !didAddStateContains) ||
                        (didAddState.isBlacklist() && didAddStateContains)) {
                    return new Pair<>(false, shouldStop);
                }
            }
        }

        return new Pair<>(true, shouldStop);
    }

    private SkillEnv getStartedSkillEnv(final ComponentBaseInfo skillBaseInfo) {
        return lockForEventLoop(new Callable<SkillEnv>() {
            @Override
            public SkillEnv call() {
                SkillEnv skillEnv = mSkillEnvMap.get(skillBaseInfo);
                if (skillEnv == null || !skillEnv.started) {
                    throw new AssertionError("skillEnv != null && skillEnv.started");
                }

                return skillEnv;
            }
        });
    }

    private AsyncTask<SkillManageException>
    stoppingStartedSkillTask() {
        return new AsyncTask<SkillManageException>() {

            @Override
            public void execute(
                    final AsyncTaskCallback<SkillManageException> callback, Object... arguments) {
                @SuppressWarnings("unchecked") BlackWhiteList<ComponentBaseInfo> shouldStop =
                        arguments.length == 0 ?
                                null : (BlackWhiteList<ComponentBaseInfo>) arguments[0];
                if (shouldStop == null) {
                    callback.onSuccess();
                    return;
                }

                List<ComponentBaseInfo> startedShouldStop =
                        resolveStartedSkillsShouldStop(shouldStop);
                if (startedShouldStop.isEmpty()) {
                    callback.onSuccess();
                    return;
                }

                stopSkillsInLoop(startedShouldStop,
                        new Callback<Void, SkillManageException>() {
                            @Override
                            public void onSuccess(Void data) {
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(SkillManageException e) {
                                throw new AssertionError("Should NOT be here.");
                            }
                        }
                );
            }
        };
    }

    private List<ComponentBaseInfo> resolveStartedSkillsShouldStop(
            final BlackWhiteList<ComponentBaseInfo> blackWhiteList) {
        final LinkedList<ComponentBaseInfo> ret = new LinkedList<>();
        lockForEventLoop(new Runnable() {
            @Override
            public void run() {
                if (mStartedSkills.isEmpty()) {
                    return;
                }

                HashSet<ComponentBaseInfo> blackWhiteSet = new HashSet<>();
                blackWhiteSet.addAll(blackWhiteList);

                if (blackWhiteList.isWhitelist()) {
                    for (ComponentBaseInfo startedSkill : mStartedSkills) {
                        if (!blackWhiteSet.contains(startedSkill)) {
                            ret.add(startedSkill);
                        }
                    }
                } else {
                    for (ComponentBaseInfo startedSkill : mStartedSkills) {
                        if (blackWhiteSet.contains(startedSkill)) {
                            ret.add(startedSkill);
                        }
                    }
                }
            }
        });

        return ret;
    }

    private void stopSkillsInLoop(
            final List<ComponentBaseInfo> skillBaseInfos,
            final Callback<Void, SkillManageException> callback) {
        if (skillBaseInfos.isEmpty()) {
            throw new AssertionError("!skillBaseInfos.isEmpty()");
        }

        final int[] count = {0};

        for (final ComponentBaseInfo skillBaseInfo : skillBaseInfos) {
            doStopSkillInLoop(
                    getStartedSkillEnv(skillBaseInfo),
                    new Callback<Void, SkillManageException>() {
                        @Override
                        public void onSuccess(Void data) {
                            count[0]++;

                            completeIfAllCallback();
                        }

                        @Override
                        public void onFailure(SkillManageException e) {
                            count[0]++;

                            LOGGER.w(e, "Stop the skill failed. Maybe timeout or crashed. Ignore it.");

                            completeIfAllCallback();
                        }

                        private void completeIfAllCallback() {
                            if (count[0] < skillBaseInfos.size()) {
                                return;
                            }

                            callback.onSuccess(null);
                        }
                    }
            );
        }
    }

    private void doStopSkillInLoop(
            final SkillEnv skillEnv,
            final Callback<Void, SkillManageException> callback) {
        mCallable.call(
                skillEnv.skillConnection,
                skillEnv.skillInfo,
                FromMasterPaths.PATH_STOP_SKILL,
                ParcelableParam.create(skillEnv.skillBaseInfo),
                new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        runInLoop(new Runnable() {
                            @Override
                            public void run() {
                                removeStartedSkillThenNotifyInLoop(skillEnv);

                                callback.onSuccess(null);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Request req, final CallException e) {
                        runInLoop(new Runnable() {
                            @Override
                            public void run() {
                                removeStartedSkillThenNotifyInLoop(skillEnv);

                                callback.onFailure(new SkillManageException(
                                        SkillManageException.CODE_INTERNAL_ERROR,
                                        "Internal error for stopping the skill." +
                                                "Maybe timeout or skill crashed.",
                                        e
                                ));
                            }
                        });
                    }
                }
        );
    }

    private void removeStartedSkillThenNotifyInLoop(final SkillEnv skillEnv) {
        lockForEventLoop(new Runnable() {
            @Override
            public void run() {
                skillEnv.reset();

                boolean removed = mStartedSkills.remove(skillEnv.skillBaseInfo);
                mSkillEnvMap.remove(skillEnv.skillBaseInfo);

                if (removed) {
                    mEventLoop.post(new Runnable() {
                        @Override
                        public void run() {
                            mEventDispatcher.publish(
                                    MasterEvents.ACTION_SKILL_STOPPED,
                                    ParcelableParam.create(skillEnv.skillInfo)
                            );
                        }
                    });
                }
            }
        });
    }

    private void doStartSkillInLoop(
            SkillEnv skillEnv,
            final Callback<Void, SkillManageException> callback) {
        mCallable.call(
                skillEnv.skillConnection,
                skillEnv.skillInfo,
                FromMasterPaths.PATH_START_SKILL,
                ParcelableParam.create(skillEnv.skillBaseInfo),
                new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        runInLoop(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(null);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Request req, final CallException e) {
                        runInLoop(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(new SkillManageException(
                                        SkillManageException.CODE_INTERNAL_ERROR,
                                        "Internal error for starting the skill.",
                                        e
                                ));
                            }
                        });
                    }
                }
        );
    }

    private void callbackFailureInLoop(
            HandlerContext context,
            final Callback<Void, SkillManageException> callback,
            final SkillManageException e) {
        context.eventLoop().post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 获取已经启动的 Skill 信息。
     *
     * @return 已经启动的 Skill 信息
     */
    public List<SkillInfo> getStartedSkills() {
        mSkillsLock.readLock().lock();
        try {
            LinkedList<SkillInfo> skillInfos = new LinkedList<>();
            for (ComponentBaseInfo baseInfo : mStartedSkills) {
                SkillEnv skillEnv = mSkillEnvMap.get(baseInfo);
                if (skillEnv == null || !skillEnv.started) {
                    throw new AssertionError("skillEnv != null && skillEnv.started");
                }

                skillInfos.add(skillEnv.skillInfo);
            }

            return skillInfos;
        } finally {
            mSkillsLock.readLock().unlock();
        }
    }

    /**
     * 设置 Skill 内部状态
     *
     * @param handlerContext 请求设置内部状态的 Skill 的连接的 HandlerContext
     * @param skillBaseInfo  Skill 基本信息
     * @param state          Skill 内部状态
     * @param callback       设置内部状态的回调
     */
    public void setSkillState(
            final HandlerContext handlerContext,
            final ComponentBaseInfo skillBaseInfo,
            final String state,
            final Callback<Void, SkillManageException> callback) {
        mTaskQueue.addTask(new TaskQueue.Task() {
            @Override
            protected void execute() {
                SkillEnv skillEnv = lockForEventLoop(new Callable<SkillEnv>() {
                    @Override
                    public SkillEnv call() {
                        return mSkillEnvMap.get(skillBaseInfo);
                    }
                });

                if (skillEnv == null || !skillEnv.started) {
                    notifyComplete();

                    callbackFailureInLoop(handlerContext, callback,
                            new SkillManageException(
                                    SkillManageException.CODE_SET_STATE_BEFORE_SKILL_RUNNING,
                                    "Set skill state failed by reason of skill not started. " +
                                            "skill=" + skillBaseInfo + ", state=" + state));

                    return;
                }

                execSetSkillStateTaskInLoop(handlerContext, skillEnv, state, this, callback);
            }
        });
    }

    private void execSetSkillStateTaskInLoop(
            final HandlerContext handlerContext,
            final SkillEnv skillEnv,
            final String state,
            final TaskQueue.Task queueTask,
            final Callback<Void, SkillManageException> callback) {
        new SeriesFlow<Void, SkillManageException>().add(new AsyncTask<SkillManageException>() {
            @Override
            public void execute(
                    final AsyncTaskCallback<SkillManageException> callback, Object... arguments) {
                MixedComponentBaseInfoList baseInfoList = new MixedComponentBaseInfoList();
                baseInfoList.getSkillBaseInfoList().add(skillEnv.skillBaseInfo);

                getComponentPolicies(
                        baseInfoList,
                        new Callback<ComponentPolicyList, SkillManageException>() {
                            @Override
                            public void onSuccess(ComponentPolicyList policies) {
                                if (policies.getSkillPolicyList().isEmpty()) {
                                    callback.onSuccess();
                                    return;
                                }

                                if (STATE_DEFAULT.equals(state)) {
                                    callback.onSuccess(policies.getSkillPolicyList().get(0).
                                            getWillStart());
                                } else {
                                    callback.onSuccess(policies.getSkillPolicyList().get(0).
                                            getWillSetState(state));
                                }
                            }

                            @Override
                            public void onFailure(SkillManageException e) {
                                callback.onFailure(e);
                            }
                        }
                );
            }
        }).add(
                stoppingStartedSkillTask()
        ).onSuccess(new SeriesFlow.SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                skillEnv.state = state;
                queueTask.notifyComplete();

                callbackSuccessInLoop(handlerContext, callback);
            }
        }).onFailure(new SeriesFlow.FailureCallback<SkillManageException>() {
            @Override
            public void onFailure(SkillManageException e) {
                queueTask.notifyComplete();

                callbackFailureInLoop(handlerContext, callback, e);
            }
        }).start();
    }

    public void stopSkill(
            final HandlerContext handlerContext,
            final ComponentBaseInfo skillBaseInfo,
            final Callback<Void, SkillManageException> callback) {
        mTaskQueue.addTask(new TaskQueue.Task() {
            @Override
            protected void execute() {
                SkillEnv skillEnv = lockForEventLoop(new Callable<SkillEnv>() {
                    @Override
                    public SkillEnv call() {
                        return mSkillEnvMap.get(skillBaseInfo);
                    }
                });

                if (skillEnv == null || !skillEnv.started) {
                    notifyComplete();

                    callbackSuccessInLoop(handlerContext, callback);
                    return;
                }

                doStopSkillInLoop(skillEnv, new Callback<Void, SkillManageException>() {
                    @Override
                    public void onSuccess(Void data) {
                        notifyComplete();

                        callbackSuccessInLoop(handlerContext, callback);
                    }

                    @Override
                    public void onFailure(SkillManageException e) {
                        notifyComplete();

                        callbackFailureInLoop(handlerContext, callback, e);
                    }
                });
            }
        });
    }

    public void stopConnectionSkills(final Connection skillConnection) {
        mTaskQueue.addTaskFirst(new TaskQueue.Task() {
            @Override
            protected void execute() {
                final String packageName = (String) skillConnection.attributes().
                        get(ConnectionConstants.ATTR_KEY_PACKAGE);
                if (packageName == null) {
                    throw new AssertionError("packageName != null");
                }

                lockForEventLoop(new Runnable() {
                    @Override
                    public void run() {
                        Iterator<ComponentBaseInfo> iterator = mStartedSkills.iterator();
                        while (iterator.hasNext()) {
                            ComponentBaseInfo skillBaseInfo = iterator.next();
                            if (!skillBaseInfo.getPackageName().equals(packageName)) {
                                continue;
                            }

                            iterator.remove();
                            final SkillEnv skillEnv = mSkillEnvMap.remove(skillBaseInfo);
                            if (skillEnv == null) {
                                return;
                            }

                            skillEnv.reset();
                            LOGGER.i("Skill stopped by reason of connection disconnected. " +
                                    "skillBaseInfo=%s", skillBaseInfo);

                            mEventLoop.post(new Runnable() {
                                @Override
                                public void run() {
                                    mEventDispatcher.publish(
                                            MasterEvents.ACTION_SKILL_STOPPED,
                                            ParcelableParam.create(skillEnv.skillInfo)
                                    );
                                }
                            });
                        }
                    }
                });

                notifyComplete();
            }
        });
    }

    private class ServiceStateAddedListener implements ServiceManager.OnServiceStateAddedListener {

        @Override
        public void onServiceStateAdded(final ComponentBaseInfo serviceBaseInfo, final String state) {
            mTaskQueue.addTask(new TaskQueue.Task() {
                @Override
                protected void execute() {
                    new SeriesFlow<Void, SkillManageException>(
                    ).add(new AsyncTask<SkillManageException>() {
                        @Override
                        public void execute(
                                final AsyncTaskCallback<SkillManageException> callback,
                                Object... arguments) {
                            // 排队执行至此，服务状态可能已经移除（正常异常或者服务异常退出）
                            Set<String> states = mServiceManager.getStatefulServices().
                                    get(serviceBaseInfo);
                            if (states == null || !states.contains(state)) {
                                callback.onSuccess();
                                return;
                            }

                            MixedComponentBaseInfoList baseInfoList = new MixedComponentBaseInfoList();
                            baseInfoList.getServiceBaseInfoList().add(serviceBaseInfo);

                            getComponentPolicies(
                                    baseInfoList,
                                    new Callback<ComponentPolicyList, SkillManageException>() {
                                        @Override
                                        public void onSuccess(ComponentPolicyList policies) {
                                            if (policies.getServicePolicyList().isEmpty()) {
                                                callback.onSuccess();
                                                return;
                                            }

                                            callback.onSuccess(policies.getServicePolicyList().
                                                    get(0).getWillAddState(state));
                                        }

                                        @Override
                                        public void onFailure(SkillManageException e) {
                                            callback.onFailure(e);
                                        }
                                    }
                            );
                        }
                    }).add(
                            stoppingStartedSkillTask()
                    ).onSuccess(new SeriesFlow.SuccessCallback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            notifyComplete();
                        }
                    }).onFailure(new SeriesFlow.FailureCallback<SkillManageException>() {
                        @Override
                        public void onFailure(SkillManageException e) {
                            notifyComplete();

                            LOGGER.e(e, "Manage skill failed after service set state. " +
                                    "component=%s, state=%s", serviceBaseInfo, state);
                        }
                    }).start();
                }
            });
        }
    }

    private static final class SkillEnv {

        boolean started;

        SkillInfo skillInfo;
        ComponentBaseInfo skillBaseInfo;
        Connection skillConnection;

        String state = STATE_DEFAULT;

        SkillEnv(SkillInfo skillInfo, ComponentBaseInfo skillBaseInfo) {
            this.skillInfo = skillInfo;
            this.skillBaseInfo = skillBaseInfo;
        }

        void reset() {
            state = STATE_DEFAULT;
            started = false;

            if (skillConnection != null && !skillConnection.isConnected()) {
                skillConnection = null;
            }
        }

        @Override
        public String toString() {
            return "SkillEnv{" +
                    "started=" + started +
                    ", skillInfo=" + skillInfo +
                    ", skillBaseInfo=" + skillBaseInfo +
                    ", skillConnection=" + skillConnection +
                    ", state='" + state + '\'' +
                    '}';
        }
    }

    private interface Callable<T> {

        T call();
    }
}