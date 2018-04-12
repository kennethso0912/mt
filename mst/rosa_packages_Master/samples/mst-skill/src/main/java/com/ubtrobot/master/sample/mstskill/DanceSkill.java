package com.ubtrobot.master.sample.mstskill;

import android.os.Handler;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.competition.ActivateCallback;
import com.ubtrobot.master.competition.ActivateException;
import com.ubtrobot.master.competition.Competing;
import com.ubtrobot.master.competition.CompetingItem;
import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.sample.mstservice.param.LightNotification;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.skill.MasterSkill;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.ULog;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-12-26.
 */

public class DanceSkill extends MasterSkill {

    private static final Logger LOGGER = ULog.getLogger("DanceSkill");

    private final Handler mHandler = new Handler();
    private final StopSkillRunnable mStopSkillRunnable = new StopSkillRunnable();

    private final MotionEventReceiver mMotionEventReceiver = new MotionEventReceiver();

    @Override
    protected void onSkillStart() {
        LOGGER.i("onSkillStart. skill=dance");

//        subscribe(mMotionEventReceiver, "action.motion.working");
    }

    @Call(path = "/dance-skill/dance")
    public void onDance(Request request, final Responder responder) {
        final CompetitionSession session = openCompetitionSession();
        session.addCompeting(new Competing() {
            @Override
            public List<CompetingItem> getCompetingItems() {
                return Collections.singletonList(new CompetingItem("light", "light-1"));
            }
        });
        session.activate(new ActivateCallback() {
            @Override
            public void onSuccess(String sessionId) {
                LOGGER.e("Activate success. sessionId=%s", sessionId);
                ServiceProxy lightService = session.createSystemServiceProxy("light");
                lightService.call("/light/notification", ParcelableParam.create(new LightNotification("light-1", 0)), new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        responder.respondSuccess();
                    }

                    @Override
                    public void onFailure(Request req, CallException e) {
                        LOGGER.e(e, "Light notification failure.");
                        responder.respondFailure(e.getCode(), e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(ActivateException e) {
                LOGGER.e(e, "Activate failure.");
            }
        });


//        ServiceProxy motionService = createSystemServiceProxy("motion");
//        BatchServoCommand commands = new BatchServoCommand();
//        commands.add(new ServoCommand("1", "moveTo", "180|3s"));
//        commands.add(new ServoCommand("2", "moveTo", "-180|2s"));
//        commands.add(new ServoCommand("3", "moveBy", "20|0.5s"));
//
//        motionService.call("/servo/batch-command", ParcelableParam.create(commands), new ResponseCallback() {
//            @Override
//            public void onResponse(Request req, Response res) {
//                responder.respondSuccess();
//            }
//
//            @Override
//            public void onFailure(Request req, CallException e) {
//                LOGGER.e(e, "Dance failed.");
//                responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "Motion service error.");
//            }
//        });

//        mHandler.removeCallbacks(mStopSkillRunnable);
//        mHandler.postDelayed(mStopSkillRunnable, 5000);
    }

    @Override
    protected void onSkillStop() {
        LOGGER.i("onSkillStop. skill=dance");

//        mHandler.removeCallbacks(mStopSkillRunnable);
//        unsubscribe(mMotionEventReceiver);
    }

    private static class MotionEventReceiver implements EventReceiver {

        @Override
        public void onReceive(MasterContext context, Event event) {
            LOGGER.i("onReceiveMotionEvent. event=%s", event);
        }
    }

    private class StopSkillRunnable implements Runnable {

        @Override
        public void run() {
            stopSkill();
        }
    }
}
