package com.ubtrobot.master.event;

import android.os.Handler;

import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.transport.message.Event;

/**
 * Created by column on 17-12-6.
 */

public class EventReceiverAdapter implements com.ubtrobot.transport.message.EventReceiver {

    private final MasterContext mMasterContext;
    private final EventReceiver mEventReceiver;
    private final Handler mHandler;

    public EventReceiverAdapter(
            MasterContext masterContext,
            EventReceiver eventReceiver,
            Handler handler) {
        mMasterContext = masterContext;
        mEventReceiver = eventReceiver;
        mHandler = handler;
    }

    @Override
    public void onReceive(final Event event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mEventReceiver.onReceive(mMasterContext, event);
            }
        });
    }
}