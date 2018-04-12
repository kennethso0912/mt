package com.ubtrobot.master.call;

import android.os.Looper;

import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.CallGlobalCode;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Response;
import com.ubtrobot.transport.message.ResponseCallback;
import com.ubtrobot.ulog.Logger;

/**
 * Created by column on 17-12-27.
 */

public class SyncCallUtils {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("SyncCallUtils");

    private SyncCallUtils() {
    }

    public static Response syncCall(
            IPCByMasterCallable callable,
            CallConfiguration configuration,
            Request request) throws CallException {
        checkSyncCallThread(callable, configuration);

        final boolean[] responded = new boolean[1];
        final Response[] response = new Response[1];
        final CallException[] exception = new CallException[1];

        callable.call(request, new ResponseCallback() {
            @Override
            public void onResponse(Request req, Response res) {
                synchronized (responded) {
                    response[0] = res;

                    responded[0] = true;
                    responded.notifyAll();
                }
            }

            @Override
            public void onFailure(Request req, CallException e) {
                synchronized (responded) {
                    exception[0] = e;

                    responded[0] = true;
                    responded.notifyAll();
                }
            }
        });

        synchronized (responded) {
            while (!responded[0]) {
                try {
                    responded.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CallException(CallGlobalCode.INTERNAL_ERROR,
                            "The thread of sync call was interrupted.", e);
                }
            }

            if (exception[0] != null) {
                throw exception[0];
            }

            return response[0];
        }
    }

    private static void checkSyncCallThread(
            IPCByMasterCallable callable, CallConfiguration configuration) {
        if (Looper.myLooper() == Looper.getMainLooper() &&
                !configuration.isSyncCallOnMainThreadWarningSuppressed()) {
            LOGGER.w("You should avoid sync calling on the main thread.");
        }

        if (callable.connection().eventLoop().inEventLoop()) {
            throw new IllegalStateException(
                    "Internal error. Sync call in the connection 's event loop.");
        }
    }
}
