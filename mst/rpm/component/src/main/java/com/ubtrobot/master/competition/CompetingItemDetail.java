package com.ubtrobot.master.competition;

import android.os.Parcel;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CompetingItemDetail extends CompetingItem {

    public static final Creator<CompetingItemDetail> CREATOR = new Creator<CompetingItemDetail>() {
        @Override
        public CompetingItemDetail createFromParcel(Parcel in) {
            return new CompetingItemDetail(in);
        }

        @Override
        public CompetingItemDetail[] newArray(int size) {
            return new CompetingItemDetail[size];
        }
    };

    private String description;
    private List<String> callPathList;

    private CompetingItemDetail(String service, String itemId) {
        super(service, itemId);
    }

    protected CompetingItemDetail(Parcel in) {
        super(in);

        description = in.readString();

        List<String> callPathList = in.createStringArrayList();
        if (callPathList == null) {
            this.callPathList = Collections.emptyList();
        } else {
            this.callPathList = Collections.unmodifiableList(callPathList);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(description);
        dest.writeStringList(callPathList);
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCallPathList() {
        return callPathList;
    }

    @Override
    public String toString() {
        return "CompetingItemDetail{" +
                "service='" + getService() + '\'' +
                ", itemId='" + getItemId() + '\'' +
                "description='" + description + '\'' +
                ", callPathList=" + callPathList +
                '}';
    }

    public static class Builder {

        private final String service;
        private final String itemId;
        private String description;
        private LinkedList<String> callPathList = new LinkedList<>();

        public Builder(String service, String itemId) {
            this.service = service;
            this.itemId = itemId;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder addCallPath(String callPath) {
            callPathList.add(callPath);
            return this;
        }

        public CompetingItemDetail build() {
            CompetingItemDetail itemDetail = new CompetingItemDetail(service, itemId);
            itemDetail.description = description;
            itemDetail.callPathList = Collections.unmodifiableList(callPathList);
            return itemDetail;
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "service='" + service + '\'' +
                    ", itemId='" + itemId + '\'' +
                    ", description='" + description + '\'' +
                    ", callPathList=" + callPathList +
                    '}';
        }
    }
}
