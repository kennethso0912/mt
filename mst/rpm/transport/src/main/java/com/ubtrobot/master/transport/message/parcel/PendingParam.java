package com.ubtrobot.master.transport.message.parcel;

/**
 * Created by column on 17-11-27.
 */

public class PendingParam extends AbstractParam {

    private final byte[] bytes;

    public PendingParam(String type, byte[] bytes) {
        super(type, bytes);
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}