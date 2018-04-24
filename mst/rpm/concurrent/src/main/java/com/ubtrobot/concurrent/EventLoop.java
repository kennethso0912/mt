package com.ubtrobot.concurrent;

/**
 * Created by column on 17-7-31.
 */

public interface EventLoop extends EventLoopGroup {

    @Override
    EventLoop next();

    EventLoopGroup parent();

    boolean inEventLoop();

    boolean inEventLoop(Thread thread);
}