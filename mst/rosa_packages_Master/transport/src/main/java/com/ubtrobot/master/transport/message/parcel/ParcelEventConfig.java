package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-27.
 */

public class ParcelEventConfig implements Parcelable {

    private boolean isCareful;
    private boolean isInternal;

    public static final Creator<ParcelEventConfig> CREATOR = new Creator<ParcelEventConfig>() {
        @Override
        public ParcelEventConfig createFromParcel(Parcel in) {
            return new ParcelEventConfig(in);
        }

        @Override
        public ParcelEventConfig[] newArray(int size) {
            return new ParcelEventConfig[size];
        }
    };

    private ParcelEventConfig(Parcel in) {
        isCareful = in.readByte() != 0;
        isInternal = in.readByte() != 0;
    }

    public ParcelEventConfig(boolean isCareful, boolean isInternal) {
        this.isCareful = isCareful;
        this.isInternal = isInternal;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isCareful ? 1 : 0));
        dest.writeByte((byte) (isInternal ? 1 : 0));
    }

    public boolean isCareful() {
        return isCareful;
    }

    public boolean isInternal() {
        return isInternal;
    }

    @Override
    public String toString() {
        return "EventConfig{" +
                "isCareful=" + isCareful +
                ", isInternal=" + isInternal +
                '}';
    }
}
