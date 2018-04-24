package com.ubtrobot.master.service;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.transport.message.Param;

import java.util.List;

/**
 * Created by column on 17-12-6.
 */

public abstract class MasterSystemService extends MasterService {

    private final Unsafe mUnsafe;

    public MasterSystemService() {
        mUnsafe = Unsafe.get();
    }

    public void publishCarefully(String action) {
        mUnsafe.getPublisher().publishCarefully(action);
    }

    public void publishCarefully(String action, Param param) {
        mUnsafe.getPublisher().publishCarefully(action, param);
    }

    public void addState(String state) {
        mUnsafe.getServiceLifecycle().addState(getClass(), state);
    }

    public void removeState(String state) {
        mUnsafe.getServiceLifecycle().removeState(getClass(), state);
    }

    public List<String> getStates() {
        return mUnsafe.getServiceLifecycle().getStates(getClass());
    }

    public boolean didAddState(String state) {
        return mUnsafe.getServiceLifecycle().didAddState(getClass(), state);
    }
}