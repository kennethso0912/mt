package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 粘滞调用请求的响应回调
 */
public interface StickyResponseCallback {

    /**
     * 中间状态的回调
     *
     * @param req 请求
     * @param res 中间状态的响应
     */
    void onResponseStickily(Request req, Response res);

    /**
     * 最终结果的回调
     *
     * @param req 请求
     * @param res 最终结果的响应
     */
    void onResponseCompletely(Request req, Response res);

    /**
     * 调用请求失败
     *
     * @param req 请求
     * @param e   异常
     */
    void onFailure(Request req, CallException e);
}