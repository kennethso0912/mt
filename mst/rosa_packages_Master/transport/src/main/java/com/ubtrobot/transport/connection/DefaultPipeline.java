package com.ubtrobot.transport.connection;

/**
 * Created by column on 17-8-28.
 */

public class DefaultPipeline implements Pipeline {

    private final Connection mConnection;

    private final AbstractHandlerContext mHead;
    private final AbstractHandlerContext mTail;

    public DefaultPipeline(Connection connection) {
        mConnection = connection;

        mHead = new HeadContext(mConnection);
        mTail = new TailContext(mConnection);

        mHead.next = mTail;
        mTail.prev = mHead;
    }

    @Override
    public void add(ConnectionHandler handler) {
        DefaultHandlerContext newContext = new DefaultHandlerContext(
                mConnection, handler);
        AbstractHandlerContext prev = mTail.prev;

        newContext.prev = prev;
        newContext.next = mTail;

        prev.next = newContext;
        mTail.prev = newContext;
    }

    @Override
    public void onConnected() {
        mHead.onConnected();
    }

    @Override
    public void onDisconnected() {
        mHead.onDisconnected();
    }

    @Override
    public void onRead(Object message) {
        mHead.onRead(message);
    }

    @Override
    public void connect(OutgoingCallback callback) {
        mTail.connect(callback);
    }

    @Override
    public void disconnect(OutgoingCallback callback) {
        mTail.disconnect(callback);
    }

    @Override
    public void write(Object message, OutgoingCallback callback) {
        mTail.write(message, callback);
    }

    private static class HeadContext extends AbstractHandlerContext implements OutgoingHandler {

        private final Connection.Unsafe mUnsafe;

        protected HeadContext(Connection connection) {
            super(connection, false, true);
            mUnsafe = connection.unsafe();
        }

        @Override
        public ConnectionHandler handler() {
            return this;
        }

        @Override
        public void connect(HandlerContext context, OutgoingCallback callback) {
            mUnsafe.connect(callback);
        }

        @Override
        public void disconnect(HandlerContext context, OutgoingCallback callback) {
            mUnsafe.disconnect(callback);
        }

        @Override
        public void write(HandlerContext context, Object message, OutgoingCallback callback) {
            mUnsafe.write(message, callback);
        }
    }

    private static class TailContext extends AbstractHandlerContext implements IncomingHandler {

        protected TailContext(Connection connection) {
            super(connection, true, false);
        }

        @Override
        public ConnectionHandler handler() {
            return this;
        }

        @Override
        public void onConnected(HandlerContext handlerContext) {
            // NOOP
        }

        @Override
        public void onDisconnected(HandlerContext handlerContext) {
            // NOOP
        }

        @Override
        public void onRead(HandlerContext handlerContext, Object message) {
            // NOOP
        }
    }

    private static class DefaultHandlerContext extends AbstractHandlerContext {

        private final ConnectionHandler mHandler;

        protected DefaultHandlerContext(Connection connection, ConnectionHandler handler) {
            super(connection, handler instanceof IncomingHandler, handler instanceof OutgoingHandler);
            mHandler = handler;
        }

        @Override
        public ConnectionHandler handler() {
            return mHandler;
        }
    }
}
