package com.ubtrobot.master;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.connection.ProviderCallProcessor;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-10-23.
 */

public class MasterProvider extends ContentProvider {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterProvider");

    private ProviderCallProcessor mProcessor;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException(
                    "Android framework error. getContext() == null after onCreate.");
        }
        mProcessor = new ProviderCallProcessor(context);

        Initializer.get().initialize(context);
        startKeepAliveService(context);
        return true;
    }

    private void startKeepAliveService(Context context) {
        // 避免 Provider 销毁后，避免整个进程成为空进程（容易被杀）
        context.startService(new Intent(getContext(), KeepAliveService.class));
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        return mProcessor.call(method, extras);
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        LOGGER.e("Unsupported operation.");
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        LOGGER.e("Unsupported operation.");
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        LOGGER.e("Unsupported operation.");
        return null;
    }

    @Override
    public int delete(
            @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        LOGGER.e("Unsupported operation.");
        return 0;
    }

    @Override
    public int update(
            @NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        LOGGER.e("Unsupported operation.");
        return 0;
    }
}