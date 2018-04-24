package com.ubtrobot.master.skill;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by zhu on 18-1-11.
 */

public class SkillInfoList implements Parcelable {

    private List<SkillInfo> skillInfoList;

    public static final Creator<SkillInfoList> CREATOR = new Creator<SkillInfoList>() {
        @Override
        public SkillInfoList createFromParcel(Parcel in) {
            return new SkillInfoList(in);
        }

        @Override
        public SkillInfoList[] newArray(int size) {
            return new SkillInfoList[size];
        }
    };

    private SkillInfoList(Parcel in) {
        skillInfoList = in.createTypedArrayList(SkillInfo.CREATOR);
    }

    public SkillInfoList(List<SkillInfo> skillInfoList) {
        this.skillInfoList = skillInfoList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(skillInfoList);
    }

    public List<SkillInfo> getSkillInfoList() {
        return skillInfoList;
    }

    @Override
    public String toString() {
        return "SkillInfoList{" +
                "skillInfoList=" + skillInfoList +
                '}';
    }
}
