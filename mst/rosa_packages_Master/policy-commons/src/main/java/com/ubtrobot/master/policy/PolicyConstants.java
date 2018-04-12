package com.ubtrobot.master.policy;

/**
 * Created by column on 26/11/2017.
 */

public final class PolicyConstants {

    private PolicyConstants() {
    }


    public static final String POLICY_PACKAGE_NAME = "com.ubtrobot.master.policy";
    public static final String POLICY_SERVICE_NAME = "policy";

    // 供 Master 调用
    public static final String CALL_PATH_COMPONENT_POLICIES = "/component/policies";

    // 向 Master 调用
    public static final String PATH_NOTIFY_SKILL_POLICIES_CHANGED =
            "/master/policy/skill/notify-changed";
    public static final String PATH_NOTIFY_SERVICE_POLICIES_CHANGED =
            "/master/policy/service/notify-changed";

    // 参数 Bundle Key
    public static final String KEY_SKILL_BASE_INFO_LIST = "skill-base-info-list";
    public static final String KEY_SERVICE_BASE_INFO_LIST = "service-base-info-list";
    public static final String KEY_SKILL_POLICIES = "skill-policies";
    public static final String KEY_SERVICE_POLICIES = "service-policies";
}