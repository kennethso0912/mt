package com.ubtrobot.transport.connection;

import com.ubtrobot.concurrent.EventLoop;

/**
 * Created by column on 17-8-26.
 */

public abstract class AbstractHandlerContext implements HandlerContext {

    volatile AbstractHandlerContext prev;
    volatile AbstractHandlerContext next;

    private final Connection mConnection;
    private final EventLoop mEventLoop;

    private final boolean mIncoming;
    private final boolean mOutgoing;

    protected AbstractHandlerContext(
            Connection connection, boolean incoming, boolean outgoing) {
        mConnection = connection;
        mEventLoop = connection().eventLoop();

        mIncoming = incoming;
        mOutgoing = outgoing;
    }

    private AbstractHandlerContext findContextIncoming() {
        AbstractHandlerContext ctx = this;
        do {
            ctx = ctx.next;
        } while (!ctx.mIncoming);
        return ctx;
    }

    private AbstractHandlerContext findContextOutgoing() {
        AbstractHandlerContext ctx = this;
        do {
            ctx = ctx.prev;
        } while (!ctx.mOutgoing);
        return ctx;
    }

    @Override
    public EventLoop eventLoop() {
        return mConnection.eventLoop();
    }

    @Override
    public Connection connection() {
        return mConnection;
    }

    @Override
    public void onConnected() {
        invokeOnConnected(findContextIncoming());
    }

    private void invokeOnConnected(final AbstractHandlerContext next) {
        if (mEventLoop.inEventLoop()) {
            ((IncomingHandler) next.handler()).onConnected(next);
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    ((IncomingHandler) next.handler()).onConnected(next);
                }
            });
        }
    }

    @Override
    public void onDisconnected() {
        invokeOnDisconnected(findContextIncoming());
    }

    private void invokeOnDisconnected(final AbstractHandlerContext next) {
        if (mEventLoop.inEventLoop()) {
            ((IncomingHandler) next.handler()).onDisconnected(next);
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    ((IncomingHandler) next.handler()).onDisconnected(next);
                }
            });
        }
    }

    @Override
    public void onRead(Object message) {
        invokeOnRead(findContextIncoming(), message);
    }

    private void invokeOnRead(final AbstractHandlerContext next, final Object message) {
        if (mEventLoop.inEventLoop()) {
            ((IncomingHandler) next.handler()).onRead(next, message);
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    ((IncomingHandler) next.handler()).onRead(next, message);
                }
            });
        }
    }

    @Override
    public void connect(OutgoingCallback callback) {
        invokeBind(findContextOutgoing(), callback);
    }

    private void invokeBind(final AbstractHandlerContext next, final OutgoingCallback callback) {
        if (mEventLoop.inEventLoop()) {
            ((OutgoingHandler) next.handler()).connect(next, callback);
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    ((OutgoingHandler) next.handler()).connect(next, callback);
                }
            });
        }
    }

    @Override
    public void disconnect(OutgoingCallback callback) {
        invokeUnbind(findContextOutgoing(), callback);
    }

    private void invokeUnbind(final AbstractHandlerContext next, final OutgoingCallback callback) {
        if (mEventLoop.inEventLoop()) {
            ((OutgoingHandler) next.handler()).disconnect(next, callback);
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    ((OutgoingHandler) next.handler()).disconnect(next, callback);
                }
            });
        }
    }

    @Override
    public void write(Object message, OutgoingCallback callback) {
        invokeWrite(findContextOutgoing(), message, callback);
    }

    private void invokeWrite(
            final AbstractHandlerContext next,
            final Object message, final OutgoingCallback callback) {
        if (mEventLoop.inEventLoop()) {
            ((OutgoingHandler) next.handler()).write(next, message, callback);
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    ((OutgoingHandler) next.handler()).write(next, message, callback);
                }
            });
        }
    }
}
