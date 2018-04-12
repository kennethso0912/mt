package com.ubtrobot.master.transport.message;

/**
 * Created by column on 10/09/2017.
 */

public final class ParamBundleConstants {

    private ParamBundleConstants() {
    }

    public static final String KEY_IS_INTERNAL_EVENT = "event-is-internal";
    public static final String KEY_EVENT_ACTIONS = "event-actions";

    public static final String KEY_CALL_EXCEPTION_CODE = "exception-code";

    public static final String KEY_CALL_EXCEPTION_MESSAGE = "exception-message";
    public static final String KEY_CALL_EXCEPTION_DETAIL = "exception-detail";

    public static final String KEY_SKILL_NAME = "skill-name";
    public static final String KEY_SERVICE_NAME = "service-name";
    public static final String KEY_STATE = "state";
    public static final String VAL_SKILL_STATE_DEFAULT = "skill.state.default";

    public static final String KEY_COMPETING_SESSION_ID = "service.competing-session.id";
    public static final String KEY_COMPETING_SESSION_RESUMED = "service.competing-session.resumed";
}