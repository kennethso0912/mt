package com.ubtrobot.master.sample.mstskill;

import android.os.Bundle;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.skill.MasterSkill;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.ULog;

/**
 * Created by column on 17-12-26.
 */

public class TalkingSkill extends MasterSkill {

    private static final Logger LOGGER = ULog.getLogger("TalkingSkill");

    @Override
    protected void onSkillStart() {
        LOGGER.i("onSkillStart. skill=talking");
    }

    @Call(path = "/talking-skill/talk")
    public void onTalk(Request request, final Responder responder) {
        ServiceProxy motionService = createSystemServiceProxy("speech");
        Bundle bundle = new Bundle();
        bundle.putString("tts", "闲聊一句");

        motionService.call("/tts", ParcelableParam.create(bundle), new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                responder.respondSuccess();
            }

            @Override
            public void onFailure(Request req, CallException e) {
                LOGGER.e(e, "Tak failed.");
                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "Speech service error.");
            }
        });
    }

    @Override
    protected void onCall(Request request, Responder responder) {
        responder.respondFailure(CallGlobalCode.NOT_IMPLEMENTED, "Not implemented.");
    }

    @Override
    protected void onSkillStop() {
        LOGGER.i("onSkillStop. skill=talking");
    }
}