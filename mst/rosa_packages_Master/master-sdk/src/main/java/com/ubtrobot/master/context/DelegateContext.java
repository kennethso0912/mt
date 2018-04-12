package com.ubtrobot.master.context;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.competition.CompetitionSessionImpl;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.event.SubscriberAdapter;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.ulog.Logger;

import java.util.List;

/**
 * Created by column on 17-12-6.
 */

public class DelegateContext extends BaseContext {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("DelegateContext");

    private final Unsafe mUnsafe;
    private final MasterContext mRealContext;
    private final SubscriberAdapter mSubscriberDelegate;
    private volatile boolean mOpened;

    public DelegateContext(
            Unsafe unsafe, MasterContext context, String contextType, String contextName) {
        super(unsafe, contextType, contextName);
        mUnsafe = unsafe;
        mRealContext = context;
        mSubscriberDelegate = new SubscriberAdapter(context, unsafe.getSubscriberForSdkUser(),
                unsafe.getHandlerOnMainThread());
    }

    public void open() {
        mOpened = true;
    }

    @Override
    public void subscribe(EventReceiver receiver, String action) {
        if (!mOpened) {
            throw new IllegalStateException();
        }

        mSubscriberDelegate.subscribe(receiver, action);
    }

    @Override
    public void unsubscribe(EventReceiver receiver) {
        if (receiver == null) {
            throw new IllegalArgumentException("Argument receiver was null.");
        }

        mSubscriberDelegate.unsubscribe(receiver);
    }

    @Override
    public CompetitionSession openCompetitionSession() {
        return new CompetitionSessionImpl(mRealContext, mUnsafe);
    }

    public void close() {
        mOpened = false;

        List<EventReceiver> receivers = mSubscriberDelegate.unsubscribeAll();
        if (!receivers.isEmpty()) {
            LOGGER.e("EventReceivers NOT unsubscribe when context is NOT invalid.");
        }
    }
}