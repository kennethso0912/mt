package com.ubtrobot.master.async;

public interface AsyncTaskCallback<E extends Exception> {

    void onSuccess(Object... values);

    void onFailure(E e);
}
