package com.ubtrobot.master.competition;

/**
 * Created by zhu on 18-1-31.
 */
public interface InterruptionListener {

    void onBegan(CompetitionSession session);

    void onEnded(CompetitionSession session, boolean resume);
}