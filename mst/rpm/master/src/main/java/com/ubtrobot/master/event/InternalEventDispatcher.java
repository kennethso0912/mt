package com.ubtrobot.master.event;

import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelEvent;
import com.ubtrobot.master.transport.message.parcel.ParcelEventConfig;
import com.ubtrobot.transport.message.Param;

/**
 * Created by zhu on 18-1-11.
 */

public class InternalEventDispatcher extends EventDispatcher {

    public void publish(String action) {
        publish(new ParcelEvent(new ParcelEventConfig(false, true), action));
    }

    public void publish(String action, Param param) {
        publish(new ParcelEvent(new ParcelEventConfig(false, true), action, (AbstractParam) param));
    }
}