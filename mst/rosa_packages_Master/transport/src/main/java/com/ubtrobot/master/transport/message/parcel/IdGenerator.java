package com.ubtrobot.master.transport.message.parcel;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Column Fang on 17-6-14.
 */

/**
 * 消息 id 生成器，进程内唯一
 */
class IdGenerator {

    private static final AtomicLong sGlobalId = new AtomicLong(0);
    private static final String PREFIX = UUID.randomUUID().toString().substring(0, 5) + "-";

    private IdGenerator() {
    }

    static String nextId() {
        return PREFIX + sGlobalId.incrementAndGet();
    }
}