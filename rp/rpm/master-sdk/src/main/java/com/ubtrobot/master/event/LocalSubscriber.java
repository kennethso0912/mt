package com.ubtrobot.master.event;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.transport.message.Event;
import com.ubtrobot.transport.message.EventReceiver;
import com.ubtrobot.transport.message.Subscriber;
import com.ubtrobot.ulog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by column on 17-8-29.
 */

public class LocalSubscriber implements Subscriber, EventReceiver {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("LocalSubscriber");

    private final HashMap<String, List<EventReceiver>> mReceives = new HashMap<>();

    private final FirstTimeSubscribeListener mFirstTimeSubscribeListener;
    private boolean mFirstTimeSubscribed;

    private ActionChangeListener mActionChangeListener;

    public LocalSubscriber(FirstTimeSubscribeListener listener) {
        mFirstTimeSubscribeListener = listener;
    }

    public void setActionChangeListener(ActionChangeListener listener) {
        mActionChangeListener = listener;
    }

    @Override
    public void subscribe(EventReceiver receiver, String action) {
        processFirstTimeSubscribe();

        final ArrayList<String> actionsAdd = new ArrayList<>();
        synchronized (mReceives) {
            List<EventReceiver> receivers = mReceives.get(action);
            if (receivers == null) {
                receivers = new ArrayList<>();
                mReceives.put(action, receivers);
                actionsAdd.add(action);
            }

            if (!receivers.contains(receiver)) {
                receivers.add(receiver);
            }
        }

        if (!actionsAdd.isEmpty() && mActionChangeListener != null) {
            mActionChangeListener.onAdd(actionsAdd);
        }
    }

    private void processFirstTimeSubscribe() {
        if (!mFirstTimeSubscribed) {
            mFirstTimeSubscribed = true;
            mFirstTimeSubscribeListener.onFirstTimeSubscribe();
        }
    }

    @Override
    public void unsubscribe(EventReceiver receiver) {
        if (receiver == null) {
            throw new IllegalArgumentException("Argument receiver was null");
        }

        final ArrayList<String> actionsRemove = new ArrayList<>();
        synchronized (mReceives) {
            Iterator<Map.Entry<String, List<EventReceiver>>> iterator = mReceives.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<EventReceiver>> entry = iterator.next();
                List<EventReceiver> aAction = entry.getValue();

                Iterator<EventReceiver> aActionIterator = aAction.iterator();
                while (aActionIterator.hasNext()) {
                    EventReceiver aReceiver = aActionIterator.next();
                    if (aReceiver.equals(receiver)) {
                        aActionIterator.remove();
                    }
                }

                if (aAction.isEmpty()) {
                    iterator.remove();
                    actionsRemove.add(entry.getKey());
                }
            }

            if (!actionsRemove.isEmpty() && mActionChangeListener != null) {
                mActionChangeListener.onRemove(actionsRemove);
            }
        }
    }

    public List<String> getSubscribedActions() {
        synchronized (mReceives) {
            return Collections.unmodifiableList(new ArrayList<>(mReceives.keySet()));
        }
    }

    @Override
    public void onReceive(final Event event) {
        synchronized (mReceives) {
            List<EventReceiver> receivers = mReceives.get(event.getAction());
            if (receivers == null || receivers.isEmpty()) {
                LOGGER.w("Receive event, but no receivers. event=%s", event);
                return;
            }

            for (final EventReceiver receiver : receivers) {
                receiver.onReceive(event);
            }
        }
    }

    public interface ActionChangeListener {

        void onAdd(List<String> actions);

        void onRemove(List<String> actions);
    }

    public interface FirstTimeSubscribeListener {

        void onFirstTimeSubscribe();
    }
}