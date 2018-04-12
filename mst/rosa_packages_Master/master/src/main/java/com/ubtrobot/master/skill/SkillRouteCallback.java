package com.ubtrobot.master.skill;

import android.content.Context;
import android.support.annotation.Nullable;

import com.ubtrobot.master.async.Callback;
import com.ubtrobot.master.call.CallRouteCallback;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.master.transport.message.IPCResponder;
import com.ubtrobot.master.transport.message.parcel.AbstractParcelRequest;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.message.Request;

/**
 * Created by column on 17-11-29.
 */

public class SkillRouteCallback extends CallRouteCallback {

    private final SkillManager mSkillManager;

    public SkillRouteCallback(
            Context context,
            HandlerContext handlerContext, SkillManager skillManager) {
        super(context, handlerContext);
        mSkillManager = skillManager;
    }

    @Override
    public void onRoute(
            final Request request,
            final ComponentInfo destinationComponentInfo,
            @Nullable final Connection destinationConnection) {
        final SkillInfo skillInfo = (SkillInfo) destinationComponentInfo;
        mSkillManager.startSkill(
                handlerContext(),
                skillInfo,
                destinationConnection,
                new Callback<Void, SkillManageException>() {
                    @Override
                    public void onSuccess(Void data) {
                        forwardRequest(request, destinationConnection, skillInfo);
                    }

                    @Override
                    public void onFailure(SkillManageException e) {
                        IPCResponder responder = new IPCResponder(
                                handlerContext().connection(), (AbstractParcelRequest) request);
                        if (SkillManageException.CODE_FORBIDDEN == e.getCode()) {
                            responder.respondFailure(CallGlobalCode.FORBIDDEN, "Forbidden.");
                        } else {
                            responder.respondFailure(CallGlobalCode.INTERNAL_ERROR, "Internal error.");
                        }
                    }
                }
        );
    }
}