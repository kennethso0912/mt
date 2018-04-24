package com.ubtrobot.master.skill;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.StringResource;

import java.util.Collections;
import java.util.List;

/**
 * Created by column on 17-11-21.
 */

public class SkillIntentFilter implements Parcelable {

    public static final String[] CATEGORIES = new String[]{"speech"};
    public static final String CATEGORY_SPEECH = CATEGORIES[0];

    private String category;
    private List<StringResource> utteranceList;

    public static final Creator<SkillIntentFilter> CREATOR = new Creator<SkillIntentFilter>() {
        @Override
        public SkillIntentFilter createFromParcel(Parcel in) {
            return new SkillIntentFilter(in);
        }

        @Override
        public SkillIntentFilter[] newArray(int size) {
            return new SkillIntentFilter[size];
        }
    };

    public SkillIntentFilter(String category, List<StringResource> utteranceList) {
        this.category = category;
        this.utteranceList = Collections.unmodifiableList(utteranceList);
    }

    private SkillIntentFilter(Parcel in) {
        category = in.readString();
        utteranceList = in.createTypedArrayList(StringResource.CREATOR);
        if (utteranceList != null) {
            utteranceList = Collections.unmodifiableList(utteranceList);
        }
    }

    public String getCategory() {
        return category;
    }

    public List<StringResource> getUtteranceList() {
        return utteranceList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(category);
        dest.writeTypedList(utteranceList);
    }

    @Override
    public String toString() {
        return "SkillIntentFilter{" +
                "category='" + category + '\'' +
                ", utteranceList=" + utteranceList +
                '}';
    }
}