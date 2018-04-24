package com.ubtrobot.master.transport.connection;

/**
 * Created by column on 17-8-31.
 */

public final class ConnectionConstants {

    private ConnectionConstants() {
    }

    public static final String PRVDR_CALL_MTHD_CONNECT = "connect";

    public static final String PRVDR_CALL_BUNDLE_KEY_VERSION = "version";
    public static final String PRVDR_CALL_BUNDLE_KEY_BINDER = "binder";
    public static final String PRVDR_CALL_BUNDLE_KEY_PACKAGE = "package";

    public static final String PRVDR_CALL_BUNDLE_KEY_CODE = "code";
    public static final String PRVDR_CALL_BUNDLE_KEY_ERR_MSG = "error_message";

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_UNSUPPORTED_METHOD = 1;
    public static final int BAD_CALL = 2;

    public static final int TRANS_CODE_DISCONNECT = ('D' << 24) | ('S' << 16) | ('C' << 8) | 'N';
    public static final int TRANS_CODE_WRITE = ('W' << 24) | ('R' << 16) | ('I' << 8) | 'T';

    public static final String ATTR_KEY_PACKAGE = "attr.package";
}