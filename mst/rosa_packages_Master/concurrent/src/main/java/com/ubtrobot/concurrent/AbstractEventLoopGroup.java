package com.ubtrobot.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * Created by column on 17-8-3.
 */

public abstract class AbstractEventLoopGroup implements EventLoopGroup {

    @Override
    public void post(Runnable task) {
        next().post(task);
    }

    @Override
    public Cancelable postDelay(Runnable task, long delay, TimeUnit unit) {
        return next().postDelay(task, delay, unit);
    }
}