package com.ubtrobot.transport.message;

/**
 * Created by column on 17-8-22.
 */

/**
 * 参数
 */
public interface Param {

    /**
     * 获取参数类型
     *
     * @return 参数类型
     */
    String getType();

    /**
     * 参数是否为空
     *
     * @return 参数是否为空
     */
    boolean isEmpty();
}
