package com.ubtrobot.master.context;

import android.content.ComponentName;
import android.content.Intent;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.competition.CompetitionSessionImpl;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.service.ServiceProxyImpl;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;

/**
 * Created by column on 17-12-6.
 */

public abstract class BaseContext implements MasterContext {

    private final Unsafe mUnsafe;
    private final String contextType;
    private final String contextName;

    public BaseContext(Unsafe unsafe, String contextType, String contextName) {
        mUnsafe = unsafe;
        this.contextType = contextType;
        this.contextName = contextName;
    }

    @Override
    public ServiceProxy createSystemServiceProxy(String serviceName) {
        return new ServiceProxyImpl(
                mUnsafe.getGlobalCallMasterCallable(),
                mUnsafe.getIpcByMasterCallable(),
                mUnsafe.getHandlerOnMainThread(),
                new ParcelRequestContext.Builder(
                        ParcelRequestContext.RESPONDER_TYPE_SERVICE, serviceName).
                        setRequesterType(contextType).
                        setRequester(contextName).
                        build()
        );
    }

    @Override
    public ServiceProxy createServiceProxy(String packageName, String serviceName) {
        return new ServiceProxyImpl(
                mUnsafe.getGlobalCallMasterCallable(),
                mUnsafe.getIpcByMasterCallable(),
                mUnsafe.getHandlerOnMainThread(),
                new ParcelRequestContext.Builder(
                        ParcelRequestContext.RESPONDER_TYPE_SERVICE, serviceName).
                        setRequesterType(contextType).
                        setRequester(contextName).
                        setResponderPackage(packageName).
                        build()
        );
    }

    @Override
    public void startMasterService(final String service) {
        mUnsafe.getMasterConnection().eventLoop().post(new Runnable() {
            @Override
            public void run() {
                ServiceInfo serviceInfo = mUnsafe.getComponentInfoSource().getServiceInfo(service);
                if (serviceInfo == null) {
                    throw new IllegalStateException("Can NOT start master service. " +
                            "service " + service + " NOT found.");
                }

                if (mUnsafe.getApplicationContext().startService(new Intent().
                        setComponent(new ComponentName(
                                serviceInfo.getPackageName(), serviceInfo.getClassName()
                        ))) == null) {
                    throw new IllegalStateException("Can NOT start master service. " +
                            "Unknown reason. Maybe master internal error or system error???");
                }
            }
        });
    }

    @Override
    public CompetitionSession openCompetitionSession() {
        return new CompetitionSessionImpl(this, mUnsafe);
    }
}