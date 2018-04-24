package com.ubtrobot.master.service;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.call.AbstractCallComponent;
import com.ubtrobot.master.competition.CompetingItemDetail;
import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.competition.CompetitionSessionInfo;
import com.ubtrobot.master.context.DelegateContext;
import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.message.Param;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-10-26.
 */

public abstract class MasterService extends AbstractCallComponent implements MasterContext {

    private final Unsafe mUnsafe;
    private DelegateContext mContextDelegate;
    private String mName;

    public MasterService() {
        super(Unsafe.get());
        mUnsafe = Unsafe.get();
    }

    @Override
    protected final void onCallableCreate() {
        mName = mUnsafe.getServiceLifecycle().onServiceCreate(getClass(), this);
        mContextDelegate = new DelegateContext(mUnsafe, this,
                ParcelRequestContext.REQUESTER_TYPE_SERVICE, mName);
        mContextDelegate.open();

        onServiceCreate();
    }

    @Override
    public String getName() {
        return mName;
    }

    protected void onServiceCreate() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // For Override
        return null;
    }

    protected List<CompetingItemDetail> getCompetingItems() {
        // For Override
        return Collections.emptyList();
    }

    protected void onCompetitionSessionActive(CompetitionSessionInfo sessionInfo) {
        // For Override
    }

    public final CompetitionSessionInfo getActiveCompetitionSession(String competingItemId)
            throws CompetitionSessionNotFoundException {
        CompetitionSessionInfo sessionInfo = mUnsafe.getServiceLifecycle().
                getActiveCompetitionSessionByItemId(getClass(), competingItemId);
        if (sessionInfo == null) {
            throw new CompetitionSessionNotFoundException();
        }

        return sessionInfo;
    }

    public final boolean isCompetitionSessionActive(String sessionId) {
        return mUnsafe.getServiceLifecycle().
                getActiveCompetitionSession(getClass(), sessionId) != null;
    }

    protected void onCompetitionSessionInactive(CompetitionSessionInfo sessionInfo) {
        // For Override
    }

    @Override
    public final void subscribe(EventReceiver receiver, String action) {
        mContextDelegate.subscribe(receiver, action);
    }

    @Override
    public final void unsubscribe(EventReceiver receiver) {
        mContextDelegate.unsubscribe(receiver);
    }

    public void publish(String action) {
        mUnsafe.getPublisher().publish(action);
    }

    public void publish(String action, Param param) {
        mUnsafe.getPublisher().publish(action, param);
    }

    @Override
    public final ServiceProxy createSystemServiceProxy(String serviceName) {
        return mContextDelegate.createSystemServiceProxy(serviceName);
    }

    @Override
    public final ServiceProxy createServiceProxy(String packageName, String serviceName) {
        return mContextDelegate.createServiceProxy(packageName, serviceName);
    }

    @Override
    public CompetitionSession openCompetitionSession() {
        return mContextDelegate.openCompetitionSession();
    }

    @Override
    public final void startMasterService(String service) {
        mContextDelegate.startMasterService(service);
    }

    @Override
    protected final void onCallableDestroy() {
        onServiceDestroy();

        mUnsafe.getServiceLifecycle().onServiceDestroy(getClass());
        mContextDelegate.close();
    }

    protected void onServiceDestroy() {

    }

    @Override
    protected final IPCResponder newRemoteResponder(Connection connection, ParcelRequest request) {
        return new IPCResponder(connection, request);
    }

    public static class CompetitionSessionNotFoundException extends Exception {
    }
}