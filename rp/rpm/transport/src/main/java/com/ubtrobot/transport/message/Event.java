package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 事件
 */
public interface Event {

    /**
     * 获取事件 id
     *
     * @return 事件 id
     */
    String getId();

    /**
     * 获取事件类型
     *
     * @return 事件类型
     */
    String getAction();

    /**
     * 获取事件发布时间
     *
     * @return 事件发布时间
     */
    long getWhen();

    /**
     * 获取事件参数
     *
     * @return 事件参数
     */
    Param getParam();
}