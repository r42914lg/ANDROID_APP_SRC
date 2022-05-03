package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;
import static com.r42914lg.arkados.vitalk.ViTalkConstants.VIDEO_GALLERY_ADAPTER_COLUMNS;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.databinding.FragmentThirdBinding;
import com.r42914lg.arkados.vitalk.model.LocalVideo;
import com.r42914lg.arkados.vitalk.model.VideoGalleryScanner;

public class ThirdFragment extends Fragment {
    public static final String TAG = "LG> ThirdFragment";

    private FragmentThirdBinding binding;
    private ViTalkPresenter viTalkPresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viTalkPresenter = ((MainActivity) getActivity()).getViTalkPresenter();
        binding = FragmentThirdBinding.inflate(inflater, container, false);

        binding.videoGalleryRecycler.setLayoutManager(new GridLayoutManager(container.getContext(), VIDEO_GALLERY_ADAPTER_COLUMNS));
        VideoGalleryScanner videoGalleryScanner = new VideoGalleryScanner(getActivity());
        VideoGalleryAdapter videoGalleryAdapter = new VideoGalleryAdapter(videoGalleryScanner, this);
        binding.videoGalleryRecycler.setAdapter(videoGalleryAdapter);

        if (LOG) {
            Log.d(TAG, ".onCreateView");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viTalkPresenter.initGalleryChooserFragment();

        if (LOG) {
            Log.d(TAG, ".onViewCreated");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        if (LOG) {
            Log.d(TAG, ".onDestroyView");
        }
    }

    public void startVideoUploadNavigateToFirst(LocalVideo localVideoSelected) {
        viTalkPresenter.startVideoUpload(localVideoSelected);
        NavHostFragment.findNavController(ThirdFragment.this).navigate(R.id.action_ThirdFragment_to_FirstFragment);

        if (LOG) {
            Log.d(TAG, ".startVideoUploadNavigateToFirst");
        }
    }
}