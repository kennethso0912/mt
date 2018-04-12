package com.ubtrobot.master.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.CallInfo;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.component.StringResource;

/**
 * Created by column on 17-11-23.
 */

public class ServiceCallInfo extends CallInfo implements Parcelable {

    public static final Creator<ServiceCallInfo> CREATOR = new Creator<ServiceCallInfo>() {
        @Override
        public ServiceCallInfo createFromParcel(Parcel in) {
            return new ServiceCallInfo(in);
        }

        @Override
        public ServiceCallInfo[] newArray(int size) {
            return new ServiceCallInfo[size];
        }
    };

    public ServiceCallInfo(ComponentInfo parentComponent, String path) {
        this(parentComponent, path, null);
    }

    public ServiceCallInfo(
            ComponentInfo parentComponent,
            String path, StringResource description) {
        super(parentComponent, path, description);
    }

    private ServiceCallInfo(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return super.describeContents();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public String toString() {
        return "ServiceCallInfo{" +
                "parentComponent=" + getParentComponent().getName() +
                ", path='" + getPath() + '\'' +
                ", description=" + getDescription() +
                '}';
    }
}