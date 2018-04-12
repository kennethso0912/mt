package com.ubtrobot.master.param;

import com.ubtrobot.master.transport.message.parcel.AbstractParam;

/**
 * Created by column on 17-9-19.
 */

public class ProtoLiteParam extends AbstractParam {

    public static final String TYPE = "ProtoLiteParam";

    protected ProtoLiteParam(byte[] bytes) {
        super(TYPE, bytes);
    }

    public static class InvalidProtoLiteParamException extends Exception {

        public InvalidProtoLiteParamException() {
        }

        public InvalidProtoLiteParamException(String message) {
            super(message);
        }

        public InvalidProtoLiteParamException(Throwable cause) {
            super(cause);
        }
    }
}