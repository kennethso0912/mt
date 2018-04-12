package com.ubtrobot.master.component;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-20.
 */

public class StringResource implements Parcelable {

    private String content;
    private int resourceId;

    public static final Creator<StringResource> CREATOR = new Creator<StringResource>() {
        @Override
        public StringResource createFromParcel(Parcel in) {
            return new StringResource(in);
        }

        @Override
        public StringResource[] newArray(int size) {
            return new StringResource[size];
        }
    };

    private StringResource(Parcel in) {
        content = in.readString();
        resourceId = in.readInt();
    }

    public StringResource(String content) {
        this(content, 0);
    }

    public StringResource(int resourceId) {
        this(null, resourceId);
    }

    private StringResource(String content, int resourceId) {
        this.content = content;
        this.resourceId = resourceId;
    }

    public String getContent() {
        return content;
    }

    public int getResourceId() {
        return resourceId;
    }

    public boolean isEmpty() {
        return content == null && resourceId == 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(content);
        dest.writeInt(resourceId);
    }

    @Override
    public String toString() {
        return "StringResource{" +
                "content='" + content + '\'' +
                ", resourceId=" + resourceId +
                '}';
    }
}
