package com.r42914lg.arkados.triviacard.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.r42914lg.arkados.triviacard.R;

public class AdDialogFragment extends DialogFragment {

    private static final long COUNTER_TIME = 5;
    private static final String TAG = "AdDialogFragment";

    private CountDownTimer countDownTimer;
    private long timeRemaining;
    private AdDialogInteractionListener listener;

    public static AdDialogFragment newInstance() {
        AdDialogFragment fragment = new AdDialogFragment();
        return fragment;
    }

    public void setAdDialogInteractionListener(AdDialogInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.reward, null);
        builder.setView(view);


        builder.setTitle(R.string.dialog_reward_title);

        builder.setNegativeButton(R.string.dialog_reward_negative,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });
        Dialog dialog = builder.create();
        createTimer(COUNTER_TIME, view);
        return dialog;
    }

    private void createTimer(long time, View dialogView) {
        final TextView textView = dialogView.findViewById(R.id.timer);
        countDownTimer =
                new CountDownTimer(time * 1000, 50) {
                    @Override
                    public void onTick(long millisUnitFinished) {
                        timeRemaining = ((millisUnitFinished / 1000) + 1);
                        textView.setText(String.format(getString(R.string.dialog_reward_message), timeRemaining));
                    }

                    @Override
                    public void onFinish() {
                        getDialog().dismiss();

                        if (listener != null) {
                            Log.d(TAG, "onFinish: Calling onShowAd().");
                            listener.onShowAd();
                        }
                    }
                };
        countDownTimer.start();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (listener != null) {
            Log.d(TAG, "onCancel: Calling onCancelAd().");
            listener.onCancelAd();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cancelling the timer.");
        countDownTimer.cancel();
        countDownTimer = null;
    }

    public interface AdDialogInteractionListener {
        void onShowAd();
        void onCancelAd();
    }
}
