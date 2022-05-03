package com.r42914lg.arkados.smarthit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.MessageFormat;

import com.facebook.internal.Logger;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.r42914lg.arkados.common.ImageButtonWithPic;
import org.jetbrains.annotations.NotNull;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

public class MainActivity extends AppCompatActivity {
    public static final boolean STRICT_MODE = false;
    public static final boolean ADS_ENABLED = true;

    public static final String TIME_KEY = "timeTempoXType";
    public static final String TEMPO_CHOSEN_KEY = "tempoChosen";
    public static final String TYPE_CHOSEN_KEY = "typeChosen";

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String GAME_SEQUENCE_CODE = "gameSequenceCode";
    private static final int REQUEST_CODE = 1;

    SharedPreferences prefs;
    private int gameTempoChosen;
    private int gameTypeChosen;
    private int[][] timeTypeXTempo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStrictMode();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main_landscape);
        } else {
            setContentView(R.layout.activity_main);
        }

        timeTypeXTempo = new int[GameManager.TYPE_SEQUENCE][GameManager.TEMPO_FAST];

        if (savedInstanceState == null) {
            gameTempoChosen = 1;
            gameTypeChosen = 1;
        } else {
            gameTempoChosen = savedInstanceState.getInt(TEMPO_CHOSEN_KEY);
            gameTypeChosen = savedInstanceState.getInt(TYPE_CHOSEN_KEY);
        }

        highlightField(gameTypeChosen, gameTempoChosen, true);

        ImageButtonWithPic startButton = findViewById(R.id.button_start);
        startButton.setOnTouchListener(startButton);

        loadHighscore();

        AppEventsLogger logger = AppEventsLogger.newLogger(this);
        logger.logEvent("MainActivity.onCreate");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TEMPO_CHOSEN_KEY, gameTempoChosen);
        outState.putInt(TYPE_CHOSEN_KEY, gameTypeChosen);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = prefs.edit();
        for (int i=0; i<GameManager.TYPE_SEQUENCE; i++) {
            for (int j=0; j<GameManager.TEMPO_FAST; j++) {
                editor.putInt(TIME_KEY+i+""+j, timeTypeXTempo[i][j]);
            }
        }
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                int[] allScores = data.getIntArrayExtra(SHBoardActivity.LOCAL_SCORE_KEY);
                if (allScores != null) {
                    updateHighScore(allScores);
                    askRatings();
                }
            }
        }
    }

    public void onClickStart(View view) {
        Intent intent = new Intent(MainActivity.this, SHBoardActivity.class);
        intent.putExtra(GAME_SEQUENCE_CODE, GameManager.getSequenceCodeByUserInput(gameTypeChosen, gameTempoChosen));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void onClickScoreText(View view) {
        highlightField(gameTypeChosen, gameTempoChosen, false);
        int twoDigitTag = Integer.parseInt((String) view.getTag());
        gameTypeChosen = twoDigitTag / 10;
        gameTempoChosen = twoDigitTag % 10;
        highlightField(gameTypeChosen, gameTempoChosen, true);
    }

    public void onClickClearScore(View v) {
        timeTypeXTempo[gameTypeChosen - 1][gameTempoChosen - 1] = 0;
        updateHighScoreTV();
    }

    public void onClickDisplayRules(View v) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(getText(R.string.GAME_RULES));
        dialog.setMessage(getText(R.string.RULES_TEXT));
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.LTGRAY);
    }

    private void highlightField(int type, int tempo, boolean needHighlight) {
        String tag = String.valueOf(GameManager.getSequenceCodeByUserInput(type, tempo));
        View view = findViewByTag(findViewById(R.id.root_linear), tag);
        if (view != null) {
            if (needHighlight) {
                view.setBackgroundColor(getResources().getColor(R.color.shb_1));
            } else {
                view.setBackgroundColor(getResources().getColor(R.color.purple_200));
            }
        } else {
            throw new IllegalStateException("Could not find view with TAG " + tag);
        }
    }

    private View findViewByTag(View view, String tag) {
        if (view.getTag() != null && ((String) view.getTag()).equalsIgnoreCase(tag)) {
            return view;
        } else {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View viewToReturn = findViewByTag(viewGroup.getChildAt(i), tag);
                    if (viewToReturn != null) {
                        return viewToReturn;
                    }
                }
            }
            return null;
        }
    }

    private void updateHighScore(int[] allScores) {
        for (int i = 0; i < allScores.length; i++) {
            if ((allScores[i] != 0 && allScores[i] < timeTypeXTempo[i][gameTempoChosen - 1]) || (timeTypeXTempo[i][gameTempoChosen - 1] == 0)) {
                timeTypeXTempo[i][gameTempoChosen - 1] = allScores[i];
            }
        }
        updateHighScoreTV();
    }

    private void loadHighscore() {
        prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        for (int i=0; i<GameManager.TYPE_SEQUENCE; i++) {
            for (int j=0; j<GameManager.TEMPO_FAST; j++) {
                timeTypeXTempo[i][j] = prefs.getInt(TIME_KEY+i+""+j, 0);
            }
        }
        updateHighScoreTV();
    }

    private void updateHighScoreTV() {
        for (int i=0; i<GameManager.TYPE_SEQUENCE; i++) {
            for (int j=0; j<GameManager.TEMPO_FAST; j++) {
                TextView view = (TextView) findViewByTag(findViewById(R.id.root_linear), ""+(i+1)+""+(j+1));
                if (view != null) {
                    view.setText(formatHighScore(timeTypeXTempo[i][j]));
                }
            }
        }
    }

    private String formatHighScore(int valueInSeconds) {
        if (valueInSeconds == 0) {
            return getResources().getString(R.string.empty_time);
        } else {
            int minutes = valueInSeconds / 60;
            int seconds = valueInSeconds % 60;
            if (seconds < 10) {
                return MessageFormat.format("{0}:0{1}",minutes, seconds);
            } else {
                return MessageFormat.format("{0}:{1}",minutes, seconds);
            }
        }
    }

    private void askRatings() {
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<ReviewInfo> task) {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
                    flow.addOnCompleteListener(task2 -> {
                    });
                }
            }
        });
    }

    private void initStrictMode() {
        if (STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDialog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());
        }
    }
}