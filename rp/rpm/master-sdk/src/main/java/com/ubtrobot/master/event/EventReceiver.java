package com.ubtrobot.master.event;

import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.transport.message.Event;

/**
 * Created by column on 17-12-5.
 */

public interface EventReceiver {

    void onReceive(MasterContext context, Event event);
}