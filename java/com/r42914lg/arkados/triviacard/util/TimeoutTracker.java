package com.r42914lg.arkados.triviacard.util;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.Executor;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class TimeoutTracker {
    public static final String TAG = "LG> TimeoutTracker";

    private final Executor executor;
    private final Handler resultHandler;
    private ITimeoutListener listener;

    public TimeoutTracker(Executor executor, Handler resultHandler) {
        this.executor = executor;
        this.resultHandler = resultHandler;
        if (LOG) {
            Log.d(TAG, ": instance created");
        }
    }

    public void setTimeoutListener(ITimeoutListener listener) {
        this.listener = listener;
        if (LOG) {
            Log.d(TAG, ".setTimeoutListener: listener set " + listener);
        }
    }

    public void start(int timeInMilliseconds, int questionIndex) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (LOG) {
                    Log.d(TAG, ".start: timeout thread started to wait " + timeInMilliseconds);
                }
                SystemClock.sleep(timeInMilliseconds);
                if (listener == null) {
                    return;
                }
                resultHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (LOG) {
                            Log.d(TAG, ".start: timeout thread finished to wait " + timeInMilliseconds + "  ms, doing callback for questionIndex = " + questionIndex);
                        }
                        listener.callbackWaitTimeExpired(questionIndex);
                    }
                });
            }
        });
    }
}
