package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.tabs.TabLayout;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.databinding.FragmentSecondBinding;
import com.r42914lg.arkados.vitalk.graph.DaggerFragmentGraph;
import com.r42914lg.arkados.vitalk.graph.VmProviderModule;
import com.r42914lg.arkados.vitalk.media.MediaOrchestrator;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.presenter.ViTalkPresenterSecondFragment;

import javax.inject.Inject;

public class SecondFragment extends Fragment implements IViTalkWorker {
    public static final String TAG = "LG> SecondFragment";

    private FragmentSecondBinding binding;

    @Inject
    ViTalkVM viTalkVM;

    @Inject
    ViTalkPresenterSecondFragment viTalkPresenterSecondFragment;

    private MediaOrchestrator mediaOrchestrator;
    private int currentTabPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        DaggerFragmentGraph.builder()
                .vmProviderModule(new VmProviderModule(getActivity()))
                .build()
                .inject(this);

        binding = FragmentSecondBinding.inflate(inflater, container, false);

        if (LOG) {
            Log.d(TAG, ".onCreateView");
        }

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.shareLinkButton.setEnabled(false);

        viTalkPresenterSecondFragment.initWorkerFragment(this, getContext());
        initTabsOneTwo();

        mediaOrchestrator = new MediaOrchestrator(getContext(), getViewLifecycleOwner(),
                binding.youtubePlayerView, binding.youtubePlayerSeekbar, viTalkVM, this);

        binding.muteSwitch.setOnCheckedChangeListener((compoundButton, b) -> mediaOrchestrator.onMuteChecked(b));

        binding.talkButton.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                binding.talkButton.setBackgroundColor(Color.RED);
                binding.recordingIndicator.setVisibility(View.VISIBLE);
                mediaOrchestrator.onRecordResume();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                binding.talkButton.setBackgroundColor(getResources().getColor(R.color.light_blue_900, getActivity().getTheme()));
                binding.recordingIndicator.setVisibility(View.INVISIBLE);
                mediaOrchestrator.onRecordPause();
            }
            return false;
        });

        viTalkVM.setYoutubeVideoIdToShareOrPreview(viTalkVM.getCurrentYoutubeId());
        binding.shareLinkButton.setOnClickListener(view12 -> viTalkVM.getUiActionMutableLiveData().setValue(ViTalkVM.SHARE_ACTION_CODE));
        binding.finishRecordingButton.setOnClickListener(view13 -> mediaOrchestrator.stopRecordingSession(true));

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

    private void initTabsOneTwo() {
        TabLayout.Tab tab1 = binding.tabLayout.newTab();
        tab1.setText("Source");
        binding.tabLayout.addTab(tab1);

        TabLayout.Tab tab2 = binding.tabLayout.newTab();
        tab2.setText("Record");
        binding.tabLayout.addTab(tab2);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                mediaOrchestrator.getMyUiController().animateButton(false);
                mediaOrchestrator.rewindVideo();

                currentTabPosition = tab.getPosition();
                switch (currentTabPosition) {
                    case 0:
                        tabOne();
                        break;
                    case 1:
                        tabTwo();
                        break;
                    case 2:
                        tabThree();
                        break;
                    default:
                        throw new IllegalStateException("Wrong TAB state --> " + currentTabPosition);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void initTabThree() {
        TabLayout.Tab tab3 = binding.tabLayout.newTab();
        tab3.setText("Preview");
        binding.tabLayout.addTab(tab3);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void prepareTab1(boolean flag) {
        binding.youtubePlayerSeekbar.setColor(flag ? Color.RED : Color.GRAY);
        binding.youtubePlayerSeekbar.getSeekBar().setOnTouchListener((view, motionEvent) -> !flag);
    }

    private void prepareTab2(boolean flag) {
        onRecordButtonEnabledFlag(false);
        binding.talkButton.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
        binding.finishRecordingButton.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
        if (flag) {
            mediaOrchestrator.releaseAudioPlayer();
        }
    }

    private void prepareTab3(boolean flag) {
        binding.shareLinkButton.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
        binding.muteSwitch.setVisibility(flag ? View.VISIBLE : View.INVISIBLE);
        if (flag) {
            mediaOrchestrator.releaseAudioRecorder();
            mediaOrchestrator.initAudioPlayer();
            binding.muteSwitch.setChecked(false);
        }
    }

    @Override
    public Mode getMode() {
        switch (currentTabPosition) {
            case 0:
                return Mode.SOURCE;
            case 1:
                return Mode.RECORD;
            case 2:
                return Mode.PREVIEW;
            default:
                throw new IllegalStateException("Current Tab position --> " + currentTabPosition);
        }
    }

    @Override
    public void onRecordButtonEnabledFlag(boolean aBoolean) {
        if (binding.tabLayout.getSelectedTabPosition() != 1) {
            return;
        }
        binding.talkButton.setBackgroundColor(aBoolean ? getResources().getColor(R.color.light_blue_900, getActivity().getTheme()) : Color.DKGRAY);
        binding.talkButton.setEnabled(aBoolean);
        binding.finishRecordingButton.setBackgroundColor(aBoolean ? getResources().getColor(R.color.light_blue_900, getActivity().getTheme()) : Color.DKGRAY);
        binding.finishRecordingButton.setEnabled(aBoolean);
    }

    @Override
    public void onRecordSessionEndedFlag(boolean aBoolean) {
        if (aBoolean) {
            if (binding.tabLayout.getTabCount() == 2) {
                initTabThree();
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(2));
                tabThree();
                binding.muteSwitch.setChecked(false);
                mediaOrchestrator.onMuteChecked(false);
            }
        } else {
            if (binding.tabLayout.getTabCount() == 3) {
                binding.tabLayout.removeTabAt(2);
            }
        }
    }

    @Override
    public void onFirebaseUploadFinishedFlag(boolean aBoolean) {
        binding.shareLinkButton.setEnabled(aBoolean);
    }

    @Override
    public void navigateToWorkItems() {
        NavHostFragment.findNavController(SecondFragment.this).navigate(R.id.action_SecondFragment_to_FirstFragment);
    }

    @Override
    public void onYouTubePlayHit() {
        if (currentTabPosition < 2 &&  binding.tabLayout.getTabCount() == 3) {
            binding.tabLayout.removeTabAt(2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaOrchestrator != null) {
            mediaOrchestrator.onResumeFragment();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaOrchestrator != null) {
            mediaOrchestrator.onPauseFragment();
        }
    }

    public void setVideoIdForWork(String youTubeId) {
        viTalkVM.onVideIdSelected(youTubeId);
        viTalkVM.getRecordSessionEndedFlagLiveData().setValue(false);
    }

    private void tabOne()  {
        prepareTab1(true);
        prepareTab2(false);
        prepareTab3(false);
    }

    private void tabTwo()  {
        prepareTab1(false);
        prepareTab2(true);
        prepareTab3(false);
    }

    private void tabThree()  {
        prepareTab1(false);
        prepareTab2(false);
        prepareTab3(true);
    }
}