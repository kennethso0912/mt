package com.ubtrobot.master.param;

import com.google.gson.Gson;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;

/**
 * Created by column on 17-9-18.
 */

public class GsonParam extends AbstractParam {

    public static final String TYPE = "GsonParam";

    private static final Gson GSON = new Gson();

    private GsonParam(byte[] bytes) {
        super(TYPE, bytes);
    }

    public static class InvalidGsonParamException extends Exception {

        public InvalidGsonParamException() {
        }

        public InvalidGsonParamException(String message) {
            super(message);
        }

        public InvalidGsonParamException(Throwable cause) {
            super(cause);
        }
    }
}
