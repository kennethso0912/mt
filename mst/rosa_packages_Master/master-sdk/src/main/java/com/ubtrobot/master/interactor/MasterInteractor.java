package com.ubtrobot.master.interactor;

import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillsProxy;

import java.util.List;

/**
 * Created by column on 17-12-7.
 */

public interface MasterInteractor extends MasterContext {

    SkillsProxy createSkillsProxy();

    List<SkillInfo> getStartedSkills();

    void registerSkillLifecycleCallbacks(SkillLifecycleCallbacks callbacks);

    void unregisterSkillLifecycleCallbacks(SkillLifecycleCallbacks callbacks);

    void dismiss();

    interface SkillLifecycleCallbacks {

        void onSkillStarted(SkillInfo skillInfo);

        void onSkillStopped(SkillInfo skillInfo);
    }
}