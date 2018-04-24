package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用异常
 */
public class CallException extends Exception {

    private final int mCode;
    private final Map<String, String> mDetail;

    public CallException(int code, @NonNull String message) {
        this(code, message, null, null);
    }

    public CallException(int code, @NonNull String message, @Nullable Throwable cause) {
        this(code, message, null, cause);
    }

    public CallException(
            int code, @NonNull String message,
            @Nullable Map<String, String> detail, @Nullable Throwable cause) {
        super(message, cause);
        mCode = code;
        mDetail = detail == null ? new HashMap<String, String>() : detail;
    }

    /**
     * 获取异常码
     *
     * @return 异常码
     */
    public int getCode() {
        return mCode;
    }

    /**
     * 获取异常细节
     *
     * @return 自定义 String k-v 结构
     */
    public Map<String, String> getDetail() {
        return mDetail;
    }
}
