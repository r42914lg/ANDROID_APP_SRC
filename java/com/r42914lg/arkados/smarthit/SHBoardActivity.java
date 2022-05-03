package com.r42914lg.arkados.smarthit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.text.MessageFormat;

import static com.r42914lg.arkados.smarthit.MainActivity.ADS_ENABLED;
import static com.r42914lg.arkados.smarthit.MainActivity.GAME_SEQUENCE_CODE;
import static com.r42914lg.arkados.smarthit.MainActivity.STRICT_MODE;

public class SHBoardActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final String LOCAL_SCORE_KEY = "localScore";
    public static final String CHRONOMETER_TIME_KEY = "chronometerTime";

    private long backPressedTime;
    private long chronometerTime;
    private long timeChronometerPaused;

    private Chronometer chronometer;
    private GameManager gameManager;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    private TextView textViewLocalScore;
    private TextView testViewGameType;
    private ImageView imageViewCurrentFruit;
    private ImageView imageViewNextFruit;
    private ImageView imageViewSecondNextFruit;
    private ObjectAnimator anim;

    private GestureDetector gestureDetector;
    private View currentViewForGestureDetector;

    private int buttonPixelSize;
    private boolean bannerLoadedFlag;
    private boolean bannerSetFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStrictMode();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_s_h_board_landscape);
        } else {
            setContentView(R.layout.activity_s_h_board);
        }

        initAdBanner();
        adjustToDisplaySize();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                ((SHImageButton) currentViewForGestureDetector).processDoubleClick();
                return super.onDoubleTap(e);
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                ((SHImageButton) currentViewForGestureDetector).processClick(chronometerTime);
                return super.onSingleTapConfirmed(e);
            }
        });

        if (savedInstanceState == null) {
            gameManager = new GameManager(this, getIntent().getIntExtra(MainActivity.GAME_SEQUENCE_CODE, 0));
        } else {
            gameManager = new GameManager(this, savedInstanceState.getInt(GAME_SEQUENCE_CODE), savedInstanceState);
        }

        if (gameManager.hasNextStep()) {
            gameManager.iterateToNextStep();
            SHGameLogic shGameLogic = gameManager.getCurrentStepRef();
            if (savedInstanceState == null) {
                chronometer.setBase(SystemClock.elapsedRealtime());
                shGameLogic.initializeGameState();
            } else {
                chronometer.setBase(SystemClock.elapsedRealtime() - savedInstanceState.getLong(CHRONOMETER_TIME_KEY));
                shGameLogic.restoreGameState(savedInstanceState);
            }
            updateGameNameTextView();
            initChronometer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chronometer != null) {
            timeChronometerPaused = chronometer.getBase() - SystemClock.elapsedRealtime();
            chronometer.stop();
        }
        if (anim != null) {
            anim.end();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timeChronometerPaused != 0) {
            chronometer.setBase(SystemClock.elapsedRealtime() + timeChronometerPaused);
        }
        chronometer.start();
        manageBlinkEffect();
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finishSHBoardActivity();
            super.onBackPressed();
        } else {
            Toast.makeText(this, getText(R.string.backPressDialog), Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    private void finishSHBoardActivity() {
        Intent resultIntent = new Intent();
        SHGameLogic shGameLogic = gameManager.getCurrentStepRef();
        int gamePoints = shGameLogic.getGamePoints();
        if (gamePoints >= getResources().getInteger(R.integer.TARGET_SCORE)) {
            anim.end();
            chronometer.stop();
            gameManager.setCurrentGameTime((int) chronometerTime / 1000);

            AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle(getText(R.string.gameOverDialogTitle));

            if (gameManager.hasNextStep()) {
                dialog.setMessage(MessageFormat.format(String.valueOf(getText(R.string.gameOverDialogText_serie)), gameManager.getCurrentStepName(), gameManager.getCurrentGameTime()/60, gameManager.getCurrentGameTime()%60, gameManager.getNextStepName()));
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "YES",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showInterstitialAd();
                        gameManager.iterateToNextStep();
                        SHGameLogic shGameLogic = gameManager.getCurrentStepRef();
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        shGameLogic.initializeGameState();
                        updateGameNameTextView();
                        initChronometer();
                        chronometer.start();
                        manageBlinkEffect();
                        dialog.cancel();
                    }
                });
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "NO",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        resultIntent.putExtra(LOCAL_SCORE_KEY, gameManager.getAllTimesArray());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                        dialog.cancel();
                    }
                });
                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.LTGRAY);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.LTGRAY);
            } else {
                dialog.setMessage(MessageFormat.format(String.valueOf(getText(R.string.gameOverDialogText)), gameManager.getCurrentStepName(), gameManager.getCurrentGameTime()/60, gameManager.getCurrentGameTime()%60));
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        resultIntent.putExtra(LOCAL_SCORE_KEY, gameManager.getAllTimesArray());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                        dialog.cancel();
                    }
                });
                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.LTGRAY);
            }

        } else {
            setResult(RESULT_OK, resultIntent);
            anim.end();
            chronometer.stop();
            finish();
        }
    }

    public void updateScoreView(int score) {
        textViewLocalScore.setText(MessageFormat.format("{0}{1}", getText(R.string.textViewLocalScore), score));
    }

    public void updateFruitsView(int currentResID, int nextResID, int secondNextResID) {
        imageViewCurrentFruit.setImageResource(currentResID);
        imageViewNextFruit.setImageResource(nextResID);
        imageViewSecondNextFruit.setImageResource(secondNextResID);
    }

    private void updateGameNameTextView() {
        testViewGameType.setText(MessageFormat.format("{0} - {1}", gameManager.getCurrentStepName(), gameManager.getCurrentStepTempoName()));
    }

    private void initChronometer() {
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chronometerTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                SHGameLogic shGameLogic = gameManager.getCurrentStepRef();
                if (shGameLogic.reValidateButtonsAndCheckIfGameOver(chronometerTime)) {
                    finishSHBoardActivity();
                }
                if (chronometerTime > getResources().getInteger(R.integer.HELP_TO_SHOW_MILLIS)) {
                    hideHelpShowBannerAd();
                }
            }
        });
    }

    private void manageBlinkEffect() {
        anim = ObjectAnimator.ofFloat(imageViewCurrentFruit, View.ALPHA, 0.25f, 1.0f);
        anim.setDuration(1500);
        anim.setRepeatCount(Animation.INFINITE);
        anim.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        currentViewForGestureDetector = v;
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHRONOMETER_TIME_KEY, chronometerTime);
        outState.putInt(GAME_SEQUENCE_CODE, getIntent().getIntExtra(MainActivity.GAME_SEQUENCE_CODE, 0));
        SHGameLogic shGameLogic = gameManager.getCurrentStepRef();
        shGameLogic.saveGameState(outState);
        gameManager.saveGameState(outState);
    }

    public void sizePlayButton(ImageView imageButton, float scale) {
        imageButton.getLayoutParams().width = (int) (buttonPixelSize * scale);
        imageButton.getLayoutParams().height = (int) (buttonPixelSize * scale);
    }

    private void adjustToDisplaySize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int minHeight = displayHeight / (getResources().getInteger(R.integer.COLUMNS_COUNT) + 3);
            int minWidth = displayWidth / (getResources().getInteger(R.integer.ROWS_COUNT) + 2);
            buttonPixelSize = Math.min(minHeight, minWidth);
        } else {
            int minHeight = displayHeight / (getResources().getInteger(R.integer.ROWS_COUNT) + 4);
            int minWidth = displayWidth / (getResources().getInteger(R.integer.COLUMNS_COUNT) + 1);
            buttonPixelSize = Math.min(minHeight, minWidth);
        }

        textViewLocalScore = findViewById(R.id.local_score);
        testViewGameType = findViewById(R.id.game_type);
        chronometer = findViewById(R.id.chronometer);
        imageViewCurrentFruit = findViewById(R.id.current_fruit);
        imageViewNextFruit = findViewById(R.id.next_fruit);
        imageViewSecondNextFruit = findViewById(R.id.second_next_fruit);
        sizePlayButton(imageViewCurrentFruit, 0.9f);
        sizePlayButton(imageViewNextFruit, 0.9f);
        sizePlayButton(imageViewSecondNextFruit, 0.9f);
    }

    private void initAdBanner() {
        if (ADS_ENABLED) {
            MobileAds.initialize(this, initializationStatus -> {
            });
            mAdView = new AdView(this);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    bannerLoadedFlag = true;
                }
            });
            mAdView.setAdSize(AdSize.SMART_BANNER);
            mAdView.setAdUnitId(getResources().getString(R.string.admob_banner_id));
            mAdView.setPadding(5, 5, 5, 5);
            mAdView.loadAd(new AdRequest.Builder().build());
        }
    }

    protected void initAdInterstitial() {
        if (ADS_ENABLED) {
            InterstitialAd.load(this, getResources().getString(R.string.admob_interstitial_id), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error
                    mInterstitialAd = null;
                }
            });
        }
    }

    private void hideHelpShowBannerAd() {
        TextView helpTV = findViewById(R.id.helpTextView);
        if (helpTV != null && bannerLoadedFlag && !bannerSetFlag) {
            LinearLayout topLayout = findViewById(R.id.top_linear_layout);
            topLayout.addView(mAdView);
            helpTV.setVisibility(View.GONE);
            bannerSetFlag = true;
        }
    }

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                }
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                }
                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    mInterstitialAd = null;
                    initAdInterstitial();
                }
            });
            mInterstitialAd.show(this);
        }
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
                    .penaltyDeath()
                    .build());
        }
    }
}

