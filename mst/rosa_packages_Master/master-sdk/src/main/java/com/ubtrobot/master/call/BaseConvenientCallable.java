package com.ubtrobot.master.call;

import android.os.Handler;
import android.support.annotation.Nullable;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-11-29.
 */

public class BaseConvenientCallable implements ConvenientCallable {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("BaseConvenientCallable");

    private final IPCByMasterCallable mCallable;
    private final Handler mMainThreadHandler;

    private final ParcelRequestContext mContext;
    private volatile CallConfiguration mConfiguration;
    private final ParcelRequestConfig.Builder mBuilder;

    protected BaseConvenientCallable(
            IPCByMasterCallable callable,
            Handler mainThreadHandler,
            ParcelRequestContext context,
            ParcelRequestConfig.Builder builder) {
        mCallable = callable;
        mMainThreadHandler = mainThreadHandler;

        mContext = context;
        mConfiguration = new CallConfiguration.Builder().build();
        mBuilder = builder;
    }

    protected Handler mainThreadHandler() {
        return mMainThreadHandler;
    }

    protected IPCByMasterCallable callable() {
        return mCallable;
    }

    public ParcelRequestContext getContext() {
        return mContext;
    }

    protected ParcelRequestConfig.Builder configBuilder() {
        return mBuilder;
    }

    @Override
    public void setConfiguration(CallConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Argument configuration is null.");
        }

        mConfiguration = configuration;
    }

    @Override
    public CallConfiguration getConfiguration() {
        return mConfiguration;
    }

    @Override
    public Response call(String path) throws CallException {
        return call(path, (Param) null);
    }

    @Override
    public Response call(String path, @Nullable Param param) throws CallException {
        if (!ParcelRequest.validatePath(path)) {
            throw new IllegalArgumentException("Illegal path argument. Should be \"/xxx/.../...\"");
        }

        return SyncCallUtils.syncCall(mCallable, mConfiguration, createRequest(path, param, true));
    }

    @Override
    public Cancelable call(String path, @Nullable ResponseCallback callback) {
        return call(path, null, callback);
    }

    @Override
    public Cancelable call(String path, @Nullable Param param, @Nullable ResponseCallback callback) {
        if (!ParcelRequest.validatePath(path)) {
            throw new IllegalArgumentException("Illegal path argument. Should be \"/xxx/.../...\"");
        }

        return mCallable.call(
                createRequest(path, param, callback != null),
                newMainThreadResponseCallback(callback)
        );
    }

    private Request createRequest(String path, Param param, boolean hasCallback) {
        ParcelRequestConfig config = mBuilder.
                setHasCallback(hasCallback).
                setStickily(false).
                setTimeout(mConfiguration.getTimeout()).
                build();
        return new ParcelRequest(mContext, config, path, (AbstractParam) param);
    }

    protected ResponseCallback newMainThreadResponseCallback(final ResponseCallback callback) {
        return callback == null ? null : new ResponseCallback() {
            @Override
            public void onResponse(final Request req, final Response res) {
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(req, res);
                    }
                });
            }

            @Override
            public void onFailure(final Request req, final CallException e) {
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(req, e);
                    }
                });
            }
        };
    }
}
