package com.ubtrobot.master.call;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.IntentExtras;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-11-25.
 */

public class CallForwarder {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("CallForwarder");

    private final Context mContext;
    private final Connection mConnection;
    private final ComponentInfo mComponentInfo;

    public CallForwarder(Context context, Connection connection, ComponentInfo componentInfo) {
        mContext = context;
        mConnection = connection;
        mComponentInfo = componentInfo;
    }

    public void forward(final ParcelRequest request, final OutgoingCallback callback) {
        if (mConnection == null) {
            forwardByStartService(request, callback);
            return;
        }

        mConnection.write(request, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.w(e, "Forward call to the connection failed. Retry by startService." +
                        " connectionId=%s", mConnection.id().asText());
                forwardByStartService(request, callback);
            }
        });
    }

    private void forwardByStartService(ParcelRequest request, OutgoingCallback callback) {
        try {
            if (mContext.startService(new Intent().
                    setComponent(new ComponentName(
                            mComponentInfo.getPackageName(), mComponentInfo.getClassName()
                    )).
                    putExtra(IntentExtras.KEY_REQUEST, request)) != null) {
                callback.onSuccess();
                return;
            }

            callback.onFailure(new IllegalStateException("Forward call failed. " +
                    "Maybe system security restrictions or the destination apk(" +
                    mComponentInfo.getPackageName() + ") was uninstalled."));
        } catch (SecurityException e) {
            callback.onFailure(new IllegalStateException("Forward call failed. The component(" +
                    mComponentInfo.getClassName() + ") should be exported. " +
                    "Configure 'android:exported=\"true\"' in the manifest."));
        }
    }
}
