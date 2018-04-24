package com.ubtrobot.master.policy;

import com.ubtrobot.master.Master;
import com.ubtrobot.ulog.logger.android.AndroidLoggerFactory;

/**
 * Created by column on 17-11-30.
 */

public class PolicyApplication extends BasePolicyApplication {

    @Override
    protected void onApplicationCreate() {
        Master.get().setLoggerFactory(new AndroidLoggerFactory());
    }
}