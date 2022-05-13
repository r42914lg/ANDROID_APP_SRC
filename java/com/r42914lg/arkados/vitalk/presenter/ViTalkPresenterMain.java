package com.r42914lg.arkados.vitalk.presenter;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;
import static com.r42914lg.arkados.vitalk.ViTalkConstants.URL_RESULT;
import static com.r42914lg.arkados.vitalk.model.ViTalkVM.ASK_RATINGS_ACTION_CODE;
import static com.r42914lg.arkados.vitalk.model.ViTalkVM.GOOGLE_SIGNIN_ACTION_CODE;
import static com.r42914lg.arkados.vitalk.model.ViTalkVM.PREVIEW_ACTION_CODE;
import static com.r42914lg.arkados.vitalk.model.ViTalkVM.SHARE_ACTION_CODE;
import static com.r42914lg.arkados.vitalk.model.ViTalkVM.UPLOAD_LOCAL_VIDEO_CODE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.model.LocalVideo;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.ui.ICoreFrame;

import java.text.MessageFormat;

import javax.inject.Inject;

public class ViTalkPresenterMain {
    public static final String TAG = "LG> ViTalkPresenterMain";

    private final ViTalkVM viTalkVM;
    private final AppCompatActivity appCompatActivity;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Long> mAccountPicker;

    @Inject
    public ViTalkPresenterMain(AppCompatActivity appCompatActivity, ViTalkVM viTalkVM) {
        this.viTalkVM = viTalkVM;
        this.appCompatActivity = appCompatActivity;

        if (LOG) {
            Log.d(TAG, ".ViTalkPresenterMain instance created");
        }
    }

    public void initMainActivity(ICoreFrame iCoreFrame) {

        viTalkVM.getFavoritesLiveData().observe(appCompatActivity, event -> iCoreFrame.renderMenuItems());
        viTalkVM.getToastLiveData().observe(appCompatActivity, s -> showToast(s, Toast.LENGTH_LONG));
        viTalkVM.getTerminateDialogEventMutableLiveData().observe(appCompatActivity, terminateDialogEvent -> showTerminateDialog(terminateDialogEvent.getTitle(), terminateDialogEvent.getText()));
        viTalkVM.getGoogleSignInLiveData().observe(appCompatActivity, iCoreFrame::updateUI);
        viTalkVM.getLiveToolBarTitle().observe(appCompatActivity, s -> appCompatActivity.getSupportActionBar().setTitle(s));
        viTalkVM.getShowFabLiveData().observe(appCompatActivity, iCoreFrame::showFab);
        viTalkVM.getShowTabOneMenuItems().observe(appCompatActivity, iCoreFrame::showTabOneMenuItems);

        viTalkVM.getProgressBarFlagLiveData().observe(appCompatActivity, aBoolean -> {
            if (aBoolean) {
                iCoreFrame.startProgressOverlay();
            } else {
                iCoreFrame.stopProgressOverlay();
            }
        });

        viTalkVM.getUiActionMutableLiveData().observe(appCompatActivity, viTalkUIAction -> {
            switch (viTalkUIAction) {
                case UPLOAD_LOCAL_VIDEO_CODE:
                    startVideoUpload(viTalkVM.getLocalVideo());
                    break;
                case SHARE_ACTION_CODE:
                    processShareRequest(viTalkVM.getYoutubeVideoIdToShareOrPreview());
                    break;
                case PREVIEW_ACTION_CODE:
                    processPreviewRequest(viTalkVM.getYoutubeVideoIdToShareOrPreview());
                    break;
                case ASK_RATINGS_ACTION_CODE:
                    ((ICoreFrame) appCompatActivity).askRatings();
                    break;
                case GOOGLE_SIGNIN_ACTION_CODE:
                    doGoogleSignIn();
                    break;
                default:
                    throw new IllegalStateException("Wrong UI Action received in observer");
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        mGoogleSignInClient = GoogleSignIn.getClient(appCompatActivity, gso);
        mAccountPicker = appCompatActivity.registerForActivityResult(
                new ActivityResultContract<Long, Intent>() {
                    @NonNull
                    @Override
                    public Intent createIntent(@NonNull Context context, Long input) {
                        return mGoogleSignInClient.getSignInIntent();
                    }

                    @Override
                    public Intent parseResult(int resultCode, @Nullable Intent intent) {
                        return intent;
                    }
                },
                new ActivityResultCallback<Intent>() {
                    @Override
                    public void onActivityResult(Intent intent) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
                        GoogleSignInAccount account = null;
                        try {
                            account = task.getResult(ApiException.class);
                            // Signed in successfully, show authenticated UI.
                        } catch (ApiException e) {
                            // The ApiException status code indicates the detailed failure reason.
                            // Please refer to the GoogleSignInStatusCodes class reference for more information.
                            if (LOG) {
                                Log.d(TAG, ".initMainActivity.GoogleSignIn signInResult:failed code = " + e.getStatusCode());
                                e.printStackTrace();
                            }
                        }
                        onSignIn(account);
                    }
                }
        );

        if (LOG) {
            Log.d(TAG, ".initMainActivity");
        }
    }

    public void checkGoogleSignInUser() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(appCompatActivity);
        onSignIn(account);
    }

    public void doGoogleSignIn() {
        if (LOG) {
            Log.d(TAG, ".doGoogleSignIn --> launching account picker");
        }
        mAccountPicker.launch(999L);
    }

    public boolean noGoogleSignIn() {
        return viTalkVM.getGoogleSignInLiveData().getValue() ==  null;
    }

    public void onSignIn(GoogleSignInAccount credential) {
        if (credential != null)  {
            viTalkVM.setGoogleAccount(credential);
        }
    }

    public void showToast(String text, int duration) {
        Toast.makeText(appCompatActivity, text, duration).show();
    }

    public void processPreviewRequest(String youTubeId) {
        if (LOG) {
            Log.d(TAG, ".processPreviewRequest");
        }

        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(MessageFormat.format(URL_RESULT, youTubeId, viTalkVM.getGoogleAccId())));
        appCompatActivity.startActivity(i);
    }

    public void processShareRequest(String youTubeId) {
        if (LOG) {
            Log.d(TAG, ".processShareRequest");
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, MessageFormat.format(URL_RESULT, youTubeId, viTalkVM.getGoogleAccId()));
        sendIntent.setType("text/plain");
        appCompatActivity.startActivity(sendIntent);
    }

    public void startVideoUpload(LocalVideo localVideoSelected) {
        startVideoUpload(localVideoSelected.uri);
    }

    public void startVideoUpload(Uri videoUri) {
        if (LOG) {
            Log.d(TAG, ".startVideoUpload --> " + videoUri.getEncodedPath());
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("video/3gpp");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Title_text");
        intent.putExtra(Intent.EXTRA_STREAM,videoUri);
        appCompatActivity.startActivity(Intent.createChooser(intent,appCompatActivity.getString(R.string.chooser_text)));
    }

    private void showTerminateDialog(String title, String text) {
        AlertDialog dialog = new AlertDialog.Builder(appCompatActivity).create();
        dialog.setTitle(title);
        dialog.setMessage(text);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                appCompatActivity.finish();
                dialog.cancel();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                appCompatActivity.finish();
                dialog.cancel();
            }
        });
        dialog.show();
    }
}

