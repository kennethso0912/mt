package com.ubtrobot.master.transport.connection;

import com.ubtrobot.transport.connection.ConnectionId;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by column on 17-9-6.
 */

public class DefaultConnectionId implements ConnectionId {

    private static final AtomicLong sGlobalSeq = new AtomicLong(0);
    private static final String CONNECTOR = "-";

    private final String mText;

    public DefaultConnectionId(String nameForUid, int pid) {
        mText = nameForUid + CONNECTOR + pid + CONNECTOR + System.currentTimeMillis() +
                CONNECTOR + sGlobalSeq.incrementAndGet();
    }

    public DefaultConnectionId(String packageName) {
        mText = packageName + CONNECTOR + System.currentTimeMillis() +
                CONNECTOR + sGlobalSeq.incrementAndGet();
    }

    @Override
    public String asText() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultConnectionId that = (DefaultConnectionId) o;

        return mText.equals(that.mText);

    }

    @Override
    public int hashCode() {
        return mText.hashCode();
    }

    @Override
    public String toString() {
        return asText();
    }
}
