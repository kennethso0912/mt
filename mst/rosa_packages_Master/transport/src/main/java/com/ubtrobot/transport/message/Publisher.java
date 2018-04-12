package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 事件发布者
 */
public interface Publisher {

    /**
     * 事件发布者
     *
     * @param event 事件
     */
    void publish(Event event);
}