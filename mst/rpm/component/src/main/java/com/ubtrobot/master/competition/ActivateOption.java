package com.ubtrobot.master.competition;

import android.os.Parcel;
import android.os.Parcelable;

public class ActivateOption implements Parcelable {

    public static final Creator<ActivateOption> CREATOR = new Creator<ActivateOption>() {
        @Override
        public ActivateOption createFromParcel(Parcel in) {
            return new ActivateOption(in);
        }

        @Override
        public ActivateOption[] newArray(int size) {
            return new ActivateOption[size];
        }
    };

    private boolean shouldResume;
    private int priority;

    private ActivateOption() {
    }

    private ActivateOption(Parcel in) {
        shouldResume = in.readByte() != 0;
        priority = in.readInt();
    }

    public boolean isShouldResume() {
        return shouldResume;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (shouldResume ? 1 : 0));
        dest.writeInt(priority);
    }

    @Override
    public String toString() {
        return "ActivateOption{" +
                "shouldResume=" + shouldResume +
                ", priority=" + priority +
                '}';
    }

    public static final class Builder {

        private boolean shouldResume;
        private int priority;

        public Builder() {
        }

        public Builder(ActivateOption option) {
            shouldResume = option.shouldResume;
            priority = option.priority;
        }

        public Builder setShouldResume(boolean shouldResume) {
            this.shouldResume = shouldResume;
            return this;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public ActivateOption build() {
            ActivateOption option = new ActivateOption();
            option.shouldResume = shouldResume;
            option.priority = priority;
            return option;
        }
    }
}