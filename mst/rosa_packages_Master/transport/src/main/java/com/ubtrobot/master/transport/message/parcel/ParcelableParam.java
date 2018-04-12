package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.transport.message.Param;

/**
 * Created by column on 17-11-27.
 */

public class ParcelableParam<T extends Parcelable> extends AbstractParam {

    public static final String TYPE = "ParcelableParam";

    private final T parcelable;

    private ParcelableParam(T parcelable, byte[] bytes) {
        super(TYPE, bytes);
        this.parcelable = parcelable;
    }

    private ParcelableParam(Class<T> clazz, byte[] bytes) {
        super(TYPE, bytes);

        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);

            //noinspection unchecked
            this.parcelable = (T) parcel.readParcelable(clazz.getClassLoader());
        } finally {
            parcel.recycle();
        }
    }

    public static <T extends Parcelable> ParcelableParam<T> create(T parcelable) {
        if (parcelable == null) {
            throw new IllegalArgumentException("Argument parcelable is null.");
        }

        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(parcelable, 0);

        try {
            return new ParcelableParam<>(parcelable, parcel.marshall());
        } finally {
            parcel.recycle();
        }
    }

    public static <T extends Parcelable> ParcelableParam<T> from(Param param, Class<T> clazz)
            throws InvalidParcelableParamException {
        if (param == null || clazz == null) {
            throw new IllegalArgumentException("Argument param or clazz is null.");
        }

        if (!(param instanceof PendingParam) || !TYPE.equals(param.getType())) {
            throw new InvalidParcelableParamException();
        }

        try {
            return new ParcelableParam<>(clazz, ((PendingParam) param).getBytes());
        } catch (ClassCastException e) {
            throw new InvalidParcelableParamException(e);
        }
    }

    public T getParcelable() {
        return parcelable;
    }

    @Override
    public String toString() {
        return "Param{" +
                "parcelable=" + parcelable +
                '}';
    }

    public static class InvalidParcelableParamException extends Exception {

        public InvalidParcelableParamException() {
        }

        public InvalidParcelableParamException(String message) {
            super(message);
        }

        public InvalidParcelableParamException(Throwable cause) {
            super(cause);
        }
    }
}
