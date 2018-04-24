package com.ubtrobot.master.call;

import android.support.annotation.Nullable;

import com.ubtrobot.master.skill.SkillIntent;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;

/**
 * Created by column on 17-11-29.
 */

public interface ConvenientIntentCallable extends ConvenientCallable {

    Response call(SkillIntent intent) throws CallException;

    Response call(SkillIntent intent, @Nullable Param param) throws CallException;

    Cancelable call(SkillIntent intent, @Nullable ResponseCallback callback);

    Cancelable call(SkillIntent intent, @Nullable Param param, @Nullable ResponseCallback callback);
}