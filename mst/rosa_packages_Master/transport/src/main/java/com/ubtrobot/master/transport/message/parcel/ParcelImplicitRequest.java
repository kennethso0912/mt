package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-11-28.
 */

public class ParcelImplicitRequest extends AbstractParcelRequest {

    private final Map<String, String> matchingRules;
    private String path;

    public static final Creator<ParcelImplicitRequest> CREATOR = new Creator<ParcelImplicitRequest>() {
        @Override
        public ParcelImplicitRequest createFromParcel(Parcel in) {
            return new ParcelImplicitRequest(in);
        }

        @Override
        public ParcelImplicitRequest[] newArray(int size) {
            return new ParcelImplicitRequest[size];
        }
    };

    private ParcelImplicitRequest(Parcel in) {
        super(in);
        matchingRules = new HashMap<>();
        in.readMap(matchingRules, matchingRules.getClass().getClassLoader());
    }

    public ParcelImplicitRequest(ParcelRequestContext context, ParcelRequestConfig config,
                                 Map<String, String> matchingRules) {
        this(context, config, matchingRules, null);
    }

    public ParcelImplicitRequest(ParcelRequestContext context, ParcelRequestConfig config,
                                 Map<String, String> matchingRules, AbstractParam param) {
        super(context, config, param);
        if (matchingRules == null) {
            throw new IllegalArgumentException("Argument matchingRules is null");
        }

        this.matchingRules = matchingRules;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeMap(matchingRules);
    }

    public Map<String, String> getMatchingRules() {
        return matchingRules;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    public ParcelRequest toExplicitRequest() {
        if (path == null) {
            throw new IllegalStateException("Set path first.");
        }

        // 注意：必须要复用 id
        return new ParcelRequest(getId(), getWhen(), getContext(), getConfig(), path, getParam());
    }

    @Override
    public String toString() {
        return "ImplicitRequest{" +
                "id='" + getId() + '\'' +
                ", when=" + getWhen() +
                ", matchingRules=" + matchingRules +
                ", context=" + getContext() +
                ", config=" + getConfig() +
                ", connectionId='" + getConnectionId() + '\'' +
                ", param=" + getParam() +
                '}';
    }
}