package com.ubtrobot.master.skill;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.call.AbstractCallComponent;
import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.context.DelegateContext;
import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.message.Param;

/**
 * Created by column on 17-10-26.
 */

public abstract class MasterSkill extends AbstractCallComponent implements MasterContext {

    public static final String STATE_DEFAULT = ParamBundleConstants.VAL_SKILL_STATE_DEFAULT;

    private final Unsafe mUnsafe;
    private DelegateContext mContextDelegate;
    private String mName;

    public MasterSkill() {
        super(Unsafe.get());

        mUnsafe = Unsafe.get();
    }

    @Override
    protected final void onCallableCreate() {
        mName = mUnsafe.getSkillLifecycle().onSkillCreate(getClass(), this);
        mContextDelegate = new DelegateContext(mUnsafe, this,
                ParcelRequestContext.REQUESTER_TYPE_SKILL, mName);
        mContextDelegate.open();

        onSkillCreate();
    }

    @Override
    public final String getName() {
        return mName;
    }

    protected void onSkillCreate() {

    }

    protected abstract void onSkillStart();

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public final void subscribe(EventReceiver receiver, String action) {
        mContextDelegate.subscribe(receiver, action);
    }

    @Override
    public final void unsubscribe(EventReceiver receiver) {
        mContextDelegate.unsubscribe(receiver);
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

    /**
     * 设置状态
     *
     * @param state 状态
     */
    public void setState(String state) {
        if (TextUtils.isEmpty(state)) {
            throw new IllegalArgumentException("Argument state is empty.");
        }

        mUnsafe.getSkillLifecycle().setState(getClass(), state);
    }

    /**
     * 获取状态
     *
     * @return 状态
     */
    public String getState() {
        return mUnsafe.getSkillLifecycle().getState(getClass());
    }

    @Override
    public final void startMasterService(String service) {
        mContextDelegate.startMasterService(service);
    }

    protected abstract void onSkillStop();

    public final void stopSkill() {
        mUnsafe.getSkillLifecycle().stopSkill(this);
    }

    @Override
    protected final void onCallableDestroy() {
        mUnsafe.getSkillLifecycle().onSkillDestroy(getClass());
        mContextDelegate.close();

        onSkillDestroy();
    }

    protected void onSkillDestroy() {

    }

    @Override
    protected final IPCResponder newRemoteResponder(Connection connection, ParcelRequest request) {
        return new IPCResponder(connection, request) {

            @Override
            public void respondStickily(Param param) {
                throw new UnsupportedOperationException("A master skill can NOT respondStickily.");
            }
        };
    }
}