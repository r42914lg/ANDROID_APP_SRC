package com.r42914lg.arkados.triviacard.model;

import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.Fragment;

import com.r42914lg.arkados.triviacard.ui.ICoreFrameTriviaCardUI;
import com.r42914lg.arkados.triviacard.ui.IQuestionViewTriviaCardUI;
import com.r42914lg.arkados.triviacard.ui.IQuizChooserTriviaCardUI;
import com.r42914lg.arkados.triviacard.util.BillingAssistant;

import java.util.concurrent.ExecutorService;

public interface ITriviaCardPresenter {
    public TriviaCardVM getViewModel();
    public BillingAssistant getBillingAssistant();

    public void onAdsButtonClicked();
    public void shutDownBillingClient();

    public int getImageWidth();
    public int getButtonPixelSize();
    public int checkShortDisplayFlag();

    public void setCurrentFragment(Fragment fragment);

    public void initCoreFrame(ICoreFrameTriviaCardUI iCoreFrame);
    public void initQuestionView(IQuestionViewTriviaCardUI iQuestionView);
    public void initQuizChooser(IQuizChooserTriviaCardUI iQuizChooser);

    public void processOption(int optionIndex);
    public void processCellOpen(int cellIndex);
    public void processFabClick();
    public void processQuizSelected(String quizId);

    public void doSelectQuizAndNavigateToFirst(JSON_Quiz quizSelected);

    public void saveGame();

    public void detachView(ICoreFrameTriviaCardUI view);
    public void detachView(IQuestionViewTriviaCardUI view);
    public void detachView(IQuizChooserTriviaCardUI view);

    public void requestFinish();
}
