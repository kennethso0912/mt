package com.ubtrobot.master.async;

import java.util.LinkedList;

public class SeriesFlow<V, E extends Exception> {

    private static final int INIT_TASK = -1;

    private LinkedList<AsyncTask<E>> mTasks = new LinkedList<>();
    private SuccessCallback<V> mOnSuccess;
    private FailureCallback<E> mOnFailure;

    private boolean mRunning;
    private int mCurrentTask = INIT_TASK;

    public synchronized SeriesFlow<V, E> add(AsyncTask<E> task) {
        checkNotRunningLocked();

        mTasks.add(task);
        return this;
    }

    private void checkNotRunningLocked() {
        if (mRunning) {
            throw new IllegalStateException("Task flow already started.");
        }
    }

    public synchronized SeriesFlow<V, E> onSuccess(SuccessCallback<V> callback) {
        if (mOnSuccess != null) {
            throw new IllegalStateException("Already set onSuccess 's callback.");
        }

        mOnSuccess = callback;
        return this;
    }

    public synchronized SeriesFlow<V, E> onFailure(FailureCallback<E> callback) {
        if (mOnFailure != null) {
            throw new IllegalStateException("Already set onFailure 's callback.");
        }

        mOnFailure = callback;
        return this;
    }

    public synchronized void start() {
        checkNotRunningLocked();
        mRunning = true;
        mCurrentTask = INIT_TASK;

        AsyncTask<E> nextTask = nextTask();
        if (nextTask == null) {
            throw new IllegalStateException("No task in flow. Add some tasks first.");
        }

        nextTask.execute(new TaskCallback<>(this));
    }

    private synchronized AsyncTask<E> nextTask() {
        if (mCurrentTask == mTasks.size() - 1) {
            return null;
        }

        return mTasks.get(++mCurrentTask);
    }

    private static class TaskCallback<V, E extends Exception> implements AsyncTaskCallback<E> {

        private final SeriesFlow<V, E> mFlow;
        private boolean mNotified;

        TaskCallback(SeriesFlow<V, E> flow) {
            mFlow = flow;
        }

        @Override
        public synchronized void onSuccess(Object... results) {
            checkNotNotifiedLocked();
            mNotified = true;

            synchronized (mFlow) {
                AsyncTask<E> nextTask = mFlow.nextTask();
                if (nextTask != null) {
                    nextTask.execute(new TaskCallback<>(mFlow), results);
                } else {
                    notifySuccessLocked(results);
                }
            }
        }

        private void checkNotNotifiedLocked() {
            if (mNotified) {
                throw new IllegalStateException("Already callback onSuccess or onFailure.");
            }
        }

        private void notifySuccessLocked(Object... values) {
            mFlow.mRunning = false;

            if (values != null && values.length > 1) {
                throw new IllegalStateException(
                        "The last task in the flow should callback onSuccess with one result.");
            }

            try {
                @SuppressWarnings("unchecked") V value = values == null ||
                        values.length == 0 ? null : (V) values[0];
                if (mFlow.mOnSuccess != null) {
                    mFlow.mOnSuccess.onSuccess(value);
                }
            } catch (ClassCastException e) {
                throw new IllegalStateException("The last task in the flow should callback " +
                        "onSuccess with the type which is same with the R generics parameter.");
            }
        }

        @Override
        public synchronized void onFailure(E e) {
            checkNotNotifiedLocked();
            mNotified = true;

            notifyFailure(e);
        }

        private void notifyFailure(E e) {
            synchronized (mFlow) {
                mFlow.mRunning = false;

                if (mFlow.mOnFailure != null) {
                    mFlow.mOnFailure.onFailure(e);
                }
            }
        }
    }

    public interface SuccessCallback<V> {

        void onSuccess(V value);
    }

    public interface FailureCallback<E extends Exception> {

        void onFailure(E e);
    }
}
