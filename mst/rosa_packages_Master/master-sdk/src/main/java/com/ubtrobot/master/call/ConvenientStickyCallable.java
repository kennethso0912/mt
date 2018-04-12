package com.ubtrobot.master.call;

import android.support.annotation.Nullable;

import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.StickyResponseCallback;

/**
 * Created by column on 17-9-23.
 */

public interface ConvenientStickyCallable extends ConvenientCallable {

    Cancelable callStickily(String path, StickyResponseCallback callback);

    Cancelable callStickily(String path, @Nullable Param param, StickyResponseCallback callback);
}