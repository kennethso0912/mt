package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.event.EventDispatcher;
import com.ubtrobot.master.event.StaticEventDispatcher;
import com.ubtrobot.master.transport.message.parcel.ParcelEvent;
import com.ubtrobot.transport.connection.HandlerContext;

/**
 * Created by column on 17-8-31.
 */

public class EventHandler extends MessageSplitter.EventHandler {

    private final EventDispatcher mEventDispatcher;
    private final StaticEventDispatcher mStaticEventDispatcher;

    public EventHandler(EventDispatcher dispatcher, StaticEventDispatcher staticDispatcher) {
        mEventDispatcher = dispatcher;
        mStaticEventDispatcher = staticDispatcher;
    }

    @Override
    public void onRead(HandlerContext context, ParcelEvent event) {
        mEventDispatcher.publish(event);

        if (event.getConfig().isCareful()) {
            mStaticEventDispatcher.publish(event);
        }

        context.onRead(event);
    }
}