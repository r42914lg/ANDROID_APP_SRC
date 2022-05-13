package com.r42914lg.arkados.vitalk.presenter;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.util.Log;

import androidx.fragment.app.Fragment;

import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.ui.IViTalkWorkItems;

import javax.inject.Inject;

public class ViTalkPresenterFirstFragment {
    public static final String TAG = "LG> ViTalkPresenterFirstFragment";

    private final ViTalkVM viTalkVM;

    @Inject
    public ViTalkPresenterFirstFragment(ViTalkVM viTalkVM) {
        this.viTalkVM = viTalkVM;

        if (LOG) {
            Log.d(TAG, ".ViTalkPresenterFirstFragment instance created");
        }
    }

    public void initWorkItemFragment(IViTalkWorkItems iViTalkWorkItems) {
        viTalkVM.getShowFabLiveData().setValue(true);
        viTalkVM.getShowTabOneMenuItems().setValue(true);
        viTalkVM.getProgressBarFlagLiveData().setValue(false);

        viTalkVM.getWorkItemsLoadedFlagLiveData().observe(((Fragment) iViTalkWorkItems).getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                iViTalkWorkItems.onAddRowsToAdapter(viTalkVM.getWorkItemVideoList());
            }
        });

        viTalkVM.getFavoritesLiveData().observe(((Fragment) iViTalkWorkItems).getViewLifecycleOwner(), event -> iViTalkWorkItems.onFavoritesChanged());
        viTalkVM.getInvalidateItemAtPositionLiveData().observe(((Fragment) iViTalkWorkItems).getViewLifecycleOwner(), iViTalkWorkItems::notifyAdapterIconLoaded);

        viTalkVM.requestToolbarUpdate();

        if (LOG) {
            Log.d(TAG, ".initWorkItemFragment");
        }
    }

    public void setVideoIdForWork(String youTubeId) {
        viTalkVM.onVideIdSelected(youTubeId);
        viTalkVM.getRecordSessionEndedFlagLiveData().setValue(false);
    }
}
