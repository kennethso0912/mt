package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.event.LocalSubscriber;
import com.ubtrobot.master.transport.message.parcel.ParcelEvent;
import com.ubtrobot.transport.connection.HandlerContext;

/**
 * Created by column on 17-8-31.
 */

public class EventHandler extends MessageSplitter.EventHandler {

    private final LocalSubscriber mSubscriberInternal;
    private final LocalSubscriber mSubscriberForSdkUser;

    public EventHandler(
            LocalSubscriber subscriberInternal,
            LocalSubscriber subscriberForSdkUser) {
        mSubscriberInternal = subscriberInternal;
        mSubscriberForSdkUser = subscriberForSdkUser;
    }

    @Override
    public void onRead(HandlerContext context, ParcelEvent event) {
        if (event.getConfig().isInternal()) {
            mSubscriberInternal.onReceive(event);
        } else {
            mSubscriberForSdkUser.onReceive(event);
        }

        context.onRead(event);
    }
}