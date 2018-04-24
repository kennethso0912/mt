package com.ubtrobot.master.skill;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by column on 17-11-29.
 */

public class SkillManageException extends Exception {

    public static final int CODE_FORBIDDEN = -1;
    public static final int CODE_SET_STATE_BEFORE_SKILL_RUNNING = -2;
    public static final int CODE_INTERNAL_ERROR = -3;

    private final int code;

    public SkillManageException(int code, @NonNull String message) {
        this(code, message, null);
    }

    public SkillManageException(
            int code, @NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
