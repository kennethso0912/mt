package com.ubtrobot.master.transport.connection;

import com.ubtrobot.transport.connection.Connection;

/**
 * Created by column on 17-8-31.
 */

public interface ConnectionInitializer {

    void onConnectionInitialize(Connection connection);
}