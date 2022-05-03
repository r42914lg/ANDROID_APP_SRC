package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.ViTalkConstants;
import com.r42914lg.arkados.vitalk.databinding.ActivityMainBinding;
import com.r42914lg.arkados.vitalk.model.FavoritesEvent;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements ICoreFrame {
    public static final String TAG = "LG> MainActivity";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private View progressOverlay;
    private MenuItem checkableMenuItem;
    private MenuItem signinMenuItem;
    private boolean showFavoritesFlag;
    private boolean favoritesWasNull;

    private ViTalkVM viTalkVM;
    private ViTalkPresenter viTalkPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viTalkVM = new ViewModelProvider(this).get(ViTalkVM.class);
        viTalkPresenter =  new ViTalkPresenter(this, viTalkVM);
        viTalkPresenter.initMainActivity(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        progressOverlay = findViewById(R.id.progress_overlay);

        binding.fab.setOnClickListener(view -> navController.navigate(R.id.action_FirstFragment_to_ThirdFragment));

        initStrictMode();

        if (LOG) {
            Log.d(TAG, ".onCreate");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent(getIntent());

        if (LOG) {
            Log.d(TAG, ".onResume");
        }
    }

    private void handleIntent(Intent newIntent) {
        String type = newIntent.getType();

        if (type != null) {
            if (type.equals("YOUTUBE_LINK")) {
                Bundle extras = newIntent.getExtras();
                if (extras != null) {
                    String youTubeId = parseYouTubeId(extras.getString(Intent.EXTRA_TEXT));
                    viTalkVM.addYouTubeIdToWorkItems(youTubeId);
                }
            }
            if (type.equals("VIDEO_URI")) {
                Uri videoUri = (Uri) newIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (videoUri != null) {
                    viTalkPresenter.startVideoUpload(videoUri);
                }
            }
            newIntent.setType("CONSUMED");
        }

        viTalkPresenter.checkGoogleSignInUser();

        if (LOG) {
            Log.d(TAG, ".handleIntent");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);

        if (LOG) {
            Log.d(TAG, ".onNewIntent");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        checkableMenuItem = menu.findItem(R.id.favorite);
        signinMenuItem = menu.findItem(R.id.sign_in);

        if (favoritesWasNull) {
            showFavoriteIcon(showFavoritesFlag);
        }

        if (LOG) {
            Log.d(TAG, ".onCreateOptionsMenu");
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        renderMenuItems();
        if (LOG) {
            Log.d(TAG, ".onPrepareOptionsMenu");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (LOG) {
            Log.d(TAG, ".onOptionsItemSelected" + item);
        }

        if (item.getItemId() == R.id.favorite) {
            item.setChecked(!item.isChecked());
            item.setIcon(item.isChecked() ? R.drawable.ic_baseline_favorite_24 : R.drawable.ic_baseline_favorite_border_24);
            viTalkVM.setFavoritesChecked(item.isChecked());
        }

        if (item.getItemId() == R.id.sign_in) {
            viTalkPresenter.doGoogleSignIn();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public ViTalkPresenter getViTalkPresenter() {
        return viTalkPresenter;
    }

    @Override
    public void startProgressOverlay() {
        animateView(progressOverlay, View.VISIBLE, 0.6f, 200);
    }

    @Override
    public void stopProgressOverlay() {
        animateView(progressOverlay, View.GONE, 0, 200);
    }

    @Override
    public void showFavoriteIcon(boolean showIfTrue) {
        showFavoritesFlag = showIfTrue;
        if (checkableMenuItem ==  null) {
            favoritesWasNull = true;
            return;
        }

        if (!showIfTrue) {
            checkableMenuItem.setVisible(false);
        } else {
            checkableMenuItem.setVisible(true);
            renderMenuItems();
        }
    }

    @Override
    public void renderMenuItems() {
        if (checkableMenuItem == null || signinMenuItem == null) {
            return;
        }

        FavoritesEvent event = viTalkVM.getFavoritesLiveData().getValue();

        checkableMenuItem.setEnabled(event.needEnableFavorites());
        checkableMenuItem.setChecked(event.checkFavoritesChecked());
        checkableMenuItem.setIcon(checkableMenuItem.isChecked()? R.drawable.ic_baseline_favorite_24 : R.drawable.ic_baseline_favorite_border_24);

        signinMenuItem.setVisible(viTalkPresenter.noGoogleSignIn());
    }

    @Override
    public void showFab(boolean flag) {
        if (binding == null) {
            return;
        }
        binding.fab.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void showTabOneMenuItems(boolean flag) {
        if (checkableMenuItem ==  null || signinMenuItem ==  null) {
            return;
        }
        checkableMenuItem.setVisible(flag);
        signinMenuItem.setVisible(flag && viTalkPresenter.noGoogleSignIn());
        if (LOG) {
            Log.d(TAG, ".showTabOneMenuItems FLAG == " + flag);
        }
    }

    @Override
    public void updateUI(GoogleSignInAccount account) {
        if (signinMenuItem == null) {
            return;
        }
        signinMenuItem.setVisible(account == null);
        if (LOG) {
            Log.d(TAG, ".updateUI G_Account == " + account);
        }
    }

    private void animateView(final View view, final int toVisibility, float toAlpha, int duration) {
        boolean show = toVisibility == View.VISIBLE;
        if (show) {
            view.setAlpha(0);
        }
        view.setVisibility(View.VISIBLE);
        view.animate()
                .setDuration(duration)
                .alpha(show ? toAlpha : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(toVisibility);
                    }
                });
    }

    private String parseYouTubeId(String url) {
        String afterSlash = url.substring(url.lastIndexOf("/") + 1);
        int index = afterSlash.indexOf("?");

        return index == -1 ? afterSlash : afterSlash.substring(0, index);
    }

    @Override
    public void askRatings() {
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                });
            }
        });
    }

    private void initStrictMode() {
        if (ViTalkConstants.STRICT_MODE) {
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
}