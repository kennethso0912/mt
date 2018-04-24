package com.ubtrobot.master.call;

import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;

/**
 * Created by column on 10/09/2017.
 */

public class CallMasterCallable implements ConvenientCallable {

    private static final int TIMEOUT = 5000;

    private final String mContextType;
    private final String mContextName;
    private final IPCByMasterCallable mCallable;

    private final CallConfiguration mConfiguration = new CallConfiguration.Builder().
            suppressSyncCallOnMainThreadWarning(true).
            setTimeout(TIMEOUT).build();

    public CallMasterCallable(String contextType, String contextName, IPCByMasterCallable callable) {
        mContextType = contextType;
        mContextName = contextName;
        mCallable = callable;
    }

    @Override
    public void setConfiguration(CallConfiguration configuration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CallConfiguration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response call(String path) throws CallException {
        return call(path, (Param) null);
    }

    @Override
    public Response call(String path, Param param) throws CallException {
        ParcelRequest request = new ParcelRequest(
                new ParcelRequestContext.Builder(ParcelRequestContext.RESPONDER_TYPE_MASTER).
                        setRequesterType(mContextType).
                        setRequester(mContextName).
                        build(),
                new ParcelRequestConfig.Builder().
                        setHasCallback(true).
                        setStickily(false).
                        setTimeout(mConfiguration.getTimeout()).
                        build(),
                path,
                (AbstractParam) param
        );

        return SyncCallUtils.syncCall(mCallable, mConfiguration, request);
    }

    @Override
    public Cancelable call(String path, ResponseCallback callback) {
        return call(path, null, callback);
    }

    @Override
    public Cancelable call(String path, Param param, ResponseCallback callback) {
        ParcelRequest request = new ParcelRequest(
                new ParcelRequestContext.Builder(ParcelRequestContext.RESPONDER_TYPE_MASTER).
                        setRequesterType(mContextType).
                        setRequester(mContextName).
                        build(),
                new ParcelRequestConfig.Builder().
                        setHasCallback(true).
                        setStickily(false).
                        setTimeout(mConfiguration.getTimeout()).
                        build(),
                path,
                (AbstractParam) param
        );
        return mCallable.call(request, callback);
    }
}