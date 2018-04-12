package com.ubtrobot.master.transport.version;

/**
 * Created by column on 17-12-1.
 */

public class Version {

    private Version() {
    }

    /**
     * 所有支持的版本列表。添加新版本时注意顺序，越新的版本在版本数组中越考前
     */
    public static final String[] LIST = new String[]{"v1"};

    /**
     * 最新版本
     */
    public static final String LATEST = LIST[0];

    public static boolean isLegal(String version) {
        for (String aVersion : LIST) {
            if (aVersion.equals(version)) {
                return true;
            }
        }

        return false;
    }
}