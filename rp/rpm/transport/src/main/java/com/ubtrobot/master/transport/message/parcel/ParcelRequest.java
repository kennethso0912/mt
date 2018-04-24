package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;

/**
 * Created by column on 17-11-28.
 */

public class ParcelRequest extends AbstractParcelRequest {

    private final String path;

    public static final Creator<ParcelRequest> CREATOR = new Creator<ParcelRequest>() {
        @Override
        public ParcelRequest createFromParcel(Parcel in) {
            return new ParcelRequest(in);
        }

        @Override
        public ParcelRequest[] newArray(int size) {
            return new ParcelRequest[size];
        }
    };

    private ParcelRequest(Parcel in) {
        super(in);
        path = in.readString();
    }

    public ParcelRequest(ParcelRequestContext context, ParcelRequestConfig config, String path) {
        this(context, config, path, null);
    }

    public ParcelRequest(
            ParcelRequestContext context, ParcelRequestConfig config,
            String path, AbstractParam param) {
        super(context, config, param);
        this.path = path;
    }

    ParcelRequest(String id, long when, ParcelRequestContext context,
                  ParcelRequestConfig config, String path, AbstractParam param) {
        super(id, when, context, config, param);
        this.path = path;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(path);
    }

    @Override
    public String getPath() {
        return path;
    }

    public static boolean validatePath(String path) {
        return path != null && path.startsWith("/");
    }

    @Override
    public String toString() {
        return "Request{" +
                "id='" + getId() + '\'' +
                ", when=" + getWhen() +
                ", path=" + path +
                ", context=" + getContext() +
                ", config=" + getConfig() +
                ", connectionId='" + getConnectionId() + '\'' +
                ", param=" + getParam() +
                '}';
    }
}
