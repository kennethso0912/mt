package com.ubtrobot.master;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.ubtrobot.concurrent.impl.HandlerThreadEventLoop;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.call.IPCByMasterCallable;
import com.ubtrobot.master.component.ComponentInfoSource;
import com.ubtrobot.master.component.validate.ComponentValidator;
import com.ubtrobot.master.component.validate.ValidateException;
import com.ubtrobot.master.context.ContextRunnable;
import com.ubtrobot.master.context.GlobalContext;
import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.event.LocalSubscriber;
import com.ubtrobot.master.event.RemotePublisher;
import com.ubtrobot.master.interactor.InteractorManager;
import com.ubtrobot.master.interactor.MasterInteractor;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.MasterService;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.service.ServiceLifecycle;
import com.ubtrobot.master.skill.MasterSkill;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillLifecycle;
import com.ubtrobot.master.transport.connection.AutoReconnectConnection;
import com.ubtrobot.master.transport.connection.ConnectionInitializer;
import com.ubtrobot.master.transport.connection.handler.EventHandler;
import com.ubtrobot.master.transport.connection.handler.FromMasterRequestHandler;
import com.ubtrobot.master.transport.connection.handler.LoggingHandler;
import com.ubtrobot.master.transport.connection.handler.MessageCodec;
import com.ubtrobot.master.transport.connection.handler.MessageSplitter;
import com.ubtrobot.master.transport.connection.handler.RemoteSubscribeHandler;
import com.ubtrobot.master.transport.connection.handler.RequestHandler;
import com.ubtrobot.master.transport.connection.handler.ResponseHandler;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.LoggerFactory;

import java.util.HashMap;

/**
 * Created by column on 17-8-29.
 */

/**
 * 机器人服务 Master ，单例
 */
public class Master {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("Master");

    private static volatile Context sApplicationContext;

    @SuppressLint("StaticFieldLeak")
    private static Master sMaster;

    private final Unsafe mUnsafe;
    private final GlobalContext mGlobalContext;

    private final InteractorManager mInteractorManager;

    /**
     * 初始化机器人服务 Master
     *
     * @param context Android 上下文
     */
    public static void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Master initialized with null context."
            );
        }

        if (sApplicationContext == null) {
            synchronized (Master.class) {
                if (sApplicationContext == null) {
                    sApplicationContext = context.getApplicationContext();
                    sMaster = new Master();
                    return;
                }

                LOGGER.w("Master has been initialized before.");
            }
        }
    }

    private Master() {
        mUnsafe = Unsafe.get();
        mUnsafe.setApplicationContext(sApplicationContext);

        mInteractorManager = new InteractorManager(mUnsafe);

        final ComponentValidator componentValidator = new ComponentValidator(sApplicationContext);
        try {
            componentValidator.validatePermissionOnly();
        } catch (ValidateException e) {
            throw new IllegalStateException(e);
        }

        // 线程相关对象初始化
        // call 处理专用线程，MasterSkill | Master Service 中 @Call 注解的方法及 onCall 方法执行的线程
        mUnsafe.setCallProcessLoop(new HandlerThreadEventLoop());
        mUnsafe.setHandlerOnMainThread(new Handler(sApplicationContext.getMainLooper()));
        // 所有 connection 相关的操作（IO）都在同一个线程的循环中执行
        HandlerThreadEventLoop connectionEventLoop = new HandlerThreadEventLoop();

        // 确保第一次 post 的任务是验证组件配置，确保其他任务在该任务后执行
        // 这样也能保证 Connection 中相关操作都在验证任务后执行，Connection 相关操作都在 connectionEventLoop 中
        final MasterComponentInfoSource componentInfoSource = new MasterComponentInfoSource();
        mUnsafe.setComponentInfoSource(componentInfoSource);
        connectionEventLoop.post(new Runnable() {
            @Override
            public void run() {
                try {
                    componentValidator.validate();

                    componentInfoSource.onValidateSuccess(componentValidator);
                    mUnsafe.notifyComponentsValidated();
                } catch (ValidateException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        });

        // 与 Master 的连接的对象初始化，断开（比如：Master 被杀）情况该对象会自动处理重连
        mUnsafe.setMasterConnection(new AutoReconnectConnection(
                sApplicationContext, connectionEventLoop, new ConnectionInitializer() {
            @Override
            public void onConnectionInitialize(Connection connection) {
                // 会在 connectionEventLoop 循环中执行，在验证任务后执行
                connection.pipeline().add(new LoggingHandler());
                connection.pipeline().add(new MessageCodec());
                connection.pipeline().add(new MessageSplitter(
                        new EventHandler(
                                mUnsafe.getSubscriberInternal(),
                                mUnsafe.getSubscriberForSdkUser()
                        ),
                        new RequestHandler(
                                mUnsafe.getCallRouter(),
                                new FromMasterRequestHandler(
                                        mUnsafe.getSkillLifecycle(),
                                        mUnsafe.getServiceLifecycle())
                        ),
                        new ResponseHandler(mUnsafe.getIpcByMasterCallable())
                ));
                connection.pipeline().add(new RemoteSubscribeHandler(
                        mUnsafe.getSubscriberInternal(),
                        mUnsafe.getSubscriberForSdkUser(),
                        mUnsafe.getGlobalCallMasterCallable()
                ));
            }
        }));

        LocalSubscriber.FirstTimeSubscribeListener listener =
                new LocalSubscriber.FirstTimeSubscribeListener() {

                    boolean callbacked;

                    @Override
                    public synchronized void onFirstTimeSubscribe() {
                        if (callbacked) {
                            return;
                        }
                        callbacked = true;

                        mUnsafe.getMasterConnection().connect(new OutgoingCallback() {
                            @Override
                            public void onSuccess() {
                                // NOOP
                            }

                            @Override
                            public void onFailure(Exception e) {
                                LOGGER.e(e, "Connect master failed.");
                            }
                        });
                    }
                };

        // 事件订阅相关对象初始化
        mUnsafe.setSubscriberInternal(new LocalSubscriber(listener));
        mUnsafe.setSubscriberForSdkUser(new LocalSubscriber(listener));
        mUnsafe.setPublisher(new RemotePublisher(mUnsafe.getMasterConnection()));

        // 发送调用的相关对象初始化
        mUnsafe.setIpcByMasterCallable(new IPCByMasterCallable(mUnsafe.getMasterConnection())); // 经由 Master 转发
        mUnsafe.setGlobalCallMasterCallable(new CallMasterCallable(
                ParcelRequestContext.REQUESTER_TYPE_GLOBAL_CONTEXT, null,
                mUnsafe.getIpcByMasterCallable())); // Master 接收处理

        // 收到的请求路由的对象，路由到 MasterSkill | Master Service 中 @Call 注解的方法及 onCall 方法
        mUnsafe.setCallRouter(new CallRouter(sApplicationContext, componentInfoSource));
        // Skill 生命周期管理对象
        mUnsafe.setSkillLifecycle(new SkillLifecycle(
                sApplicationContext, mUnsafe.getHandlerOnMainThread(),
                componentInfoSource, mUnsafe.getGlobalCallMasterCallable())
        );
        // Service 生命周期管理对象
        mUnsafe.setServiceLifecycle(new ServiceLifecycle(sApplicationContext, componentInfoSource,
                mUnsafe.getGlobalCallMasterCallable(), mUnsafe.getCallProcessLoop()));

        mGlobalContext = new GlobalContext(mUnsafe);
    }

    /**
     * 获取机器人服务 Master 单例
     *
     * @return 机器人服务 Master 单例
     */
    public static Master get() {
        if (sApplicationContext == null) {
            synchronized (Master.class) {
                if (sApplicationContext == null) {
                    throw new IllegalStateException("Master MUST be initialized first.");
                }
            }
        }

        return sMaster;
    }

    /**
     * 设置日志工厂。默认情况只打印警告及以上级别的日志。
     *
     * @param loggerFactory 日志工厂
     */
    public void setLoggerFactory(LoggerFactory loggerFactory) {
        MasterLoggerFactory.setup(loggerFactory);
    }

    /**
     * 获取全局上下文
     *
     * @return 全局上下文
     */
    public MasterContext getGlobalContext() {
        return mGlobalContext;
    }

    /**
     * 获取交互器（未创建情况先创建）
     *
     * @param name 交互器名称。用于在政策服务中配置
     * @return 交互器
     */
    public MasterInteractor getOrCreateInteractor(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Argument name is empty.");
        }

        return mInteractorManager.getOrCreateInteractor(name);
    }

    /**
     * 在某个上下文下执行任务，用于在 MasterSkill 或 MasterService 组件类外部调用其方法
     *
     * @param contextClass MasterSkill 或 MasterService 的 class
     * @param runnable     任务。注意：runnable.run(context) 执行在调用者线程。run 返回后，execute 才会返回
     * @param <T>          MasterContext 范型参数，只支持 MasterSkill 和 MasterService
     * @return 是否执行。如果 MasterSkill 和 MasterService 没有创建或超出生命周期范围，将不会执行，返回 false
     */
    @SuppressWarnings("unchecked")
    public <T extends MasterContext> boolean execute(
            Class<T> contextClass, ContextRunnable<T> runnable) {
        if (contextClass == null || runnable == null) {
            throw new IllegalArgumentException("Arguments contextClass or runnable are null.");
        }

        if (isChild(MasterSkill.class, contextClass)) {
            MasterSkill livingSkill = mUnsafe.getSkillLifecycle().
                    getLivingSkill((Class<? extends MasterSkill>) contextClass);
            if (livingSkill == null) {
                return false;
            }

            runnable.run((T) livingSkill);
            return true;
        }

        if (isChild(MasterService.class, contextClass)) {
            MasterService livingService = mUnsafe.getServiceLifecycle().
                    getLivingService((Class<? extends MasterService>) contextClass);
            if (livingService == null) {
                return false;
            }

            runnable.run((T) livingService);
            return true;
        }

        throw new IllegalArgumentException(
                "Argument contextClass only support subClass of MasterSkill of MasterService");
    }

    private boolean isChild(Class parent, Class child) {
        //noinspection unchecked
        return parent.isAssignableFrom(child) && parent != child;
    }

    private class MasterComponentInfoSource implements ComponentInfoSource {

        private final HashMap<String, SkillInfo> mNameSkillInfoMap = new HashMap<>();
        private final HashMap<String, SkillInfo> mClassSkillInfoMap = new HashMap<>();

        private final HashMap<String, ServiceInfo> mNameServiceInfoMap = new HashMap<>();
        private final HashMap<String, ServiceInfo> mClassServiceInfoMap = new HashMap<>();

        void onValidateSuccess(ComponentValidator validator) {
            for (SkillInfo skillInfo : validator.getSkillInfoList()) {
                mNameSkillInfoMap.put(skillInfo.getName(), skillInfo);
                mClassSkillInfoMap.put(skillInfo.getClassName(), skillInfo);
            }

            for (ServiceInfo serviceInfo : validator.getServiceInfoList()) {
                mNameServiceInfoMap.put(serviceInfo.getName(), serviceInfo);
                mClassServiceInfoMap.put(serviceInfo.getClassName(), serviceInfo);
            }
        }

        @Override
        public SkillInfo getSkillInfo(String name) {
            return mNameSkillInfoMap.get(name);
        }

        @Override
        public SkillInfo getSkillInfo(Class<? extends MasterSkill> skillClass) {
            return mClassSkillInfoMap.get(skillClass.getName());
        }

        @Override
        public ServiceInfo getServiceInfo(String name) {
            return mNameServiceInfoMap.get(name);
        }

        @Override
        public ServiceInfo getServiceInfo(Class<? extends MasterService> serviceClass) {
            return mClassServiceInfoMap.get(serviceClass.getName());
        }

        @Override
        public boolean requestSystemServicePermission() {
            // TODO
            return false;
        }
    }
}