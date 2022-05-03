package com.r42914lg.arkados.triviacard.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.ads.AdRequest;
import com.r42914lg.arkados.triviacard.TriviaCardConstants;
import com.r42914lg.arkados.triviacard.databinding.FragmentFirstBinding;
import com.r42914lg.arkados.triviacard.model.ITriviaCardPresenter;
import com.r42914lg.arkados.triviacard.R;
import com.r42914lg.arkados.triviacard.model.UI_VM_EVENT;

import org.jetbrains.annotations.NotNull;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.ADS_ENABLED;

public class FirstFragment extends Fragment implements IQuestionViewTriviaCardUI, View.OnClickListener {
    public static final String TAG = "LG> FirstFragment";

    private FragmentFirstBinding binding;
    private ITriviaCardPresenter presenter;
    private ObjectAnimator anim;

    private TextView taskDefTextView;
    private TextView questionIndexTextView;
    private TextView pointsAllTextView;
    private TextView pointsCurrentTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        presenter = ((ICoreFrameTriviaCardUI) getActivity()).getPresenterReference();
        presenter.setCurrentFragment(this);

        binding = FragmentFirstBinding.inflate(inflater, container, false);

        binding.option1.setOnClickListener(this);
        binding.option2.setOnClickListener(this);
        binding.option3.setOnClickListener(this);
        binding.option4.setOnClickListener(this);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCellsAndImageView();

        if (presenter.checkShortDisplayFlag() == TriviaCardConstants.DISPLAY_LOW_RES) {
            binding.banner.setVisibility(View.GONE);
        } else {
            showAdBanner();
        }

        if (presenter.checkShortDisplayFlag() != TriviaCardConstants.DISPLAY_NORMAL) {
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
            }
            adjustViewsToShortDisplay();
        } else {
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().show();
            }
            binding.taskDef.setEnabled(false);
            binding.pointsAll.setEnabled(false);
            binding.pointsCurrent.setEnabled(false);
            binding.questionIndex.setEnabled(false);
        }

        presenter.getViewModel().getLiveUIEvent().observe(getViewLifecycleOwner(), new Observer<UI_VM_EVENT>() {
            @Override
            public void onChanged(UI_VM_EVENT event) {
                if (event.getEventType() == UI_VM_EVENT.TYPE_ADS_STATE && binding.banner.getVisibility() != View.GONE) {
                    binding.banner.setVisibility(event.checkHideAds() ? View.INVISIBLE : View.VISIBLE);
                }
            }
        });

        if (LOG) {
            Log.d(TAG, ".onViewCreated");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.initQuestionView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (anim != null) {
            anim.end();
        }
        presenter.detachView(this);

        if (LOG) {
            Log.d(TAG, ".onDestroyView");
        }
    }

    @Override
    public void updatePictureView(Bitmap image) {
        ImageView imageView = binding.relativeLayout.findViewWithTag("image");
        if (image == null) {
            imageView.setImageDrawable(null);
        } else {
            imageView.setImageBitmap(Bitmap.createScaledBitmap(image, presenter.getImageWidth(), presenter.getImageWidth(), false));
        }
    }

    @Override
    public void updateCellView(int index, int value) {
        TextView view = binding.relativeLayout.findViewWithTag(index);
        switch (value) {
            case 0:
                view.setVisibility(View.INVISIBLE);
                view.setClickable(true);
                break;
            case -1:
                view.setText(null);
                view.setVisibility(View.VISIBLE);
                view.setClickable(false);
                break;
            case -2:
                view.setText(null);
                view.setVisibility(View.INVISIBLE);
                view.setClickable(false);
                break;
            default:
                view.setText(""+value);
                view.setVisibility(View.VISIBLE);
                view.setClickable(true);
        }
    }

    @Override
    public void openAllCells() {
        binding.option1.setClickable(false);
        binding.option2.setClickable(false);
        binding.option3.setClickable(false);
        binding.option4.setClickable(false);
        for (int i = 0; i < TriviaCardConstants.CELLS_COUNT * TriviaCardConstants.CELLS_COUNT; i++) {
            updateCellView(i + 1, -2);
        }
    }

    @Override
    public void updateQuestionTaskView(String text) {
        if (presenter.checkShortDisplayFlag() != TriviaCardConstants.DISPLAY_NORMAL) {
            taskDefTextView.setText(text);
        } else {
            binding.taskDef.setText(text);
        }
    }

    @Override
    public void updateQuestionIndexView(String index) {
        if (presenter.checkShortDisplayFlag() != TriviaCardConstants.DISPLAY_NORMAL) {
            questionIndexTextView.setText(index);
        } else {
            binding.questionIndex.setText(index);
        }
    }

    @Override
    public void updateOptionsText(String option1, String option2, String option3, String option4) {
        binding.option1.setText(option1);
        binding.option2.setText(option2);
        binding.option3.setText(option3);
        binding.option4.setText(option4);
        binding.option1.setClickable(option1 != null);
        binding.option2.setClickable(option2 != null);
        binding.option3.setClickable(option3 != null);
        binding.option4.setClickable(option4 != null);
    }

    @Override
    public void updateLocalPoints(String points) {
        if (presenter.checkShortDisplayFlag() != TriviaCardConstants.DISPLAY_NORMAL) {
            pointsCurrentTextView.setText(points);
        } else {
            binding.pointsCurrent.setText(points);
        }
    }

    @Override
    public void updateTotalPoints(String points) {
        if (presenter.checkShortDisplayFlag() != TriviaCardConstants.DISPLAY_NORMAL) {
            pointsAllTextView.setText(points);
        } else {
            binding.pointsAll.setText(points);
        }
    }

    @Override
    public void lockQuestionView() {
        binding.option1.setClickable(false);
        binding.option2.setClickable(false);
        binding.option3.setClickable(false);
        binding.option4.setClickable(false);
        for (int i = 0; i < TriviaCardConstants.CELLS_COUNT * TriviaCardConstants.CELLS_COUNT; i++) {
            updateCellView(i + 1, -1);
        }
    }

    @Override
    public void animateOptionButton(int optionIndex, int animationType) {
        Button buttonToAnimate;
        switch (optionIndex) {
            case 1:
                buttonToAnimate = binding.option1;
                break;
            case 2:
                buttonToAnimate = binding.option2;
                break;
            case 3:
                buttonToAnimate = binding.option3;
                break;
            case 4:
                buttonToAnimate = binding.option4;
                break;
            default:
                throw new IllegalStateException("FR1.animateOptionButton --> Unexpected value: " + optionIndex);
        }
        doAnimate(buttonToAnimate, animationType);
    }

    @Override
    public void handleOptionClick(int index) {
        if (LOG) {
            Log.d(TAG, ".handleOptionClick #" + index);
        }
        presenter.processOption(index);
    }

    @Override
    public void handleTapOnCell(View v) {
        presenter.processCellOpen((int) v.getTag());
        v.setVisibility(View.INVISIBLE);
    }

    @Override
    public void navigateToNextFragment() {
        NavHostFragment.findNavController(this).navigate(R.id.action_FirstFragment_to_SecondFragment);
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
    }

    private void initCellsAndImageView() {
        ImageView picture = new ImageView(getContext());
        picture.setTag("image");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        binding.relativeLayout.addView(picture, params);

        View.OnClickListener cellListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTapOnCell(v);
            }
        };

        int currentIndex = 0;
        for (int i = 0; i < TriviaCardConstants.CELLS_COUNT; i++) {
            for (int j = 0; j < TriviaCardConstants.CELLS_COUNT; j++) {
                TextView cell = new TextView(getContext());
                cell.setTag(++currentIndex);
                cell.setId(currentIndex);
                cell.setBackgroundColor(getResources().getColor(R.color.my_grey));
                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    params.addRule(RelativeLayout.BELOW, currentIndex - TriviaCardConstants.CELLS_COUNT);
                }
                if (j > 0) {
                    params.addRule(RelativeLayout.RIGHT_OF, currentIndex - 1);
                }
                cell.setOnClickListener(cellListener);
                binding.relativeLayout.addView(cell, params);
                sizeView(cell, presenter.getButtonPixelSize());
            }
        }
    }

    private void sizeView(View view, int buttonPixelSize) {
        view.getLayoutParams().width = buttonPixelSize;
        view.getLayoutParams().height = buttonPixelSize;
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin = 1;
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin = 1;
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin = 1;
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin = 1;
    }

    private void doAnimate(Button button, int animationType) {
        anim = ObjectAnimator.ofFloat(button, View.ALPHA, 0.25f, 1.0f);
        anim.setDuration(1000);

        switch (animationType) {
            case TriviaCardConstants.ANIMATE_OPTION_WIN:
                button.setBackgroundColor(Color.GREEN);
                break;
            case TriviaCardConstants.ANIMATE_OPTION_FAIL_1:
                button.setBackgroundColor(Color.RED);
                break;
            case TriviaCardConstants.ANIMATE_OPTION_FAIL_2:
                button.setBackgroundColor(Color.RED);
                anim.setRepeatCount(1);
                break;
        }

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button.setBackgroundColor(getResources().getColor(R.color.my_prime, getContext().getTheme()));
            }
        });
        anim.start();
    }

    private void adjustViewsToShortDisplay() {
        binding.taskDefTl.setVisibility(View.GONE);
        binding.pointsAllTl.setVisibility(View.GONE);
        binding.questionIndexTl.setVisibility(View.GONE);
        binding.pointsCurrentTl.setVisibility(View.GONE);

        pointsAllTextView = new TextView(getActivity());
        questionIndexTextView = new TextView(getActivity());
        pointsCurrentTextView = new TextView(getActivity());
        taskDefTextView = new TextView(getActivity());


        pointsAllTextView.setText("Quiz points: ");
        pointsAllTextView.setId(View.generateViewId());
        questionIndexTextView.setText("Question index: ");
        questionIndexTextView.setId(View.generateViewId());
        pointsCurrentTextView.setText("Question points: ");
        pointsCurrentTextView.setId(View.generateViewId());
        taskDefTextView.setText("Question: ");
        taskDefTextView.setId(View.generateViewId());
        taskDefTextView.setTextColor(getResources().getColor(R.color.design_default_color_secondary));

        ConstraintLayout layout = binding.getRoot();
        layout.addView(pointsAllTextView);
        layout.addView(questionIndexTextView);
        layout.addView(pointsCurrentTextView);
        layout.addView(taskDefTextView);

        ConstraintSet set = new ConstraintSet();
        int[] chainIds = {pointsAllTextView.getId(), questionIndexTextView.getId(), pointsCurrentTextView.getId() };

        set.clone(layout);

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            set.connect(pointsAllTextView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
            set.connect(pointsAllTextView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
            set.connect(pointsCurrentTextView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            set.createHorizontalChain(pointsAllTextView.getId(), ConstraintSet.LEFT, pointsCurrentTextView.getId(), ConstraintSet.RIGHT, chainIds, null, ConstraintSet.CHAIN_SPREAD);
            set.connect(binding.relativeLayout.getId(), ConstraintSet.TOP, questionIndexTextView.getId(), ConstraintSet.BOTTOM, 0);
            set.connect(taskDefTextView.getId(), ConstraintSet.TOP, binding.relativeLayout.getId(), ConstraintSet.BOTTOM, 0);
            set.connect(taskDefTextView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
            set.connect(taskDefTextView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            set.connect(binding.option1.getId(), ConstraintSet.TOP, taskDefTextView.getId(), ConstraintSet.BOTTOM, 0);
        } else {
            set.connect(pointsAllTextView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
            set.connect(pointsAllTextView.getId(), ConstraintSet.START, binding.relativeLayout.getId(), ConstraintSet.END, 0);
            set.connect(pointsCurrentTextView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            set.createHorizontalChain(pointsAllTextView.getId(), ConstraintSet.LEFT, pointsCurrentTextView.getId(), ConstraintSet.RIGHT, chainIds, null, ConstraintSet.CHAIN_SPREAD);
            set.connect(taskDefTextView.getId(), ConstraintSet.TOP, pointsAllTextView.getId(), ConstraintSet.BOTTOM, 0);
            set.connect(taskDefTextView.getId(), ConstraintSet.START, binding.relativeLayout.getId(), ConstraintSet.END, 0);
            set.connect(taskDefTextView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            set.connect(binding.option1.getId(), ConstraintSet.TOP, taskDefTextView.getId(), ConstraintSet.BOTTOM, 0);
            set.connect(binding.option1.getId(), ConstraintSet.START, binding.relativeLayout.getId(), ConstraintSet.END, 0);
        }

        set.applyTo(layout);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.option1) {
            handleOptionClick(1);
        } else if (v == binding.option2) {
            handleOptionClick(2);
        } else if (v == binding.option3) {
            handleOptionClick(3);
        } else if (v == binding.option4) {
            handleOptionClick(4);
        }
    }


    private void showAdBanner() {
        if (!ADS_ENABLED)
            return;

        binding.banner.loadAd(new AdRequest.Builder().build());
    }
}