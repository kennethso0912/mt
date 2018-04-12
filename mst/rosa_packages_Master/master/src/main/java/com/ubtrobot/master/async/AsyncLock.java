package com.ubtrobot.master.async;

import android.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 异步锁
 */
public class AsyncLock {

    private final HashMap<String, HashSet<String>> mAcquired = new HashMap<>();
    private final LinkedList<Pair<HashSet<String>, AcquireCallback>> mAcquiring = new LinkedList<>();

    /**
     * 申请异步锁
     *
     * @param callback 申请结果回调
     * @param keys     锁的键列表。键列表为空或包含 null 时，针对所有 key 加锁
     */
    public void acquire(AcquireCallback callback, String... keys) {
        acquire(callback, Arrays.asList(keys));
    }

    /**
     * 申请异步锁
     *
     * @param callback 申请结果回调
     * @param keys     锁的键列表。键列表为空或包含 null 时，针对所有 key 加锁
     */
    public void acquire(AcquireCallback callback, Collection<String> keys) {
        if (callback == null) {
            throw new IllegalArgumentException("Argument callback is null.");
        }

        if (keys == null) {
            throw new IllegalArgumentException("Argument keys is null.");
        }

        HashSet<String> keySet = new HashSet<>();
        for (String key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("Argument keys contain null value.");
            }

            keySet.add(key);
        }

        acquire(callback, keySet);
    }

    private void acquire(AcquireCallback callback, HashSet<String> keys) {
        synchronized (this) {
            if (containAcquiredLocked(keys)) {
                mAcquiring.add(new Pair<>(keys, callback));
            } else {
                acquiredLocked(callback, keys);
            }
        }
    }

    private boolean containAcquiredLocked(HashSet<String> keys) {
        if (keys.isEmpty()) {
            return !mAcquired.isEmpty();
        }

        for (HashSet<String> acquiredGroup : mAcquired.values()) {
            for (String acquired : acquiredGroup) {
                if (keys.contains(acquired)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void acquiredLocked(AcquireCallback callback, HashSet<String> keys) {
        String token = newToken();
        mAcquired.put(token, keys);

        callback.onAcquired(token);
    }

    private String newToken() {
        return System.nanoTime() + "";
    }

    public void release(String token) {
        synchronized (this) {
            if (mAcquired.remove(token) == null) {
                throw new IllegalArgumentException("Unknown token.");
            }

            Iterator<Pair<HashSet<String>, AcquireCallback>> iterator = mAcquiring.iterator();
            while (iterator.hasNext()) {
                Pair<HashSet<String>, AcquireCallback> acquiring = iterator.next();
                if (containAcquiredLocked(acquiring.first)) {
                    continue;
                }

                iterator.remove();
                acquiredLocked(acquiring.second, acquiring.first);
            }
        }
    }

    public interface AcquireCallback {

        void onAcquired(String token);
    }
}