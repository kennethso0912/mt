package com.ubtrobot.master.sample.mstservice.param;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-12-26.
 */

public class ServoCommand implements Parcelable {

    private String servoId;
    private String command;
    private String param;

    public ServoCommand(String servoId, String command, String param) {
        this.servoId = servoId;
        this.command = command;
        this.param = param;
    }

    private ServoCommand(Parcel in) {
        servoId = in.readString();
        command = in.readString();
        param = in.readString();
    }

    public static final Creator<ServoCommand> CREATOR = new Creator<ServoCommand>() {
        @Override
        public ServoCommand createFromParcel(Parcel in) {
            return new ServoCommand(in);
        }

        @Override
        public ServoCommand[] newArray(int size) {
            return new ServoCommand[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(servoId);
        dest.writeString(command);
        dest.writeString(param);
    }


    public String getServoId() {
        return servoId;
    }

    public String getCommand() {
        return command;
    }

    public String getParam() {
        return param;
    }

    @Override
    public String toString() {
        return "ServoCommand{" +
                "servoId='" + servoId + '\'' +
                ", command='" + command + '\'' +
                ", param='" + param + '\'' +
                '}';
    }
}
