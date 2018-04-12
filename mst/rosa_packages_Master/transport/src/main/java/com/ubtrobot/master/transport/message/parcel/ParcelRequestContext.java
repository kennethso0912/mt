package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelRequestContext implements Parcelable {

    public static final String REQUESTER_TYPE_MASTER = "master";
    public static final String REQUESTER_TYPE_SKILL = "skill";
    public static final String REQUESTER_TYPE_SERVICE = "service";
    public static final String REQUESTER_TYPE_INTERACTOR = "interactor";
    public static final String REQUESTER_TYPE_EVENT_RECEIVER = "event-receiver";
    public static final String REQUESTER_TYPE_GLOBAL_CONTEXT = "global-context";

    public static final String RESPONDER_TYPE_MASTER = "master";
    public static final String RESPONDER_TYPE_SERVICE = "service";
    public static final String RESPONDER_TYPE_SKILLS = "skills";
    public static final String RESPONDER_TYPE_SKILL_OR_SERVICE = "skill|service";

    public static final Creator<ParcelRequestContext> CREATOR = new Creator<ParcelRequestContext>() {
        @Override
        public ParcelRequestContext createFromParcel(Parcel in) {
            return new ParcelRequestContext(in);
        }

        @Override
        public ParcelRequestContext[] newArray(int size) {
            return new ParcelRequestContext[size];
        }
    };

    private String requesterType;
    private String requester;
    private String competingSession;
    private String responderType;
    private String responder;
    private String responderPackage;

    private ParcelRequestContext(String responderType, String responder) {
        this.responderType = responderType;
        this.responder = responder;
    }

    private ParcelRequestContext(Parcel in) {
        requesterType = in.readString();
        requester = in.readString();
        competingSession = in.readString();
        responderType = in.readString();
        responder = in.readString();
        responderPackage = in.readString();
    }

    public String getRequesterType() {
        return requesterType;
    }

    public String getRequester() {
        return requester;
    }

    public String getCompetingSession() {
        return competingSession;
    }

    public String getResponderType() {
        return responderType;
    }

    public String getResponder() {
        return responder;
    }

    public void changeResponder(String responder) {
        this.responder = responder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(requesterType);
        dest.writeString(requester);
        dest.writeString(competingSession);
        dest.writeString(responderType);
        dest.writeString(responder);
        dest.writeString(responderPackage);
    }

    @Override
    public String toString() {
        return "RequestContext{" +
                "requesterType='" + requesterType + '\'' +
                ", requester='" + requester + '\'' +
                ", competingSession='" + competingSession + '\'' +
                ", responderType='" + responderType + '\'' +
                ", responder='" + responder + '\'' +
                ", responderPackage='" + responderPackage + '\'' +
                '}';
    }

    public static class Builder {

        private String requesterType;
        private String requester;
        private String competingSession;
        private String responderType;
        private String responder;
        private String responderPackage;

        public Builder(String responderType, String responder) {
            this.responderType = responderType;
            this.responder = responder;
        }

        public Builder(String responderType) {
            this(responderType, null);
        }

        public Builder setRequesterType(String requesterType) {
            this.requesterType = requesterType;
            return this;
        }

        public Builder setRequester(String requester) {
            this.requester = requester;
            return this;
        }

        public Builder setCompetingSession(String competingSession) {
            this.competingSession = competingSession;
            return this;
        }

        public Builder setResponderPackage(String responderPackage) {
            this.responderPackage = responderPackage;
            return this;
        }

        public ParcelRequestContext build() {
            ParcelRequestContext context = new ParcelRequestContext(responderType, responder);
            context.requesterType = requesterType;
            context.requester = requester;
            context.competingSession = competingSession;
            context.responderPackage = responderPackage;
            return context;
        }
    }
}
