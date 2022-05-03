package com.r42914lg.arkados.vitalk.model;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.net.Uri;
import android.util.Log;

public class LocalVideo {
    public static final String TAG = "LG> LocalVideo";

    public final Uri uri;
    public final String name;
    public final int duration;
    public final int size;

    public LocalVideo(Uri uri, String name, int duration, int size) {

        this.uri = uri;
        this.name = name;
        this.duration = duration;
        this.size = size;

        if (LOG) {
            Log.d(TAG, " instance created {uri, name, duration,  size} -->  {" + uri + ", " + name + ", " + duration + ", " + size + "}");
        }
    }
}
