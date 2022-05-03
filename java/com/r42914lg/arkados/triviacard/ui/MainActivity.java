package com.r42914lg.arkados.triviacard.ui;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.r42914lg.arkados.triviacard.TriviaCardConstants;
import com.r42914lg.arkados.triviacard.databinding.ActivityMainBinding;
import com.r42914lg.arkados.triviacard.model.JSON_Quiz;
import com.r42914lg.arkados.triviacard.model.UI_VM_EVENT;
import com.r42914lg.arkados.triviacard.model.ITriviaCardPresenter;
import com.r42914lg.arkados.triviacard.model.TriviaCardPresenter;
import com.r42914lg.arkados.triviacard.R;
import com.r42914lg.arkados.triviacard.model.TriviaCardVM;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import org.jetbrains.annotations.NotNull;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.ADS_ENABLED;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class MainActivity extends AppCompatActivity implements ICoreFrameTriviaCardUI {
    public static final String TAG = "LG> MainActivity";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ITriviaCardPresenter presenter;
    private DisplayMetrics displayMetrics;
    private InterstitialAd mInterstitialAd;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private MenuItem checkable;
    private MenuItem removeads;

    private boolean showFavoritesFlag;
    private boolean favoritesWasNull;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initStrictMode();
        initAdBanner();
        initAdInterstitial();
        initAdRewardedInterstitial();

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        TriviaCardVM triviaCardVM = new ViewModelProvider(this).get(TriviaCardVM.class);
        presenter = new TriviaCardPresenter(triviaCardVM, this);

        int imageWidth = getDisplayWidth();
        triviaCardVM.setBitmapDimensions(imageWidth, imageWidth, imageWidth/ 2,imageWidth / 6);

        presenter.initCoreFrame(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setExtended(false);
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.processFabClick();
            }
        });

        triviaCardVM.getLiveToolBarTitle().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                getSupportActionBar().setTitle(s);
            }
        });

        presenter.getBillingAssistant().fetchPurchases();

        if (LOG) {
            Log.d(TAG, ".onCreate");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.getBillingAssistant().fetchPurchases();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        checkable = menu.findItem(R.id.favorite);
        removeads = menu.findItem(R.id.removeads);
        removeads.setVisible(false);

        presenter.getViewModel().getLiveUIEvent().observe(this, new Observer<UI_VM_EVENT>() {
            @Override
            public void onChanged(UI_VM_EVENT ui_vm_event) {
                if (ui_vm_event.getEventType() == UI_VM_EVENT.TYPE_TOAST) {
                    showToast(ui_vm_event.getToastText(), Toast.LENGTH_LONG);
                } else {
                    renderMenuItems();
                }
            }
        });

        if (favoritesWasNull) {
            showFavoriteIcon(showFavoritesFlag);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        renderMenuItems();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.favorite) {
            item.setChecked(!item.isChecked());
            item.setIcon(item.isChecked() ? R.drawable.ic_baseline_favorite_24 : R.drawable.ic_baseline_favorite_border_24);
            presenter.getViewModel().setFavoritesChecked(item.isChecked());
        }
        if (item.getItemId() == R.id.removeads) {
            presenter.onAdsButtonClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView(this);
        presenter.shutDownBillingClient();

        if (LOG) {
            Log.d(TAG, ".onDestroy");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public int getDisplayWidth() {
        int valueToReturn;
        valueToReturn = getOrientation() == Configuration.ORIENTATION_PORTRAIT ? this.displayMetrics.widthPixels : this.displayMetrics.heightPixels;
        if (LOG) {
            Log.d(TAG, ".getDisplayWidth --> " + valueToReturn);
        }
        return valueToReturn;
    }

    @Override
    public int getDisplayHeight() {
        int valueToReturn;
        valueToReturn = getOrientation() == Configuration.ORIENTATION_PORTRAIT ? this.displayMetrics.heightPixels : this.displayMetrics.widthPixels;
        if (LOG) {
            Log.d(TAG, ".getDisplayHeight --> " + valueToReturn);
        }
        return valueToReturn;
    }

    @Override
    public int getOrientation() {
        return getResources().getConfiguration().orientation;
    }

    @Override
    public int getAppBarHeight() {
        TypedValue tv = new TypedValue();
        int actionBarHeight = 0;
        if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
        {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        if (LOG) {
            Log.d(TAG, ".getAppBarHeight --> " + actionBarHeight);
        }
        return actionBarHeight;
    }

    @Override
    public int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        if (LOG) {
            Log.d(TAG, ".getStatusBarHeight --> " + statusBarHeight);
        }
        return statusBarHeight;
    }

    @Override
    public void showProgressBar(int progress) {
        if (progress == -1) {
            binding.toolbar.getMenu().getItem(1).setVisible(true);
        } else {
            binding.toolbar.getMenu().getItem(0).setVisible(true);
            ((ProgressBar) binding.toolbar.getMenu().getItem(0).getActionView()).setProgress(progress);
        }
    }

    @Override
    public void stopProgressBar() {
        binding.toolbar.getMenu().getItem(0).setVisible(false);
        binding.toolbar.getMenu().getItem(1).setVisible(false);
    }

    @Override
    public void updateFabAction(int fabActionCode) {
        if (binding != null) {
            binding.fab.setIcon(getResources().getDrawable(
                    fabActionCode == TriviaCardConstants.FAB_ACTION_REFRESH ? android.R.drawable.ic_popup_sync : android.R.drawable.ic_media_next));
            switch (fabActionCode) {
                case TriviaCardConstants.FAB_ACTION_REFRESH:
                    binding.fab.setText("Update");
                    break;
                case TriviaCardConstants.FAB_ACTION_NEXT:
                    binding.fab.setText("Next");
                    break;
                case TriviaCardConstants.FAB_ACTION_SKIP:
                    binding.fab.setText("Skip");
                    break;
                case TriviaCardConstants.FAB_ACTION_FINISH:
                    binding.fab.setText("Finish");
                    break;
            }
        }
        if (LOG) {
            Log.d(TAG, ".updateFabAction fabActionCode --> " + fabActionCode + " binding is NULL --> " + (binding == null));
        }
    }

    @Override
    public void showToast(String text, int duration) {
        Toast.makeText(this, text, duration).show();
    }

    @Override
    public ITriviaCardPresenter getPresenterReference() {
        return presenter;
    }

    @Override
    public void requestFinishActivity() {
        finish();
    }

    private void initStrictMode() {
        if (TriviaCardConstants.STRICT_MODE) {
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
            if (LOG) {
                Log.d(TAG, ".initStrictMode: STRICT_MODE is ON");
            }
        }
    }

    private void initAdBanner() {
        if (!ADS_ENABLED)
            return;

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
    }

    private void initAdInterstitial() {
        if (!ADS_ENABLED)
            return;

        InterstitialAd.load(this, getResources().getString(R.string.admob_interstitial_id),
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                if (LOG) {
                    Log.d(TAG, ".onAdLoaded-INTERSTITIAL");
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                mInterstitialAd = null;
                if (LOG) {
                    Log.d(TAG, "onAdFailedToLoad-INTERSTITIAL");
                }
            }
        });
    }

    private void initAdRewardedInterstitial() {
        if (!ADS_ENABLED)
            return;

        RewardedInterstitialAd.load(this, getResources().getString(R.string.admob_rewarded_id),
                new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        if (LOG) {
                            Log.d(TAG, ".onAdLoaded-REWARDED");
                        }
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        if (LOG) {
                            Log.d(TAG, "onAdFailedToLoad-REWARDED");
                        }
                    }
                });
    }

    @Override
    public void showRewardedInterstitialAd(JSON_Quiz quiz) {
        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    if (LOG) {
                        Log.d(TAG, ".onAdDismissedFullScreenContent-REWARD");
                    }
                }
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                    if (LOG) {
                        Log.d(TAG, ".onAdFailedToShowFullScreenContent-REWARD");
                    }
                }
                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    if (LOG) {
                        Log.d(TAG, ".onAdShowedFullScreenContent-REWARD");
                    }
                    rewardedInterstitialAd = null;
                    initAdRewardedInterstitial();
                }
            });
            rewardedInterstitialAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    if (LOG) {
                        Log.d(TAG, "The user earned the reward.");
                    }
                    presenter.doSelectQuizAndNavigateToFirst(quiz);
                    presenter.getViewModel().storeRewardedShownForQuiz(quiz.getId());
                }
            });
        } else {
            initAdRewardedInterstitial();
        }
    }

    @Override
    public void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    if (LOG) {
                        Log.d(TAG, ".onAdDismissedFullScreenContent-INTERSTITIAL");
                    }
                }
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                    if (LOG) {
                        Log.d(TAG, ".onAdFailedToShowFullScreenContent-INTERSTITIAL");
                    }
                }
                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    if (LOG) {
                        Log.d(TAG, ".onAdShowedFullScreenContent-INTERSTITIAL");
                    }
                    mInterstitialAd = null;
                    initAdInterstitial();
                }
            });
            mInterstitialAd.show(this);
        } else {
            initAdInterstitial();
        }
    }

    @Override
    public void askRatings() {
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

    @Override
    public void showFavoriteIcon(boolean showIfTrue) {
        showFavoritesFlag = showIfTrue;
        if (checkable ==  null) {
            favoritesWasNull = true;
            return;
        }

        if (!showIfTrue) {
            checkable.setVisible(false);
        } else {
            checkable.setVisible(true);
            renderMenuItems();
        }
    }

    private void renderMenuItems() {
        UI_VM_EVENT event = presenter.getViewModel().getLiveUIEvent().getValue();

        removeads.setEnabled(!event.checkDisableAds());
        removeads.setVisible(!event.checkHideAds());

        checkable.setEnabled(event.needEnableFavorites());
        checkable.setChecked(event.checkFavoritesChecked());
        checkable.setIcon(checkable.isChecked()? R.drawable.ic_baseline_favorite_24 : R.drawable.ic_baseline_favorite_border_24);
    }
}