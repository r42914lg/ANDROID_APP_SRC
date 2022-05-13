package com.r42914lg.arkados.vitalk.presenter;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.ui.IViTalkWorker;

import javax.inject.Inject;

public class ViTalkPresenterSecondFragment {
    public static final String TAG = "LG> ViTalkPresenterSecondFragment";

    private final ViTalkVM viTalkVM;

    @Inject
    public ViTalkPresenterSecondFragment(ViTalkVM viTalkVM) {
        this.viTalkVM = viTalkVM;

        if (LOG) {
            Log.d(TAG, ".ViTalkPresenterSecondFragment instance created");
        }
    }

    public void initWorkerFragment(IViTalkWorker iWorkerFragment, Context context) {
        viTalkVM.getShowFabLiveData().setValue(false);
        viTalkVM.getShowTabOneMenuItems().setValue(false);

        viTalkVM.getRecordSessionEndedFlagLiveData().observe(((Fragment) iWorkerFragment).getViewLifecycleOwner(), aBoolean -> iWorkerFragment.onRecordSessionEndedFlag(aBoolean));

        viTalkVM.getFirebaseUploadFinishedLiveData().observe(((Fragment) iWorkerFragment).getViewLifecycleOwner(), aBoolean -> {
            iWorkerFragment.onFirebaseUploadFinishedFlag(aBoolean);
            viTalkVM.getUiActionMutableLiveData().setValue(ViTalkVM.ASK_RATINGS_ACTION_CODE);
        });

        viTalkVM.getDialogEventMutableLiveData().observe(((Fragment) iWorkerFragment).getViewLifecycleOwner(), dialogEvent -> showRetryUploadDialog(dialogEvent.getTitle(), dialogEvent.getText(), iWorkerFragment, context));

        if (LOG) {
            Log.d(TAG, ".initWorkerFragment");
        }
    }

    private void showRetryUploadDialog(String title, String text, IViTalkWorker iViTalkWorker, Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context).create();
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
}
