package com.ubtrobot.master.transport.message;

/**
 * Created by column on 17-9-11.
 */

public final class CallGlobalCode {

    private CallGlobalCode() {
    }

    public static final int BAD_REQUEST = 400;

    public static final int UNAUTHORIZED = 401;

    public static final int FORBIDDEN = 403;

    public static final int NOT_FOUND = 404;

    public static final int REQUEST_TIMEOUT = 408;

    public static final int CONFLICT = 409;

    public static final int INTERNAL_ERROR = 500;

    public static final int NOT_IMPLEMENTED = 501;

    public static final int RESPOND_TIMEOUT = 504;
}