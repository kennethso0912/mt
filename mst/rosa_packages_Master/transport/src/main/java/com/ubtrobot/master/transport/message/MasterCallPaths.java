package com.ubtrobot.master.transport.message;

/**
 * Created by column on 10/09/2017.
 */

public final class MasterCallPaths {

    private MasterCallPaths() {
    }

    public static final String PATH_SUBSCRIBE_EVENT = "/master/event/subscribe";
    public static final String PATH_UNSUBSCRIBE_EVENT = "/master/event/unsubscribe";

    public static final String PATH_SET_SKILL_STATE = "/master/skill/state/set";
    public static final String PATH_STOP_SKILL = "/master/skill/stop";
    public static final String PATH_GET_STARTED_SKILLS = "/master/skill/started";

    public static final String PATH_ADD_SERVICE_STATE = "/master/service/state/add";
    public static final String PATH_REMOVE_SERVICE_STATE = "/master/service/state/remove";
    public static final String PATH_QUERY_SERVICE_STATE_DID_ADD = "/master/service/state/did-add";

    public static final String PATH_ACTIVATE_COMPETING_SESSION =
            "/master/service/competing-session/activate";
    public static final String PATH_DEACTIVATE_COMPETING_SESSION =
            "/master/service/competing-session/deactivate";
}