package com.ubtrobot.master.competition;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;
import java.util.List;

public class CompetingItemDetailList implements Parcelable {

    public static final Creator<CompetingItemDetailList> CREATOR = new Creator<CompetingItemDetailList>() {
        @Override
        public CompetingItemDetailList createFromParcel(Parcel in) {
            return new CompetingItemDetailList(in);
        }

        @Override
        public CompetingItemDetailList[] newArray(int size) {
            return new CompetingItemDetailList[size];
        }
    };

    private List<CompetingItemDetail> competingItemList;

    public CompetingItemDetailList() {
        competingItemList = new LinkedList<>();
    }

    public CompetingItemDetailList(List<CompetingItemDetail> competingItemList) {
        this.competingItemList = competingItemList;
    }

    protected CompetingItemDetailList(Parcel in) {
        competingItemList = in.createTypedArrayList(CompetingItemDetail.CREATOR);
    }

    public List<CompetingItemDetail> getCompetingItemList() {
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
        return "CompetingItemDetailList{" +
                "competingItemDetailList=" + competingItemList +
                '}';
    }
}
