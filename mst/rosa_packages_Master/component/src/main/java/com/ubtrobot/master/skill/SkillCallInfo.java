package com.ubtrobot.master.skill;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.CallInfo;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.component.StringResource;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-11-20.
 */

public class SkillCallInfo extends CallInfo implements Parcelable {

    private List<SkillIntentFilter> intentFilterList;

    public static final Creator<SkillCallInfo> CREATOR = new Creator<SkillCallInfo>() {
        @Override
        public SkillCallInfo createFromParcel(Parcel in) {
            return new SkillCallInfo(in);
        }

        @Override
        public SkillCallInfo[] newArray(int size) {
            return new SkillCallInfo[size];
        }
    };

    private SkillCallInfo(Parcel in) {
        super(in);

        intentFilterList = in.createTypedArrayList(SkillIntentFilter.CREATOR);
        if (intentFilterList != null) {
            intentFilterList = Collections.unmodifiableList(intentFilterList);
        }
    }

    public SkillCallInfo(ComponentInfo parentComponent, String path) {
        this(parentComponent, path, null, null);
    }

    public SkillCallInfo(ComponentInfo parentComponent, String path,
                         List<SkillIntentFilter> intentFilterList) {
        this(parentComponent, path, null, intentFilterList);
    }

    public SkillCallInfo(ComponentInfo parentComponent, String path, StringResource description,
                         List<SkillIntentFilter> intentFilterList) {
        super(parentComponent, path, description);

        this.intentFilterList = Collections.unmodifiableList(intentFilterList);
    }

    public List<SkillIntentFilter> getIntentFilterList() {
        return intentFilterList;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(intentFilterList);
    }

    @Override
    public String toString() {
        return "SkillCallInfo{" +
                "parentComponent=" + getParentComponent().getName() +
                ", path='" + getPath() + '\'' +
                ", description=" + getDescription() +
                ", intentFilterList=" + intentFilterList +
                '}';
    }
}