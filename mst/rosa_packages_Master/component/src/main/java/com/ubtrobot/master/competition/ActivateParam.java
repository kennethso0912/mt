package com.ubtrobot.master.competition;

import android.os.Parcel;
import android.os.Parcelable;

public class ActivateParam implements Parcelable {

    public static final Creator<ActivateParam> CREATOR = new Creator<ActivateParam>() {
        @Override
        public ActivateParam createFromParcel(Parcel in) {
            return new ActivateParam(in);
        }

        @Override
        public ActivateParam[] newArray(int size) {
            return new ActivateParam[size];
        }
    };

    private ActivateOption option;
    private CompetitionSessionInfo sessionInfo;

    public ActivateParam(ActivateOption option, CompetitionSessionInfo sessionInfo) {
        this.option = option;
        this.sessionInfo = sessionInfo;
    }

    private ActivateParam(Parcel in) {
        option = in.readParcelable(ActivateOption.class.getClassLoader());
        sessionInfo = in.readParcelable(CompetitionSessionInfo.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(option, flags);
        dest.writeParcelable(sessionInfo, flags);
    }

    public ActivateOption getOption() {
        return option;
    }

    public CompetitionSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public String toString() {
        return "ActivateParam{" +
                "option=" + option +
                ", sessionInfo=" + sessionInfo +
                '}';
    }
}
