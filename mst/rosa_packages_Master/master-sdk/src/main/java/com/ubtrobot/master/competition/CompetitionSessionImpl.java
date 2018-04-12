package com.ubtrobot.master.competition;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.context.GlobalContext;
import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.event.EventReceiverContext;
import com.ubtrobot.master.event.LocalSubscriber;
import com.ubtrobot.master.interactor.MasterInteractor;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.MasterService;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.service.ServiceProxyImpl;
import com.ubtrobot.master.skill.MasterSkill;
import com.ubtrobot.master.transport.message.MasterCallPaths;
import com.ubtrobot.master.transport.message.MasterEvents;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.EventReceiver;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhu on 18-1-31.
 */
public class CompetitionSessionImpl implements CompetitionSession {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("CompetitionSessionImpl");

    private static final int STATE_INACTIVE = 0;
    private static final int STATE_ACTIVE = 1;
    private static final int STATE_ACTIVATING = 2;
    private static final int STATE_INTERRUPTED = 3;

    private final MasterContext mMasterContext;

    private final Unsafe mUnsafe;
    private final CallMasterCallable mCallMasterCallable;
    private final LocalSubscriber mSubscriberInternal;

    private String mSessionId;
    private InterruptEventReceiver mInterruptEventReceiver = new InterruptEventReceiver();
    private final LinkedList<Competing> mCompetings = new LinkedList<>();
    private final LinkedList<InterruptionListener> mListeners = new LinkedList<>();

    private int mState = STATE_INACTIVE;
    private final Map<String, List<ActivateCallback>> mActivating = new HashMap<>();
    private ActivateOption mActivateOption;

    public CompetitionSessionImpl(MasterContext context, Unsafe unsafe) {
        mMasterContext = context;
        mUnsafe = unsafe;
        mCallMasterCallable = unsafe.getGlobalCallMasterCallable();
        mSubscriberInternal = unsafe.getSubscriberInternal();

        mActivateOption = new ActivateOption.Builder().build();
    }

    @Override
    public MasterContext getContext() {
        return mMasterContext;
    }

    @Override
    public CompetitionSession addCompeting(Competing competing) {
        if (competing == null) {
            throw new IllegalArgumentException("Argument competing is null.");
        }

        synchronized (this) {
            if (mState != STATE_INACTIVE) {
                throw new IllegalStateException("Can NOT add competing when session is " +
                        "active or interrupted or activating.");
            }

            if (containsCompetingRefLocked(competing)) {
                throw new IllegalStateException(competing.getClass() +
                        " has been added to the session.");
            }

            mCompetings.add(competing);
        }

        return this;
    }

    @Override
    public boolean containsCompeting(Competing competing) {
        synchronized (this) {
            return containsCompetingRefLocked(competing);
        }
    }

    @Override
    public CompetitionSession removeCompeting(Competing competing) {
        if (competing == null) {
            throw new IllegalArgumentException("Argument competing is null.");
        }

        synchronized (this) {
            if (mState != STATE_INACTIVE) {
                throw new IllegalStateException("Can NOT remove competing when session is " +
                        "active or interrupted or activating.");
            }

            Iterator<Competing> iterator = mCompetings.iterator();
            while (iterator.hasNext()) {
                Competing aCompeting = iterator.next();
                if (aCompeting == competing) {
                    iterator.remove();
                }
            }
        }

        return this;
    }

    private boolean containsCompetingRefLocked(Competing competing) {
        for (Competing aCompeting : mCompetings) {
            if (aCompeting == competing) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void registerInterruptionListener(InterruptionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Argument listener is null.");
        }

        synchronized (this) {
            if (mListeners.contains(listener)) {
                throw new IllegalStateException("The listener has been registered.");
            }

            mListeners.add(listener);
        }
    }

    @Override
    public void unregisterInterruptionListener(InterruptionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Argument listener is null.");
        }

        synchronized (this) {
            mListeners.remove(listener);
        }
    }

    @Override
    public CompetitionSession setActivateOption(ActivateOption option) {
        if (option == null) {
            throw new IllegalArgumentException("Argument option is null.");
        }

        synchronized (this) {
            if (mState != STATE_INACTIVE) {
                throw new IllegalStateException("Can NOT set activate operation when session is " +
                        "active or interrupted or activating.");
            }

            mActivateOption = option;
        }

        return this;
    }

    @Override
    public void activate(final ActivateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Argument callback is null.");
        }

        synchronized (this) {
            if (mState == STATE_ACTIVE) {
                throw new IllegalStateException("Already active.");
            }

            if (mState == STATE_ACTIVATING) {
                getActivatingCallbacksLocked().add(callback);
                return;
            }

            if (mState == STATE_INTERRUPTED) {
                doDeactivateLocked(true);
            }
            mState = STATE_ACTIVATING;

            final String activatingId = System.nanoTime() + "";
            LinkedList<ActivateCallback> callbackList = new LinkedList<>();
            callbackList.add(callback);
            mActivating.put(activatingId, callbackList);

            doActivate(new ActivateCallback() {
                @Override
                public void onSuccess(String sessionId) {
                    synchronized (CompetitionSessionImpl.this) {
                        List<ActivateCallback> callbacks = mActivating.remove(activatingId);
                        if (callbacks == null) {
                            doDeactivate(sessionId);
                            return;
                        }

                        mSessionId = sessionId;
                        mState = STATE_ACTIVE;

                        subscribeInterruptionLocked(mSessionId);
                        // TODO 查询1次

                        for (ActivateCallback activateCallback : callbacks) {
                            activateCallback.onSuccess(mSessionId);
                        }
                    }
                }

                @Override
                public void onFailure(ActivateException e) {
                    synchronized (CompetitionSessionImpl.this) {
                        List<ActivateCallback> callbacks = mActivating.remove(activatingId);
                        if (callbacks == null) {
                            return;
                        }

                        mState = STATE_INACTIVE;
                        for (ActivateCallback activateCallback : callbacks) {
                            activateCallback.onFailure(e);
                        }
                    }
                }
            });
        }
    }

    private List<ActivateCallback> getActivatingCallbacksLocked() {
        if (mActivating.size() != 1) {
            throw new AssertionError("Should have one and the only one callback list.");
        }

        for (List<ActivateCallback> activateCallbacks : mActivating.values()) {
            return activateCallbacks;
        }

        throw new IllegalStateException("Impossible here.");
    }

    private void doDeactivateLocked(boolean toRemote) {
        if (toRemote) {
            doDeactivate(mSessionId);
        }
        mSessionId = null;
        unsubscribeInterruptionLocked();
    }

    private void unsubscribeInterruptionLocked() {
        mSubscriberInternal.unsubscribe(mInterruptEventReceiver);
    }

    private void doDeactivate(final String sessionId) {
        Bundle bundle = new Bundle();
        bundle.putString(ParamBundleConstants.KEY_COMPETING_SESSION_ID, sessionId);
        mCallMasterCallable.call(MasterCallPaths.PATH_DEACTIVATE_COMPETING_SESSION,
                ParcelableParam.create(bundle), new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        // Ignore
                    }

                    @Override
                    public void onFailure(Request req, CallException e) {
                        LOGGER.e("Deactivate session from remote master., sessionId=%s", sessionId);
                    }
                });
    }

    private void doActivate(final ActivateCallback callback) {
        CompetitionSessionInfo.Builder sessionInfoBuilder = new CompetitionSessionInfo.Builder();
        for (Competing competing : mCompetings) {
            sessionInfoBuilder.addCompetingItemAll(competing.getCompetingItems());
        }

        ResponseCallback responseCallback = new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                String sessionId = null;
                ActivateException ae = null;
                try {
                    sessionId = ParcelableParam.from(res.getParam(), Bundle.class).
                            getParcelable().getString(
                            ParamBundleConstants.KEY_COMPETING_SESSION_ID);
                    if (TextUtils.isEmpty(sessionId)) {
                        ae = new ActivateException(); // TODO
                    }
                } catch (ParcelableParam.InvalidParcelableParamException e) {
                    ae = new ActivateException(); // TODO
                }

                final String finalSessionId = sessionId;
                final ActivateException finalAe = ae;
                mUnsafe.getHandlerOnMainThread().post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalAe == null) {
                            callback.onSuccess(finalSessionId);
                        } else {
                            callback.onFailure(finalAe);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Request req, CallException e) {
                mUnsafe.getHandlerOnMainThread().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(new ActivateException()); // TODO
                    }
                });
            }
        };

        mCallMasterCallable.call(
                MasterCallPaths.PATH_ACTIVATE_COMPETING_SESSION,
                ParcelableParam.create(
                        new ActivateParam(mActivateOption, sessionInfoBuilder.build())),
                responseCallback
        );
    }

    private void subscribeInterruptionLocked(String sessionId) {
        mSubscriberInternal.subscribe(mInterruptEventReceiver,
                MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_BEGAN + sessionId);
        mSubscriberInternal.subscribe(mInterruptEventReceiver,
                MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_ENDED + sessionId);
    }

    @Override
    public boolean isActive() {
        synchronized (this) {
            return mState == STATE_ACTIVE;
        }
    }

    @Override
    public boolean isInterrupted() {
        synchronized (this) {
            return mState == STATE_INTERRUPTED;
        }
    }

    private void checkActive() {
        if (!isActive()) {
            throw new IllegalStateException("Session is activating or NOT active.");
        }
    }

    @Override
    public String getSessionId() {
        synchronized (this) {
            checkActive();
            return mSessionId;
        }
    }

    @Override
    public ServiceProxy createSystemServiceProxy(String serviceName) {
        synchronized (this) {
            checkActive();
            Pair<String, String> context = resolveRequestContext();
            return new ServiceProxyImpl(
                    mUnsafe.getGlobalCallMasterCallable(),
                    mUnsafe.getIpcByMasterCallable(),
                    mUnsafe.getHandlerOnMainThread(),
                    new ParcelRequestContext.Builder(
                            ParcelRequestContext.RESPONDER_TYPE_SERVICE, serviceName).
                            setCompetingSession(mSessionId).
                            setRequesterType(context.first).
                            setRequester(context.second).
                            build()
            );
        }
    }

    @Override
    public ServiceProxy createServiceProxy(String packageName, String serviceName) {
        synchronized (this) {
            checkActive();

            Pair<String, String> context = resolveRequestContext();
            return new ServiceProxyImpl(
                    mUnsafe.getGlobalCallMasterCallable(),
                    mUnsafe.getIpcByMasterCallable(),
                    mUnsafe.getHandlerOnMainThread(),
                    new ParcelRequestContext.Builder(
                            ParcelRequestContext.RESPONDER_TYPE_SERVICE, serviceName).
                            setResponderPackage(packageName).
                            setRequesterType(context.first).
                            setRequester(context.second).
                            setCompetingSession(mSessionId).
                            build()
            );
        }
    }

    private Pair<String, String> resolveRequestContext() {
        if (mMasterContext instanceof MasterSkill) {
            return new Pair<>(ParcelRequestContext.REQUESTER_TYPE_SKILL,
                    ((MasterSkill) mMasterContext).getName());
        }

        if (mMasterContext instanceof MasterService) {
            return new Pair<>(ParcelRequestContext.REQUESTER_TYPE_SERVICE,
                    ((MasterService) mMasterContext).getName());
        }

        if (mMasterContext instanceof MasterInteractor) {
            return new Pair<>(ParcelRequestContext.REQUESTER_TYPE_INTERACTOR, null);
        }

        if (mMasterContext instanceof EventReceiverContext) {
            return new Pair<>(ParcelRequestContext.REQUESTER_TYPE_EVENT_RECEIVER, null);
        }

        if (mMasterContext instanceof GlobalContext) {
            return new Pair<>(ParcelRequestContext.REQUESTER_TYPE_GLOBAL_CONTEXT, null);
        }

        throw new IllegalStateException("Internal error. Unknown context type.");
    }

    @Override
    public void deactivate() {
        synchronized (this) {
            if (mState == STATE_INACTIVE) {
                return;
            }

            int previousState = mState;
            mState = STATE_INACTIVE;

            if (previousState == STATE_ACTIVE || previousState == STATE_INTERRUPTED) {
                doDeactivateLocked(true);
                return;
            }

            final ActivateException ae = new ActivateException(); // TODO
            for (List<ActivateCallback> activateCallbacks : mActivating.values()) {
                for (final ActivateCallback activateCallback : activateCallbacks) {
                    mUnsafe.getHandlerOnMainThread().post(new Runnable() {
                        @Override
                        public void run() {
                            activateCallback.onFailure(ae);
                        }
                    });
                }
            }
            mActivating.clear();
        }
    }

    private class InterruptEventReceiver implements EventReceiver {

        @Override
        public void onReceive(Event event) {
            synchronized (CompetitionSessionImpl.this) {
                if (mState == STATE_ACTIVE &&
                        (MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_BEGAN + mSessionId).
                                equals(event.getAction())) {
                    mState = STATE_INTERRUPTED;

                    for (InterruptionListener listener : mListeners) {
                        listener.onBegan(CompetitionSessionImpl.this);
                    }
                    return;
                }

                if (mState == STATE_INTERRUPTED &&
                        (MasterEvents.ACTION_PREFIX_SESSION_INTERRUPTION_ENDED + mSessionId).
                                equals(event.getAction())) {
                    try {
                        boolean resumed = ParcelableParam.from(event.getParam(), Bundle.class).
                                getParcelable().getBoolean(ParamBundleConstants.
                                KEY_COMPETING_SESSION_RESUMED);

                        if (resumed) {
                            mState = STATE_ACTIVE;
                        } else {
                            mState = STATE_INACTIVE;
                            doDeactivateLocked(false);
                        }
                        // TODO 跟激活过程是否有冲突？

                        for (InterruptionListener listener : mListeners) {
                            listener.onEnded(CompetitionSessionImpl.this, resumed);
                        }
                    } catch (ParcelableParam.InvalidParcelableParamException e) {
                        LOGGER.e(e, "Receive illegal event. action=%s", event.getAction());
                    }
                }
            }
        }
    }
}
