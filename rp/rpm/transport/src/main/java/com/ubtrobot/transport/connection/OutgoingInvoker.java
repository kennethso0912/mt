package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

/**
 * 连接输出操作接口
 */
public interface OutgoingInvoker {

    /**
     * 建立连接
     *
     * @param callback 建立连接结果回调
     */
    void connect(OutgoingCallback callback);

    /**
     * 断开连接
     *
     * @param callback 断开连接结果回调
     */
    void disconnect(OutgoingCallback callback);

    /**
     * 向连接写入数据消息
     *
     * @param message 消息
     * @param callback 写入结果回调
     */
    void write(Object message, OutgoingCallback callback);
}