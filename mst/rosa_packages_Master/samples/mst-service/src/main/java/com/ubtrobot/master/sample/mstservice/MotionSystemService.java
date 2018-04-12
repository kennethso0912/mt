package com.ubtrobot.master.sample.mstservice;

import android.os.Handler;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.sample.mstservice.param.BatchServoCommand;
import com.ubtrobot.master.sample.mstservice.param.ServoCommand;
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

public class MotionSystemService extends MasterSystemService {

    private static final Logger LOGGER = ULog.getLogger("MotionSystemService");

    @Override
    protected void onServiceCreate() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                addState("low_voltage");
            }
        }, 15000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                removeState("low_voltage");
            }
        }, 25000);
    }

    @Call(path = "/servo/batch-command")
    public void onBatchCommand(Request request, Responder responder) {
        try {
            BatchServoCommand batchServoCommand = ParcelableParam.from(
                    request.getParam(), BatchServoCommand.class).getParcelable();
            if (batchServoCommand.isEmpty()) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments. size=0");
                return;
            }

            for (ServoCommand servoCommand : batchServoCommand) {
                LOGGER.i("Exec command. command=%s", servoCommand);
            }

            responder.respondSuccess();

            publish("action.motion.working");
        } catch (ParcelableParam.InvalidParcelableParamException e) {
            responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
        }
    }

    @Override
    protected void onCall(Request request, Responder responder) {
        responder.respondFailure(CallGlobalCode.NOT_IMPLEMENTED, "Not implement.");
    }
}
