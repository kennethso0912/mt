package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-30.
 */

public class DuplexHandlerAdapter extends IncomingHandlerAdapter implements OutgoingHandler {

    @Override
    public void connect(HandlerContext context, OutgoingCallback callback) {
        context.connect(callback);
    }

    @Override
    public void disconnect(HandlerContext context, OutgoingCallback callback) {
        context.disconnect(callback);
    }

    @Override
    public void write(HandlerContext context, Object message, OutgoingCallback callback) {
        context.write(message, callback);
    }
}