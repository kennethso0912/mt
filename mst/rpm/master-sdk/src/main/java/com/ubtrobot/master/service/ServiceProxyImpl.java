package com.ubtrobot.master.service;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.ubtrobot.master.call.BaseConvenientStickyCallable;
import com.ubtrobot.master.call.CallMasterCallable;
import com.ubtrobot.master.call.IPCByMasterCallable;
import com.ubtrobot.master.competition.CompetingItem;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.ulog.Logger;

import java.util.List;

import static com.ubtrobot.master.transport.message.MasterCallPaths.PATH_QUERY_SERVICE_STATE_DID_ADD;

/**
 * Created by column on 17-9-5.
 */

public class ServiceProxyImpl extends BaseConvenientStickyCallable implements ServiceProxy {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("ServiceProxyImpl");

    private final CallMasterCallable mCallMasterCallable;
    private final String mServiceName;

    public ServiceProxyImpl(
            CallMasterCallable callMasterCallable,
            IPCByMasterCallable ipcByMasterCallable,
            Handler mainThreadHandler,
            ParcelRequestContext context) {
        super(ipcByMasterCallable, mainThreadHandler, context, new ParcelRequestConfig.Builder());

        mCallMasterCallable = callMasterCallable;
        mServiceName = context.getResponder();
    }

    @Override
    public List<CompetingItem> getCompetingItems() {
        // TODO
        return null;
    }

    @Override
    public boolean didAddState(String state) {
        if (TextUtils.isEmpty(state)) {
            throw new IllegalArgumentException("Argument state is empty.");
        }

        Bundle paramBundle = new Bundle();
        paramBundle.putString(ParamBundleConstants.KEY_SERVICE_NAME, mServiceName);
        paramBundle.putString(ParamBundleConstants.KEY_STATE, state);

        try {
            Response response = mCallMasterCallable.call(
                    PATH_QUERY_SERVICE_STATE_DID_ADD, ParcelableParam.create(paramBundle));
            Bundle resParamBundle = ParcelableParam.from(
                    response.getParam(), Bundle.class).getParcelable();

            return state.equals(resParamBundle.get(ParamBundleConstants.KEY_STATE));
        } catch (CallException e) {
            LOGGER.e(e, "Get service state failed. service=%s, state=%s", mServiceName, state);
            return false;
        } catch (ParcelableParam.InvalidParcelableParamException e) {
            LOGGER.e(e, "Get service state failed. service=%s, state=%s", mServiceName, state);
            return false;
        }
    }
}