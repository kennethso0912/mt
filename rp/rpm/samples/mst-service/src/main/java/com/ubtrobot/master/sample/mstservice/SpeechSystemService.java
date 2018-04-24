package com.ubtrobot.master.sample.mstservice;

import android.os.Bundle;
import android.text.TextUtils;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.service.MasterSystemService;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.ULog;

/**
 * Created by column on 17-12-26.
 */

public class SpeechSystemService extends MasterSystemService {

    private static final Logger LOGGER = ULog.getLogger("SpeechSystemService");

    @Call(path = "/tts")
    public void onTts(Request request, Responder responder) {
        try {
            String tts = ParcelableParam.from(request.getParam(), Bundle.class).
                    getParcelable().getString("tts");
            if (TextUtils.isEmpty(tts)) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments");
                return;
            }

            LOGGER.i("Play tts content. tts=%s", tts);
            responder.respondSuccess();
        } catch (ParcelableParam.InvalidParcelableParamException e) {
            responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
        }
    }

    @Override
    protected void onCall(Request request, Responder responder) {
        responder.respondFailure(CallGlobalCode.NOT_IMPLEMENTED, "Not implement.");
    }
}