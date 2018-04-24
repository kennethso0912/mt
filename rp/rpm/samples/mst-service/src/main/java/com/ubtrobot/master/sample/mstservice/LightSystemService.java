package com.ubtrobot.master.sample.mstservice;

import com.ubtrobot.master.annotation.Call;
import com.ubtrobot.master.competition.CompetingItemDetail;
import com.ubtrobot.master.competition.CompetitionSessionInfo;
import com.ubtrobot.master.sample.mstservice.param.LightNotification;
import com.ubtrobot.master.service.MasterSystemService;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.parcel.ParcelableParam;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;
import com.ubtrobot.ulog.Logger;
import com.ubtrobot.ulog.ULog;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-12-26.
 */

public class LightSystemService extends MasterSystemService {

    private static final Logger LOGGER = ULog.getLogger("LightSystemService");

    @Override
    protected List<CompetingItemDetail> getCompetingItems() {
        return Collections.singletonList(new CompetingItemDetail.Builder(getName(), "light-1").
                addCallPath("/light/notification").setDescription("light competing item").build());
    }

    @Override
    protected void onCompetitionSessionActive(CompetitionSessionInfo sessionInfo) {
        LOGGER.e("onCompetitionSessionActive. sessionInfo=%s", sessionInfo);
    }

    @Call(path = "/light/notification")
    public void onNotify(Request request, Responder responder) {
        try {
            LightNotification notification = ParcelableParam.from(
                    request.getParam(), LightNotification.class).getParcelable();
            LOGGER.i("Notify a light notification. notification=%s", notification);
            responder.respondSuccess();
        } catch (ParcelableParam.InvalidParcelableParamException e) {
            responder.respondFailure(CallGlobalCode.BAD_REQUEST, "Illegal arguments.");
        }
    }

    @Override
    protected void onCompetitionSessionInactive(CompetitionSessionInfo sessionInfo) {
        LOGGER.e("onCompetitionSessionInactive. sessionInfo=%s", sessionInfo);
    }
}