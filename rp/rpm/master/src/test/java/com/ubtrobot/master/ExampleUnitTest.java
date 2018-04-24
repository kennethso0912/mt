package com.ubtrobot.master;

import com.ubtrobot.master.async.AsyncTask;
import com.ubtrobot.master.async.AsyncTaskCallback;
import com.ubtrobot.master.async.ParallelFlow;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);

        new ParallelFlow<IOException>(
        ).add(new AsyncTask<IOException>() {
            @Override
            public void execute(AsyncTaskCallback<IOException> callback, Object... arguments) {
                System.out.println("Task");
                callback.onFailure(new IOException());
            }
        }).onComplete(new ParallelFlow.CompleteCallback<IOException>() {
            @Override
            public void onComplete(ParallelFlow.Results<IOException> results) {
                System.out.println("OnComplete.");
            }
        }).start();
    }
}