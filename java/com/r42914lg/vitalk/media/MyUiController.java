package com.r42914lg.arkados.vitalk.media;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.ui.IViTalkWorker;

public class MyUiController extends AbstractYouTubePlayerListener {
    public static final String TAG = "LG> MyUiController";

    private final Context context;
    private final YouTubePlayer youTubePlayer;
    private final IViTalkWorker iViTalkWorker;
    private final MediaOrchestrator orchestrator;
    private View panel;
    private ObjectAnimator anim;
    private Button playPauseButton;

    private final YouTubePlayerTracker playerTracker;

    public MyUiController(Context context, View customPlayerUi, YouTubePlayer youTubePlayer, MediaOrchestrator orchestrator, IViTalkWorker iViTalkWorker) {

        this.context = context;
        this.youTubePlayer = youTubePlayer;
        this.orchestrator = orchestrator;
        this.iViTalkWorker = iViTalkWorker;

        playerTracker = new YouTubePlayerTracker();
        youTubePlayer.addListener(playerTracker);

        initViews(customPlayerUi);

        if (LOG) {
            Log.d(TAG, ". controller instance created");
        }
    }

    private void initViews(View playerUi) {
        panel = playerUi.findViewById(R.id.panel);
        playPauseButton = playerUi.findViewById(R.id.play_pause_button);

        playPauseButton.setOnClickListener(view -> {
            if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.RECORD)  && !orchestrator.recorderReady()) {
                orchestrator.initAudioRecorder();
            }

            iViTalkWorker.onYouTubePlayHit();

            if (checkIfPlaying()) {
                animateButton(false);
                iViTalkWorker.onRecordButtonEnabledFlag(false);
                youTubePlayer.pause();
                if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.RECORD)) {
                    orchestrator.getAudioRecorder().pause();
                }
                if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.PREVIEW)) {
                    orchestrator.getAudioPlayer().pause();
                }
            } else {
                animateButton(true);
                iViTalkWorker.onRecordButtonEnabledFlag(true);
                youTubePlayer.play();
                if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.RECORD)) {
                    youTubePlayer.mute();
                    orchestrator.getAudioRecorder().setMicMuted(true);
                    orchestrator.getAudioRecorder().resumeOrStart();
                }
                if (iViTalkWorker.getMode().equals(IViTalkWorker.Mode.PREVIEW)) {
                    youTubePlayer.setVolume(1);
                    orchestrator.getAudioPlayer().start();
                }
            }
        });
    }

    @Override
    public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
        if (state == PlayerConstants.PlayerState.PLAYING || state == PlayerConstants.PlayerState.PAUSED || state == PlayerConstants.PlayerState.VIDEO_CUED) {
            panel.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        } else if (state == PlayerConstants.PlayerState.BUFFERING) {
            panel.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
        if (LOG) {
            Log.d(TAG, ".onStateChange STATE --> " + state);
        }
    }

    public void animateButton(boolean flag) {
        if (flag) {
            if (anim == null) {
                anim = ObjectAnimator.ofFloat(playPauseButton, View.ALPHA, 0.25f, 1.0f);
                anim.setDuration(1500);
                anim.setRepeatCount(Animation.INFINITE);
                anim.start();
            } else {
                anim.resume();
            }
        } else {
            if (anim != null) {
                anim.pause();
                anim.setCurrentFraction(1.0f);
            }
        }
    }

    public void endAnimation() {
        if (anim != null) {
            anim.end();
        }
    }

    public void resumeAnimation() {
        if (checkIfPlaying()) {
            animateButton(true);
        }
    }

    public boolean checkIfPlaying() {
        return playerTracker.getState() == PlayerConstants.PlayerState.PLAYING;
    }

}
