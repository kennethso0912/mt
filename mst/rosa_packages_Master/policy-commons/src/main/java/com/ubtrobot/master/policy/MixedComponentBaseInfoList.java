package com.ubtrobot.master.policy;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.ComponentBaseInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by column on 17-12-26.
 */

public class MixedComponentBaseInfoList implements Parcelable {

    private List<ComponentBaseInfo> skillBaseInfoList;
    private List<ComponentBaseInfo> serviceBaseInfoList;

    public static final Creator<MixedComponentBaseInfoList> CREATOR = new Creator<MixedComponentBaseInfoList>() {
        @Override
        public MixedComponentBaseInfoList createFromParcel(Parcel in) {
            return new MixedComponentBaseInfoList(in);
        }

        @Override
        public MixedComponentBaseInfoList[] newArray(int size) {
            return new MixedComponentBaseInfoList[size];
        }
    };

    public MixedComponentBaseInfoList() {
        skillBaseInfoList = new LinkedList<>();
        serviceBaseInfoList = new LinkedList<>();
    }

    private MixedComponentBaseInfoList(Parcel in) {
        skillBaseInfoList = in.createTypedArrayList(ComponentBaseInfo.CREATOR);
        serviceBaseInfoList = in.createTypedArrayList(ComponentBaseInfo.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(skillBaseInfoList);
        dest.writeTypedList(serviceBaseInfoList);
    }

    public List<ComponentBaseInfo> getSkillBaseInfoList() {
        return skillBaseInfoList;
    }

    public List<ComponentBaseInfo> getServiceBaseInfoList() {
        return serviceBaseInfoList;
    }

    @Override
    public String toString() {
        return "MixedComponentBaseInfoList{" +
                "skillBaseInfoList=" + skillBaseInfoList +
                ", serviceBaseInfoList=" + serviceBaseInfoList +
                '}';
    }
}
