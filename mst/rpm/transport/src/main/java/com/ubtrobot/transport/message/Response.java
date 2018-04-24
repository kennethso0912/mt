package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 响应
 */
public interface Response {

    /**
     * 获取响应 id
     *
     * @return 响应 id
     */
    String getId();

    /**
     * 获取对应的调用请求 id
     *
     * @return 请求 id
     */
    String getRequestId();

    /**
     * 获取对应的调用请求路径
     *
     * @return 请求路径
     */
    String getPath();

    /**
     * 获取响应时间
     *
     * @return 响应时间
     */
    long getWhen();

    /**
     * 获取响应参数
     *
     * @return 响应参数
     */
    Param getParam();
}