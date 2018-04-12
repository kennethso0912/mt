package com.ubtrobot.master.log;

import android.util.Log;

import com.ubtrobot.ulog.logger.dummy.DummyLogger;

/**
 * Created by column on 17-9-6.
 */

public class InfrequentLogger extends DummyLogger {

    private final String mTag;

    public InfrequentLogger(String tag) {
        mTag = tag;
    }

    @Override
    public void w(String msg, Object... args) {
        Log.w(mTag, String.format(msg, args));
    }

    @Override
    public void w(Throwable t, String msg, Object... args) {
        Log.w(mTag, String.format(msg, args), t);
    }

    @Override
    public void w(Throwable t) {
        Log.w(mTag, t);
    }

    @Override
    public void e(String msg, Object... args) {
        Log.e(mTag, String.format(msg, args));
    }

    @Override
    public void e(Throwable t, String msg, Object... args) {
        Log.e(mTag, String.format(msg, args), t);
    }

    @Override
    public void e(Throwable t) {
        Log.e(mTag, null, t);
    }

    @Override
    public void wtf(String msg, Object... args) {
        Log.wtf(mTag, String.format(msg, args));
    }

    @Override
    public void wtf(Throwable t, String msg, Object... args) {
        Log.wtf(mTag, String.format(msg, args), t);
    }

    @Override
    public void wtf(Throwable t) {
        Log.wtf(mTag, t);
    }
}
