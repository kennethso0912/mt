package com.ubtrobot.master;

import android.content.Context;
import android.os.Handler;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.call.IPCByMasterCallable;
import com.ubtrobot.master.component.ComponentInfoSource;
import com.ubtrobot.master.event.LocalSubscriber;
import com.ubtrobot.master.event.RemotePublisher;
import com.ubtrobot.master.event.StaticEventReceiver;
import com.ubtrobot.master.service.MasterService;
import com.ubtrobot.master.service.MasterSystemService;
import com.ubtrobot.master.service.ServiceLifecycle;
import com.ubtrobot.master.skill.MasterSkill;
import com.ubtrobot.master.skill.SkillLifecycle;
import com.ubtrobot.master.transport.connection.AutoReconnectConnection;

/**
 * Created by column on 17-12-6.
 */

public class Unsafe {

    /**
     * 授权哪些 Class 能获取 Unsafe 对象
     */
    private static final String[] PERMISSIBLE_CLAZZS = new String[]{
            Master.class.getName(),
            StaticEventReceiver.class.getName(),
            MasterSkill.class.getName(),
            MasterService.class.getName(),
            MasterSystemService.class.getName(),
            "com.ubtrobot.master.policy.BasePolicyApplication" // 该类在其他模块，注意不能混淆
    };

    private Context applicationContext;
    private Handler handlerOnMainThread;

    private volatile boolean mComponentsValidated;

    private EventLoop callProcessLoop;
    private AutoReconnectConnection masterConnection;

    private ComponentInfoSource componentInfoSource;

    private CallMasterCallable globalCallMasterCallable;
    private IPCByMasterCallable ipcByMasterCallable;
    private CallRouter callRouter;

    private SkillLifecycle skillLifecycle;
    private ServiceLifecycle serviceLifecycle;

    private LocalSubscriber subscriberInternal;
    private LocalSubscriber subscriberForSdkUser;
    private RemotePublisher mPublisher;

    private static final Unsafe sSingleton = new Unsafe();

    private Unsafe() {
    }

    public static Unsafe get() {
        String clazz = new Throwable().getStackTrace()[1].getClassName();
        for (String permissibleClazz : PERMISSIBLE_CLAZZS) {
            if (permissibleClazz.equals(clazz)) {
                return sSingleton;
            }
        }

        throw new IllegalStateException(
                "Permission Denial. " + clazz + " is NOT allowed to use Unsafe");
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public AutoReconnectConnection getMasterConnection() {
        return masterConnection;
    }

    void setMasterConnection(AutoReconnectConnection masterConnection) {
        this.masterConnection = masterConnection;
    }

    public CallMasterCallable getGlobalCallMasterCallable() {
        return globalCallMasterCallable;
    }

    void setGlobalCallMasterCallable(CallMasterCallable globalCallMasterCallable) {
        this.globalCallMasterCallable = globalCallMasterCallable;
    }

    public EventLoop getCallProcessLoop() {
        return callProcessLoop;
    }

    void setCallProcessLoop(EventLoop callProcessLoop) {
        this.callProcessLoop = callProcessLoop;
    }

    public Handler getHandlerOnMainThread() {
        return handlerOnMainThread;
    }

    void setHandlerOnMainThread(Handler handlerOnMainThread) {
        this.handlerOnMainThread = handlerOnMainThread;
    }

    public CallRouter getCallRouter() {
        return callRouter;
    }

    void setCallRouter(CallRouter callRouter) {
        this.callRouter = callRouter;
    }

    public SkillLifecycle getSkillLifecycle() {
        return skillLifecycle;
    }

    void setSkillLifecycle(SkillLifecycle skillLifecycle) {
        this.skillLifecycle = skillLifecycle;
    }

    public ServiceLifecycle getServiceLifecycle() {
        return serviceLifecycle;
    }

    void setServiceLifecycle(ServiceLifecycle serviceLifecycle) {
        this.serviceLifecycle = serviceLifecycle;
    }

    public ComponentInfoSource getComponentInfoSource() {
        return componentInfoSource;
    }

    void setComponentInfoSource(ComponentInfoSource componentInfoSource) {
        this.componentInfoSource = componentInfoSource;
    }

    public LocalSubscriber getSubscriberInternal() {
        return subscriberInternal;
    }

    void setSubscriberInternal(LocalSubscriber subscriberInternal) {
        this.subscriberInternal = subscriberInternal;
    }

    public LocalSubscriber getSubscriberForSdkUser() {
        return subscriberForSdkUser;
    }

    void setSubscriberForSdkUser(LocalSubscriber subscriberForSdkUser) {
        this.subscriberForSdkUser = subscriberForSdkUser;
    }

    public IPCByMasterCallable getIpcByMasterCallable() {
        return ipcByMasterCallable;
    }

    void setIpcByMasterCallable(IPCByMasterCallable ipcByMasterCallable) {
        this.ipcByMasterCallable = ipcByMasterCallable;
    }

    public RemotePublisher getPublisher() {
        return mPublisher;
    }

    void setPublisher(RemotePublisher publisher) {
        mPublisher = publisher;
    }

    void notifyComponentsValidated() {
        synchronized (this) {
            mComponentsValidated = true;
            notifyAll();
        }
    }

    public void waitUntilComponentsValidated() {
        if (mComponentsValidated) {
            return;
        }

        synchronized (this) {
            while (true) {
                if (mComponentsValidated) {
                    return;
                }

                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}