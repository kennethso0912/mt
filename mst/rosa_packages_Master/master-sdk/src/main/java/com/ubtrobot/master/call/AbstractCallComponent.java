package com.ubtrobot.master.call;

import android.app.Service;
import android.content.Intent;

import com.ubtrobot.master.Master;
import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.annotation.CallAnnotationsLoader;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.IntentExtras;
import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.ulog.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-9-18.
 */

public abstract class AbstractCallComponent extends Service {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("AbstractCallComponent");

    private Unsafe mUnsafe;

    private final CallRoute mCallRoute = new CallRoute();
    private Map<String, Method> mCalls;
    private final HashMap<String, IPCResponder> mResponders = new HashMap<>();

    protected AbstractCallComponent(Unsafe unsafe) {
        mUnsafe = unsafe;
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        try {
            Master.get();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("You should initialize Master first in your Application.onCreate");
        }

        mUnsafe.waitUntilComponentsValidated();
        mCalls = CallAnnotationsLoader.loadCallMethods(getClass());
        mUnsafe.getCallRouter().addRoute(getClass(), mCallRoute);

        onCallableCreate();
    }

    public abstract String getName();

    protected void onCallableCreate() {
        // NOOP, for override
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public final int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null) {
            // masterContext.startMasterService | 外部非法启动
            // TODO 验证
            return START_STICKY;
        }

        try {
            ParcelRequest request = intent.getParcelableExtra(IntentExtras.KEY_REQUEST);
            if (request != null) {
                postRequestToConnection(request);
            }
        } catch (ClassCastException e) {
            LOGGER.e(e);
        }

        return START_STICKY;
    }

    // 将 Request 投递到 Connection，伪造出从 Connection 收到了 Request，
    // 从而复用真正从 Connection 中收到 Request 的处理过程，因此此处不直接处理 Request
    private void postRequestToConnection(ParcelRequest request) {
        mUnsafe.getMasterConnection().onRead(new ParcelMessage(request), new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // Ignore
            }

            @Override
            public void onFailure(Exception e) {
                // Master 连接不上
                LOGGER.e(e, "Request from onStartCommand pass to pipeline failed.");
            }
        });
    }

    protected void onCall(Request request, Responder responder) {
        responder.respondFailure(CallGlobalCode.NOT_IMPLEMENTED, "Not implemented.");
    }

    @Override
    public final void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public final boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void handleRequest(final ParcelRequest request) {
        if (request.getConfig().isCancelPrevious()) {
            IPCResponder responder = mResponders.get(request.getConfig().getPreviousRequestId());
            if (responder != null) {
                responder.cancel(mUnsafe.getHandlerOnMainThread());
            }

            return;
        }

        final IPCResponder responder = newRemoteResponder(mUnsafe.getMasterConnection(), request);
        responder.startTimeoutTimingIfCan();
        mResponders.put(request.getId(), responder);

        responder.setUnavailableListener(new IPCResponder.UnavailableListener() {
            @Override
            public void onUnavailable() {
                mUnsafe.getMasterConnection().eventLoop().post(new Runnable() {
                    @Override
                    public void run() {
                        mResponders.remove(request.getId());
                    }
                });
            }
        });

        mUnsafe.getCallProcessLoop().post(new Runnable() {
            @Override
            public void run() {
                Method method = mCalls.get(request.getPath());
                if (method == null) {
                    onCall(request, responder);
                    return;
                }

                //noinspection TryWithIdenticalCatches
                try {
                    method.invoke(AbstractCallComponent.this, request, responder);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    protected abstract IPCResponder newRemoteResponder(Connection connection, ParcelRequest request);

    @Override
    public final void onDestroy() {
        super.onDestroy();

        mUnsafe.getCallRouter().removeRoute(mCallRoute);
        onCallableDestroy();
    }

    protected void onCallableDestroy() {
        // NOOP, for override
    }

    private class CallRoute implements CallRouter.Route {

        @Override
        public void onRoute(ParcelRequest request) {
            handleRequest(request);
        }
    }
}