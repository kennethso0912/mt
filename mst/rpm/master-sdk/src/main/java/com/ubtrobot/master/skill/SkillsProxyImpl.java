package com.ubtrobot.master.skill;

import android.os.Handler;

import com.ubtrobot.master.call.BaseConvenientIntentCallable;
import com.ubtrobot.master.call.IPCByMasterCallable;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;

/**
 * Created by column on 17-9-5.
 */

public class SkillsProxyImpl extends BaseConvenientIntentCallable implements SkillsProxy {

    public SkillsProxyImpl(
            IPCByMasterCallable callable, Handler mainThreadHandler, ParcelRequestContext context) {
        super(callable, mainThreadHandler, context, new ParcelRequestConfig.Builder());
    }
}