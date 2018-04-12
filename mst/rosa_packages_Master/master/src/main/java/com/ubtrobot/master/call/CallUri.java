package com.ubtrobot.master.call;

/**
 * Created by column on 17-11-24.
 */

public class CallUri {

    public static String createSkillCallUri(String path) {
        return "skill://" + path;
    }

    public static String createSkillCallUri(String path, String packageName) {
        return createSkillCallUri(path) + "#" + packageName;
    }

    public static String createServiceCallUri(String service, String path) {
        return "service://" + service + path;
    }

    public static String createServiceCallsUri(String service, String packageName) {
        return "service://" + service + "#" + packageName;
    }
}