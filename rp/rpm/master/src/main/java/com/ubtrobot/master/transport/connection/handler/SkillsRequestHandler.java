package com.ubtrobot.master.transport.connection.handler;

import android.content.Context;

import com.ubtrobot.master.call.CallRouter;
import com.ubtrobot.master.skill.SkillManager;
import com.ubtrobot.master.skill.SkillRouteCallback;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.HandlerContext;

/**
 * Created by column on 17-11-28.
 */

public class SkillsRequestHandler extends MessageSplitter.RequestHandler {

    private final Context mContext;
    private final CallRouter mCallRouter;
    private final SkillManager mSkillManager;

    public SkillsRequestHandler(Context context, CallRouter callRouter, SkillManager skillManager) {
        mContext = context;
        mCallRouter = callRouter;
        mSkillManager = skillManager;
    }

    @Override
    public void onRead(HandlerContext context, ParcelRequest request) {
        mCallRouter.routeSkill(context.eventLoop(), request,
                new SkillRouteCallback(mContext, context, mSkillManager));
    }
}