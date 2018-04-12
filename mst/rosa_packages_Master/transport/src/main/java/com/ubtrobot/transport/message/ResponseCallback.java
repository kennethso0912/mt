package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 响应回调
 */
public interface ResponseCallback {

    /**
     * 收到调用响应的回调
     *
     * @param req 请求
     * @param res 响应
     */
    void onResponse(Request req, Response res);

    /**
     * 调用出错的回调
     *
     * @param req 请求
     * @param e   调用异常
     */
    void onFailure(Request req, CallException e);
}