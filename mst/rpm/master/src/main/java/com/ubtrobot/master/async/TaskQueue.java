package com.ubtrobot.master.async;

import com.ubtrobot.concurrent.EventLoop;

import java.util.LinkedList;

/**
 * Created by column on 17-12-2.
 */

public class TaskQueue {

    private final EventLoop mEventLoop;
    private final LinkedList<Task> mTasks = new LinkedList<>();
    private boolean mExecuting;

    public TaskQueue(EventLoop eventLoop) {
        this.mEventLoop = eventLoop;
    }

    public void addTask(final Task task) {
        runInLoop(new Runnable() {
            @Override
            public void run() {
                task.setTaskQueue(TaskQueue.this);
                mTasks.add(task);

                triggerNextTaskExcute();
            }
        });
    }

    private void runInLoop(final Runnable runnable) {
        if (mEventLoop.inEventLoop()) {
            runnable.run();
        } else {
            mEventLoop.post(runnable);
        }
    }

    private void triggerNextTaskExcute() {
        runInLoop(new Runnable() {
            @Override
            public void run() {
                if (mExecuting || mTasks.isEmpty()) {
                    return;
                }

                Task task = mTasks.remove(0);

                mExecuting = true;
                task.execute();
            }
        });
    }

    public void addTaskFirst(final Task task) {
        runInLoop(new Runnable() {
            @Override
            public void run() {
                task.setTaskQueue(TaskQueue.this);
                mTasks.add(0, task);

                triggerNextTaskExcute();
            }
        });
    }

    public abstract static class Task {

        private TaskQueue mTaskQueue;

        void setTaskQueue(TaskQueue queue) {
            mTaskQueue = queue;
        }

        protected abstract void execute();

        public final void notifyComplete() {
            mTaskQueue.runInLoop(new Runnable() {
                @Override
                public void run() {
                    mTaskQueue.mExecuting = false;

                    mTaskQueue.triggerNextTaskExcute();
                }
            });
        }
    }
}
