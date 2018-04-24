package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.transport.message.Response;

/**
 * Created by column on 17-11-27.
 */

public class ParcelResponse implements Parcelable, Response {

    public static final String RESULT_TYPE_SUCCESS = "success";
    public static final String RESULT_TYPE_FAILURE = "failure";
    public static final String RESULT_TYPE_STICKILY = "stickily";

    private String id;
    private long when;
    private String requestId;
    private String requestConnectionId;
    private String path;
    private String resultType;
    private AbstractParam param;

    public static final Creator<ParcelResponse> CREATOR = new Creator<ParcelResponse>() {
        @Override
        public ParcelResponse createFromParcel(Parcel in) {
            return new ParcelResponse(in);
        }

        @Override
        public ParcelResponse[] newArray(int size) {
            return new ParcelResponse[size];
        }
    };

    private ParcelResponse(Parcel in) {
        id = in.readString();
        when = in.readLong();
        requestId = in.readString();
        requestConnectionId = in.readString();
        path = in.readString();
        resultType = in.readString();

        ParcelParamWrap paramWrap = in.readParcelable(ParcelParamWrap.class.getClassLoader());
        param = paramWrap.unwrap();
    }

    public ParcelResponse(AbstractParcelRequest request, String resultType) {
        this(request, resultType, null);
    }

    public ParcelResponse(AbstractParcelRequest request, String resultType, AbstractParam param) {
        id = IdGenerator.nextId();
        when = System.currentTimeMillis() / 1000;

        requestId = request.getId();
        requestConnectionId = request.getConnectionId() == null ? "" : request.getConnectionId();
        path = request.getPath() == null ? "" : request.getPath(); // ParcelImplicitRequest 可能为 null
        this.resultType = resultType;
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
        dest.writeString(requestId);
        dest.writeString(requestConnectionId);
        dest.writeString(path);
        dest.writeString(resultType);
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
    public String getRequestId() {
        return requestId;
    }

    public String getRequestConnectionId() {
        return requestConnectionId;
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getResultType() {
        return resultType;
    }

    @Override
    public AbstractParam getParam() {
        return param;
    }

    @Override
    public String toString() {
        return "Response{" +
                "requestId='" + requestId + '\'' +
                ", requestConnectionId='" + requestConnectionId + '\'' +
                ", path='" + path + '\'' +
                ", resultType='" + resultType + '\'' +
                '}';
    }
}
