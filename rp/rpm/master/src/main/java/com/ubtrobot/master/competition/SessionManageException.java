package com.ubtrobot.master.competition;

public class SessionManageException extends Exception {

    public static final int CODE_COMPETING_ITEM_NOT_FOUND = 1;
    public static final int CODE_FORBIDDEN_TO_INTERRUPT = 2;
    public static final int CODE_SERVICE_INTERNAL_ERROR = 3;
    public static final int CODE_ACTIVE_SESSION_NOT_FOUND = 4;
    public static final int CODE_CALL_IN_INCORRECT_SESSION = 5;
    public static final int CODE_CALL_NOT_IN_SESSION = 6;
    public static final int CODE_SESSION_NOT_FOUND = 7;

    // 激活 session
    // - CompetingItem 不存在
    // - 竞争不过，不允许打断
    // - 服务内部错误

    // 检查 session
    // - 没有对应激活的 session
    // - 调用跟 session 不匹配
    // - 调用需要在 session 中执行

    private final int mCode;

    public SessionManageException(int code, String message) {
        super(message);
        mCode = code;
    }

    public SessionManageException(int code, String message, Throwable cause) {
        super(message, cause);
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }
}
