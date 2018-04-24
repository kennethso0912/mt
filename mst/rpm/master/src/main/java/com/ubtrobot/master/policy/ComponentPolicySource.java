package com.ubtrobot.master.policy;

import android.content.Context;

import com.ubtrobot.master.async.Callback;
import com.ubtrobot.master.call.IPCToPolicyCallable;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;

/**
 * Created by column on 17-11-28.
 */

public class ComponentPolicySource extends BasePolicySource {

    public ComponentPolicySource(Context context, IPCToPolicyCallable callable) {
        super(context, callable);
    }

    public void getComponentPolicies(
            final MixedComponentBaseInfoList baseInfoList,
            final Callback<ComponentPolicyList, PolicyException> callback) {
        if (!isPolicyServiceInstalled() || (baseInfoList.getSkillBaseInfoList().isEmpty() &&
                baseInfoList.getServiceBaseInfoList().isEmpty())) {
            callable().eventLoop().post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(new ComponentPolicyList());
                }
            });

            return;
        }

        callable().call(
                PolicyConstants.CALL_PATH_COMPONENT_POLICIES,
                ParcelableParam.create(baseInfoList),
                new ResponseCallback() {
                    @Override
                    public void onResponse(Request req, Response res) {
                        try {
                            ComponentPolicyList policyList = ParcelableParam.from(
                                    res.getParam(), ComponentPolicyList.class).getParcelable();
                            callback.onSuccess(policyList);
                        } catch (ParcelableParam.InvalidParcelableParamException e) {
                            callback.onFailure(new PolicyException()); // TODO
                        }
                    }

                    @Override
                    public void onFailure(Request req, CallException e) {
                        callback.onFailure(new PolicyException()); // TODO
                    }
                });
    }

    public void notifySkillLifecyclePoliciesChanged(MixedComponentBaseInfoList baseInfos) {
        // TODO 做了缓存再考虑
    }
}