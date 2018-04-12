package com.ubtrobot.master.async;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ParallelFlow<E extends Exception> {

    private final LinkedList<AsyncTask<E>> mTasks = new LinkedList<>();
    private CompleteCallback<E> mOnComplete;
    private Results<E> mResults;

    private boolean mRunning;
    private int mCompleted;

    public synchronized ParallelFlow<E> add(AsyncTask<E> task) {
        checkNotRunningLocked();

        mTasks.add(task);
        return this;
    }

    private void checkNotRunningLocked() {
        if (mRunning) {
            throw new IllegalStateException("Task flow already started.");
        }
    }

    public synchronized ParallelFlow<E> onComplete(CompleteCallback<E> callback) {
        if (mOnComplete != null) {
            throw new IllegalStateException("Already set onFailure 's callback.");
        }

        mOnComplete = callback;
        return this;
    }

    public synchronized void start() {
        if (mTasks.isEmpty()) {
            throw new IllegalStateException("No task in flow. Add some tasks first.");
        }

        checkNotRunningLocked();

        mRunning = true;
        mCompleted = 0;
        mResults = new Results<>(mTasks.size());

        for (int i = 0; i < mTasks.size(); i++) {
            mTasks.get(i).execute(new TaskCallback<>(this, i));
        }
    }

    public interface CompleteCallback<E extends Exception> {

        void onComplete(Results<E> results);
    }

    public static class Result<E extends Exception> {

        private List<Object> values;
        private E exception;

        public Result(List<Object> values) {
            this(values, null);
        }

        private Result(E exception) {
            this(null, exception);
        }

        private Result(List<Object> values, E exception) {
            this.values = values == null ? Collections.emptyList() : values;
            this.exception = exception;
        }

        public List<Object> getValues() {
            return values;
        }

        public E getException() {
            return exception;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "isSuccess=" + isSuccess() +
                    ", values=" + values +
                    ", exception=" + exception +
                    '}';
        }
    }

    public static class Results<E extends Exception> implements Iterable<Result<E>> {

        private ArrayList<Result<E>> mList;

        private List<Integer> mSuccess;
        private List<Integer> mFailure;

        private Results(int initialCapacity) {
            mList = new ArrayList<>(initialCapacity);
            for (int i = 0; i < initialCapacity; i++) {
                mList.add(null);
            }

            mSuccess = new LinkedList<>();
            mFailure = new LinkedList<>();
        }

        public int size() {
            return mList.size();
        }

        public boolean isEmpty() {
            return mList.isEmpty();
        }

        public Result<E> get(int index) {
            return mList.get(index);
        }

        @NonNull
        @Override
        public Iterator<Result<E>> iterator() {
            return new Iterator<Result<E>>() {

                private final Iterator<Result<E>> iterator = mList.iterator();

                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Result<E> next() {
                    return iterator.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        void set(int index, Result<E> result) {
            mList.set(index, result);

            if (result.isSuccess()) {
                mSuccess.add(index);
            } else {
                mFailure.add(index);
            }
        }

        void complete() {
            mSuccess = Collections.unmodifiableList(mSuccess);
            mFailure = Collections.unmodifiableList(mFailure);
        }

        public List<Integer> success() {
            return mSuccess;
        }

        public List<Integer> failure() {
            return mFailure;
        }

        public boolean isAllSuccess() {
            return mFailure.isEmpty();
        }
    }

    private static class TaskCallback<E extends Exception> implements AsyncTaskCallback<E> {

        private final ParallelFlow<E> mFlow;
        private final int mIndex;

        private boolean mNotified;

        public TaskCallback(ParallelFlow<E> flow, int index) {
            mFlow = flow;
            mIndex = index;
        }

        @Override
        public synchronized void onSuccess(Object... values) {
            checkNotNotifiedLocked();
            mNotified = true;

            synchronized (mFlow) {
                mFlow.mCompleted++;
                mFlow.mResults.set(mIndex, new Result<E>(Arrays.asList(values)));
                notifyCompleteLocked();
            }
        }

        private void checkNotNotifiedLocked() {
            if (mNotified) {
                throw new IllegalStateException("Already callback onSuccess or onFailure.");
            }
        }

        private void notifyCompleteLocked() {
            if (mFlow.mCompleted >= mFlow.mResults.size()) {
                mFlow.mResults.complete();
                mFlow.mRunning = false;

                if (mFlow.mOnComplete != null) {
                    mFlow.mOnComplete.onComplete(mFlow.mResults);
                }
            }
        }

        @Override
        public synchronized void onFailure(E e) {
            checkNotNotifiedLocked();
            mNotified = true;

            synchronized (mFlow) {
                mFlow.mCompleted++;
                mFlow.mResults.set(mIndex, new Result<>(e));
                notifyCompleteLocked();
            }
        }
    }
}
