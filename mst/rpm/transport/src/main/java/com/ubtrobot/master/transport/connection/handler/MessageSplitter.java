package com.ubtrobot.master.transport.connection.handler;

import android.os.Parcelable;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.ParcelEvent;
import com.ubtrobot.master.transport.message.parcel.ParcelImplicitRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelResponse;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.transport.connection.IncomingHandlerAdapter;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;

/**
 * Created by column on 17-11-27.
 */

public class MessageSplitter extends IncomingHandlerAdapter {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MessageSplitter");

    private final HashMap<Class<? extends Parcelable>, MessageHandler> mHandlers = new HashMap<>();

    public MessageSplitter(MessageHandler<? extends Parcelable>... handlers) {
        for (MessageHandler<? extends Parcelable> handler : handlers) {
            mHandlers.put(handler.getMessageClass(), handler);
        }
    }

    @Override
    public void onRead(HandlerContext context, Object message) {
        ParcelMessage parcelMessage = (ParcelMessage) message;
        MessageHandler handler = mHandlers.get(parcelMessage.getContentClass());
        if (handler == null) {
            LOGGER.e("Unexpected message type. message class is %s", message.getClass().getName());
            return;
        }

        //noinspection unchecked
        handler.onRead(context, parcelMessage.getContent());
    }

    public interface MessageHandler<T extends Parcelable> {

        Class<T> getMessageClass();

        void onRead(HandlerContext context, T message);
    }

    public static abstract class EventHandler implements MessageHandler<ParcelEvent> {

        @Override
        public Class<ParcelEvent> getMessageClass() {
            return ParcelEvent.class;
        }
    }

    public static abstract class RequestHandler implements MessageHandler<ParcelRequest> {

        @Override
        public Class<ParcelRequest> getMessageClass() {
            return ParcelRequest.class;
        }
    }

    public static abstract class ImplicitRequestHandler implements MessageHandler<ParcelImplicitRequest> {

        @Override
        public Class<ParcelImplicitRequest> getMessageClass() {
            return ParcelImplicitRequest.class;
        }
    }

    public static abstract class ResponseHandler implements MessageHandler<ParcelResponse> {

        @Override
        public Class<ParcelResponse> getMessageClass() {
            return ParcelResponse.class;
        }
    }
}
