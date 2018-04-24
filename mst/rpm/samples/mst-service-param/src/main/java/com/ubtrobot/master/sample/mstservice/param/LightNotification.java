package com.ubtrobot.master.sample.mstservice.param;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-12-27.
 */

public class LightNotification implements Parcelable {

    private String lightId;
    private int color;

    public static final Creator<LightNotification> CREATOR = new Creator<LightNotification>() {
        @Override
        public LightNotification createFromParcel(Parcel in) {
            return new LightNotification(in);
        }

        @Override
        public LightNotification[] newArray(int size) {
            return new LightNotification[size];
        }
    };

    private LightNotification(Parcel in) {
        lightId = in.readString();
        color = in.readInt();
    }

    public LightNotification(String lightId, int color) {
        this.lightId = lightId;
        this.color = color;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(lightId);
        dest.writeInt(color);
    }

    public String getLightId() {
        return lightId;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "LightNotification{" +
                "lightId='" + lightId + '\'' +
                ", color=" + String.format("%08X", color) +
                '}';
    }
}