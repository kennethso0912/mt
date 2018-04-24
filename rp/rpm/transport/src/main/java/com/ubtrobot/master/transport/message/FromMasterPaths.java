package com.ubtrobot.master.transport.message;

/**
 * Created by column on 17-11-30.
 */

public class FromMasterPaths {

    private FromMasterPaths() {
    }

    public static final String PATH_START_SKILL = "/master/skill/start";

    public static final String PATH_STOP_SKILL = "/master/skill/stop";

    public static final String PATH_SYNC_COMPONENT_LIFETCYCLE = "/master/component/lifecycle/sync";

    public static final String PATH_GET_COMPETING_ITEMS = "/master/service/competing-item/list";
    public static final String PATH_ACTIVATE_COMPETING_SESSION = "/master/service/competing-session/activate";
    public static final String PATH_DEACTIVATE_COMPETING_SESSION = "/master/service/competing-session/deactivate";
}