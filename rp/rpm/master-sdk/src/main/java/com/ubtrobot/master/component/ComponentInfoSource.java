package com.ubtrobot.master.component;

import com.ubtrobot.master.service.MasterService;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.skill.MasterSkill;
import com.ubtrobot.master.skill.SkillInfo;

/**
 * Created by column on 17-12-2.
 */

public interface ComponentInfoSource {

    SkillInfo getSkillInfo(String name);

    SkillInfo getSkillInfo(Class<? extends MasterSkill> skillClass);

    ServiceInfo getServiceInfo(String name);

    ServiceInfo getServiceInfo(Class<? extends MasterService> serviceClass);

    boolean requestSystemServicePermission();
}