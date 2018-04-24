package com.ubtrobot.master.call;

import android.support.annotation.Nullable;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.policy.PolicyConstants;
import com.ubtrobot.master.service.ServiceCallInfo;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.ResponseCallback;

import java.util.List;

/**
 * Created by column on 17-11-29.
 */

public class IPCToPolicyCallable {

    private static final int TIMEOUT = 5000;

    private final ComponentInfoPool mComponentInfoPool;
    private final CallDestinations mCallDestinations;
    private final IPCFromMasterCallable mCallable;

    public IPCToPolicyCallable(
            ComponentInfoPool componentInfoPool,
            CallDestinations callDestinations,
            IPCFromMasterCallable callable) {
        mComponentInfoPool = componentInfoPool;
        mCallDestinations = callDestinations;
        mCallable = callable;
    }

    public EventLoop eventLoop() {
        return mCallable.eventLoop();
    }

    public Cancelable call(String path, @Nullable ResponseCallback callback) {
        return call(path, null, callback);
    }

    public Cancelable call(String path, @Nullable Param param, @Nullable ResponseCallback callback) {
        List<ServiceCallInfo> callInfos = mComponentInfoPool.getServiceCallInfos(
                PolicyConstants.POLICY_SERVICE_NAME, path);
        ComponentInfo policyInfo = null;
        for (ServiceCallInfo callInfo : callInfos) {
            if (PolicyConstants.POLICY_PACKAGE_NAME.equals(
                    callInfo.getParentComponent().getPackageName())) {
                policyInfo = callInfo.getParentComponent();
            }
        }

        ParcelRequest request = createRequest(path, param, callback != null);
        if (policyInfo == null) {
            callbackFailure(callback, request, new CallException(CallGlobalCode.NOT_FOUND, "Not found."));
            return new Cancelable() {
                @Override
                public void cancel() {
                    // Nothing
                }
            };
        }

        return mCallable.call(
                mCallDestinations.getServiceCallDestination(
                        PolicyConstants.POLICY_PACKAGE_NAME,
                        PolicyConstants.POLICY_SERVICE_NAME
                ), policyInfo, request, callback
        );
    }

    private ParcelRequest createRequest(String path, Param param, boolean hasCallback) {
        return new ParcelRequest(
                new ParcelRequestContext.Builder(
                        ParcelRequestContext.RESPONDER_TYPE_SERVICE,
                        PolicyConstants.POLICY_SERVICE_NAME).
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

    private void callbackFailure(final ResponseCallback callback, final Request req, final CallException e) {
        if (callback == null) {
            return;
        }

        eventLoop().post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(req, e);
            }
        });
    }
}