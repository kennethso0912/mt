package com.ubtrobot.master.transport.message;

/**
 * Created by zhu on 18-1-11.
 */

public class MasterEvents {

    public static final String ACTION_SKILL_STARTED = "ubtrobot.event.action.SKILL_STARTED";
    public static final String ACTION_SKILL_STOPPED = "ubtrobot.event.action.SKILL_STOPPED";

    public static final String ACTION_PREFIX_SESSION_INTERRUPTION_BEGAN =
            "ubtrobot.event.action.COMPETITION_SESSION_INTERRUPTION_BEGAN | sessionId=";
    public static final String ACTION_PREFIX_SESSION_INTERRUPTION_ENDED =
            "ubtrobot.event.action.COMPETITION_SESSION_INTERRUPTION_ENDED | sessionId=";
}