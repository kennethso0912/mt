package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by column on 17-11-27.
 */

public class ParcelMessage implements Parcelable {

    private Parcelable content;

    public static final Creator<ParcelMessage> CREATOR = new Creator<ParcelMessage>() {
        @Override
        public ParcelMessage createFromParcel(Parcel in) {
            return new ParcelMessage(in);
        }

        @Override
        public ParcelMessage[] newArray(int size) {
            return new ParcelMessage[size];
        }
    };

    public ParcelMessage(Parcelable content) {
        this.content = content;
    }

    private ParcelMessage(Parcel in) {
        Serializable contentClazz = in.readSerializable();
        if (!(contentClazz instanceof Class<?>)) {
            throw new IllegalStateException("Illegal parcel.");
        }

        content = in.readParcelable(((Class) contentClazz).getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(content.getClass());
        dest.writeParcelable(content, flags);
    }

    public Parcelable getContent() {
        return content;
    }

    public Class<? extends Parcelable> getContentClass() {
        return content.getClass();
    }

    @Override
    public String toString() {
        return "Message{" +
                "content=" + content +
                '}';
    }
}