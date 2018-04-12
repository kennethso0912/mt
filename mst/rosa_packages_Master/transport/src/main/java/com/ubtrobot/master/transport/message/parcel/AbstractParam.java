package com.ubtrobot.master.transport.message.parcel;

import com.ubtrobot.transport.message.Param;

/**
 * Created by column on 17-11-27.
 */

public class AbstractParam implements Param {

    private final String type;
    private final byte[] bytes;

    protected AbstractParam(String type, byte[] bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean isEmpty() {
        return bytes == null || bytes.length <= 0;
    }

    ParcelParamWrap wrap() {
        return new ParcelParamWrap(type, bytes);
    }

    @Override
    public String toString() {
        return "Param{" +
                "type='" + type + '\'' +
                '}';
    }
}