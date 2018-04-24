package com.ubtrobot.master.transport.message.parcel;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.BundleCompat;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-11-3.
 */

public final class BundleCompatWrapper {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("BundleCompatWrapper");

    private BundleCompatWrapper() {
    }

    public static IBinder getBinder(Bundle bundle, String key) {
        if (Build.VERSION.SDK_INT >= 18) {
            return bundle.getBinder(key);
        } else {
            try {
                return BundleCompat.getBinder(bundle, key);
            } catch (NoClassDefFoundError e) {
                LOGGER.e("Your android version < 4.3," +
                        " you MUST add 'com.android.support:appcompat-v7:x.y.z' gradle dependency.");
                throw e;
            }
        }
    }

    public static void putBinder(Bundle bundle, String key, IBinder binder) {
        if (Build.VERSION.SDK_INT >= 18) {
            bundle.putBinder(key, binder);
        } else {
            try {
                BundleCompat.putBinder(bundle, key, binder);
            } catch (NoClassDefFoundError e) {
                LOGGER.e("Your android version < 4.3," +
                        " you MUST add 'com.android.support:appcompat-v7:x.y.z' gradle dependency.");
                throw e;
            }
        }
    }
}
