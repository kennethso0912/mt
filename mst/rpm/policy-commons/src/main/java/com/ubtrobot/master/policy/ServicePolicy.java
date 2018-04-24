package com.ubtrobot.master.policy;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.ComponentBaseInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-11-28.
 */

public class ServicePolicy implements Parcelable {

    private ComponentBaseInfo service;

    private Map<String, BlackWhiteList<ComponentBaseInfo>> didAddState;
    private Map<String, BlackWhiteList<ComponentBaseInfo>> willAddState;

    public static final Creator<ServicePolicy> CREATOR = new Creator<ServicePolicy>() {
        @Override
        public ServicePolicy createFromParcel(Parcel in) {
            return new ServicePolicy(in);
        }

        @Override
        public ServicePolicy[] newArray(int size) {
            return new ServicePolicy[size];
        }
    };

    public ServicePolicy(ComponentBaseInfo service) {
        this.service = service;

        didAddState = new HashMap<>();
        willAddState = new HashMap<>();
    }

    private ServicePolicy(Parcel in) {
        service = in.readParcelable(ComponentBaseInfo.class.getClassLoader());

        didAddState = new HashMap<>();
        in.readMap(didAddState, BlackWhiteList.class.getClassLoader());
        willAddState = new HashMap<>();
        in.readMap(willAddState, BlackWhiteList.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(service, flags);

        dest.writeMap(didAddState);
        dest.writeMap(willAddState);
    }

    public ComponentBaseInfo getService() {
        return service;
    }

    public BlackWhiteList<ComponentBaseInfo> getDidAddState(String state) {
        BlackWhiteList<ComponentBaseInfo> blackWhiteList = didAddState.get(state);
        if (blackWhiteList == null) {
            blackWhiteList = new BlackWhiteList<>();
            didAddState.put(state, blackWhiteList);
        }

        return blackWhiteList;
    }

    public BlackWhiteList<ComponentBaseInfo> getWillAddState(String state) {
        BlackWhiteList<ComponentBaseInfo> blackWhiteList = willAddState.get(state);
        if (blackWhiteList == null) {
            blackWhiteList = new BlackWhiteList<>();
            willAddState.put(state, blackWhiteList);
        }

        return blackWhiteList;
    }

    @Override
    public String toString() {
        return "ServicePolicy{" +
                "service=" + service +
                ", didAddState=" + didAddState +
                ", willAddState=" + willAddState +
                '}';
    }
}