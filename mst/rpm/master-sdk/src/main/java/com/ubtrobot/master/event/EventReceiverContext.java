package com.ubtrobot.master.event;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.context.BaseContext;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;

/**
 * Created by column on 17-12-6.
 */

public class EventReceiverContext extends BaseContext {

    public EventReceiverContext(Unsafe unsafe) {
        super(unsafe, ParcelRequestContext.REQUESTER_TYPE_EVENT_RECEIVER, null);
    }

    @Override
    public void subscribe(EventReceiver receiver, String action) {
        throw new UnsupportedOperationException("Can NOT subscribe event in the EventReceiver");
    }

    @Override
    public void unsubscribe(EventReceiver receiver) {
        throw new UnsupportedOperationException("Can NOT unsubscribe event in the EventReceiver");
    }
}