package com.ubtrobot.master.sample.mstservice.param;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by column on 17-12-26.
 */

public class BatchServoCommand extends LinkedList<ServoCommand> implements Parcelable {

    public static final Creator<BatchServoCommand> CREATOR = new Creator<BatchServoCommand>() {
        @Override
        public BatchServoCommand createFromParcel(Parcel in) {
            return new BatchServoCommand(in);
        }

        @Override
        public BatchServoCommand[] newArray(int size) {
            return new BatchServoCommand[size];
        }
    };

    private BatchServoCommand(Parcel in) {
        in.readList(this, BatchServoCommand.class.getClassLoader());
    }

    public BatchServoCommand() {
    }

    public BatchServoCommand(@NonNull Collection<? extends ServoCommand> c) {
        super(c);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this);
    }
}
