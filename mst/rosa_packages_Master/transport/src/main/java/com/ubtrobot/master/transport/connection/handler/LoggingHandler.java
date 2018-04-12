package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.transport.connection.DuplexHandlerAdapter;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-9-6.
 */

public class LoggingHandler extends DuplexHandlerAdapter {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("LoggingHandler");

    @Override
    public void onConnected(HandlerContext context) {
        LOGGER.d("onConnected. connectionId=%s", context.connection().id().asText());
        context.onConnected();
    }

    @Override
    public void onDisconnected(HandlerContext context) {
        LOGGER.d("onDisconnected. connectionId=%s", context.connection().id().asText());
        context.onDisconnected();
    }

    @Override
    public void onRead(HandlerContext context, Object message) {
        LOGGER.d("onRead. message=%s, connectionId=%s", message.toString(),
                context.connection().id().asText());
        context.onRead(message);
    }

    @Override
    public void connect(HandlerContext context, OutgoingCallback callback) {
        LOGGER.d("connect. connectionId=%s", context.connection().id().asText());
        context.connect(callback);
    }

    @Override
    public void disconnect(HandlerContext context, OutgoingCallback callback) {
        LOGGER.d("disconnect. connectionId=%s", context.connection().id().asText());
        context.disconnect(callback);
    }

    @Override
    public void write(HandlerContext context, Object message, OutgoingCallback callback) {
        LOGGER.d("write. message=%s, connectionId=%s", message.toString(),
                context.connection().id().asText());
        context.write(message, callback);
    }
}