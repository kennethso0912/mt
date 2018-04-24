package com.ubtrobot.master.concurrent;

import com.ubtrobot.concurrent.AbstractEventLoopGroup;
import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.concurrent.impl.HandlerThreadEventLoop;

import java.util.Iterator;

/**
 * Created by column on 17-9-26.
 */

// TODO
public class MasterEventLoopGroup extends AbstractEventLoopGroup {

    private final EventLoop mCommonEventLoop;
    private final EventLoop mManagerEventLoop;

    public MasterEventLoopGroup() {
        mCommonEventLoop = new HandlerThreadEventLoop();
        mManagerEventLoop = new HandlerThreadEventLoop();
    }

    @Override
    public EventLoop next() {
        return mCommonEventLoop;
    }

    public EventLoop managerEventLoop() {
        return mManagerEventLoop;
    }

    @Override
    public void quitSafely() {
        mCommonEventLoop.quitSafely();
        mManagerEventLoop.quitSafely();
    }

    @Override
    public Iterator<EventLoop> iterator() {
        // TODO
        return null;
    }
}
