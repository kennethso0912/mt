package com.ubtrobot.master.call;

import android.os.Handler;
import android.support.annotation.Nullable;

import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.StickyResponseCallback;

/**
 * Created by column on 17-11-29.
 */

public abstract class BaseConvenientStickyCallable extends BaseConvenientCallable
        implements ConvenientStickyCallable {

    protected BaseConvenientStickyCallable(
            IPCByMasterCallable callable,
            Handler mainThreadHandler,
            ParcelRequestContext context,
            ParcelRequestConfig.Builder builder) {
        super(callable, mainThreadHandler, context, builder);
    }

    @Override
    public Cancelable callStickily(String path, StickyResponseCallback callback) {
        return callStickily(path, null, callback);
    }

    @Override
    public Cancelable callStickily(
            String path, @Nullable Param param, final StickyResponseCallback callback) {
        if (!ParcelRequest.validatePath(path)) {
            throw new IllegalArgumentException("Illegal path argument. Should be \"/xxx/.../...\"");
        }

        if (callback == null) {
            throw new IllegalArgumentException("Callback argument is null.");
        }

        ParcelRequestConfig config = configBuilder().
                setHasCallback(true).
                setStickily(true).
                build();
        return callable().callStickily(
                new ParcelRequest(getContext(), config, path, (AbstractParam) param),
                new StickyResponseCallback() {
                    @Override
                    public void onResponseStickily(final Request req, final Response res) {
                        mainThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResponseStickily(req, res);
                            }
                        });
                    }

                    @Override
                    public void onResponseCompletely(final Request req, final Response res) {
                        mainThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResponseCompletely(req, res);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final Request req, final CallException e) {
                        mainThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(req, e);
                            }
                        });
                    }
                }
        );
    }
}
