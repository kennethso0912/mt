package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 事件订阅者
 */
public interface Subscriber {

    /**
     * 订阅事件
     *
     * @param receiver 事件接收者
     * @param action   事件类型
     */
    void subscribe(EventReceiver receiver, String action);

    /**
     * 取消订阅事件
     *
     * @param receiver 事件接收者
     */
    void unsubscribe(EventReceiver receiver);
}