package com.r42914lg.arkados.triviacard.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class CheckInternetHelper {
    public static final String TAG = "LG> CheckInternetHelper";

    private Context context;
    private IOnlineStatusListener listener;

    public CheckInternetHelper() {
        if (LOG) {
            Log.d(TAG, ": instance created");
        }
    }

    public void setOnlineStatusListener(Context context, IOnlineStatusListener listener) {
        this.context = context;
        this.listener = listener;
        if (LOG) {
            Log.d(TAG, ".setOnlineStatusListener: context, listener set " + listener);
        }
    }

    public void requestAsyncCheck(Executor executor, Handler resultHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                    boolean isOnline = checkIfOnline();
                    resultHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.callbackInternetStatusChecked(isOnline);
                        }
                    });
            }
        });
    }

    private boolean checkIfOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

            if (isConnected) {
                    final long time = System.currentTimeMillis();
                    final boolean[] connectedFlag = {false};

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                HttpURLConnection urlc = (HttpURLConnection)
                                        (new URL("https://clients3.google.com/generate_204")
                                                .openConnection());
                                urlc.setRequestProperty("User-Agent", "Android");
                                urlc.setRequestProperty("Connection", "close");
                                urlc.setConnectTimeout(1500);
                                urlc.setReadTimeout(1500);
                                urlc.connect();
                                if (urlc.getResponseCode() == 204 && urlc.getContentLength() == 0) {
                                    if (LOG) {
                                        Log.d(TAG, ".checkIfOnline: SUCCESS");
                                    }
                                    connectedFlag[0] = true;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    while ((System.currentTimeMillis() - time) <= 1500) {
                        if (connectedFlag[0]) {
                            return true;
                        }
                    }
                    if (LOG) {
                        Log.d(TAG, ".checkIfOnline: Error checking internet connection");
                    }
            } else {
                if (LOG) {
                    Log.d(TAG, ".checkIfOnline: No network available!");
                }
            }

        } else {
            if (LOG) {
                Log.d(TAG, ".checkIfOnline: ConnectivityManager is null, returning FALSE");
            }
        }

        return false;
    }
}
