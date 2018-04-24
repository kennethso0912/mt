package com.ubtrobot.master.policy;

import android.app.Application;

import com.ubtrobot.master.Master;
import com.ubtrobot.master.Unsafe;

/**
 * Created by column on 17-11-30.
 */

public class BasePolicyApplication extends Application {

    private Policy mPolicy;

    @Override
    public final void onCreate() {
        super.onCreate();

        EnvironmentChecker.checkPackageName(this);
        Master.initialize(this);

        onApplicationCreate();

        mPolicy = createPolicy();
        if (mPolicy == null) {
            throw new IllegalStateException("Your Application 's createPolicy return null.");
        }
    }

    protected void onApplicationCreate() {
        // For override
    }

    protected Policy createPolicy() {
        return new DefaultPolicy(this, Unsafe.get());
    }

    public final Policy getPolicy() {
        return mPolicy;
    }
}
