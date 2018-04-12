package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

import com.ubtrobot.concurrent.EventLoop;

/**
 * 提供方法触发责任链上的 Handler 传递执行。
 * HandlerContext.onXxxx 触发输入操作的传递，HandlerContext.xxxx 出发输出操作的传递
 */
public interface HandlerContext extends IncomingInvoker, OutgoingInvoker {

    ConnectionHandler handler();

    EventLoop eventLoop();

    Connection connection();
}