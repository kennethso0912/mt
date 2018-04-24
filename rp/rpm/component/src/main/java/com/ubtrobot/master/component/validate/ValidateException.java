package com.ubtrobot.master.component.validate;

/**
 * Created by column on 17-11-20.
 */

public class ValidateException extends Exception {

    public static final int CODE_NO_PERMISSION = 1;
    public static final int CODE_ILLEGAL_CONFIGURATION = 2;

    private final int code;

    public ValidateException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ValidateException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}