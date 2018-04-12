package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-26.
 */

/**
 * 连接上输出操作的统一回调接口
 */
public interface OutgoingCallback {

    void onSuccess();

    void onFailure(Exception e);
}