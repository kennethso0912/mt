package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 请求
 */
public interface Request {

    /**
     * 获取请求 id
     *
     * @return 请求 id
     */
    String getId();

    /**
     * 获取请求路径
     *
     * @return 请求路径
     */
    String getPath();

    /**
     * 获取请求时间
     *
     * @return 请求时间
     */
    long getWhen();

    /**
     * 获取请求参数
     *
     * @return 请求参数
     */
    Param getParam();
}
