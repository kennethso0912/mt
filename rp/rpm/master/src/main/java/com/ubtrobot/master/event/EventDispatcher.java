package com.ubtrobot.master.event;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.connection.OutgoingCallback;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.Publisher;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by column on 17-9-9.
 */

public class EventDispatcher implements Publisher {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("EventDispatcher");

    private final HashMap<String, HashSet<Connection>> mSubscribers = new HashMap<>();

    @Override
    public void publish(final Event event) {
        synchronized (mSubscribers) {
            HashSet<Connection> connections = mSubscribers.get(event.getAction());
            if (connections == null) {
                LOGGER.i("Publish event, but no subscribers. event=%s", event);
                return;
            }

            for (final Connection connection : connections) {
                connection.write(event, new OutgoingCallback() {
                    @Override
                    public void onSuccess() {
                        // NOOP
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LOGGER.e(
                                e,
                                "Publish event to some subscriber failed. event=%s, connectionId=%s",
                                event,
                                connection.id().asText()
                        );
                    }
                });
            }
        }
    }

    public void subscribe(Connection connection, List<String> actions) {
        synchronized (mSubscribers) {
            for (String action : actions) {
                HashSet<Connection> connections = mSubscribers.get(action);
                if (connections == null) {
                    connections = new HashSet<>();
                    mSubscribers.put(action, connections);
                }

                if (connections.add(connection)) {
                    LOGGER.i(
                            "Subscribe event success. action=%s, connectionId=%s",
                            action,
                            connection.id().asText()
                    );
                } else {
                    LOGGER.w(
                            "Subscribe event, but event was subscribed. action=%s, connectionId=%s",
                            action,
                            connection.id().asText()
                    );
                }
            }
        }
    }

    public void unsubscribe(Connection connection, List<String> actions) {
        synchronized (mSubscribers) {
            for (String action : actions) {
                HashSet<Connection> connections = mSubscribers.get(action);
                if (connections != null && connections.remove(connection)) {
                    LOGGER.i(
                            "Unsubscribe event success. action=%s, connectionId=%s",
                            action,
                            connection.id().asText()
                    );

                    if (connections.isEmpty()) {
                        mSubscribers.remove(action);
                    }

                    continue;
                }

                LOGGER.w(
                        "Unsubscribe event, but event was not subscribed. action=%s, connectionId=%s",
                        action,
                        connection.id().asText()
                );
            }
        }
    }

    public void unsubscribe(Connection connection) {
        synchronized (mSubscribers) {
            Iterator<Map.Entry<String, HashSet<Connection>>> subscriberIterator =
                    mSubscribers.entrySet().iterator();
            while (subscriberIterator.hasNext()) {
                Map.Entry<String, HashSet<Connection>> aAction = subscriberIterator.next();
                Iterator<Connection> connectionIterator = aAction.getValue().iterator();

                while (connectionIterator.hasNext()) {
                    Connection aConnection = connectionIterator.next();
                    if (connection.equals(aConnection)) {
                        connectionIterator.remove();
                        LOGGER.i(
                                "Unsubscribe event success. action=%s, connectionId=%s",
                                aAction.getKey(),
                                connection.id().asText()
                        );

                        break;
                    }
                }

                if (aAction.getValue().isEmpty()) {
                    subscriberIterator.remove();
                }
            }
        }
    }
}
