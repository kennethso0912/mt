package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

/**
 * 基于连接的输出操作
 */
public interface OutgoingHandler extends ConnectionHandler {

    void connect(HandlerContext context, OutgoingCallback callback);

    void disconnect(HandlerContext context, OutgoingCallback callback);

    void write(HandlerContext context, Object message, OutgoingCallback callback);
}