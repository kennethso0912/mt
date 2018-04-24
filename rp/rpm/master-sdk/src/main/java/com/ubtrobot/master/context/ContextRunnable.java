package com.ubtrobot.master.context;

/**
 * Created by column on 17-12-16.
 */

public interface ContextRunnable<T extends MasterContext> {

    void run(T context);
}
