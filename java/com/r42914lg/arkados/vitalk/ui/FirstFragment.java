package com.r42914lg.arkados.vitalk.ui;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.databinding.FragmentFirstBinding;
import com.r42914lg.arkados.vitalk.graph.DaggerFragmentGraph;
import com.r42914lg.arkados.vitalk.graph.VmProviderModule;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.model.WorkItemVideo;
import com.r42914lg.arkados.vitalk.presenter.ViTalkPresenterFirstFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class FirstFragment extends Fragment implements IViTalkWorkItems {
    public static final String TAG = "LG> FirstFragment";

    private FragmentFirstBinding binding;

    @Inject
    ViTalkVM viTalkVM;

    @Inject
    ViTalkPresenterFirstFragment viTalkPresenterFirstFragment;

    private WorkItemAdapter adapter;
    private final ArrayList<WorkItemVideo> adapterVideoList = new ArrayList();
    private final ArrayList<WorkItemVideo> adapterVideoListFiltered = new ArrayList();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        DaggerFragmentGraph.builder()
                .vmProviderModule(new VmProviderModule(getActivity()))
                .build()
                .inject(this);

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
        viTalkPresenterFirstFragment.initWorkItemFragment(this);

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

    public ViTalkPresenterFirstFragment getViTalkPresenter() {
        return viTalkPresenterFirstFragment;
    }

    public void setVideoIdForWorkNavigateToSecond(String youTubeId) {
        if (viTalkVM.isOnline()) {
            viTalkPresenterFirstFragment.setVideoIdForWork(youTubeId);
            NavHostFragment.findNavController(FirstFragment.this).navigate(R.id.action_FirstFragment_to_SecondFragment);
        } else {
            viTalkVM.notifyUIShowToast("Check your internet connection and retry");
        }
        if (LOG) {
            Log.d(TAG, ".setVideoIdForWorkNavigateToSecond Video selected == " + youTubeId + " Online == " + viTalkVM.isOnline());
        }
    }

    private void doFilterQuizzes() {
        Set<String> favorites = viTalkVM.getFavoriteIDs();
        adapterVideoListFiltered.clear();
        if (viTalkVM.getFavoritesLiveData().getValue().checkFavoritesChecked()
                && viTalkVM.getFavoritesLiveData().getValue().needEnableFavorites()) {
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