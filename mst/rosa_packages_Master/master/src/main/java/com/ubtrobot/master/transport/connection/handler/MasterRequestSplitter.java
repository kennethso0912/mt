package com.ubtrobot.master.transport.connection.handler;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.HandlerContext;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;

/**
 * Created by column on 17-11-28.
 */

public class MasterRequestSplitter extends MessageSplitter.RequestHandler {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("MasterRequestSplitter");

    private final HashMap<String, MessageSplitter.RequestHandler> mHandlers = new HashMap<>();

    public MasterRequestSplitter addHandler(
            String path, MessageSplitter.RequestHandler handler) {
        mHandlers.put(path, handler);
        return this;
    }

    @Override
    public void onRead(HandlerContext context, ParcelRequest request) {
        MessageSplitter.RequestHandler handler = mHandlers.get(request.getPath());
        if (handler == null) {
            LOGGER.e("Unexpected path for calling master. path=%s", request.getPath());
            return;
        }

        handler.onRead(context, request);
    }
}