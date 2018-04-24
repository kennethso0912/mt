package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 事件接收者
 */
public interface EventReceiver {

    /**
     * 事件接收回调
     *
     * @param event 事件
     */
    void onReceive(Event event);
}
