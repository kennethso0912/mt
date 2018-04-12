package com.ubtrobot.master.event;

import android.text.TextUtils;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.parcel.AbstractParam;
import com.ubtrobot.master.transport.message.parcel.ParcelEvent;
import com.ubtrobot.master.transport.message.parcel.ParcelEventConfig;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.Param;
import com.ubtrobot.transport.message.Publisher;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 03/09/2017.
 */

public class RemotePublisher implements ConvenientPublisher, Publisher {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("RemotePublisher");

    private final Connection mConnection;

    public RemotePublisher(Connection connection) {
        mConnection = connection;
    }

    @Override
    public void publish(String action) {
        if (TextUtils.isEmpty(action)) {
            throw new IllegalArgumentException("action is null");
        }

        publish(new ParcelEvent(new ParcelEventConfig(false, false), action));
    }

    @Override
    public void publish(String action, Param param) {
        if (param != null && !(param instanceof AbstractParam)) {
            throw new IllegalArgumentException("Unsupported param type.");
        }

        publish(new ParcelEvent(new ParcelEventConfig(false, false), action, (AbstractParam) param));
    }

    @Override
    public void publishCarefully(String action) {
        publish(new ParcelEvent(new ParcelEventConfig(true, false), action));
    }

    @Override
    public void publishCarefully(String action, Param param) {
        publish(new ParcelEvent(new ParcelEventConfig(true, false), action, (AbstractParam) param));
    }

    @Override
    public void publish(final Event event) {
        mConnection.write(event, new OutgoingCallback() {
            @Override
            public void onSuccess() {
                // Ignore
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.e(e, "Publish event failed. event=%s", event);
            }
        });
    }
}