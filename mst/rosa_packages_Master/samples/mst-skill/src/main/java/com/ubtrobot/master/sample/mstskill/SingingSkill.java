package com.ubtrobot.master.sample.mstskill;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.sample.mstservice.param.LightNotification;
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

public class SingingSkill extends MasterSkill {

    private static final Logger LOGGER = ULog.getLogger("SingingSkill");

    @Override
    protected void onSkillStart() {
        LOGGER.i("onSkillStart. skill=singing");
    }

    @Call(path = "/singing-skill/sing")
    public void onSing(Request request, final Responder responder) {
        ServiceProxy motionService = createSystemServiceProxy("light");
        LightNotification notification = new LightNotification("A", 0xff34a35a);

        motionService.call("/light/notification", ParcelableParam.create(notification), new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                responder.respondSuccess();
            }

            @Override
            public void onFailure(Request req, CallException e) {
                LOGGER.e(e, "Sing failed.");
                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "Light service error.");
            }
        });
    }

    @Override
    protected void onCall(Request request, Responder responder) {
        responder.respondFailure(CallGlobalCode.NOT_IMPLEMENTED, "Not implemented.");
    }

    @Override
    protected void onSkillStop() {
        LOGGER.i("onSkillStop. skill=singing");
    }
}