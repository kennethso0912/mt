package com.ubtrobot.master.context;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.event.SubscriberAdapter;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;

/**
 * Created by column on 17-12-6.
 */

public class GlobalContext extends BaseContext {

    private final SubscriberAdapter mSubscriberDelegate;

    public GlobalContext(Unsafe unsafe) {
        super(unsafe, ParcelRequestContext.REQUESTER_TYPE_GLOBAL_CONTEXT, null);
        mSubscriberDelegate = new SubscriberAdapter(this, unsafe.getSubscriberForSdkUser(),
                unsafe.getHandlerOnMainThread());
    }

    @Override
    public void subscribe(final EventReceiver receiver, String action) {
        mSubscriberDelegate.subscribe(receiver, action);
    }

    @Override
    public void unsubscribe(EventReceiver receiver) {
        mSubscriberDelegate.unsubscribe(receiver);
    }
}