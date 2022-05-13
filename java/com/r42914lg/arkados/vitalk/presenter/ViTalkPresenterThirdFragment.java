package com.r42914lg.arkados.vitalk.presenter;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.util.Log;

import com.r42914lg.arkados.vitalk.model.ViTalkVM;

import javax.inject.Inject;

public class ViTalkPresenterThirdFragment {
    public static final String TAG = "LG> ViTalkPresenterThirdFragment";

    private final ViTalkVM viTalkVM;

    @Inject
    public ViTalkPresenterThirdFragment(ViTalkVM viTalkVM) {
        this.viTalkVM = viTalkVM;

        if (LOG) {
            Log.d(TAG, ".ViTalkPresenterThirdFragment instance created");
        }
    }

    public void initGalleryChooserFragment() {
        viTalkVM.getShowFabLiveData().setValue(false);
        viTalkVM.getShowTabOneMenuItems().setValue(false);

        if (LOG) {
            Log.d(TAG, ".initGalleryChooserFragment");
        }
    }
}
