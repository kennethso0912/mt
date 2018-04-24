package com.ubtrobot.transport.connection;

import com.ubtrobot.concurrent.EventLoop;

import java.util.Map;

/**
 * Created by column on 17-8-25.
 */

/**
 * 两个端联系到一起进行通信的通道的抽象
 */
public interface Connection extends OutgoingInvoker {

    /**
     * 获取连接的标识
     *
     * @return 连接标识
     */
    ConnectionId id();

    /**
     * 获取连接关联事件循环，基于该连接的所有（IO）操作均在该事件循环中执行
     *
     * @return 关联的事件循环
     */
    EventLoop eventLoop();

    /**
     * 获取连接关联的管道，管道用户链式处理（IO）操作
     *
     * @return 关联的管道
     */
    Pipeline pipeline();

    /**
     * 获取是否已连接
     *
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 连接相关的自定义属性
     *
     * @return 自定义属性
     */
    Map<String, Object> attributes();

    /**
     * 获取连接关联的不安全操作。外部调用不应该直接调用，用于内部实现，固取名 Unsafe 从而从名字上提示。
     *
     * @return 连接关联的不安全操作
     */
    Unsafe unsafe();

    /**
     * 连接不安全操作，是真正实现连接 IO 操作的地方。Connection 中各个 IO 操作方法不实际执行 IO 操作，而是交由管道链式处理
     */
    interface Unsafe extends OutgoingInvoker {
    }
}