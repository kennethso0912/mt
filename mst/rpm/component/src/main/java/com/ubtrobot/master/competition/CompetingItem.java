package com.ubtrobot.master.competition;

import android.os.Parcel;
import android.os.Parcelable;

public class CompetingItem implements Parcelable {

    private final String service;
    private final String itemId;

    public static final Creator<CompetingItem> CREATOR = new Creator<CompetingItem>() {
        @Override
        public CompetingItem createFromParcel(Parcel in) {
            return new CompetingItem(in);
        }

        @Override
        public CompetingItem[] newArray(int size) {
            return new CompetingItem[size];
        }
    };

    public CompetingItem(String service, String itemId) {
        this.service = service;
        this.itemId = itemId;
    }

    protected CompetingItem(Parcel in) {
        service = in.readString();
        itemId = in.readString();
    }

    public String getService() {
        return service;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(service);
        dest.writeString(itemId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompetingItem item = (CompetingItem) o;

        if (service != null ? !service.equals(item.service) : item.service != null) return false;
        return itemId != null ? itemId.equals(item.itemId) : item.itemId == null;
    }

    @Override
    public int hashCode() {
        int result = service != null ? service.hashCode() : 0;
        result = 31 * result + (itemId != null ? itemId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CompetingItem{" +
                "service='" + service + '\'' +
                ", itemId='" + itemId + '\'' +
                '}';
    }
}