package com.ubtrobot.concurrent.impl;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.ubtrobot.concurrent.AbstractEventLoop;
import com.ubtrobot.concurrent.Cancelable;
import com.ubtrobot.concurrent.EventLoopGroup;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Created by column on 17-7-31.
 */

public class HandlerThreadEventLoop extends AbstractEventLoop {

    private static final int ST_NOT_STARTED = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_QUIT = 3;

    private final AtomicIntegerFieldUpdater<HandlerThreadEventLoop> mStateUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HandlerThreadEventLoop.class, "mState");

    private final HandlerThread mHandlerThread;
    private volatile Handler mHandler;
    private volatile Thread mThread;

    private volatile int mState = ST_NOT_STARTED;

    private final HashSet<TaskCancelable> mCancelables = new HashSet<>();

    public HandlerThreadEventLoop() {
        this(null);
    }

    public HandlerThreadEventLoop(EventLoopGroup parent) {
        super(parent);
        mHandlerThread = new HandlerThread("EventLoop");
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return mThread == thread;
    }

    @Override
    public void post(Runnable task) {
        postDelay(task, 0);
    }

    private void postDelay(Runnable task, long delayMillis) {
        if (mState >= ST_QUIT) {
            throw new RejectedExecutionException("event loop quit");
        }

        startThreadIfNecessary();

        if (mState == ST_STARTED && mHandler.postDelayed(task, delayMillis)) {
            return;
        }

        // 1. HandlerThread.start 之前已经 quit
        // 2. HandlerThread.start 之后，mHandler.postDelayed 之前 quit
        throw new RejectedExecutionException("event loop quit");
    }

    private void startThreadIfNecessary() {
        if (mState == ST_NOT_STARTED) {
            synchronized (mHandlerThread) {
                if (mState == ST_NOT_STARTED) {
                    mHandlerThread.start();

                    mHandler = new Handler(mHandlerThread.getLooper());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mThread = Thread.currentThread();
                        }
                    });

                    mStateUpdater.compareAndSet(this, ST_NOT_STARTED, ST_STARTED);
                }
            }
        }
    }

    @Override
    public Cancelable postDelay(final Runnable task, long delay, TimeUnit unit) {
        Cancelable cancelable = newTaskCancelable(task); // 保证在 postDelay 之前
        postDelay(task, unit.toMillis(delay));
        return cancelable;
    }

    @Override
    public void quitSafely() {
        synchronized (mHandlerThread) {
            if (mState == ST_QUIT) {
                return;
            }

            if (mStateUpdater.compareAndSet(this, ST_NOT_STARTED, ST_QUIT)) {
                return;
            }

            if (mStateUpdater.compareAndSet(this, ST_STARTED, ST_QUIT)) {
                handlerThreadQuitSafely();

                synchronized (mCancelables) {
                    Iterator<TaskCancelable> iterator = mCancelables.iterator();
                    while (iterator.hasNext()) {
                        mHandler.removeCallbacks(iterator.next().task);
                        iterator.remove();
                    }
                }
            }
        }
    }

    private boolean hasTask() {
        return mHandler.hasMessages(new Message().what);
    }

    private void handlerThreadQuitSafely() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (hasTask()) {
                    mHandler.post(this);
                    return;
                }

                mHandlerThread.quit();
            }
        });
    }

    private TaskCancelable newTaskCancelable(Runnable task) {
        TaskCancelable cancelable = new TaskCancelable(task);
        synchronized (mCancelables) {
            mCancelables.add(cancelable);
        }
        return cancelable;
    }

    private class TaskCancelable implements Cancelable {

        Runnable task;

        public TaskCancelable(Runnable task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            mHandler.removeCallbacks(task);

            synchronized (mCancelables) {
                mCancelables.remove(this);
            }
        }
    }
}