package com.ubtrobot.master.event;

import com.ubtrobot.transport.message.Param;

/**
 * Created by column on 17-8-29.
 */

public interface ConvenientPublisher {

    void publish(String action);

    void publish(String action, Param param);

    void publishCarefully(String action);

    void publishCarefully(String action, Param param);
}