package com.ubtrobot.master.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.ComponentInfo;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-11-23.
 */

public class ServiceInfo extends ComponentInfo implements Parcelable {

    private List<ServiceCallInfo> callInfoList; // 该字段不参与序列化。ServiceCallInfo.parentComponent 会引起栈溢出

    private boolean mutable;

    public static final Creator<ServiceInfo> CREATOR = new Creator<ServiceInfo>() {
        @Override
        public ServiceInfo createFromParcel(Parcel in) {
            return new ServiceInfo(in);
        }

        @Override
        public ServiceInfo[] newArray(int size) {
            return new ServiceInfo[size];
        }
    };

    private ServiceInfo(ComponentInfo.Builder builder) {
        super(builder);
    }

    protected ServiceInfo(Parcel in) {
        super(in);
    }

    public List<ServiceCallInfo> getCallInfoList() {
        return callInfoList;
    }

    public void makeImmutable() {
        if (mutable) {
            callInfoList = Collections.unmodifiableList(callInfoList);
            mutable = false;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "name='" + getName() + '\'' +
                ", packageName='" + getPackageName() + '\'' +
                ", className='" + getClassName() + '\'' +
                ", isSystemPackage=" + isSystemPackage() +
                ", label=" + getLabel() +
                ", description=" + getDescription() +
                ", iconRes=" + getIconResource() +
                ", callInfoList=" + callInfoList +
                '}';
    }

    public static class Builder extends ComponentInfo.Builder<Builder> {

        private List<ServiceCallInfo> callInfoList;

        public Builder(String name, String packageName, String className) {
            super(name, packageName, className);
        }

        public Builder setCallInfoList(List<ServiceCallInfo> callInfoList) {
            this.callInfoList = callInfoList;
            return this;
        }

        public ServiceInfo build() {
            ServiceInfo serviceInfo = new ServiceInfo(this);
            serviceInfo.callInfoList = callInfoList;
            serviceInfo.mutable = true;
            return serviceInfo;
        }
    }
}