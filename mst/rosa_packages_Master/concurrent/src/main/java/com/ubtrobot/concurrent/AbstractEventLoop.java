package com.ubtrobot.concurrent;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by column on 17-8-3.
 */

public abstract class AbstractEventLoop implements EventLoop {

    private final EventLoopGroup mParent;
    private final Collection<EventLoop> mSelfCollection = Collections.<EventLoop>singleton(this);

    protected AbstractEventLoop() {
        this(null);
    }

    protected AbstractEventLoop(EventLoopGroup parent) {
        mParent = parent;
    }

    @Override
    public EventLoopGroup parent() {
        return mParent;
    }

    @Override
    public EventLoop next() {
        return this;
    }

    @Override
    public boolean inEventLoop() {
        return inEventLoop(Thread.currentThread());
    }

    @NonNull
    @Override
    public Iterator<EventLoop> iterator() {
        return mSelfCollection.iterator();
    }
}
