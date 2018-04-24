package com.ubtrobot.master.service;

import com.ubtrobot.master.component.ComponentBaseInfo;
import com.ubtrobot.master.transport.connection.ConnectionConstants;
import com.ubtrobot.transport.connection.Connection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-11-28.
 */

public class ServiceManager {

    private final HashMap<ComponentBaseInfo, Set<String>> mServicesStates = new HashMap<>();
    private final ReentrantReadWriteLock mServicesStatesLock = new ReentrantReadWriteLock();

    private final LinkedList<OnServiceStateAddedListener> mListeners = new LinkedList<>();

    public void addServiceState(
            final ComponentBaseInfo serviceBaseInfo,
            final String state) {
        mServicesStatesLock.writeLock().lock();
        try {
            Set<String> states = mServicesStates.get(serviceBaseInfo);
            if (states == null) {
                states = new HashSet<>();
                mServicesStates.put(serviceBaseInfo, states);
            }

            states.add(state);

            synchronized (mListeners) {
                for (OnServiceStateAddedListener listener : mListeners) {
                    listener.onServiceStateAdded(serviceBaseInfo, state);
                }
            }
        } finally {
            mServicesStatesLock.writeLock().unlock();
        }
    }

    public void removeServiceState(
            final ComponentBaseInfo serviceBaseInfo,
            final String state) {
        mServicesStatesLock.writeLock().lock();
        try {
            Set<String> states = mServicesStates.get(serviceBaseInfo);
            if (states == null) {
                return;
            }

            states.remove(state);
            if (states.isEmpty()) {
                mServicesStates.remove(serviceBaseInfo);
            }
        } finally {
            mServicesStatesLock.writeLock().unlock();
        }

    }

    public Map<ComponentBaseInfo, Set<String>> getStatefulServices() {
        mServicesStatesLock.readLock().lock();
        try {
            Map<ComponentBaseInfo, Set<String>> result = new HashMap<>();
            for (Map.Entry<ComponentBaseInfo, Set<String>> entry : mServicesStates.entrySet()) {
                HashSet<String> states = new HashSet<>();
                for (String state : entry.getValue()) {
                    states.add(state);
                }

                result.put(entry.getKey(), states);
            }

            return result;
        } finally {
            mServicesStatesLock.readLock().unlock();
        }
    }

    public boolean didServiceAddState(String service, String state) {
        mServicesStatesLock.readLock().lock();
        try {
            for (Map.Entry<ComponentBaseInfo, Set<String>> entry : mServicesStates.entrySet()) {
                if (entry.getKey().getName().equals(service)) {
                    return entry.getValue().contains(state);
                }
            }

            return false;
        } finally {
            mServicesStatesLock.readLock().unlock();
        }
    }

    public void removeConnectionServicesStates(Connection connection) {
        mServicesStatesLock.writeLock().lock();
        try {
            String packageName = (String) connection.attributes().
                    get(ConnectionConstants.ATTR_KEY_PACKAGE);
            if (packageName == null) {
                throw new AssertionError("packageName != null");
            }

            Iterator<Map.Entry<ComponentBaseInfo, Set<String>>> iterator =
                    mServicesStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ComponentBaseInfo, Set<String>> entry = iterator.next();
                if (entry.getKey().getPackageName().equals(packageName)) {
                    iterator.remove();
                }
            }
        } finally {
            mServicesStatesLock.writeLock().unlock();
        }
    }

    public void addOnServiceStateAddedListener(OnServiceStateAddedListener listener) {
        synchronized (mListeners) {
            if (mListeners.contains(listener)) {
                throw new IllegalStateException("Already registered.");
            }

            mListeners.add(listener);
        }
    }

    public void removeOnServiceStateAddedListener(OnServiceStateAddedListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public interface OnServiceStateAddedListener {

        void onServiceStateAdded(ComponentBaseInfo serviceBaseInfo, String state);
    }
}