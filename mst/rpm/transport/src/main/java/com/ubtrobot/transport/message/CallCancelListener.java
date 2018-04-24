package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 调用取消的监听器
 */
public interface CallCancelListener {

    /**
     * 调用被取消的回调
     *
     * @param request 调用的请求
     */
    void onCancel(Request request);
}