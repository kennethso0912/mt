package com.ubtrobot.master.call;

import android.content.Context;
import android.support.annotation.Nullable;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.transport.message.AbstractIPCCallable;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.transport.message.StickyResponseCallback;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-11-29.
 */

public class IPCFromMasterCallable extends AbstractIPCCallable {

    private static final int TIMEOUT = 5000;

    private final Context mContext;
    private final HashMap<String, Map.Entry<Connection, ComponentInfo>> mDestinations = new HashMap<>();

    public IPCFromMasterCallable(Context context, EventLoop eventLoop) {
        super(eventLoop);
        mContext = context;
    }

    @Override
    protected EventLoop eventLoop() {
        // 让 XIPCToPolicyCallable 能访问到
        return super.eventLoop();
    }

    @Override
    protected void sendCallRequest(Request request, final OutgoingCallback callback) {
        Map.Entry<Connection, ComponentInfo> destination;
        synchronized (mDestinations) {
            destination = mDestinations.remove(request.getId());
        }

        if (destination == null) {
            throw new AssertionError("destination != null");
        }

        ParcelRequest parcelRequest = (ParcelRequest) request;
        parcelRequest.changeConnectionId(""); // 让接收端识别到发送自 Master，而不是由 Master 转发

        if (destination.getKey() != null && destination.getValue() == null) {
            destination.getKey().write(parcelRequest, new OutgoingCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });

            return;
        }

        if (destination.getValue() != null) {
            CallForwarder forwarder = new CallForwarder(mContext,
                    destination.getKey(), destination.getValue());
            forwarder.forward(parcelRequest, new OutgoingCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });

            return;
        }

        throw new AssertionError("!(connection == null && componentInfo == null)");
    }

    @Override
    public Response call(Request req) throws CallException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cancelable call(Request req, ResponseCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cancelable callStickily(Request req, StickyResponseCallback callback) {
        throw new UnsupportedOperationException();
    }

    public Cancelable call(Connection connection, String path, @Nullable ResponseCallback callback) {
        return call(connection, path, null, callback);
    }

    public Cancelable call(
            Connection connection, String path,
            @Nullable Param param, @Nullable ResponseCallback callback) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection argument is null.");
        }
        if (!ParcelRequest.validatePath(path)) {
            throw new IllegalArgumentException("path argument is illegal.");
        }

        return call(connection, null, createRequest(path, param, callback != null), callback);
    }

    Cancelable call(
            Connection connection, ComponentInfo componentInfo,
            Request req, ResponseCallback callback) {
        if (connection == null && componentInfo == null) {
            throw new AssertionError("!(connection == null && componentInfo == null)");
        }

        synchronized (mDestinations) {
            if (mDestinations.put(req.getId(),
                    new AbstractMap.SimpleEntry<>(connection, componentInfo)) != null) {
                throw new IllegalStateException(
                        "The request has already called. reqId=" + req.getId());
            }
        }

        return super.call(req, callback);
    }

    public Cancelable call(
            @Nullable Connection connection,
            ComponentInfo componentInfo,
            String path, @Nullable ResponseCallback callback) {
        return call(connection, componentInfo, path, null, callback);
    }

    public Cancelable call(
            @Nullable Connection connection,
            ComponentInfo componentInfo,
            String path,
            @Nullable Param param, @Nullable ResponseCallback callback) {
        if (componentInfo == null) {
            throw new IllegalArgumentException("componentInfo argument is null.");
        }
        if (!ParcelRequest.validatePath(path)) {
            throw new IllegalArgumentException("path argument is illegal.");
        }

        return call(connection, componentInfo,
                createRequest(path, param, callback != null), callback);
    }

    private ParcelRequest createRequest(String path, Param param, boolean hasCallback) {
        return new ParcelRequest(
                new ParcelRequestContext.Builder(
                        ParcelRequestContext.RESPONDER_TYPE_SKILL_OR_SERVICE).
                        setRequesterType(ParcelRequestContext.REQUESTER_TYPE_MASTER).
                        build(),
                new ParcelRequestConfig.Builder().
                        setHasCallback(hasCallback).
                        setStickily(false).
                        setTimeout(TIMEOUT).
                        build(),
                path,
                (AbstractParam) param
        );
    }
}