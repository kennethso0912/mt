package com.ubtrobot.master.log;

import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.LoggerFactory;

/**
 * Created by column on 17-9-6.
 */

public class InfrequentLoggerFactory implements LoggerFactory {

    @Override
    public Logger getLogger(String tag) {
        return new InfrequentLogger(tag);
    }
}
