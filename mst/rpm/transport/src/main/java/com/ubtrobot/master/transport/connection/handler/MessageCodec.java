package com.ubtrobot.master.transport.connection.handler;

import android.os.Parcelable;

import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.transport.connection.DuplexHandlerAdapter;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.OutgoingCallback;

/**
 * Created by column on 17-11-27.
 */

public class MessageCodec extends DuplexHandlerAdapter {

    @Override
    public void onRead(HandlerContext context, Object message) {
        if (!(message instanceof ParcelMessage)) {
            throw new IllegalStateException("Unexpected message type. message class is " +
                    message.getClass().getName());
        }

        context.onRead(message);
    }

    @Override
    public void write(HandlerContext context, Object message, OutgoingCallback callback) {
        if (!(message instanceof Parcelable)) {
            throw new IllegalStateException("Unexpected message type. message class is " +
                    message.getClass().getName());
        }

        context.write(new ParcelMessage((Parcelable) message), callback);
    }
}