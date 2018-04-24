package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

/**
 * 连接上操作的执行管道，实现类似拦截器的机制（责任链设计模式）。
 * 每个 ConnectionHandler 负责某个流程中的一个环节，Pipeline 进行衔接
 */
public interface Pipeline extends IncomingInvoker, OutgoingInvoker {

    /**
     * 添加连接处理器
     *
     * @param handler 处理器
     */
    void add(ConnectionHandler handler);
}