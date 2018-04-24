package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;

/**
 * Created by column on 17-11-28.
 */

public class ResponderTypeRequestSplitter extends MessageSplitter.RequestHandler {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("ResponderTypeRequestSplitter");

    private final HashMap<String, MessageSplitter.RequestHandler> mHandlers = new HashMap<>();

    public ResponderTypeRequestSplitter addHandler(
            String processorType, MessageSplitter.RequestHandler handler) {
        mHandlers.put(processorType, handler);
        return this;
    }

    @Override
    public void onRead(HandlerContext context, ParcelRequest request) {
        MessageSplitter.MessageHandler handler = mHandlers.get(request.getContext().getResponderType());
        if (handler == null) {
            LOGGER.e("Unexpected processor type. processType=%s", request.getContext().getResponderType());
            return;
        }

        //noinspection unchecked
        handler.onRead(context, request);
    }
}