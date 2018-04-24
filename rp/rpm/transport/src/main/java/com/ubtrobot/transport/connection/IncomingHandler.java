package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

/**
 * 基于连接的输入操作
 */
public interface IncomingHandler extends ConnectionHandler {

    void onConnected(HandlerContext context);

    void onDisconnected(HandlerContext context);

    void onRead(HandlerContext context, Object message);
}