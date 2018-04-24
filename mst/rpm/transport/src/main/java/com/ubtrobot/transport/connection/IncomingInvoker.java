package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

/**
 * 连接输入操作通知接口
 */
public interface IncomingInvoker {

    /**
     * 通知连接已经建立
     */
    void onConnected();

    /**
     * 通知连接已建立
     */
    void onDisconnected();

    /**
     * 通知从连接中读取到数据消息
     *
     * @param message 消息
     */
    void onRead(Object message);
}