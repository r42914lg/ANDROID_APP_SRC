package com.r42914lg.arkados.vitalk.model;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.util.Log;

public class WorkItemVideo {
    public static final String TAG = "LG> WorkItemVideo";

    public final String youTubeId;
    public String title;
    public int positionInAdapter;
    public boolean recordExists;

    public WorkItemVideo(boolean recordExists, String youTubeId) {

        this.youTubeId = youTubeId;
        this.recordExists = recordExists;
        this.positionInAdapter = -1;

        if (LOG) {
            Log.d(TAG, " instance created {youTubeId, recordExists} -->  {" + youTubeId + ", " + recordExists + "}");
        }
    }
}
