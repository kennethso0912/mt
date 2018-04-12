package com.ubtrobot.master.policy;

import android.content.Context;
import android.content.pm.PackageManager;

import com.ubtrobot.master.call.IPCToPolicyCallable;

/**
 * Created by column on 17-11-30.
 */

public class BasePolicySource {

    private final Context mContext;
    private final IPCToPolicyCallable mCallable;

    protected BasePolicySource(Context context, IPCToPolicyCallable callable) {
        mContext = context;
        mCallable = callable;
    }

    protected Context context() {
        return mContext;
    }

    protected IPCToPolicyCallable callable() {
        return mCallable;
    }

    protected boolean isPolicyServiceInstalled() {
        try {
            return mContext.getPackageManager().
                    getPackageInfo(PolicyConstants.POLICY_PACKAGE_NAME, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
