package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-25.
 */

/**
 * 连接的唯一标识
 */
public interface ConnectionId {

    /**
     * 唯一标识转化成字符串
     *
     * @return 唯一标识字符串
     */
    String asText();
}
