package com.r42914lg.arkados.vitalk.media;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.views.YouTubePlayerSeekBar;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.ViTalkConstants;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.ui.IViTalkWorker;

public class MediaOrchestrator {
    public static final String TAG = "LG> MediaOrchestrator";

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final YouTubePlayerView youTubePlayerView;
    private final YouTubePlayerSeekBar youTubePlayerSeekBar;
    private final ViTalkVM viTalkVM;
    private final IViTalkWorker iViTalkWorker;
    private final String dataSource;

    private AudioRecorderWithMute audioRecorderWithMute;
    private AudioPlayer audioPlayer;
    private boolean recorderReady;

    private YouTubePlayer youTubePlayer;
    private MyUiController myUiController;

    public MediaOrchestrator(Context context, LifecycleOwner lifecycleOwner, YouTubePlayerView youTubePlayerView,
                             YouTubePlayerSeekBar youTubePlayerSeekBar, ViTalkVM viTalkVM, IViTalkWorker iViTalkWorker) {

        this.dataSource = context.getExternalCacheDir().getAbsolutePath() + ViTalkConstants.FILE_NAME;
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.youTubePlayerView = youTubePlayerView;
        this.youTubePlayerSeekBar = youTubePlayerSeekBar;
        this.viTalkVM = viTalkVM;
        this.iViTalkWorker = iViTalkWorker;

        viTalkVM.setDataSource(dataSource);
        initVideoPlayer();

        if (LOG) {
            Log.d(TAG, " instance created, video player initialized");
        }
    }

    public void initAudioRecorder() {
        audioRecorderWithMute = new AudioRecorderWithMute(context, dataSource);
        audioRecorderWithMute.initAudioRecorder();
        recorderReady = true;
    }

    public void initAudioPlayer() {
        audioPlayer = new AudioPlayer();
        audioPlayer.initAudioPlayer(dataSource);
    }

    public boolean recorderReady() {
        return audioRecorderWithMute !=  null && recorderReady;
    }

    public AudioRecorderWithMute getAudioRecorder() {
        return audioRecorderWithMute;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public void releaseAudioRecorder() {
        if (audioRecorderWithMute == null) {
            return;
        }

        audioRecorderWithMute.release();
        recorderReady = false;

        if (LOG) {
            Log.d(TAG, ".releaseAudioRecorder: AudioRecorder released");
        }
    }

    public void releaseAudioPlayer() {
        if (audioPlayer == null) {
            return;
        }
        audioPlayer.release();
        if (LOG) {
            Log.d(TAG, ".releaseAudioPlayer: audioPlayer released");
        }
    }

    private void initVideoPlayer() {
        lifecycleOwner.getLifecycle().addObserver(youTubePlayerView);
        youTubePlayerView.initialize(
                new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                        MediaOrchestrator.this.youTubePlayer = youTubePlayer;
                        myUiController = new MyUiController (
                                context,
                                youTubePlayerView.inflateCustomPlayerUi(R.layout.my_player_ui),
                                youTubePlayer,
                                MediaOrchestrator.this,
                                iViTalkWorker
                        );
                        youTubePlayer.addListener(myUiController);
                        youTubePlayer.addListener(youTubePlayerSeekBar);
                        youTubePlayerSeekBar.setYoutubePlayerSeekBarListener(youTubePlayer::seekTo);
                        youTubePlayer.cueVideo(viTalkVM.getCurrentYoutubeId(),0f);
                        viTalkVM.onYouTubePlayerReady();
                    }
                    @Override
                    public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                        if (state == PlayerConstants.PlayerState.VIDEO_CUED) {
                            viTalkVM.onVideoCued();
                        }
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.RECORD)) {
                                stopRecordingSession(false);
                            }
                            if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.PREVIEW)) {
                                audioPlayer.clearEndOfAudioFlag();
                            }
                            myUiController.animateButton(false);
                        }
                    }
                }, new IFramePlayerOptions.Builder().controls(0).build());
    }

    public void rewindVideo() {
        youTubePlayer.seekTo(0);
        youTubePlayer.pause();
    }

    public void onMuteChecked(boolean b) {
        if (b) {
            youTubePlayer.mute();
        } else {
            youTubePlayer.unMute();
        }
    }

    public void onRecordResume() {
        audioRecorderWithMute.setMicMuted(false);
    }

    public void onRecordPause() {
        audioRecorderWithMute.setMicMuted(true);
    }

    public void onResumeFragment() {
        if (myUiController == null) {
            return;
        }
        myUiController.resumeAnimation();
    }

    public void onPauseFragment() {
        if (myUiController == null) {
            return;
        }
        myUiController.endAnimation();
        releaseAudioPlayer();
        releaseAudioRecorder();
    }

    public MyUiController getMyUiController() {
        return myUiController;
    }

    public void stopRecordingSession(boolean forceYoutubeStopFlag) {
        if (forceYoutubeStopFlag) {
            youTubePlayer.pause();
        }
        audioRecorderWithMute.stop();
        viTalkVM.onRecordSessionEnded(dataSource);
    }
}
