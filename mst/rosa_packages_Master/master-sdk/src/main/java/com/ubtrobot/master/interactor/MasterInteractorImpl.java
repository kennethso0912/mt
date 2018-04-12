package com.ubtrobot.master.interactor;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.context.BaseContext;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.event.LocalSubscriber;
import com.ubtrobot.master.event.SubscriberAdapter;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillInfoList;
import com.ubtrobot.master.skill.SkillsProxy;
import com.ubtrobot.master.skill.SkillsProxyImpl;
import com.ubtrobot.master.transport.message.MasterCallPaths;
import com.ubtrobot.master.transport.message.MasterEvents;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.ulog.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by column on 17-12-6.
 */

public class MasterInteractorImpl extends BaseContext implements MasterInteractor {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterInteractorImpl");

    private final Unsafe mUnsafe;
    private final InteractorManager mManager;
    private final LocalSubscriber mSubscriberInternal;
    private final SubscriberAdapter mSubscriberDelegate;
    private final CallMasterCallable mCallMasterCallable;

    private final SkillStartedReceiver mSkillStartedReceiver = new SkillStartedReceiver();
    private final SkillStoppedReceiver mSkillStoppedReceiver = new SkillStoppedReceiver();
    private final LinkedList<SkillLifecycleCallbacks> mSkillLifecycleCallbacks = new LinkedList<>();

    private final LinkedList<SkillInfo> mStarted = new LinkedList<>();
    private List<SkillInfo> mUnmodifiableStarted = Collections.unmodifiableList(mStarted);
    private boolean mStartedValid;

    public MasterInteractorImpl(InteractorManager manager, Unsafe unsafe) {
        super(unsafe, ParcelRequestContext.REQUESTER_TYPE_INTERACTOR, null);
        mUnsafe = unsafe;
        mManager = manager;

        mSubscriberInternal = mUnsafe.getSubscriberInternal();
        mSubscriberDelegate = new SubscriberAdapter(this, unsafe.getSubscriberForSdkUser(),
                unsafe.getHandlerOnMainThread());
        mCallMasterCallable = mUnsafe.getGlobalCallMasterCallable();

        mSubscriberInternal.subscribe(mSkillStartedReceiver, MasterEvents.ACTION_SKILL_STARTED);
        mSubscriberInternal.subscribe(mSkillStoppedReceiver, MasterEvents.ACTION_SKILL_STOPPED);
    }

    @Override
    public void subscribe(EventReceiver receiver, String action) {
        // TODO 根据有效性的判断
        mSubscriberDelegate.subscribe(receiver, action);
    }

    @Override
    public void unsubscribe(EventReceiver receiver) {
        mSubscriberDelegate.unsubscribe(receiver);
    }

    @Override
    public SkillsProxy createSkillsProxy() {
        return new SkillsProxyImpl(
                mUnsafe.getIpcByMasterCallable(),
                mUnsafe.getHandlerOnMainThread(),
                new ParcelRequestContext.Builder(ParcelRequestContext.RESPONDER_TYPE_SKILLS).
                        setRequesterType(ParcelRequestContext.REQUESTER_TYPE_INTERACTOR).build()
        );
    }

    @Override
    public List<SkillInfo> getStartedSkills() {
        synchronized (mStarted) {
            if (mStartedValid) {
                return mUnmodifiableStarted;
            }

            try {
                Response response = mCallMasterCallable.call(MasterCallPaths.PATH_GET_STARTED_SKILLS);
                SkillInfoList skillInfoList = ParcelableParam.from(
                        response.getParam(), SkillInfoList.class).getParcelable();

                mStarted.clear();
                mStarted.addAll(skillInfoList.getSkillInfoList());

                mUnmodifiableStarted = Collections.unmodifiableList(mStarted);
                mStartedValid = true;
            } catch (CallException e) {
                LOGGER.e(e, "Get started skills failed.");
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                LOGGER.e(e, "Get started skills failed.");
            }

            return mUnmodifiableStarted;
        }
    }

    public void invalidateStartedSkills() {
        // TODO 处理跟 Master 连接断开的情况
        synchronized (mStarted) {
            mStarted.clear();
            mUnmodifiableStarted = Collections.unmodifiableList(mStarted);

            mStartedValid = false;
        }
    }

    @Override
    public void registerSkillLifecycleCallbacks(SkillLifecycleCallbacks callbacks) {
        synchronized (mSkillLifecycleCallbacks) {
            if (callbacks == null) {
                throw new IllegalArgumentException("Illegal callbacks argument which is null.");
            }

            if (mSkillLifecycleCallbacks.contains(callbacks)) {
                throw new IllegalStateException("Illegal state. The callbacks has been registered.");
            }

            mSkillLifecycleCallbacks.add(callbacks);
        }
    }

    @Override
    public void unregisterSkillLifecycleCallbacks(SkillLifecycleCallbacks callbacks) {
        synchronized (mSkillLifecycleCallbacks) {
            if (callbacks == null) {
                throw new IllegalArgumentException("Illegal callbacks argument which is null.");
            }

            mSkillLifecycleCallbacks.remove(callbacks);
        }
    }

    @Override
    public void dismiss() {
        mManager.removeInteractor(this);

        mSubscriberInternal.unsubscribe(mSkillStartedReceiver);
        mSubscriberInternal.unsubscribe(mSkillStoppedReceiver);

        synchronized (mSkillLifecycleCallbacks) {
            mSkillLifecycleCallbacks.clear();
        }
    }

    private class SkillStartedReceiver implements com.ubtrobot.transport.message.EventReceiver {

        @Override
        public void onReceive(Event event) {
            synchronized (mStarted) {
                try {
                    SkillInfo skillInfo = ParcelableParam.from(
                            event.getParam(), SkillInfo.class).getParcelable();
                    if (skillInfo == null) {
                        LOGGER.e("Illegal skill started event. param is null.");
                        return;
                    }

                    Iterator<SkillInfo> iterator = mStarted.iterator();
                    boolean contains = false;
                    while (iterator.hasNext()) {
                        SkillInfo previous = iterator.next();
                        if (skillInfoEquals(previous, skillInfo)) {
                            contains = true;
                            iterator.remove();
                            break;
                        }
                    }

                    mStarted.add(skillInfo);
                    mUnmodifiableStarted = Collections.unmodifiableList(mStarted);

                    if (!contains) {
                        notifySkillStarted(skillInfo);
                    }
                } catch (ParcelableParam.InvalidParcelableParamException e) {
                    LOGGER.e(e, "Illegal skill started event.");
                }
            }
        }

        private void notifySkillStarted(final SkillInfo skillInfo) {
            synchronized (mSkillLifecycleCallbacks) {
                for (final SkillLifecycleCallbacks skillLifecycleCallbacks : mSkillLifecycleCallbacks) {
                    mUnsafe.getHandlerOnMainThread().post(new Runnable() {
                        @Override
                        public void run() {
                            skillLifecycleCallbacks.onSkillStarted(skillInfo);
                        }
                    });
                }
            }
        }
    }

    private boolean skillInfoEquals(SkillInfo lhs, SkillInfo rhs) {
        return lhs.getPackageName().equals(rhs.getPackageName()) &&
                lhs.getClassName().equals(rhs.getClassName());
    }

    private class SkillStoppedReceiver implements com.ubtrobot.transport.message.EventReceiver {

        @Override
        public void onReceive(Event event) {
            synchronized (mStarted) {
                try {
                    SkillInfo skillInfo = ParcelableParam.from(
                            event.getParam(), SkillInfo.class).getParcelable();
                    if (skillInfo == null) {
                        LOGGER.e("Illegal skill started event. param is null.");
                        return;
                    }

                    Iterator<SkillInfo> iterator = mStarted.iterator();
                    boolean contains = false;
                    while (iterator.hasNext()) {
                        SkillInfo previous = iterator.next();
                        if (skillInfoEquals(previous, skillInfo)) {
                            iterator.remove();
                            contains = true;
                            break;
                        }
                    }

                    if (contains) {
                        mUnmodifiableStarted = Collections.unmodifiableList(mStarted);
                        notifySkillStopped(skillInfo);
                    }
                } catch (ParcelableParam.InvalidParcelableParamException e) {
                    LOGGER.e(e, "Illegal skill started event.");
                }
            }
        }

        private void notifySkillStopped(final SkillInfo skillInfo) {
            synchronized (mSkillLifecycleCallbacks) {
                for (final SkillLifecycleCallbacks skillLifecycleCallbacks : mSkillLifecycleCallbacks) {
                    mUnsafe.getHandlerOnMainThread().post(new Runnable() {
                        @Override
                        public void run() {
                            skillLifecycleCallbacks.onSkillStopped(skillInfo);
                        }
                    });
                }
            }
        }
    }
}