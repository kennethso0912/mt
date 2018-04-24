package com.ubtrobot.master.competition;

/**
 * Created by zhu on 26/02/2018.
 */

public interface ActivateCallback extends ActivateFailureCallback {

    void onSuccess(String sessionId);
}
