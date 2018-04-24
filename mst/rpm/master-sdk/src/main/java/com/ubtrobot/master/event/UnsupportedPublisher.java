package com.ubtrobot.master.event;

import com.ubtrobot.transport.message.Param;

/**
 * Created by column on 17-8-31.
 */

public class UnsupportedPublisher implements ConvenientPublisher {

    @Override
    public void publish(String action) {
        throw new UnsupportedOperationException("Skill can NOT publish event.");
    }

    @Override
    public void publish(String action, Param param) {
        throw new UnsupportedOperationException("Skill can NOT publish event.");
    }

    @Override
    public void publishCarefully(String action) {
        throw new UnsupportedOperationException("Skill can NOT publish event.");
    }

    @Override
    public void publishCarefully(String action, Param param) {
        throw new UnsupportedOperationException("Skill can NOT publish event.");
    }
}