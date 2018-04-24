package com.ubtrobot.master.policy;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.MasterService;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 26/11/2017.
 */

public class PolicyService extends MasterService {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("PolicyService");

    private Policy mPolicy;

    @Override
    protected void onServiceCreate() {
        EnvironmentChecker.checkApplication(this);

        BasePolicyApplication application = (BasePolicyApplication) getApplication();
        mPolicy = application.getPolicy();
    }

    @Call(path = PolicyConstants.CALL_PATH_COMPONENT_POLICIES)
    public void onGetComponentPolicies(Request request, Responder responder) {
        try {
            MixedComponentBaseInfoList baseInfoList = ParcelableParam.from(
                    request.getParam(), MixedComponentBaseInfoList.class).getParcelable();

            if (baseInfoList.getSkillBaseInfoList().isEmpty() &&
                    baseInfoList.getServiceBaseInfoList().isEmpty()) {
                responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
                return;
            }

            ComponentPolicyList policyList = new ComponentPolicyList();
            mPolicy.getSkillPolicies(baseInfoList.getSkillBaseInfoList(), policyList.getSkillPolicyList());
            mPolicy.getServicePolicies(baseInfoList.getServiceBaseInfoList(), policyList.getServicePolicyList());

            responder.respondSuccess(ParcelableParam.create(policyList));
        } catch (ParcelableParam.InvalidParcelableParamException e) {
            responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
        }
    }

    @Override
    protected void onCall(Request request, Responder responder) {
        LOGGER.e("Some request miss @Call method.");
    }
}