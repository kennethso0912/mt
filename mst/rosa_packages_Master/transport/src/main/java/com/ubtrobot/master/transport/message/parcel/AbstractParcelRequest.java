package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.transport.message.Request;

/**
 * Created by column on 17-11-27.
 */

public abstract class AbstractParcelRequest implements Parcelable, Request {

    private String id;
    private long when;
    private ParcelRequestContext context;
    private ParcelRequestConfig config;
    private String connectionId;
    private AbstractParam param;

    protected AbstractParcelRequest(Parcel in) {
        id = in.readString();
        when = in.readLong();

        context = in.readParcelable(ParcelRequestContext.class.getClassLoader());
        config = in.readParcelable(ParcelRequestConfig.class.getClassLoader());
        connectionId = in.readString();

        ParcelParamWrap paramWrap = in.readParcelable(ParcelParamWrap.class.getClassLoader());
        param = paramWrap.unwrap();
    }

    protected AbstractParcelRequest(ParcelRequestContext context, ParcelRequestConfig config) {
        this(context, config, null);
    }

    protected AbstractParcelRequest(
            ParcelRequestContext context, ParcelRequestConfig config, AbstractParam param) {
        this(IdGenerator.nextId(), System.currentTimeMillis() / 1000, context, config, param);
    }

    protected AbstractParcelRequest(
            String id, long when,
            ParcelRequestContext context, ParcelRequestConfig config, AbstractParam param) {
        this.id = id;
        this.when = when;

        this.context = context;
        this.config = config;
        connectionId = "";
        this.param = param == null ? new EmptyParam() : param;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(when);
        dest.writeParcelable(context, flags);
        dest.writeParcelable(config, flags);
        dest.writeString(connectionId);

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

    public ParcelRequestContext getContext() {
        return context;
    }

    public ParcelRequestConfig getConfig() {
        return config;
    }

    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Master 转发 AbstractParcelRequest 时填充，在收到 ParcelResponse 后,
     * 通过 ParcelResponse.getRequestConnectionId() 知道需要将 ParcelResponse 转发到那个 Connection。
     * <p>
     * 当设置 connectionId = null | "" 时，表明由 Master 直接发送，而不是转发。
     *
     * @param connectionId 请求的连接 id
     */
    public void changeConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public AbstractParam getParam() {
        return param;
    }
}