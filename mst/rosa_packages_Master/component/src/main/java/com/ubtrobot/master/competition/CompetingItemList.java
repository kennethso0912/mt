package com.ubtrobot.master.competition;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;
import java.util.List;

public class CompetingItemList implements Parcelable {

    public static final Creator<CompetingItemList> CREATOR = new Creator<CompetingItemList>() {
        @Override
        public CompetingItemList createFromParcel(Parcel in) {
            return new CompetingItemList(in);
        }

        @Override
        public CompetingItemList[] newArray(int size) {
            return new CompetingItemList[size];
        }
    };

    private List<CompetingItem> competingItemList;

    public CompetingItemList() {
        competingItemList = new LinkedList<>();
    }

    public CompetingItemList(List<CompetingItem> competingItemList) {
        this.competingItemList = competingItemList;
    }

    private CompetingItemList(Parcel in) {
        competingItemList = in.createTypedArrayList(CompetingItem.CREATOR);
    }

    public List<CompetingItem> getCompetingItemList() {
        return competingItemList;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(competingItemList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "CompetingItemList{" +
                "competingItemList=" + competingItemList +
                '}';
    }
}
