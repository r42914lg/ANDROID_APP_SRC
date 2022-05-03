package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.databinding.FragmentFirstBinding;
import com.r42914lg.arkados.vitalk.model.WorkItemVideo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FirstFragment extends Fragment implements IViTalkWorkItems {
    public static final String TAG = "LG> FirstFragment";

    private FragmentFirstBinding binding;
    private ViTalkPresenter viTalkPresenter;

    private WorkItemAdapter adapter;
    private final ArrayList<WorkItemVideo> adapterVideoList = new ArrayList();
    private final ArrayList<WorkItemVideo> adapterVideoListFiltered = new ArrayList();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viTalkPresenter = ((MainActivity) getActivity()).getViTalkPresenter();
        binding = FragmentFirstBinding.inflate(inflater, container, false);

        binding.workItemRecycler.setLayoutManager(new LinearLayoutManager(container.getContext()));
        adapter = new WorkItemAdapter(adapterVideoListFiltered, this);
        binding.workItemRecycler.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDelete(adapter));
        itemTouchHelper.attachToRecyclerView(binding.workItemRecycler);

        if (LOG) {
            Log.d(TAG, ".onCreateView");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viTalkPresenter.initWorkItemFragment(this);

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

    public ViTalkPresenter getViTalkPresenter() {
        return viTalkPresenter;
    }

    public void setVideoIdForWorkNavigateToSecond(String youTubeId) {
        if (viTalkPresenter.getViTalkVM().isOnline()) {
            viTalkPresenter.setVideoIdForWork(youTubeId);
            NavHostFragment.findNavController(FirstFragment.this).navigate(R.id.action_FirstFragment_to_SecondFragment);
        } else {
            viTalkPresenter.showToast("Check your internet connection and retry", Toast.LENGTH_LONG);
        }
        if (LOG) {
            Log.d(TAG, ".setVideoIdForWorkNavigateToSecond Video selected == " + youTubeId + " Online == " + viTalkPresenter.getViTalkVM().isOnline());
        }
    }

    private void doFilterQuizzes() {
        Set<String> favorites = viTalkPresenter.getViTalkVM().getFavoriteIDs();
        adapterVideoListFiltered.clear();
        if (viTalkPresenter.getViTalkVM().getFavoritesLiveData().getValue().checkFavoritesChecked()
                && viTalkPresenter.getViTalkVM().getFavoritesLiveData().getValue().needEnableFavorites()) {
            for (WorkItemVideo workItemVideo : adapterVideoList) {
                if (favorites.contains(workItemVideo.youTubeId)) {
                    adapterVideoListFiltered.add(workItemVideo);
                }
            }
        } else {
            adapterVideoListFiltered.addAll(adapterVideoList);
        }
    }

    @Override
    public void onAddRowsToAdapter(List<WorkItemVideo> workItemVideoList) {
        if (workItemVideoList == null) {
            return;
        }
        adapterVideoList.clear();
        adapterVideoList.addAll(workItemVideoList);
        onFavoritesChanged();
    }

    @Override
    public void notifyAdapterIconLoaded(int position) {
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onFavoritesChanged() {
        doFilterQuizzes();
        adapter.notifyDataSetChanged();
    }
}