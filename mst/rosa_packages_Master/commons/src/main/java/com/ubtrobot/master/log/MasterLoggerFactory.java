package com.ubtrobot.master.log;

import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.LoggerFactory;

/**
 * Created by column on 17-9-6.
 */

public class MasterLoggerFactory {

    private static LoggerFactory sLoggerFactory = new InfrequentLoggerFactory();

    private MasterLoggerFactory() {
    }

    public static void setup(LoggerFactory loggerFactory) {
        if (loggerFactory == null) {
            throw new IllegalArgumentException("Argument loggerFactory is null.");
        }

        sLoggerFactory = loggerFactory;
    }

    public static Logger getLogger(String tag) {
        return sLoggerFactory.getLogger("MST|" + tag);
    }
}