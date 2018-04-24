package com.ubtrobot.master.async;

public interface Callback<V, E extends Exception> {

    void onSuccess(V value);

    void onFailure(E e);
}