package com.ubtrobot.master.transport.connection.handler;

import android.os.Bundle;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.AbstractIPCCallable;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelResponse;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.ulog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by column on 17-8-31.
 */

public class ResponseHandler extends MessageSplitter.ResponseHandler {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("ResponseCallbackHandler");

    private final AbstractIPCCallable mCallable;

    public ResponseHandler(AbstractIPCCallable callable) {
        mCallable = callable;
    }

    @Override
    public void onRead(HandlerContext context, ParcelResponse response) {
        if (ParcelResponse.RESULT_TYPE_SUCCESS.equals(response.getResultType())) {
            mCallable.callbackResponse(response);
            context.onRead(context);
            return;
        }

        if (ParcelResponse.RESULT_TYPE_FAILURE.equals(response.getResultType())) {
            ParcelableParam<Bundle> param;
            try {
                param = ParcelableParam.from(response.getParam(), Bundle.class);
            } catch (ParcelableParam.InvalidParcelableParamException e) {
                mCallable.callbackResponse(
                        response.getRequestId(), new CallException(
                                CallGlobalCode.INTERNAL_ERROR,
                                "Unexpected response param from the callable. param=" +
                                        response.getParam()
                        )
                );

                return;
            }

            int code = param.getParcelable().getInt(ParamBundleConstants.KEY_CALL_EXCEPTION_CODE);
            String message = param.getParcelable().getString(ParamBundleConstants.KEY_CALL_EXCEPTION_MESSAGE);

            List<String> detailList = param.getParcelable().getStringArrayList(
                    ParamBundleConstants.KEY_CALL_EXCEPTION_DETAIL);
            if (detailList == null) {
                detailList = new ArrayList<>();
            }

            if (code <= 0) {
                mCallable.callbackResponse(response.getRequestId(), new CallException(
                        CallGlobalCode.INTERNAL_ERROR,
                        "Unexpected code from the callable. originCode=" + code +
                                ", originMessage='" + message + "', detail=" + detailList
                ));
                return;
            }

            if (detailList.size() % 2 != 0) {
                mCallable.callbackResponse(response.getRequestId(), new CallException(
                        CallGlobalCode.INTERNAL_ERROR,
                        "Unexpected detail from the callable. originCode=" + code +
                                ", originMessage='" + message + "', detail=" + detailList
                ));
                return;
            }

            HashMap<String, String> detail = new HashMap<>();
            for (int i = 0; i < detailList.size(); i += 2) {
                detail.put(detailList.get(i), detailList.get(i + 1));
            }
            mCallable.callbackResponse(response.getRequestId(),
                    new CallException(code, message == null ? "" : message, detail, null));
            context.onRead(context);
            return;
        }

        if (ParcelResponse.RESULT_TYPE_STICKILY.equals(response.getResultType())) {
            mCallable.callbackStickilyResponse(response);
            context.onRead(context);
            return;
        }

        LOGGER.e("Unexpected response type. response=%s", response);
    }
}