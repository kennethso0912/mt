package com.ubtrobot.master.policy;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by column on 26/11/2017.
 */

public class BlackWhiteList<T extends Parcelable> extends LinkedList<T> implements Parcelable {

    private boolean whitelist;

    public static final Creator<BlackWhiteList> CREATOR = new Creator<BlackWhiteList>() {
        @Override
        public BlackWhiteList createFromParcel(Parcel in) {
            return new BlackWhiteList(in);
        }

        @Override
        public BlackWhiteList[] newArray(int size) {
            return new BlackWhiteList[size];
        }
    };

    public BlackWhiteList() {
    }

    public BlackWhiteList(@NonNull Collection<? extends T> c, boolean whitelist) {
        super(c);
    }

    private BlackWhiteList(Parcel in) {
        whitelist = in.readByte() != 0;
        in.readList(this, BlackWhiteList.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (whitelist ? 1 : 0));
        dest.writeList(this);
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public boolean isBlacklist() {
        return !whitelist;
    }

    public void setBlacklist(boolean blacklist) {
        this.whitelist = !blacklist;
    }

    @Override
    public String toString() {
        return "BlackWhiteList{" +
                "isWhitelist=" + whitelist +
                ", isBlacklist=" + !whitelist +
                ", list=" + super.toString() +
                '}';
    }
}
