package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.transport.message.Event;

/**
 * Created by column on 17-11-27.
 */

public class ParcelEvent implements Parcelable, Event {

    private String id;
    private long when;
    private String action;
    private ParcelEventConfig config;
    private AbstractParam param;

    public static final Creator<ParcelEvent> CREATOR = new Creator<ParcelEvent>() {
        @Override
        public ParcelEvent createFromParcel(Parcel in) {
            return new ParcelEvent(in);
        }

        @Override
        public ParcelEvent[] newArray(int size) {
            return new ParcelEvent[size];
        }
    };

    public ParcelEvent(ParcelEventConfig config, String action) {
        this(config, action, null);
    }

    public ParcelEvent(ParcelEventConfig config, String action, AbstractParam param) {
        id = IdGenerator.nextId();
        when = System.currentTimeMillis() / 1000;

        this.config = config;
        this.action = action;
        this.param = param == null ? new EmptyParam() : param;
    }

    private ParcelEvent(Parcel in) {
        id = in.readString();
        when = in.readLong();
        action = in.readString();
        config = in.readParcelable(ParcelEventConfig.class.getClassLoader());

        ParcelParamWrap paramWrap = in.readParcelable(ParcelParamWrap.class.getClassLoader());
        param = paramWrap.unwrap();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(when);
        dest.writeString(action);
        dest.writeParcelable(config, flags);

        dest.writeParcelable(param.wrap(), flags);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getWhen() {
        return when;
    }

    @Override
    public String getAction() {
        return action;
    }

    public ParcelEventConfig getConfig() {
        return config;
    }

    @Override
    public AbstractParam getParam() {
        return param;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", when=" + when +
                ", action='" + action + '\'' +
                ", config=" + config +
                ", param=" + param +
                '}';
    }
}
