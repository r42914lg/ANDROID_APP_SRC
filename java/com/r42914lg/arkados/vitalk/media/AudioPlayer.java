package com.r42914lg.arkados.vitalk.media;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class AudioPlayer extends MediaPlayer {
    public static final String TAG = "LG> AudioPlayer";
    private boolean endOfAudio;

    public AudioPlayer() {
        setOnCompletionListener(mediaPlayer -> {
            endOfAudio = true;
            if (LOG) {
                Log.d(TAG, ".audioCompletionListener: end of audio reached --> flag set to TRUE");
            }
        });
        if (LOG) {
            Log.d(TAG, "  Instance created");
        }
    }

    public void initAudioPlayer(String dataSource) {
        try {
            setDataSource(dataSource);
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (LOG) {
            Log.d(TAG, ".initAudioPlayer: audioPlayer initialized");
        }
    }

    @Override
    public void start() throws IllegalStateException {
        if (LOG) {
            Log.d(TAG, ".start: end of audio flag == " + endOfAudio);
        }
        if (endOfAudio) {
            return;
        }
        super.start();
    }

    public void clearEndOfAudioFlag() {
        endOfAudio = false;
        if (LOG) {
            Log.d(TAG, ".clearEndOfAudioFlag: end of audio  flag cleared");
        }
    }
}
