package com.ubtrobot.master.component;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 26/11/2017.
 */

public class ComponentBaseInfo implements Parcelable {

    private String name;
    private String packageName;

    public static final Creator<ComponentBaseInfo> CREATOR = new Creator<ComponentBaseInfo>() {
        @Override
        public ComponentBaseInfo createFromParcel(Parcel in) {
            return new ComponentBaseInfo(in);
        }

        @Override
        public ComponentBaseInfo[] newArray(int size) {
            return new ComponentBaseInfo[size];
        }
    };

    public ComponentBaseInfo(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }

    private ComponentBaseInfo(Parcel in) {
        name = in.readString();
        packageName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(packageName);
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentBaseInfo that = (ComponentBaseInfo) o;

        if (!name.equals(that.name)) return false;
        return packageName.equals(that.packageName);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + packageName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ComponentBaseInfo{" +
                "name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}