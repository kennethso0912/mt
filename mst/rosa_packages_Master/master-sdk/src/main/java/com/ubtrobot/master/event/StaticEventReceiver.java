package com.ubtrobot.master.event;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ubtrobot.master.Master;
import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.annotation.SubscribeAnnotationLoader;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.IntentExtras;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.ulog.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by column on 17-9-16.
 */

public abstract class StaticEventReceiver extends Service implements EventReceiver {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("StaticEventReceiver");

    private Unsafe mUnsafe;

    private Map<String, Method> mReceivers;

    @Override
    public final void onCreate() {
        super.onCreate();

        try {
            Master.get();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("You should initialize Master first in your Application.onCreate");
        }

        mUnsafe = Unsafe.get();
        mReceivers = SubscribeAnnotationLoader.loadReceiveMethods(getClass());
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public final int onStartCommand(@Nullable final Intent intent, int flags, final int startId) {
        final Event event = parseEvent(intent);
        if (event == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        // 投递到 Connection 的 EventLoop 中，那么该任务将在验证配置的任务之后执行，见 Master 初始化过程
        // onStartCommand 在主线程执行，可能抢在验证配置的任务之前执行
        mUnsafe.getMasterConnection().eventLoop().post(new Runnable() {
            @Override
            public void run() {
                dispatchEvent(event, startId);
            }
        });
        return START_NOT_STICKY;
    }

    private Event parseEvent(Intent intent) {
        if (intent != null) {
            try {
                return intent.getParcelableExtra(IntentExtras.KEY_EVENT);
            } catch (ClassCastException e) {
                LOGGER.e(e);
            }
        }

        return null;
    }

    private void dispatchEvent(final Event event, final int startId) {
        mUnsafe.getHandlerOnMainThread().post(new Runnable() {
            @Override
            public void run() {
                Method method = mReceivers.get(event.getAction());
                EventReceiverContext context = new EventReceiverContext(mUnsafe); // TODO
                if (method == null) {
                    onReceive(context, event);
                    return;
                }

                //noinspection TryWithIdenticalCatches
                try {
                    method.invoke(StaticEventReceiver.this, context, event);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException(e);
                } finally {
                    stopSelf(startId);
                }
            }
        });
    }

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public final void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public final boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
    }
}