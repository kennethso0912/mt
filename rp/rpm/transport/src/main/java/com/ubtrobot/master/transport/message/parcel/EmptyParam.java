package com.ubtrobot.master.transport.message.parcel;

/**
 * Created by column on 17-11-27.
 */

public class EmptyParam extends AbstractParam {

    public static final String TYPE = "EmptyParam";

    public EmptyParam() {
        super(TYPE, new byte[0]);
    }
}