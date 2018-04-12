package com.ubtrobot.transport.connection;

import com.ubtrobot.concurrent.EventLoop;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-8-25.
 */

/**
 * 连接的抽象类实现，复用公用的处理逻辑
 */
public abstract class AbstractConnection implements Connection {

    private final ConnectionId mId;
    private final EventLoop mEventLoop;

    private final Pipeline mPipeline;
    private final Unsafe mUnsafe;

    private HashMap<String, Object> mAttributes;

    /**
     * 构造器
     *
     * @param id        连接 id
     * @param eventLoop 与连接关联的事件循环
     */
    protected AbstractConnection(ConnectionId id, EventLoop eventLoop) {
        this(id, eventLoop, false);
    }

    /**
     * 构造器
     *
     * @param id            连接 id
     * @param eventLoop     与连接关联的事件循环
     * @param hasAttributes 是否拥有连接属性
     */
    protected AbstractConnection(ConnectionId id, EventLoop eventLoop, boolean hasAttributes) {
        mId = id;
        mEventLoop = eventLoop;
        mUnsafe = newUnsafe();

        mPipeline = new DefaultPipeline(this);

        if (hasAttributes) {
            mAttributes = new HashMap<>();
        }
    }

    @Override
    public ConnectionId id() {
        return mId;
    }

    @Override
    public EventLoop eventLoop() {
        return mEventLoop;
    }

    @Override
    public Pipeline pipeline() {
        return mPipeline;
    }

    @Override
    public void connect(OutgoingCallback callback) {
        mPipeline.connect(callback);
    }

    @Override
    public void disconnect(OutgoingCallback callback) {
        mPipeline.disconnect(callback);
    }

    @Override
    public void write(Object message, OutgoingCallback callback) {
        mPipeline.write(message, callback);
    }

    @Override
    public Map<String, Object> attributes() {
        return mAttributes;
    }

    @Override
    public Unsafe unsafe() {
        return mUnsafe;
    }

    /**
     * 子类的连接实现返回对应的不安全操作
     *
     * @return 不安全操作
     */
    protected abstract Unsafe newUnsafe();

    /**
     * 在事件循环中回调成功的工具方法
     *
     * @param callback 回调对象
     */
    protected void callbackSuccess(final OutgoingCallback callback) {
        eventLoop().post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess();
            }
        });
    }

    /**
     * 在事件循环中回调失败的工具方法
     *
     * @param callback 回调对象
     */
    protected void callbackFailure(final OutgoingCallback callback, final Exception e) {
        eventLoop().post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 在事件循环中回调连接建立的工具方法。先回调建立连接的发起者，而后通过 pipeline 通过到各个 Handler
     */
    protected void onConnectedAfterCallback() {
        eventLoop().post(new Runnable() {
            @Override
            public void run() {
                pipeline().onConnected();
            }
        });
    }

    /**
     * 在事件循环中回调连接断开的工具方法。先回调断开连接的发起者，而后通过 pipeline 通过到各个 Handler
     */
    protected void onDisconnectedAfterCallback() {
        eventLoop().post(new Runnable() {
            @Override
            public void run() {
                pipeline().onDisconnected();
            }
        });
    }
}