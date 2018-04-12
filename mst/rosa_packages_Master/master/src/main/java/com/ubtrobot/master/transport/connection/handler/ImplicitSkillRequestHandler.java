package com.ubtrobot.master.transport.connection.handler;

import android.content.Context;

import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.skill.SkillManager;
import com.ubtrobot.master.skill.SkillRouteCallback;
import com.ubtrobot.master.transport.message.parcel.ParcelImplicitRequest;
import com.ubtrobot.transport.connection.HandlerContext;

/**
 * Created by column on 17-11-28.
 */

public class ImplicitSkillRequestHandler extends MessageSplitter.ImplicitRequestHandler {

    private final Context mContext;
    private final CallRouter mCallRouter;
    private final SkillManager mSkillManager;

    public ImplicitSkillRequestHandler(
            Context context, CallRouter callRouter, SkillManager skillManager) {
        mContext = context;
        mCallRouter = callRouter;
        mSkillManager = skillManager;
    }

    @Override
    public void onRead(final HandlerContext context, final ParcelImplicitRequest request) {
        mCallRouter.routeSkill(
                context.eventLoop(), request, new SkillRouteCallback(mContext, context, mSkillManager));
    }
}