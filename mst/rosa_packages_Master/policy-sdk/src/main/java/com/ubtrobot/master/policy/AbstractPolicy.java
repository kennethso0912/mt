package com.ubtrobot.master.policy;

import android.content.Context;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.component.ComponentBaseInfo;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.ulog.Logger;

import java.util.List;

/**
 * Created by column on 26/11/2017.
 */

public abstract class AbstractPolicy implements Policy {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("AbstractPolicy");

    private final Context mContext;
    private final CallMasterCallable mCallable;

    protected AbstractPolicy(Context context, Unsafe unsafe) {
        EnvironmentChecker.checkApplication(context);

        mContext = context.getApplicationContext();

        mCallable = unsafe.getGlobalCallMasterCallable();
    }

    protected Context context() {
        return mContext;
    }

    @Override
    public final void notifySkillPoliciesChanged(List<ComponentBaseInfo> skillBaseInfos) {
//        mCallable.call(
//                PolicyConstants.PATH_NOTIFY_SKILL_POLICIES_CHANGED,
//                ParcelableParam.create(skillBaseInfos),
//                new ResponseCallback() {
//                    @Override
//                    public void onResponse(Request req, Response res) {
//                        // Ignore
//                    }
//
//                    @Override
//                    public void onFailure(Request req, CallException e) {
//                        LOGGER.e(e, "Notify skill life cycle policies changed failed.");
//                    }
//                }
//        );
    }

    @Override
    public final void notifyServicePoliciesChanged(List<ComponentBaseInfo> serviceBaseInfos) {
//        mCallable.call(
//                PolicyConstants.PATH_NOTIFY_SERVICE_POLICIES_CHANGED,
//                ParcelableParam.create(serviceBaseInfos),
//                new ResponseCallback() {
//                    @Override
//                    public void onResponse(Request req, Response res) {
//                        // Ignore
//                    }
//
//                    @Override
//                    public void onFailure(Request req, CallException e) {
//                        LOGGER.e(e, "Notify service state policies changed failed.");
//                    }
//                }
//        );
    }
}
