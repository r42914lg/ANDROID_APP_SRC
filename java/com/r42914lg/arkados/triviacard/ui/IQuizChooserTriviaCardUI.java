package com.r42914lg.arkados.triviacard.ui;

import androidx.recyclerview.widget.RecyclerView;

import com.r42914lg.arkados.triviacard.model.JSON_Quiz;

import java.util.List;

public interface IQuizChooserTriviaCardUI {
    public void addRowsToAdapter(List<JSON_Quiz> quizList, boolean clearFirst);
    public void notifyAdapterIconLoaded(int position);
    public void navigateToNextFragment();
}
