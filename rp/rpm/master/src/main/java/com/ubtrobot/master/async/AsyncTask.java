package com.ubtrobot.master.async;

public interface AsyncTask<E extends Exception> {

    void execute(AsyncTaskCallback<E> callback, Object... arguments);
}
