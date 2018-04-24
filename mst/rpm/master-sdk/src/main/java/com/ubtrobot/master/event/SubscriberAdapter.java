package com.ubtrobot.master.event;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;

import com.ubtrobot.master.context.MasterContext;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by column on 17-12-6.
 */

public class SubscriberAdapter {

    private final MasterContext mMasterContext;
    private final com.ubtrobot.transport.message.Subscriber mSubscriber;
    private final Handler mHandler;

    // 不能用 HashMap，EventReceiver hashcode 没法保证
    private final LinkedList<Pair<EventReceiver, EventReceiverAdapter>> mReceivers =
            new LinkedList<>();


    public SubscriberAdapter(
            MasterContext masterContext,
            com.ubtrobot.transport.message.Subscriber subscriber,
            Handler handler) {
        mMasterContext = masterContext;
        mSubscriber = subscriber;
        mHandler = handler;
    }

    public void subscribe(final EventReceiver receiver, String action) {
        if (receiver == null || TextUtils.isEmpty(action)) {
            throw new IllegalArgumentException(
                    "Argument receiver was null, or argument action was null or empty."
            );
        }

        synchronized (mReceivers) {
            EventReceiverAdapter adapter = getOrCreateEventReceiverAdapterLocked(receiver);
            mSubscriber.subscribe(adapter, action);
        }
    }

    private EventReceiverAdapter getOrCreateEventReceiverAdapterLocked(EventReceiver eventReceiver) {
        EventReceiverAdapter adapter = getEventReceiverAdapterLocked(eventReceiver);
        if (adapter == null) {
            adapter = new EventReceiverAdapter(mMasterContext, eventReceiver, mHandler);
            mReceivers.add(new Pair<>(eventReceiver, adapter));
        }

        return adapter;
    }

    private EventReceiverAdapter getEventReceiverAdapterLocked(EventReceiver eventReceiver) {
        for (Pair<EventReceiver, EventReceiverAdapter> receiver : mReceivers) {
            if (eventReceiver == receiver.first) {
                return receiver.second;
            }
        }

        return null;
    }

    public void unsubscribe(EventReceiver receiver) {
        if (receiver == null) {
            throw new IllegalArgumentException("Argument receiver was null.");
        }

        synchronized (mReceivers) {
            EventReceiverAdapter adapter = getEventReceiverAdapterLocked(receiver);
            if (adapter != null) {
                mSubscriber.unsubscribe(adapter);
            }
        }
    }

    public List<EventReceiver> unsubscribeAll() {
        synchronized (mReceivers) {
            LinkedList<EventReceiver> receivers = new LinkedList<>();
            for (Pair<EventReceiver, EventReceiverAdapter> aReceiver : mReceivers) {
                receivers.add(aReceiver.first);
                mSubscriber.unsubscribe(aReceiver.second);
            }

            return receivers;
        }
    }
}
