package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ShareActivity extends AppCompatActivity {
    public static final String TAG = "LG> ShareActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG) {
            Log.d(TAG, ".onCreate");
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Bundle extras = intent.getExtras();

        Intent i = new Intent(this, MainActivity.class);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                i.setType("YOUTUBE_LINK");
                i.putExtras(extras);
            }
            if ("video/*".equals(type)) {
                i.setType("VIDEO_URI");
                i.putExtras(extras);
            }
            if (LOG) {
                Log.d(TAG, ".onResume with Intent.action --> " + action + " Intent.type --> " + type);
            }
        } else {
            if (LOG) {
                Log.d(TAG, ".onResume with Intent --> " + action + " ... NO type/extras passed to main activity");
            }
        }

        startActivity(i);
        finish();
    }
}