package com.ubtrobot.master.skill;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-24.
 */

public class SkillIntent implements Parcelable {

    public static final String CATEGORY_SPEECH = SkillIntentFilter.CATEGORY_SPEECH;

    private String category;
    private String speechUtterance;

    public static final Creator<SkillIntent> CREATOR = new Creator<SkillIntent>() {
        @Override
        public SkillIntent createFromParcel(Parcel in) {
            return new SkillIntent(in);
        }

        @Override
        public SkillIntent[] newArray(int size) {
            return new SkillIntent[size];
        }
    };

    public SkillIntent(String category) {
        this.category = category;
    }

    protected SkillIntent(Parcel in) {
        category = in.readString();
        speechUtterance = in.readString();
    }

    public SkillIntent setSpeechUtterance(String utterance) {
        speechUtterance = utterance;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public String getSpeechUtterance() {
        return speechUtterance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(category);
        dest.writeString(speechUtterance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkillIntent that = (SkillIntent) o;

        if (category != null ? !category.equals(that.category) : that.category != null)
            return false;
        return speechUtterance != null ? speechUtterance.equals(that.speechUtterance) : that.speechUtterance == null;
    }

    @Override
    public int hashCode() {
        int result = category != null ? category.hashCode() : 0;
        result = 31 * result + (speechUtterance != null ? speechUtterance.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SkillIntent{" +
                "category='" + category + '\'' +
                ", speechUtterance='" + speechUtterance + '\'' +
                '}';
    }
}