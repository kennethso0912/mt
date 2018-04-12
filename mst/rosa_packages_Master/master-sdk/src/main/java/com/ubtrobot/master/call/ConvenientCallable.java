package com.ubtrobot.master.call;

import android.support.annotation.Nullable;

import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;

/**
 * Created by column on 17-8-29.
 */

public interface ConvenientCallable {

    void setConfiguration(CallConfiguration configuration);

    CallConfiguration getConfiguration();

    Response call(String path) throws CallException;

    Response call(String path, @Nullable Param param) throws CallException;

    Cancelable call(String path, @Nullable ResponseCallback callback);

    Cancelable call(String path, @Nullable Param param, @Nullable ResponseCallback callback);
}