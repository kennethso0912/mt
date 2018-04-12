package com.ubtrobot.master.competition;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompetitionSessionInfo implements Parcelable {

    public static final Creator<CompetitionSessionInfo>
            CREATOR = new Creator<CompetitionSessionInfo>() {
        @Override
        public CompetitionSessionInfo createFromParcel(Parcel in) {
            return new CompetitionSessionInfo(in);
        }

        @Override
        public CompetitionSessionInfo[] newArray(int size) {
            return new CompetitionSessionInfo[size];
        }
    };

    private String sessionId;
    private List<CompetingItem> competingItems;
    private Map<String, Set<CompetingItem>> competingItemMap;

    protected CompetitionSessionInfo(Parcel in) {
        sessionId = in.readString();
        competingItems = in.createTypedArrayList(CompetingItem.CREATOR);
        if (competingItems == null) {
            competingItems = new LinkedList<>();
        }
        competingItems = Collections.unmodifiableList(competingItems);
    }

    private CompetitionSessionInfo(List<CompetingItem> competingItems) {
        this.competingItems = Collections.unmodifiableList(competingItems);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sessionId);
        dest.writeTypedList(competingItems);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<CompetingItem> getCompetingItems() {
        return competingItems;
    }

    public Map<String, Set<CompetingItem>> getCompetingItemMap() {
        if (this.competingItemMap == null) {
            HashMap<String, Set<CompetingItem>> competingItemMap = new HashMap<>();

            for (CompetingItem competingItem : competingItems) {
                Set<CompetingItem> itemSet = competingItemMap.get(competingItem.getService());
                if (itemSet == null) {
                    itemSet = new HashSet<>();
                    competingItemMap.put(competingItem.getService(), itemSet);
                }

                itemSet.add(competingItem);
            }

            this.competingItemMap = new HashMap<>();
            for (Map.Entry<String, Set<CompetingItem>> entry : competingItemMap.entrySet()) {
                this.competingItemMap.put(entry.getKey(),
                        Collections.unmodifiableSet(entry.getValue()));
            }

            this.competingItemMap = Collections.unmodifiableMap(this.competingItemMap);
        }

        return this.competingItemMap;
    }

    public Set<CompetingItem> getServiceCompetingItems(String service) {
        return getCompetingItemMap().get(service);
    }

    @Override
    public String toString() {
        return "CompetitionSessionInfo{" +
                "sessionId='" + sessionId + '\'' +
                ", competingItems=" + competingItems +
                '}';
    }

    public static class Builder {

        private String sessionId;
        private LinkedList<CompetingItem> competingItems = new LinkedList<>();

        public Builder() {
        }

        public Builder(CompetitionSessionInfo sessionInfo) {
            sessionId = sessionInfo.getSessionId();
            competingItems = new LinkedList<>(sessionInfo.getCompetingItems());
        }

        public Builder setSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder addCompetingItem(CompetingItem item) {
            competingItems.add(item);
            return this;
        }

        public Builder addCompetingItemAll(Collection<CompetingItem> items) {
            competingItems.addAll(items);
            return this;
        }

        public CompetitionSessionInfo build() {
            CompetitionSessionInfo info = new CompetitionSessionInfo(competingItems);
            info.sessionId = sessionId;
            return info;
        }
    }
}
