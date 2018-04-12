package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 调用提供者
 */
public interface Callable {

    /**
     * 同步调用请求
     *
     * @param req 请求
     * @return 响应
     * @throws CallException 调用异常
     */
    Response call(Request req) throws CallException;

    /**
     * 异步调用请求
     *
     * @param req      请求
     * @param callback 响应回调
     * @return 取消器
     */
    Cancelable call(Request req, ResponseCallback callback);

    /**
     * 异步粘滞调用请求
     * 为避免滥用，使用该方法时应满足：
     * 1. 用于一问多答的长时操作；
     * 2. 保证一段时间后操作一定会结束，长期情况应该使用“订阅-发布”事件模型
     *
     * @param req      请求
     * @param callback 粘滞响应回调
     * @return 取消器
     */
    Cancelable callStickily(Request req, StickyResponseCallback callback);
}