package com.ubtrobot.master.policy;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by column on 17-12-26.
 */

public class ComponentPolicyList implements Parcelable {

    private List<SkillPolicy> skillPolicyList;
    private List<ServicePolicy> servicePolicyList;

    public static final Creator<ComponentPolicyList> CREATOR = new Creator<ComponentPolicyList>() {
        @Override
        public ComponentPolicyList createFromParcel(Parcel in) {
            return new ComponentPolicyList(in);
        }

        @Override
        public ComponentPolicyList[] newArray(int size) {
            return new ComponentPolicyList[size];
        }
    };

    private ComponentPolicyList(Parcel in) {
        skillPolicyList = in.createTypedArrayList(SkillPolicy.CREATOR);
        servicePolicyList = in.createTypedArrayList(ServicePolicy.CREATOR);
    }

    public ComponentPolicyList() {
        skillPolicyList = new LinkedList<>();
        servicePolicyList = new LinkedList<>();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(skillPolicyList);
        dest.writeTypedList(servicePolicyList);
    }

    public List<SkillPolicy> getSkillPolicyList() {
        return skillPolicyList;
    }

    public List<ServicePolicy> getServicePolicyList() {
        return servicePolicyList;
    }

    @Override
    public String toString() {
        return "ComponentPolicyList{" +
                "skillPolicyList=" + skillPolicyList +
                ", servicePolicyList=" + servicePolicyList +
                '}';
    }
}
