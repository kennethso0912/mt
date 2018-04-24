package com.ubtrobot.master.call;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ubtrobot.master.skill.SkillIntent;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelImplicitRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig;
import com.ubtrobot.master.transport.message.parcel.ParcelRequestContext;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Cancelable;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;

import java.util.HashMap;

/**
 * Created by column on 17-11-29.
 */

public class BaseConvenientIntentCallable extends BaseConvenientCallable implements ConvenientIntentCallable {

    protected BaseConvenientIntentCallable(
            IPCByMasterCallable callable,
            Handler mainThreadHandler,
            ParcelRequestContext context,
            ParcelRequestConfig.Builder builder) {
        super(callable, mainThreadHandler, context, builder);
    }

    @Override
    public Response call(SkillIntent intent) throws CallException {
        return call(intent, (Param) null);
    }

    @Override
    public Response call(SkillIntent intent, @Nullable Param param) throws CallException {
        validateIntent(intent);
        return SyncCallUtils.syncCall(
                callable(), getConfiguration(), createRequest(intent, param, true));
    }

    private void validateIntent(SkillIntent intent) {
        if (intent == null || !SkillIntent.CATEGORY_SPEECH.equals(intent.getCategory()) ||
                TextUtils.isEmpty(intent.getSpeechUtterance())) {
            throw new IllegalArgumentException("Illegal intent argument.");
        }
    }

    private Request createRequest(SkillIntent intent, Param param, boolean hasCallback) {
        ParcelRequestConfig config = configBuilder().
                setHasCallback(hasCallback).
                setStickily(false).
                setTimeout(getConfiguration().getTimeout()).
                build();

        HashMap<String, String> matchingRules = new HashMap<>();
        matchingRules.put("category", intent.getCategory());
        matchingRules.put("utterance", intent.getSpeechUtterance());

        return new ParcelImplicitRequest(getContext(), config, matchingRules, (AbstractParam) param);
    }

    @Override
    public Cancelable call(SkillIntent intent, @Nullable ResponseCallback callback) {
        return call(intent, null, callback);
    }

    @Override
    public Cancelable call(SkillIntent intent, @Nullable Param param, @Nullable ResponseCallback callback) {
        return callable().call(
                createRequest(intent, param, callback != null),
                newMainThreadResponseCallback(callback)
        );
    }
}
