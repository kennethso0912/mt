package com.ubtrobot.master.interactor;

import com.ubtrobot.master.Unsafe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-12-19.
 */

public class InteractorManager {

    private final Unsafe mUnsafe;

    private final HashMap<String, MasterInteractor> mInteractors = new HashMap<>();
    private final ReentrantReadWriteLock mInteractorsLock = new ReentrantReadWriteLock();

    public InteractorManager(Unsafe unsafe) {
        mUnsafe = unsafe;
    }

    public MasterInteractor getOrCreateInteractor(String name) {
        MasterInteractor interactor;

        mInteractorsLock.readLock().lock();
        try {
            interactor = mInteractors.get(name);
            if (interactor != null) {
                return interactor;
            }
        } finally {
            mInteractorsLock.readLock().unlock();
        }

        mInteractorsLock.writeLock().lock();
        try {
            interactor = mInteractors.get(name);
            if (interactor != null) {
                return interactor;
            }

            interactor = new MasterInteractorImpl(this, mUnsafe);
            mInteractors.put(name, interactor);

            return interactor;
        } finally {
            mInteractorsLock.writeLock().unlock();
        }
    }

    void removeInteractor(MasterInteractor interactor) {
        mInteractorsLock.writeLock().lock();
        try {
            Iterator<Map.Entry<String, MasterInteractor>> iterator = mInteractors.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, MasterInteractor> entry = iterator.next();
                if (entry.getValue() == interactor) {
                    iterator.remove();
                }
            }
        } finally {
            mInteractorsLock.writeLock().unlock();
        }
    }
}
