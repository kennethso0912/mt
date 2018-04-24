package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-27.
 */

public class ParcelRequestConfig implements Parcelable {

    private boolean hasCallback;
    private boolean isStickily;
    private int timeout;
    private boolean cancelPrevious;
    private String previousRequestId;

    public static final Creator<ParcelRequestConfig> CREATOR = new Creator<ParcelRequestConfig>() {
        @Override
        public ParcelRequestConfig createFromParcel(Parcel in) {
            return new ParcelRequestConfig(in);
        }

        @Override
        public ParcelRequestConfig[] newArray(int size) {
            return new ParcelRequestConfig[size];
        }
    };

    private ParcelRequestConfig() {
    }

    private ParcelRequestConfig(Parcel in) {
        hasCallback = in.readByte() != 0;
        isStickily = in.readByte() != 0;
        timeout = in.readInt();
        cancelPrevious = in.readByte() != 0;
        previousRequestId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (hasCallback ? 1 : 0));
        dest.writeByte((byte) (isStickily ? 1 : 0));
        dest.writeInt(timeout);
        dest.writeByte((byte) (cancelPrevious ? 1 : 0));
        dest.writeString(previousRequestId);
    }

    public boolean hasCallback() {
        return hasCallback;
    }

    public boolean isStickily() {
        return isStickily;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isCancelPrevious() {
        return cancelPrevious;
    }

    public String getPreviousRequestId() {
        return previousRequestId;
    }

    @Override
    public String toString() {
        return "RequestConfig{" +
                "hasCallback=" + hasCallback +
                ", isStickily=" + isStickily +
                ", timeout=" + timeout +
                ", cancelPrevious=" + cancelPrevious +
                ", previousRequestId='" + previousRequestId + '\'' +
                '}';
    }

    public static class Builder {

        private boolean hasCallback;
        private boolean isStickily;
        private int timeout;
        private boolean cancelPrevious;
        private String previousRequestId;

        public Builder() {
        }

        public Builder setHasCallback(boolean hasCallback) {
            this.hasCallback = hasCallback;
            return this;
        }

        public Builder setStickily(boolean stickily) {
            isStickily = stickily;
            return this;
        }

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setCancelPrevious(boolean cancelPrevious) {
            this.cancelPrevious = cancelPrevious;
            return this;
        }

        public Builder setPreviousRequestId(String previousRequestId) {
            this.previousRequestId = previousRequestId;
            return this;
        }

        public ParcelRequestConfig build() {
            ParcelRequestConfig config = new ParcelRequestConfig();
            config.hasCallback = hasCallback;
            config.isStickily = isStickily;
            config.timeout = timeout;
            config.cancelPrevious = cancelPrevious;
            config.previousRequestId = previousRequestId;
            return config;
        }
    }
}