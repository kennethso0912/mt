package com.ubtrobot.master.param;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.PendingParam;
import com.ubtrobot.transport.message.Param;

/**
 * Created by column on 10/09/2017.
 */

public final class ProtoParam<T extends Message> extends AbstractParam {

    public static final String TYPE = "ProtoParam";

    private final Any any;
    private final T protoMessage;

    private ProtoParam(byte[] bytes, Any any, T protoMessage) {
        super(TYPE, bytes);
        this.any = any;
        this.protoMessage = protoMessage;
    }

    public static <T extends Message> ProtoParam<T> create(T protoMessage) {
        if (protoMessage == null) {
            throw new IllegalArgumentException("Argument protoMessage is null.");
        }

        if (protoMessage instanceof Any) {
            return new ProtoParam<>(protoMessage.toByteArray(), (Any) protoMessage, protoMessage);
        } else {
            Any any = Any.pack(protoMessage);
            return new ProtoParam<>(any.toByteArray(), any, protoMessage);
        }
    }

    public static <T extends Message> ProtoParam<T> from(Param param, Class<T> clazz)
            throws InvalidProtoParamException {
        if (param == null) {
            throw new IllegalArgumentException("param is null.");
        }

        if (!(param instanceof PendingParam) || !TYPE.equals(param.getType())) {
            throw new InvalidProtoParamException();
        }

        byte[] bytes = ((PendingParam) param).getBytes();
        try {
            Any any = Any.parseFrom(bytes);
            if (Any.class.equals(clazz)) {
                //noinspection unchecked
                return new ProtoParam<>(bytes, any, (T) any);
            } else {
                return new ProtoParam<>(bytes, any, any.unpack(clazz));
            }
        } catch (InvalidProtocolBufferException e) {
            throw new InvalidProtoParamException(e);
        }
    }

    public T getProtoMessage() {
        return protoMessage;
    }

    public Any getAny() {
        return any;
    }

    @Override
    public String toString() {
        return "ProtoParam{" +
                "protoMessage=" + protoMessage +
                '}';
    }

    public static class InvalidProtoParamException extends Exception {

        public InvalidProtoParamException() {
        }

        public InvalidProtoParamException(String message) {
            super(message);
        }

        public InvalidProtoParamException(Throwable cause) {
            super(cause);
        }
    }
}