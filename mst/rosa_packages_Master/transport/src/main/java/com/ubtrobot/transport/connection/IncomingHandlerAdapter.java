package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-29.
 */

public class IncomingHandlerAdapter implements IncomingHandler {

    @Override
    public void onConnected(HandlerContext context) {
        context.onConnected();
    }

    @Override
    public void onDisconnected(HandlerContext context) {
        context.onDisconnected();
    }

    @Override
    public void onRead(HandlerContext context, Object message) {
        context.onRead(message);
    }
}
