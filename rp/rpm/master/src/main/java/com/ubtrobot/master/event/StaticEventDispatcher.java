package com.ubtrobot.master.event;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.ubtrobot.master.component.validate.ComponentConstants;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.IntentExtras;
import com.ubtrobot.master.transport.message.parcel.ParcelEvent;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.Publisher;
import com.ubtrobot.ulog.Logger;

import java.util.List;

/**
 * Created by column on 17-9-16.
 */

public class StaticEventDispatcher implements Publisher {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("StaticEventDispatcher");

    private final Context mContext;
    private final PackageManager mPackageManager;

    public StaticEventDispatcher(Context context) {
        mContext = context;
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public void publish(Event event) {
        Intent intent = new Intent(event.getAction()).addCategory(
                ComponentConstants.INTENT_FILTER_CATEGORY_SUBSCRIBER);
        List<ResolveInfo> infos = mPackageManager.queryIntentServices(intent, 0);
        if (infos.isEmpty()) {
            return;
        }

        intent.putExtra(IntentExtras.KEY_EVENT, (ParcelEvent) event);

        for (ResolveInfo info : infos) {
            ComponentName component = new ComponentName(
                    info.serviceInfo.packageName, info.serviceInfo.name);
            intent.setComponent(component);
            if (mContext.startService(intent) == null) {
                LOGGER.e("Publish event failed. Can NOT startService. component=%s", component);
            }
        }
    }
}