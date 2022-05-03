package com.r42914lg.arkados.triviacard.model;

import android.util.Log;

import java.util.List;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ;

public class TriviaCardQuizFromPrefs extends TriviaCardQuiz {
    public static final String TAG = "LG> TriviaCardQuizFP";

    private final int pointsValue;

    public TriviaCardQuizFromPrefs(int currentIndex, int currentPoints, JSON_Quiz json_quiz,  TriviaCardVM triviaCardVM) {
        super(json_quiz, currentIndex, triviaCardVM);
        this.pointsValue = currentPoints;
        if (LOG) {
            Log.d(TAG, " child constructor: currentPoints = " + currentPoints + "startIndex = " + currentIndex);
        }
    }

    @Override
    public void setJsonQuestions(List<JSON_Question> jsonQuestions) {
        int emptyItemsCount = NUM_OF_QUESTIONS_IN_QUIZ - jsonQuestions.size();
        for (int i = 0; i < emptyItemsCount; i++) {
            questions.add(null);
        }
        if (LOG) {
            Log.d(TAG, ".setJsonQuestions: skipping first = " + emptyItemsCount + " questions");
        }
        int ordIndexInQuiz = emptyItemsCount + 1;
        for (JSON_Question j : jsonQuestions) {
            if (LOG) {
                Log.d(TAG, ".setJsonQuestions: creating question w id = " + j.getId());
            }
            questions.add(new TriviaCardQuestion(j, ordIndexInQuiz++, isLocal(), triviaCardVM));
        }
    }

    @Override
    public int getSumOfPlayedQuestionsPoints() {
        return super.getSumOfPlayedQuestionsPoints() + pointsValue;
    }
}
