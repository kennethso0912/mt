package com.ubtrobot.master.transport.message;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.AbstractParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Callable;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.transport.message.StickyResponseCallback;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by column on 03/09/2017.
 */
// TODO 处理连接断开的问题
public abstract class AbstractIPCCallable implements Callable {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("AbstractIPCCallable");

    private final EventLoop mEventLoop;

    private final HashMap<String, Call> mCalls = new HashMap<>();
    private final HashMap<String, StickyCall> mStickyCalls = new HashMap<>();

    public AbstractIPCCallable(EventLoop eventLoop) {
        mEventLoop = eventLoop;
    }

    protected EventLoop eventLoop() {
        return mEventLoop;
    }

    @Override
    public Response call(Request req) throws CallException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cancelable call(final Request req, final ResponseCallback callback) {
        Call call = new Call(req, callback);
        if (mCalls.put(req.getId(), call) == null) {
            setupCallTimeout(call);
        } else {
            LOGGER.e("Call repeatedly with the same request id. request=%s", req);
        }

        sendCallRequest(req, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // NOOP
            }

            @Override
            public void onFailure(Exception e) {
                Call call = mCalls.remove(req.getId());
                if (call == null) {
                    return;
                }

                if (call.timeout != null) {
                    call.timeout.cancel();
                }

                if (call.callback != null) {
                    if (e instanceof CallException) {
                        call.callback.onFailure(req, (CallException) e);
                    } else {
                        call.callback.onFailure(req, new CallException(
                                CallGlobalCode.INTERNAL_ERROR, "Internal error.", e));
                    }
                }
            }
        });

        return new CallCancelable() {
            @Override
            protected void doCancel() {
                Call call = mCalls.remove(req.getId());
                if (call != null) {
                    call.timeout.cancel();

                    cancelRequest((AbstractParcelRequest) req);
                }
            }
        };
    }

    protected abstract void sendCallRequest(Request req, OutgoingCallback callback);

    private void setupCallTimeout(final Call call) {
        if (call.callback == null) {
            return;
        }

        call.timeout = mEventLoop.postDelay(new Runnable() {
            @Override
            public void run() {
                if (mCalls.remove(call.req.getId()) != null) {
                    call.callback.onFailure(
                            call.req,
                            new CallException(
                                    CallGlobalCode.RESPOND_TIMEOUT,
                                    "Respond timeout. callPath=" + call.req.getPath() +
                                            " , reqId=" + call.req.getId()
                            )
                    );
                }
            }
        }, ((AbstractParcelRequest) call.req).getConfig().getTimeout(), TimeUnit.MILLISECONDS);
    }

    private void cancelRequest(final AbstractParcelRequest req) {
        ParcelRequestConfig config = new ParcelRequestConfig.Builder().
                setCancelPrevious(true).
                setPreviousRequestId(req.getId()).
                build();
        // TODO 隐式请求处理
        ParcelRequest cancelRequest = new ParcelRequest(req.getContext(), config, req.getPath());
        sendCallRequest(cancelRequest, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // NOOP
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Cancel request failed. canceledRequestId=%s", req.getId());
            }
        });
    }

    public void callbackResponse(final Response response) {
        runInEventLoop(new Runnable() {
            @Override
            public void run() {
                Call call = mCalls.remove(response.getRequestId());
                if (call != null) {
                    if (call.timeout != null) {
                        call.timeout.cancel();
                    }

                    if (call.callback != null) {
                        call.callback.onResponse(call.req, response);
                    }
                }

                StickyCall stickyCall = mStickyCalls.remove(response.getRequestId());
                if (stickyCall != null) {
                    stickyCall.callback.onResponseCompletely(stickyCall.req, response);
                }
            }
        });
    }

    private void runInEventLoop(final Runnable runnable) {
        if (mEventLoop.inEventLoop()) {
            runnable.run();
        } else {
            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            });
        }
    }

    public void callbackResponse(final String requestId, final CallException e) {
        runInEventLoop(new Runnable() {
            @Override
            public void run() {
                Call call = mCalls.remove(requestId);
                if (call != null) {
                    if (call.timeout != null) {
                        call.timeout.cancel();
                    }

                    if (call.callback != null) {
                        call.callback.onFailure(call.req, e);
                    }
                }

                StickyCall stickyCall = mStickyCalls.remove(requestId);
                if (stickyCall != null) {
                    stickyCall.callback.onFailure(stickyCall.req, e);
                }
            }
        });
    }

    @Override
    public Cancelable callStickily(final Request req, final StickyResponseCallback callback) {
        final StickyCall stickyCall = new StickyCall(req, callback);
        if (mStickyCalls.put(req.getId(), stickyCall) != null) {
            LOGGER.e("Stick call repeatedly with the same request id. request=%s", req);
        }

        sendCallRequest(req, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // NOOP
            }

            @Override
            public void onFailure(Exception e) {
                StickyCall stickyCall = mStickyCalls.remove(req.getId());
                if (stickyCall == null) {
                    return;
                }

                callback.onFailure(req,
                        new CallException(CallGlobalCode.INTERNAL_ERROR, "Internal error.", e));
            }
        });

        return new CallCancelable() {
            @Override
            protected void doCancel() {
                if (mStickyCalls.remove(req.getId()) != null) {
                    cancelRequest((AbstractParcelRequest) req);
                }
            }
        };
    }

    public void callbackStickilyResponse(final Response response) {
        runInEventLoop(new Runnable() {
            @Override
            public void run() {
                StickyCall stickyCall = mStickyCalls.get(response.getRequestId());
                if (stickyCall != null) {
                    stickyCall.callback.onResponseStickily(stickyCall.req, response);
                }
            }
        });
    }

    private static class Call {

        Request req;
        ResponseCallback callback;
        com.ubtrobot.concurrent.Cancelable timeout;

        public Call(Request req, ResponseCallback callback) {
            this.req = req;
            this.callback = callback;
        }
    }

    private static class StickyCall {

        Request req;
        StickyResponseCallback callback;

        public StickyCall(Request req, StickyResponseCallback callback) {
            this.req = req;
            this.callback = callback;
        }
    }

    private abstract class CallCancelable implements Cancelable {

        private volatile boolean mCancelled;

        @Override
        public void cancel() {
            if (mCancelled) {
                return;
            }

            if (mEventLoop.inEventLoop()) {
                doCancel();
            } else {
                mEventLoop.post(new Runnable() {
                    @Override
                    public void run() {
                        mCancelled = true;
                        doCancel();
                    }
                });
            }
        }

        protected abstract void doCancel();
    }
}