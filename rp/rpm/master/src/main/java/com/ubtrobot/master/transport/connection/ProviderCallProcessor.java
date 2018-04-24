package com.ubtrobot.master.transport.connection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ubtrobot.master.transport.message.parcel.BundleCompatWrapper;
import com.ubtrobot.master.transport.version.Version;

/**
 * Created by column on 17-10-25.
 */

public class ProviderCallProcessor {

    private final Context mContext;

    public ProviderCallProcessor(Context context) {
        mContext = context;
    }

    public Bundle call(String method, @Nullable Bundle args) {
        // 权限检查

        if (ConnectionConstants.PRVDR_CALL_MTHD_CONNECT.equals(method)) {
            return acceptConnection(args);
        }

        return errorBundle(ConnectionConstants.CODE_UNSUPPORTED_METHOD, "Unsupported method.");
    }

    private Bundle acceptConnection(Bundle args) {
        if (args == null) {
            return errorBundle(ConnectionConstants.BAD_CALL, "Bad call. Miss args content.");
        }

        String version = args.getString(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_VERSION);
        if (!Version.isLegal(version)) { // TODO，考虑兼容性要做到什么程度，目前先这样
            return errorBundle(ConnectionConstants.BAD_CALL,
                    "Bad call. Illegal version. version=" + version);
        }

        String packageName = args.getString(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_PACKAGE);
        if (!isPackageNameLegal(packageName)) {
            return errorBundle(ConnectionConstants.BAD_CALL,
                    "Bad call. Illegal packageName. packageName=" + packageName);
        }

        int uid = Binder.getCallingUid();
        String nameForUid = mContext.getPackageManager().getNameForUid(uid);
        if (TextUtils.isEmpty(nameForUid)) {
            return errorBundle(ConnectionConstants.BAD_CALL, "Bad call. Illegal uid. uid=" + uid);
        }

        int pid = Binder.getCallingPid();
        if (pid <= 0) {
            return errorBundle(ConnectionConstants.BAD_CALL, "Bad call. Illegal pid. pid=" + pid);
        }

        IBinder connectMasterSideBinder = BundleCompatWrapper.getBinder(
                args, ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_BINDER);
        if (connectMasterSideBinder == null) {
            return errorBundle(ConnectionConstants.BAD_CALL,
                    "Bad call. Miss binder in the content args.");
        }

        MasterSideConnection connection = MasterSideConnectionPool.get().
                allocateConnection(connectMasterSideBinder, packageName, nameForUid, pid);
        if (connection == null) {
            return errorBundle(ConnectionConstants.BAD_CALL,
                    "Bad call. The binder has been connected. packageName=" + packageName +
                            ", uid=" + uid + ", pid=" + pid);
        }

        Bundle ret = successBundle();
        BundleCompatWrapper.putBinder(ret, ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_BINDER,
                connection.masterSideBinder());
        return ret;
    }

    private boolean isPackageNameLegal(String packageName) {
        try {
            return mContext.getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private Bundle errorBundle(int code, String message) {
        Bundle bundle = new Bundle();
        bundle.putInt(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_CODE, code);
        bundle.putString(ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_ERR_MSG, message);
        return bundle;
    }

    private Bundle successBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(
                ConnectionConstants.PRVDR_CALL_BUNDLE_KEY_CODE, ConnectionConstants.CODE_SUCCESS);
        return bundle;
    }
}