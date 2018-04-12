package com.ubtrobot.master.component;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-20.
 */

public class CallInfo implements Parcelable {

    private ComponentInfo parentComponent;
    private String path;
    private StringResource description;

    public static final Creator<CallInfo> CREATOR = new Creator<CallInfo>() {
        @Override
        public CallInfo createFromParcel(Parcel in) {
            return new CallInfo(in);
        }

        @Override
        public CallInfo[] newArray(int size) {
            return new CallInfo[size];
        }
    };

    protected CallInfo(Parcel in) {
        parentComponent = in.readParcelable(ComponentInfo.class.getClassLoader());
        path = in.readString();
        description = in.readParcelable(StringResource.class.getClassLoader());
    }

    public CallInfo(ComponentInfo parentComponent, String path) {
        this(parentComponent, path, null);
    }

    public CallInfo(ComponentInfo parentComponent, String path, StringResource description) {
        this.parentComponent = parentComponent;
        this.path = path;
        this.description = description;
    }

    public ComponentInfo getParentComponent() {
        return parentComponent;
    }

    public String getPath() {
        return path;
    }

    public StringResource getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(parentComponent, flags);
        dest.writeString(path);
        dest.writeParcelable(description, flags);
    }

    @Override
    public String toString() {
        return "CallInfo{" +
                "parentComponent=" + parentComponent +
                ", path='" + path + '\'' +
                ", description=" + description +
                '}';
    }
}
