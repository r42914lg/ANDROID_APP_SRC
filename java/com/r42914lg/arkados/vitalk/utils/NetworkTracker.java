package com.r42914lg.arkados.vitalk.utils;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.r42914lg.arkados.vitalk.model.ViTalkVM;

public class NetworkTracker {
    public static final String TAG = "LG> NetworkTracker";

    private boolean isOnline;

    public NetworkTracker(Activity activity, ViTalkVM viTalkVM) {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (!isOnline) {
                    viTalkVM.setNetworkStatus(true);
                }
                isOnline = true;
                if (LOG) {
                    Log.d(TAG, ".onAvailable");
                }
            }
            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (isOnline) {
                    viTalkVM.setNetworkStatus(false);
                }
                isOnline = false;
                if (LOG) {
                    Log.d(TAG, ".onLost");
                }
            }
        };

        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(networkRequest, networkCallback);

        if (LOG) {
            Log.d(TAG, "  instance created, network callback registered...");
        }
    }
}
