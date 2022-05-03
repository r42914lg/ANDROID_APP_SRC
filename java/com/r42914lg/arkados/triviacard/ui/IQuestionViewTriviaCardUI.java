package com.r42914lg.arkados.triviacard.ui;

import android.graphics.Bitmap;
import android.view.View;

public interface IQuestionViewTriviaCardUI {
    public void updatePictureView(Bitmap bitmap);
    public void updateCellView(int index, int value);
    public void openAllCells();
    public void updateQuestionTaskView(String text);
    public void updateQuestionIndexView(String index);
    public void updateOptionsText(String option1, String option2, String option3, String option4);
    public void updateLocalPoints(String points);
    public void updateTotalPoints(String points);
    public void lockQuestionView();
    public void animateOptionButton(int optionIndex, int animationType);
    public void handleOptionClick(int index);
    public void handleTapOnCell(View v);
    public void navigateToNextFragment();
}
