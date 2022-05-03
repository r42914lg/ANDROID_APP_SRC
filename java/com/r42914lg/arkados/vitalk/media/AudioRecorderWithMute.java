package com.r42914lg.arkados.vitalk.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import java.io.IOException;

public class AudioRecorderWithMute extends MediaRecorder {

    private final Context context;
    private final String audioFileName;
    private enum State {ZERO,READY,STARTED,PAUSED,RESUMED,STOPPED}
    private State currentState;

    public AudioRecorderWithMute(Context context, String dataSource) {
        this.context = context;
        audioFileName = dataSource;
        currentState = State.ZERO;
    }

    public void initAudioRecorder() {
        if (currentState.equals(State.READY)) {
            return;
        }

        if (!currentState.equals(State.ZERO)) {
            reset();
            currentState = State.ZERO;
        }

        setAudioSource(AudioSource.MIC);
        setOutputFormat(OutputFormat.AAC_ADTS);
        setAudioEncoder(AudioEncoder.AAC);
        setOutputFile(audioFileName);

        try {
            prepare();
            currentState = State.READY;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMicMuted(boolean state){
        AudioManager myAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int workingAudioMode = myAudioManager.getMode();

        myAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        if (myAudioManager.isMicrophoneMute() != state) {
            myAudioManager.setMicrophoneMute(state);
        }

        myAudioManager.setMode(workingAudioMode);
    }

    private boolean wasStarted() {
        return currentState.equals(State.STARTED) || currentState.equals(State.STOPPED) || currentState.equals(State.PAUSED) || currentState.equals(State.RESUMED);
    }

    public void resumeOrStart() {
        if (wasStarted()) {
            resume();
        } else {
            start();
        }
    }

    @Override
    public void resume() throws IllegalStateException {
        super.resume();
        currentState = State.RESUMED;
    }

    @Override
    public void start() throws IllegalStateException {
        super.start();
        currentState = State.STARTED;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        currentState = State.STOPPED;
    }

    @Override
    public void pause() throws IllegalStateException {
        super.pause();
        currentState = State.PAUSED;
    }

    @Override
    public void reset() {
        super.reset();
        currentState = State.ZERO;
    }
}
