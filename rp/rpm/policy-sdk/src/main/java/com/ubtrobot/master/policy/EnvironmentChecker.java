package com.ubtrobot.master.policy;

import android.app.Application;
import android.content.Context;

/**
 * Created by column on 17-11-30.
 */

public class EnvironmentChecker {

    private EnvironmentChecker() {
    }

    public static void checkPackageName(Context context) {
        if (!PolicyConstants.POLICY_PACKAGE_NAME.equals(context.getPackageName())) {
            throw new IllegalStateException("Your master policy apk 's package name MUST be " +
                    PolicyConstants.POLICY_PACKAGE_NAME);
        }
    }

    public static void checkApplication(Context context) {
        Application application = (Application) context.getApplicationContext();
        if (!(application instanceof BasePolicyApplication)) {
            throw new IllegalStateException("Your master policy apk 's Application MUST extend " +
                    BasePolicyApplication.class.getName());
        }
    }
}
