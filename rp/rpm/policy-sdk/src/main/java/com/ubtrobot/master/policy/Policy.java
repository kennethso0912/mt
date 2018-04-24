package com.ubtrobot.master.policy;

import com.ubtrobot.master.component.ComponentBaseInfo;

import java.util.List;

/**
 * Created by column on 26/11/2017.
 */

public interface Policy {

    void getSkillPolicies(List<ComponentBaseInfo> skillBaseInfos, List<SkillPolicy> outPolicies);

    void notifySkillPoliciesChanged(List<ComponentBaseInfo> skillBaseInfos);

    void getServicePolicies(List<ComponentBaseInfo> serviceBaseInfos, List<ServicePolicy> outPolicies);

    void notifyServicePoliciesChanged(List<ComponentBaseInfo> serviceBaseInfos);
}