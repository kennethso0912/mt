package com.ubtrobot.master.skill;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.ComponentInfo;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-11-23.
 */

public class SkillInfo extends ComponentInfo implements Parcelable {

    private List<SkillCallInfo> callInfoList; // 该字段不参与序列化。SkillInfo.parentComponent 会引起栈溢出

    private boolean mutable;

    public static final Creator<SkillInfo> CREATOR = new Creator<SkillInfo>() {
        @Override
        public SkillInfo createFromParcel(Parcel in) {
            return new SkillInfo(in);
        }

        @Override
        public SkillInfo[] newArray(int size) {
            return new SkillInfo[size];
        }
    };

    protected SkillInfo(Parcel in) {
        super(in);
    }

    private SkillInfo(String name, String packageName, String className) {
        super(name, packageName, className);
    }

    private SkillInfo(ComponentInfo.Builder<?> builder) {
        super(builder);
    }

    public List<SkillCallInfo> getCallInfoList() {
        return callInfoList;
    }

    public void makeImmutable() {
        if (mutable) {
            callInfoList = Collections.unmodifiableList(callInfoList);
            mutable = false;
        }
    }

    @Override
    public String toString() {
        return "SkillInfo{" +
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

        private List<SkillCallInfo> callInfoList;

        public Builder(String name, String packageName, String className) {
            super(name, packageName, className);
        }

        public Builder setCallInfoList(List<SkillCallInfo> callInfoList) {
            this.callInfoList = callInfoList;
            return this;
        }

        @Override
        public SkillInfo build() {
            SkillInfo skillInfo = new SkillInfo(this);
            skillInfo.callInfoList = callInfoList;
            skillInfo.mutable = true;
            return skillInfo;
        }
    }
}
