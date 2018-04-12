package com.ubtrobot.master.transport.message;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.ubtrobot.concurrent.Cancelable;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.AbstractParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelResponse;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.CallCancelListener;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.ulog.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by column on 17-9-9.
 */

public class IPCResponder implements Responder {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("IPCResponder");

    private final Connection mConnection;
    private final AbstractParcelRequest mRequest;

    private volatile CallCancelListener mCancelListener;

    private volatile UnavailableListener mUnavailableListener;
    private volatile boolean mUnavailable;

    private volatile Cancelable mTimeoutTimingCancelable;

    public IPCResponder(Connection connection, AbstractParcelRequest request) {
        mConnection = connection;
        mRequest = request;
    }

    public void startTimeoutTimingIfCan() {
        ParcelRequestConfig config = mRequest.getConfig();
        if (config.isStickily() || config.isCancelPrevious() ||
                mUnavailable || mTimeoutTimingCancelable != null) {
            return;
        }

        mTimeoutTimingCancelable = mConnection.eventLoop().postDelay(new Runnable() {
            @Override
            public void run() {
                mTimeoutTimingCancelable = null;

                toBeUnavailable();
            }
        }, config.getTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Request getRequest() {
        return mRequest;
    }

    @Override
    public void respondSuccess() {
        respondSuccess(null);
    }

    @Override
    public void respondSuccess(final Param param) {
        if (param != null && !(param instanceof AbstractParam)) {
            throw new IllegalArgumentException("Unsupported param type");
        }

        if (!checkAvailable()) {
            return;
        }

        ParcelResponse response = new ParcelResponse(
                mRequest, ParcelResponse.RESULT_TYPE_SUCCESS, (AbstractParam) param);
        mConnection.write(response, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // NOOP
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Respond success response failed. requestId=%s, param=%s",
                        mRequest.getId(), param);
            }
        });

        toBeUnavailable();
    }

    private boolean checkAvailable() {
        if (mUnavailable) {
            LOGGER.w("Responder is unavailable. Maybe: 1. responded; 2. request canceled; " +
                    "3. timeout. requestId=%s", mRequest.getId());
            return false;
        }

        return true;
    }

    private void toBeUnavailable() {
        if (mUnavailable) {
            return;
        }

        mUnavailable = true;

        if (mTimeoutTimingCancelable != null) {
            mTimeoutTimingCancelable.cancel();
        }

        if (mUnavailableListener != null) {
            mConnection.eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    mUnavailableListener.onUnavailable();
                    mUnavailableListener = null;
                }
            });
        }

        mCancelListener = null;
    }

    public void setUnavailableListener(UnavailableListener listener) {
        mUnavailableListener = listener;
    }

    @Override
    public void respondFailure(final int code, final String message) {
        respondFailure(code, message, null);
    }

    @Override
    public void respondFailure(
            final int code, final String message, @Nullable Map<String, String> detail) {
        if (code == 0) {
            throw new IllegalArgumentException("Argument code <= 0 or message is empty.");
        }

        if (!checkAvailable()) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(ParamBundleConstants.KEY_CALL_EXCEPTION_CODE, code);
        bundle.putString(ParamBundleConstants.KEY_CALL_EXCEPTION_MESSAGE, message);
        if (detail != null && detail.size() > 0) {
            ArrayList<String> detailList = new ArrayList<>();
            for (Map.Entry<String, String> entry : detail.entrySet()) {
                detailList.add(entry.getKey());
                detailList.add(entry.getValue());

                bundle.putStringArrayList(
                        ParamBundleConstants.KEY_CALL_EXCEPTION_DETAIL, detailList);
            }
        }

        ParcelResponse response = new ParcelResponse(
                mRequest, ParcelResponse.RESULT_TYPE_FAILURE, ParcelableParam.create(bundle));
        mConnection.write(response, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // Ignore
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(
                        e,
                        "Respond failure response failed. requestId=%s, code=%d, message=%s",
                        mRequest.getId(),
                        code,
                        message
                );
            }
        });

        toBeUnavailable();
    }

    @Override
    public void respondStickily(final Param param) {
        if (param == null || !(param instanceof AbstractParam)) {
            throw new IllegalArgumentException("Arugment param is null or unsupported type.");
        }

        if (!checkAvailable()) {
            return;
        }

        if (!mRequest.getConfig().isStickily()) {
            LOGGER.i(
                    "Respond ignored. Request is NOT a stick request. requestId=%s, responseParam=%s",
                    mRequest.getId(),
                    param
            );
            return;
        }

        ParcelResponse response = new ParcelResponse(
                mRequest, ParcelResponse.RESULT_TYPE_STICKILY, (AbstractParam) param);
        mConnection.write(response, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // NOOP
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Respond stick response failed. requestId=%s, param=%s",
                        mRequest.getId(), param);
            }
        });
    }

    @Override
    public void setCallCancelListener(CallCancelListener listener) {
        mCancelListener = listener;
    }

    public void cancel(final Handler mainThreadHandler) {
        mConnection.eventLoop().post(new Runnable() {
            @Override
            public void run() {
                if (mCancelListener != null) {
                    final CallCancelListener listener = mCancelListener;
                    mainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCancel(mRequest);
                        }
                    });
                }

                toBeUnavailable();
            }
        });
    }

    public interface UnavailableListener {

        void onUnavailable();
    }
}