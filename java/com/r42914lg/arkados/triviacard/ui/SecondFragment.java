package com.r42914lg.arkados.triviacard.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.r42914lg.arkados.triviacard.databinding.FragmentSecondBinding;
import com.r42914lg.arkados.triviacard.model.ITriviaCardPresenter;
import com.r42914lg.arkados.triviacard.model.JSON_Quiz;
import com.r42914lg.arkados.triviacard.R;
import com.r42914lg.arkados.triviacard.model.UI_VM_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.ADS_ENABLED;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class SecondFragment extends Fragment implements IQuizChooserTriviaCardUI {
    public static final String TAG = "LG> SecondFragment";

    private ITriviaCardPresenter presenter;
    private FragmentSecondBinding binding;
    QuizAdapter adapter;
    ArrayList<JSON_Quiz> adapterQuizzesList = new ArrayList();
    ArrayList<JSON_Quiz> adapterQuizzesListFiltered = new ArrayList();
    private long backPressedTime;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    presenter.saveGame();
                    presenter.requestFinish();
                } else {
                    Toast.makeText(getContext(), R.string.toast_back_pressed, Toast.LENGTH_SHORT).show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        presenter = ((ICoreFrameTriviaCardUI) getActivity()).getPresenterReference();
        presenter.setCurrentFragment(this);

        binding = FragmentSecondBinding.inflate(inflater, container, false);

        binding.recycler.setLayoutManager(new LinearLayoutManager(container.getContext()));
        adapter = new QuizAdapter(adapterQuizzesListFiltered, container.getContext(), presenter);
        binding.recycler.setAdapter(adapter);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.getViewModel().getLiveUIEvent().observe(getViewLifecycleOwner(), new Observer<UI_VM_EVENT>() {
            @Override
            public void onChanged(UI_VM_EVENT event) {
                if (event.getEventType() == UI_VM_EVENT.TYPE_FAVORITES_STATE) {
                    doFilterQuizzes();
                    adapter.notifyDataSetChanged();
                }
                if (event.getEventType() == UI_VM_EVENT.TYPE_ADS_STATE && binding.banner.getVisibility() != View.GONE) {
                    binding.banner.setVisibility(event.checkHideAds() ? View.INVISIBLE : View.VISIBLE);
                }
            }
        });

        showAdBanner();

        if (LOG) {
            Log.d(TAG, ".onViewCreated");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }

        presenter.initQuizChooser(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        presenter.detachView(this);

        if (LOG) {
            Log.d(TAG, ".onDestroyView");
        }
    }

    @Override
    public void addRowsToAdapter(List<JSON_Quiz> quizList, boolean clearFirst) {
        if (clearFirst) {
            adapterQuizzesList.clear();
        }
        if (quizList != null) {
            if (LOG) {
                Log.d(TAG, ".addRowsToAdapter: quizList = " + quizList);
            }
            adapterQuizzesList.addAll(quizList);
            doFilterQuizzes();
            adapter.notifyDataSetChanged();
        }
    }

    private void doFilterQuizzes() {
        Set<String> favorites = presenter.getViewModel().getFavoriteQuizIDs();
        adapterQuizzesListFiltered.clear();
        if (presenter.getViewModel().getLiveUIEvent().getValue().checkFavoritesChecked()
                && presenter.getViewModel().getLiveUIEvent().getValue().needEnableFavorites()) {
            for (JSON_Quiz quiz : adapterQuizzesList) {
                if (favorites.contains(quiz.getId())) {
                    adapterQuizzesListFiltered.add(quiz);
                }
            }
        } else {
            adapterQuizzesListFiltered.addAll(adapterQuizzesList);
        }
    }

    @Override
    public void notifyAdapterIconLoaded(int position) {
        adapter.notifyItemChanged(position);
        if (LOG) {
            Log.d(TAG, ".notifyAdapterIconLoaded position --> " + position);
        }
    }

    @Override
    public void navigateToNextFragment() {
        NavHostFragment.findNavController(this).navigate(R.id.action_SecondFragment_to_FirstFragment);
    }

    private void showAdBanner() {
        if (!ADS_ENABLED)
            return;

        binding.banner.loadAd(new AdRequest.Builder().build());
    }
}