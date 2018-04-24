package com.ubtrobot.master.sample.mstskill;

import android.os.Handler;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.sample.mstservice.param.BatchServoCommand;
import com.ubtrobot.master.sample.mstservice.param.ServoCommand;
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
 * Created by column on 17-12-8.
 */

public class SightseeSkill extends MasterSkill {

    private static final Logger LOGGER = ULog.getLogger("SightseeSkill");

    private final Handler mHandler = new Handler();
    private final SetStateRunnable mSetStateRunnable = new SetStateRunnable();

    @Override
    protected void onSkillStart() {
        LOGGER.i("onSkillStart. skill=sightsee");

        mHandler.removeCallbacks(mSetStateRunnable);
        mHandler.postDelayed(mSetStateRunnable, 10000);
    }

    @Call(path = "/dance-skill/sightsee")
    public void onSightsee(Request request, final Responder responder) {
        ServiceProxy motionService = createSystemServiceProxy("motion");
        BatchServoCommand commands = new BatchServoCommand();
        commands.add(new ServoCommand("4", "moveTo", "180|3s"));
        commands.add(new ServoCommand("5", "moveTo", "-180|2s"));
        commands.add(new ServoCommand("6", "moveBy", "20|0.5s"));

        motionService.call("/servo/batch-command", ParcelableParam.create(commands), new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                responder.respondSuccess();
            }

            @Override
            public void onFailure(Request req, CallException e) {
                LOGGER.e(e, "Sightsee failed.");
                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "Motion service error.");
            }
        });
    }

    @Override
    protected void onCall(Request request, Responder responder) {
        responder.respondFailure(CallGlobalCode.NOT_IMPLEMENTED, "Not implemented.");
    }

    @Override
    protected void onSkillStop() {
        LOGGER.i("onSkillStop. skill=sightsee");
        mHandler.removeCallbacks(mSetStateRunnable);
    }


    private class SetStateRunnable implements Runnable {

        @Override
        public void run() {
            setState("moving");
        }
    }
}
