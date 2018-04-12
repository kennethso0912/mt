package com.ubtrobot.master.skill;

/**
 * Created by column on 17-8-29.
 */

import android.support.annotation.Nullable;

import com.ubtrobot.master.call.ConvenientCallable;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;

/**
 * Master 应用群
 *
 * 直接向 Master 应用群调用请求，自动寻找最合适的应用处理请求并将结果返回
 */
public interface SkillsProxy extends ConvenientCallable {

    Response call(SkillIntent intent) throws CallException;

    Response call(SkillIntent intent, @Nullable Param param) throws CallException;

    Cancelable call(SkillIntent intent, @Nullable ResponseCallback callback);

    Cancelable call(SkillIntent intent, @Nullable Param param, @Nullable ResponseCallback callback);
}