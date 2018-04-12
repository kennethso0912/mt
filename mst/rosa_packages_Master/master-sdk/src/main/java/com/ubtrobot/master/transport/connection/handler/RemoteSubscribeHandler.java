package com.ubtrobot.master.transport.connection.handler;

import android.os.Bundle;

import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.event.LocalSubscriber;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.MasterCallPaths;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.IncomingHandlerAdapter;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by column on 17-8-31.
 */

public class RemoteSubscribeHandler extends IncomingHandlerAdapter {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("RemoteSubscribeHandler");

    private final LocalSubscriber mSubscriberInternal;
    private final LocalSubscriber mSubscriberForSdkUser;

    private final CallMasterCallable mCallMasterCallable;

    private boolean mConnected;

    public RemoteSubscribeHandler(
            LocalSubscriber subscriberInternal,
            LocalSubscriber subscriberForSdkUser,
            CallMasterCallable callMasterCallable) {
        mSubscriberInternal = subscriberInternal;
        mSubscriberInternal.setActionChangeListener(new SubscribedActionChangeListener(true));

        mSubscriberForSdkUser = subscriberForSdkUser;
        mSubscriberForSdkUser.setActionChangeListener(new SubscribedActionChangeListener(false));

        mCallMasterCallable = callMasterCallable;
    }

    @Override
    public void onConnected(HandlerContext context) {
        synchronized (this) {
            mConnected = true;

            List<String> actions = mSubscriberInternal.getSubscribedActions();
            if (!actions.isEmpty()) {
                subscribeLocked(mSubscriberInternal.getSubscribedActions(), true);
            }

            actions = mSubscriberForSdkUser.getSubscribedActions();
            if (!actions.isEmpty()) {
                subscribeLocked(mSubscriberForSdkUser.getSubscribedActions(), false);
            }

            context.onConnected();
        }
    }

    private void subscribeLocked(final List<String> actions, boolean isInternal) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ParamBundleConstants.KEY_IS_INTERNAL_EVENT, isInternal);
        bundle.putStringArrayList(ParamBundleConstants.KEY_EVENT_ACTIONS, new ArrayList<>(actions));

        mCallMasterCallable.call(MasterCallPaths.PATH_SUBSCRIBE_EVENT,
                ParcelableParam.create(bundle),
                new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        LOGGER.i("Subscribe event to remote success. actions=%s", actions);
                    }

                    @Override
                    public void onFailure(Request req, CallException e) {
                        LOGGER.e(e, "Subscribe event to remote failed. actions=%s", actions);
                    }
                }
        );
    }

    @Override
    public void onDisconnected(HandlerContext context) {
        synchronized (this) {
            mConnected = false;

            context.onDisconnected();
        }
    }

    private class SubscribedActionChangeListener implements LocalSubscriber.ActionChangeListener {

        boolean isInternal;

        SubscribedActionChangeListener(boolean isInternal) {
            this.isInternal = isInternal;
        }

        @Override
        public void onAdd(final List<String> actions) {
            synchronized (RemoteSubscribeHandler.this) {
                if (mConnected) {
                    subscribeLocked(actions, isInternal);
                }

                // mConnected == false 时，在 onConnected 回调中处理
            }
        }

        @Override
        public void onRemove(List<String> actions) {
            synchronized (RemoteSubscribeHandler.this) {
                unsubscribeLocked(actions, isInternal);
            }
        }

        private void unsubscribeLocked(final List<String> actions, boolean isInternal) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ParamBundleConstants.KEY_IS_INTERNAL_EVENT, isInternal);
            bundle.putStringArrayList(ParamBundleConstants.KEY_EVENT_ACTIONS, new ArrayList<>(actions));

            mCallMasterCallable.call(MasterCallPaths.PATH_UNSUBSCRIBE_EVENT,
                    ParcelableParam.create(bundle),
                    new ResponseCallback() {
                        @Override
                        public void onResponse(Request req, Response res) {
                            LOGGER.i("Unsubscribe event to remote success. actions=%s", actions);
                        }

                        @Override
                        public void onFailure(Request req, CallException e) {
                            LOGGER.e(e, "Unsubscribe event to remote failed. actions=%s", actions);
                        }
                    }
            );
        }
    }
}
