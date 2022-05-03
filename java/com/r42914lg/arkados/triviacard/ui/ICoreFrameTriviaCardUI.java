package com.r42914lg.arkados.triviacard.ui;

import com.r42914lg.arkados.triviacard.model.ITriviaCardPresenter;
import com.r42914lg.arkados.triviacard.model.JSON_Quiz;

public interface ICoreFrameTriviaCardUI {
    public int getDisplayWidth();
    public int getDisplayHeight();
    public int getOrientation();
    public int getAppBarHeight();
    public int getStatusBarHeight();
    public void showProgressBar(int progress);
    public void stopProgressBar();
    public void updateFabAction(int fabActionCode);
    public void showToast(String text, int duration);
    public ITriviaCardPresenter getPresenterReference();
    public void requestFinishActivity();

    public void showInterstitialAd();
    public void showRewardedInterstitialAd(JSON_Quiz quiz);
    public void askRatings();

    public void showFavoriteIcon(boolean showIfTrue);
}
