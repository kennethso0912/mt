package com.ubtrobot.master.call;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.skill.SkillCallInfo;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-11-23.
 */

public class CallDestinations {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("CallDestinations");

    // Key: uri (skill:///some/path#package_name | service://service_name#package_name)
    private final HashMap<String, Connection> mConnections = new HashMap<>();
    private final ReadWriteLock mConnectionsLock = new ReentrantReadWriteLock();

    public void addSkillCallDestinations(SkillInfo skillInfo, Connection connection) {
        mConnectionsLock.writeLock().lock();
        try {
            for (SkillCallInfo skillCallInfo : skillInfo.getCallInfoList()) {
                mConnections.put(
                        CallUri.createSkillCallUri(
                                skillCallInfo.getPath(), skillInfo.getPackageName()),
                        connection
                );

                LOGGER.i("Add skill call route. skillName=%s, path=%s, package=%s, connectionId=%s",
                        skillInfo.getName(), skillCallInfo.getPath(), skillInfo.getPackageName(),
                        connection.id().asText());
            }
        } finally {
            mConnectionsLock.writeLock().unlock();
        }
    }

    public void addServiceCallDestinations(ServiceInfo serviceInfo, Connection connection) {
        mConnectionsLock.writeLock().lock();
        try {
            mConnections.put(
                    CallUri.createServiceCallsUri(
                            serviceInfo.getName(),
                            serviceInfo.getPackageName()),
                    connection
            );

            LOGGER.i("Add service calls route. serviceName=%s, package=%s, connectionId=%s",
                    serviceInfo.getName(), serviceInfo.getPackageName(), connection.id().asText());
        } finally {
            mConnectionsLock.writeLock().unlock();
        }
    }

    public void removeCallDestinations(Connection connection) {
        mConnectionsLock.writeLock().lock();

        try {
            Iterator<Map.Entry<String, Connection>> iterator = mConnections.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Connection> connEntry = iterator.next();
                if (connEntry.getValue().id().equals(connection.id())) {
                    iterator.remove();
                    LOGGER.i("Remove call route. uriWithPackage=%s, connectionId=%s",
                            connEntry.getKey(), connEntry.getValue().id().asText());
                }
            }
        } finally {
            mConnectionsLock.writeLock().unlock();
        }
    }

    public Connection getSkillCallDestination(String packageName, String path) {
        mConnectionsLock.readLock().lock();

        try {
            return mConnections.get(CallUri.createSkillCallUri(path, packageName));
        } finally {
            mConnectionsLock.readLock().unlock();
        }
    }

    public Connection getServiceCallDestination(
            String packageName, String serviceName) {
        mConnectionsLock.readLock().lock();

        try {
            return mConnections.get(CallUri.createServiceCallsUri(serviceName, packageName));
        } finally {
            mConnectionsLock.readLock().unlock();
        }
    }
}