package com.ubtrobot.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * Created by column on 17-7-31.
 */

public interface EventLoopGroup extends Iterable<EventLoop> {

    EventLoop next();

    void post(Runnable task);

    Cancelable postDelay(Runnable task, long delay, TimeUnit unit);

    void quitSafely();
}