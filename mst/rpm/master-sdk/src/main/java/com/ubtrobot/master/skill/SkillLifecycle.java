package com.ubtrobot.master.skill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.component.ComponentBaseInfo;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-12-1.
 */

public class SkillLifecycle {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("SkillLifecycle");

    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final ComponentInfoSource mSource;
    private final CallMasterCallable mCallable;

    private final HashMap<String, LivingSkill> mLivingSkills = new HashMap<>();
    private final ReentrantReadWriteLock mLivingSkillsLock = new ReentrantReadWriteLock();

    public SkillLifecycle(
            Context context,
            Handler mainThreadHandler,
            ComponentInfoSource source,
            CallMasterCallable callable) {
        mContext = context;
        mMainThreadHandler = mainThreadHandler;
        mSource = source;
        mCallable = callable;
    }

    String onSkillCreate(
            Class<? extends MasterSkill> clazz, MasterSkill skill) {
        mLivingSkillsLock.writeLock().lock();
        try {
            SkillInfo skillInfo = mSource.getSkillInfo(clazz);
            if (skillInfo == null) {
                throw new IllegalStateException("Skill NOT configured in the xml file.");
            }

            if (mLivingSkills.put(clazz.getName(),
                    new LivingSkill(clazz, skillInfo.getName(), skill)) != null) {
                throw new IllegalStateException("Skill lifecycle error. onCreate has been invoked. " +
                        "skillClass=" + clazz.getName());
            }

            return skillInfo.getName();
        } finally {
            mLivingSkillsLock.writeLock().unlock();
        }
    }

    void onSkillDestroy(Class<? extends MasterSkill> clazz) {
        mLivingSkillsLock.writeLock().lock();
        try {
            if (mLivingSkills.remove(clazz.getName()) == null) {
                throw new IllegalStateException("Skill lifecycle error. onCreate was NOT invoked. " +
                        "skillClass=" + clazz.getName());
            }
        } finally {
            mLivingSkillsLock.writeLock().unlock();
        }
    }

    void setState(Class<? extends MasterSkill> clazz, final String state) {
        mLivingSkillsLock.writeLock().lock();
        try {
            final LivingSkill livingSkill = mLivingSkills.get(clazz.getName());
            if (livingSkill == null) {
                throw new IllegalStateException(
                        "MasterSkill did NOT created. skillClass=" + clazz.getName());
            }

            if (!livingSkill.onSetState(state)) {
                LOGGER.w("State already setted. state=" + state);
                return;
            }

            Bundle paramBundle = new Bundle();
            paramBundle.putString(ParamBundleConstants.KEY_SKILL_NAME, livingSkill.skillName);
            paramBundle.putString(ParamBundleConstants.KEY_STATE, state);

            mCallable.call(
                    MasterCallPaths.PATH_SET_SKILL_STATE,
                    ParcelableParam.create(paramBundle),
                    new ResponseCallback() {
                        @Override
                        public void onResponse(Request req, Response res) {
                            // Ignore
                        }

                        @Override
                        public void onFailure(Request req, CallException e) {
                            // 如果是 Master Crash 的情况，在 Master 重启后会自动同步状态
                            LOGGER.e(e, "Set state for skill failed. skillClass=" +
                                    livingSkill.getClass().getName() + ", state=" + state);
                        }
                    }
            );
        } finally {
            mLivingSkillsLock.writeLock().unlock();
        }
    }

    String getState(Class<? extends MasterSkill> clazz) {
        mLivingSkillsLock.readLock().lock();
        try {
            LivingSkill livingSkill = mLivingSkills.get(clazz.getName());
            if (livingSkill == null) {
                throw new IllegalStateException(
                        "MasterSkill did NOT created. skillClass=" + clazz.getName());
            }

            return livingSkill.state;
        } finally {
            mLivingSkillsLock.readLock().unlock();
        }
    }

    public MasterSkill getLivingSkill(Class<? extends MasterSkill> clazz) {
        mLivingSkillsLock.readLock().lock();
        try {
            LivingSkill livingSkill = mLivingSkills.get(clazz.getName());
            if (livingSkill == null) {
                return null;
            }

            return livingSkill.getSkill();
        } finally {
            mLivingSkillsLock.readLock().unlock();
        }
    }

    public void notifySkillStart(
            final EventLoop eventLoop, ParcelRequest request,
            final SkillNotifyCallback callback) {
        notifySkillOperation(eventLoop, request, callback, new SkillOperation() {
            @Override
            public void onOperate(LivingSkill livingSkill) {
                livingSkill.onSkillStart();
            }
        }, false);
    }

    private void notifySkillOperation(
            final EventLoop eventLoop,
            ParcelRequest request,
            final SkillNotifyCallback callback,
            final SkillOperation operation,
            boolean ignoreIfNotCreate) {
        ComponentBaseInfo skillBaseInfo;
        try {
            ParcelableParam<ComponentBaseInfo> param = ParcelableParam.from(
                    request.getParam(), ComponentBaseInfo.class);
            skillBaseInfo = param.getParcelable();
        } catch (ParcelableParam.InvalidParcelableParamException e) {
            callbackFailure(eventLoop, callback,
                    new CallException(CallGlobalCode.BAD_REQUEST, "Illegal argument."));
            return;
        }

        SkillInfo skillInfo = mSource.getSkillInfo(skillBaseInfo.getName());
        if (skillInfo == null) {
            callbackFailure(eventLoop, callback, new CallException(CallGlobalCode.BAD_REQUEST,
                    skillBaseInfo.getName() + " skill NOT found."));
            return;
        }

        final LivingSkill livingSkill;
        mLivingSkillsLock.readLock().lock();
        try {
            livingSkill = mLivingSkills.get(skillInfo.getClassName());
        } finally {
            mLivingSkillsLock.readLock().unlock();
        }


        // 对应的 Android Service 已经启动
        if (livingSkill != null) {
            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLivingSkillsLock.writeLock().lock();
                    try {
                        operation.onOperate(livingSkill);
                    } finally {
                        mLivingSkillsLock.writeLock().unlock();
                    }

                    callbackSuccess(eventLoop, callback);
                }
            });

            return;
        }

        if (ignoreIfNotCreate) {
            callbackSuccess(eventLoop, callback);
            return;
        }

        // 对应的 Android Service 没有启动，把 Request 投递过去，notifySkillOperation 将会被重新调用
        if (mContext.startService(new Intent().
                setComponent(new ComponentName(
                        skillInfo.getPackageName(), skillInfo.getClassName()
                )).
                putExtra(IntentExtras.KEY_REQUEST, request)) != null) {
            callbackRetrySoon(eventLoop, callback);
            return;
        }

        callbackFailure(eventLoop, callback, new CallException(
                CallGlobalCode.INTERNAL_ERROR,
                "Can NOT start android service. serviceClass=" + skillInfo.getClassName()));
    }

    private void callbackSuccess(EventLoop eventLoop, final SkillNotifyCallback callback) {
        eventLoop.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess();
            }
        });
    }

    private void callbackRetrySoon(EventLoop eventLoop, final SkillNotifyCallback callback) {
        eventLoop.post(new Runnable() {
            @Override
            public void run() {
                callback.onRetrySoon();
            }
        });
    }

    private void callbackFailure(
            EventLoop eventLoop, final SkillNotifyCallback callback, final CallException e) {
        eventLoop.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
            }
        });
    }

    public void notifySkillStop(
            final EventLoop eventLoop, ParcelRequest request, final SkillNotifyCallback callback) {
        notifySkillOperation(eventLoop, request, callback, new SkillOperation() {
            @Override
            public void onOperate(LivingSkill livingSkill) {
                livingSkill.onSkillStop();
            }
        }, true);
    }

    public void stopSkill(MasterSkill skill) {
        SkillInfo skillInfo = mSource.getSkillInfo(skill.getClass());
        if (skillInfo == null) {
            throw new AssertionError("skill != null");
        }

        mLivingSkillsLock.readLock().lock();
        try {
            if (!mLivingSkills.containsKey(skillInfo.getClassName())) {
                throw new IllegalStateException("Skill NOT start. skillName=" + skillInfo.getName());
            }
        } finally {
            mLivingSkillsLock.readLock().unlock();
        }

        Bundle paramBundle = new Bundle();
        paramBundle.putString(ParamBundleConstants.KEY_SKILL_NAME, skillInfo.getName());
        mCallable.call(
                MasterCallPaths.PATH_STOP_SKILL,
                ParcelableParam.create(paramBundle),
                new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        // Ignore
                    }

                    @Override
                    public void onFailure(Request req, CallException e) {
                        LOGGER.e(e, "Stop skill failed.");
                    }
                }
        );
    }

    private interface SkillOperation {

        void onOperate(LivingSkill livingSkill);
    }

    public interface SkillNotifyCallback {

        void onSuccess();

        void onRetrySoon();

        void onFailure(CallException e);
    }

    static class LivingSkill {

        Class<? extends MasterSkill> skillClass;
        String skillName;
        MasterSkill skill;
        boolean started;
        long startedAt;

        String state = MasterSkill.STATE_DEFAULT;
        long stateSettedAt;

        LivingSkill(
                Class<? extends MasterSkill> skillClass,
                String skillName,
                MasterSkill skill) {
            this.skillClass = skillClass;
            this.skillName = skillName;
            this.skill = skill;
        }

        void onSkillStart() {
            started = true;
            startedAt = System.currentTimeMillis();

            skill.onSkillStart();
        }

        void onSkillStop() {
            started = false;
            startedAt = 0;

            skill.onSkillStop();
        }

        boolean onSetState(String state) {
            if (this.state.equals(state)) {
                return false;
            }

            this.state = state;
            this.stateSettedAt = System.currentTimeMillis();
            return true;
        }

        public Class<? extends MasterSkill> getSkillClass() {
            return skillClass;
        }

        public MasterSkill getSkill() {
            return skill;
        }

        public String getState() {
            return state;
        }

        @Override
        public String toString() {
            return "LivingSkill{" +
                    "skillClass=" + skillClass +
                    ", started=" + started +
                    ", startedAt=" + startedAt +
                    ", state='" + state + '\'' +
                    ", stateSettedAt=" + stateSettedAt +
                    '}';
        }
    }
}