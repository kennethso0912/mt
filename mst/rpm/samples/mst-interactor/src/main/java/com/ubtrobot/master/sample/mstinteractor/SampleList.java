package com.ubtrobot.master.sample.mstinteractor;

import android.content.Context;

import com.ubtrobot.master.interactor.MasterInteractor;
import com.ubtrobot.master.service.ServiceProxy;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillIntent;
import com.ubtrobot.master.skill.SkillsProxy;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.ULog;

import java.lang.ref.WeakReference;

/**
 * Created by column on 17-12-8.
 */

public class SampleList {

    private static final Logger LOGGER = ULog.getLogger("SampleList");

    private final WeakReference<Context> mContext;
    private final MasterInteractor mInteractor;

    public SampleList(Context context, MasterInteractor interactor) {
        mContext = new WeakReference<>(context);
        mInteractor = interactor;
        mInteractor.registerSkillLifecycleCallbacks(new MasterInteractor.SkillLifecycleCallbacks() {
            @Override
            public void onSkillStarted(SkillInfo skillInfo) {
                LOGGER.e("onSkillStarted. skillInfo=%s", skillInfo);
            }

            @Override
            public void onSkillStopped(SkillInfo skillInfo) {
                LOGGER.e("onSkillStopped. skillInfo=%s", skillInfo);
            }
        });
    }

    public void dance() {
        sendSpeechCommand("跳个舞");
    }

    private void sendSpeechCommand(String speechCommand) {
        SkillsProxy skillsProxy = mInteractor.createSkillsProxy();

        SkillIntent skillIntent = new SkillIntent(SkillIntent.CATEGORY_SPEECH);
        skillIntent.setSpeechUtterance(speechCommand);

        skillsProxy.call(skillIntent, new ResponseCallback() {

            @Override
            public void onResponse(Request req, Response res) {
                // NOOP
            }

            @Override
            public void onFailure(Request req, CallException e) {
                ULog.e(e);
            }
        });
    }

    public void sightsee() {
        sendSpeechCommand("带我游览优必选");
    }

    public void sing() {
        sendSpeechCommand("唱歌");
    }

    public void talk() {
        sendSpeechCommand("闲聊");

        ServiceProxy motionService = mInteractor.createSystemServiceProxy("motion");
        LOGGER.e("didAddState ========== " + motionService.didAddState("low_voltage"));
    }

    public void enterRemoteControlInteractor() {
        // TODO
        LOGGER.e("getStartedSkills. skillCount=%d, skills=%s",
                mInteractor.getStartedSkills().size(),
                mInteractor.getStartedSkills());
    }

    public void enterLauncherInteractor() {
        // TODO

        mInteractor.dismiss();
    }
}