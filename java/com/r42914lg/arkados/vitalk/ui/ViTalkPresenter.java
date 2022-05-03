package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;
import static com.r42914lg.arkados.vitalk.ViTalkConstants.URL_RESULT;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.model.LocalVideo;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.utils.NetworkTracker;
import com.r42914lg.arkados.vitalk.utils.PermissionsHelper;

import java.text.MessageFormat;

public class ViTalkPresenter {
    public static final String TAG = "LG> ViTalkPresenter";
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity

    private final ViTalkVM viTalkVM;
    private final AppCompatActivity appCompatActivity;
    private ICoreFrame iCoreFrame;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Long> mAccountPicker;

    public ViTalkPresenter(AppCompatActivity appCompatActivity, ViTalkVM viTalkVM) {
        new PermissionsHelper(appCompatActivity, viTalkVM).checkPermissions();
        new NetworkTracker(appCompatActivity, viTalkVM);

        this.viTalkVM = viTalkVM;
        this.appCompatActivity = appCompatActivity;

        if (LOG) {
            Log.d(TAG, ".ViTalkPresenter instance created");
        }
    }

    public ViTalkVM getViTalkVM() {
        return viTalkVM;
    }

    public void initMainActivity(ICoreFrame iCoreFrame) {
        this.iCoreFrame = iCoreFrame;

        viTalkVM.getProgressBarFlagLiveData().observe(appCompatActivity, aBoolean -> {
            if (aBoolean) {
                iCoreFrame.startProgressOverlay();
            } else {
                iCoreFrame.stopProgressOverlay();
            }
        });

        viTalkVM.getFavoritesLiveData().observe(appCompatActivity, event -> iCoreFrame.renderMenuItems());

        viTalkVM.getToastLiveData().observe(appCompatActivity, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                showToast(s, Toast.LENGTH_LONG);
            }
        });

        viTalkVM.getTerminateDialogEventMutableLiveData().observe(appCompatActivity, terminateDialogEvent -> showTerminateDialog(terminateDialogEvent.getTitle(), terminateDialogEvent.getText()));
        viTalkVM.getGoogleSignInLiveData().observe(appCompatActivity, account -> {
            iCoreFrame.updateUI(account);
        });
        viTalkVM.getLiveToolBarTitle().observe(appCompatActivity, s -> appCompatActivity.getSupportActionBar().setTitle(s));

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

    public void initWorkItemFragment(IViTalkWorkItems iViTalkWorkItems) {
        iCoreFrame.showFab(true);
        iCoreFrame.showTabOneMenuItems(true);

        viTalkVM.getProgressBarFlagLiveData().setValue(false);

        viTalkVM.getWorkItemsLoadedFlagLiveData().observe(((Fragment) iViTalkWorkItems).getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                iViTalkWorkItems.onAddRowsToAdapter(viTalkVM.getWorkItemVideoList());
            }
        });

        viTalkVM.getFavoritesLiveData().observe(((Fragment) iViTalkWorkItems).getViewLifecycleOwner(), event -> iViTalkWorkItems.onFavoritesChanged());
        viTalkVM.getInvalidateItemAtPositionLiveData().observe(((Fragment) iViTalkWorkItems).getViewLifecycleOwner(), iViTalkWorkItems::notifyAdapterIconLoaded);

        if (LOG) {
            Log.d(TAG, ".initWorkItemFragment");
        }
    }

    public void initWorkerFragment(IViTalkWorker iWorkerFragment) {
        iCoreFrame.showFab(false);
        iCoreFrame.showTabOneMenuItems(false);

        viTalkVM.getRecordSessionEndedFlagLiveData().observe(((Fragment) iWorkerFragment).getViewLifecycleOwner(), aBoolean -> iWorkerFragment.onRecordSessionEndedFlag(aBoolean));

        viTalkVM.getFirebaseUploadFinishedLiveData().observe(((Fragment) iWorkerFragment).getViewLifecycleOwner(), aBoolean -> {
            iWorkerFragment.onFirebaseUploadFinishedFlag(aBoolean);
            iCoreFrame.askRatings();
        });

        viTalkVM.getDialogEventMutableLiveData().observe(((Fragment) iWorkerFragment).getViewLifecycleOwner(), dialogEvent -> showRetryUploadDialog(dialogEvent.getTitle(), dialogEvent.getText(), iWorkerFragment));

        if (LOG) {
            Log.d(TAG, ".initWorkerFragment");
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

    public void initGalleryChooserFragment() {
        iCoreFrame.showFab(false);
        iCoreFrame.showTabOneMenuItems(false);

        if (LOG) {
            Log.d(TAG, ".initGalleryChooserFragment");
        }
    }

    public void startVideoUpload(LocalVideo localVideoSelected) {
        startVideoUpload(localVideoSelected.uri);
    }

    public void startVideoUpload(Uri videoUri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("video/3gpp");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Title_text");
        intent.putExtra(Intent.EXTRA_STREAM,videoUri);
        appCompatActivity.startActivity(Intent.createChooser(intent,appCompatActivity.getString(R.string.chooser_text)));
    }

    public void setVideoIdForWork(String youTubeId) {
        viTalkVM.onVideIdSelected(youTubeId);
        viTalkVM.getRecordSessionEndedFlagLiveData().setValue(false);
    }

    public void showToast(String text, int duration) {
        Toast.makeText(appCompatActivity, text, duration).show();
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
    public void processPreviewRequest(String youTubeId) {
        if (LOG) {
            Log.d(TAG, ".processPreviewRequest");
        }

        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(MessageFormat.format(URL_RESULT, youTubeId, viTalkVM.getGoogleAccId())));
        appCompatActivity.startActivity(i);
    }

    private void showRetryUploadDialog(String title, String text, IViTalkWorker iViTalkWorker) {
        AlertDialog dialog = new AlertDialog.Builder(appCompatActivity).create();
        dialog.setTitle(title);
        dialog.setMessage(text);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                viTalkVM.onRecordSessionEnded(viTalkVM.getDataSource());
                dialog.cancel();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                iViTalkWorker.navigateToWorkItems();
                dialog.cancel();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                iViTalkWorker.navigateToWorkItems();
                dialog.cancel();
            }
        });
        dialog.show();
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
