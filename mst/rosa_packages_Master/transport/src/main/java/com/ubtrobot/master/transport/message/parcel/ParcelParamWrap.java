package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-27.
 */

public class ParcelParamWrap implements Parcelable {

    private String paramType;
    private byte[] bytes;

    public static final Creator<ParcelParamWrap> CREATOR = new Creator<ParcelParamWrap>() {
        @Override
        public ParcelParamWrap createFromParcel(Parcel in) {
            return new ParcelParamWrap(in);
        }

        @Override
        public ParcelParamWrap[] newArray(int size) {
            return new ParcelParamWrap[size];
        }
    };

    private ParcelParamWrap(Parcel in) {
        paramType = in.readString();
        bytes = in.createByteArray();
    }

    public ParcelParamWrap(String paramType, byte[] bytes) {
        this.paramType = paramType;
        this.bytes = bytes;
    }

    public boolean isEmpty() {
        return bytes == null || bytes.length <= 0;
    }

    public AbstractParam unwrap() {
        if (EmptyParam.TYPE.equals(paramType)) {
            return new EmptyParam();
        }

        return new PendingParam(paramType, bytes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(paramType);
        dest.writeByteArray(bytes);
    }
}
