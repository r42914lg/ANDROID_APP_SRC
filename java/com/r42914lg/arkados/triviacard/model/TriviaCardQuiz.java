package com.r42914lg.arkados.triviacard.model;

import android.util.Log;
import com.r42914lg.arkados.triviacard.TriviaCardConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class TriviaCardQuiz {
    public static final String TAG = "LG> TriviaCardQuiz";

    private final JSON_Quiz jsonQuiz;
    protected final ArrayList<TriviaCardQuestion> questions = new ArrayList<>(TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ);
    protected int currentQuestionIndex;
    protected int numOfImagesLoaded;
    private boolean quizIsInProgress;
    private boolean q1NotShownYet;
    protected final TriviaCardVM triviaCardVM;

    public TriviaCardQuiz(JSON_Quiz json_quiz, TriviaCardVM triviaCardVM) {
        this(json_quiz, 0, triviaCardVM);
    }

    public TriviaCardQuiz(JSON_Quiz json_quiz, int currentQuestionIndex, TriviaCardVM triviaCardVM) {
        this.triviaCardVM = triviaCardVM;
        this.jsonQuiz = json_quiz;
        this.currentQuestionIndex = currentQuestionIndex;
        this.numOfImagesLoaded = currentQuestionIndex;
        this.quizIsInProgress = true;
        this.q1NotShownYet = true;

        triviaCardVM.setCurrentQuiz(this);

        if (jsonQuiz.isLocal()) {
            List<JSON_Question> jsonQuestions = triviaCardVM.getLocalActiveQuestionsGivenQuiz(json_quiz);
            setJsonQuestions(jsonQuestions);
        } else {
            triviaCardVM.subscribeForActiveQuestionsGivenQuiz(this);
        }

        if (LOG) {
            Log.d(TAG, " base constructor: quiz ID - current question index = " + json_quiz.getId() + "-" + currentQuestionIndex);
        }
    }
    public void setQ1NotShownYet(boolean q1NotShownYet) {
        this.q1NotShownYet = q1NotShownYet;
    }

    public boolean checkIfQ1NotShownYet() {
        return q1NotShownYet;
    }

    public void setQuizIsInProgress(boolean quizIsInProgress) {
        this.quizIsInProgress = quizIsInProgress;
    }

    public boolean checkIfQuizInProgress() {
        return quizIsInProgress;
    }

    public boolean checkIfImageLoaded(int indexStartFromZero) {
        return (questions.size() > indexStartFromZero) && questions.get(indexStartFromZero).checkImageLoaded();
    }

    public void setJsonQuestions(List<JSON_Question> jsonQuestions) {
        Collections.shuffle(jsonQuestions);

        int ordIndexInQuiz = 1;
        int i = 0;
        while (questions.size() < TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ && i < jsonQuestions.size()) {
            if (triviaCardVM.readFreqByQiD(jsonQuestions.get(i).getId()) > 0) {
                if (LOG) {
                    Log.d(TAG, ".setJsonQuestions: skipping question w id = " + jsonQuestions.get(i).getId());
                }
                i++;
            } else {
                if (LOG) {
                    Log.d(TAG, ".setJsonQuestions: creating question w id = " + jsonQuestions.get(i).getId());
                }
                questions.add(new TriviaCardQuestion(jsonQuestions.get(i), ordIndexInQuiz++, jsonQuiz.isLocal(), triviaCardVM));
                jsonQuestions.remove(i);
            }
        }

        i = 0;
        while (questions.size() < TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ && i < jsonQuestions.size()) {
            if (LOG) {
                Log.d(TAG, ".setJsonQuestions: [SECOND PASS] creating question w id = " + jsonQuestions.get(i).getId());
            }
            questions.add(new TriviaCardQuestion(jsonQuestions.get(i++), ordIndexInQuiz++, jsonQuiz.isLocal(), triviaCardVM));
        }
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public boolean isLocal() {
        return jsonQuiz.isLocal();
    }

    public boolean iterateToNextQuestion() {
        boolean valueToReturn = false;
        if (checkIfImageLoaded(currentQuestionIndex + 1)) {
            currentQuestionIndex++;
            valueToReturn = true;
        }
        if (LOG) {
            Log.d(TAG, ".iterateToNextQuestion: " + valueToReturn);
        }
        return valueToReturn;
    }

    public int getSumOfPlayedQuestionsPoints() {
        int sumOfPoints = 0;
        for (TriviaCardQuestion tcq: questions) {
            if (tcq != null && tcq.getState() == TriviaCardConstants.Q_WON) {
                sumOfPoints += tcq.getPoints();
            }
        }
        if (LOG) {
            Log.d(TAG, ".getSumOfPlayedQuestionsPoints: " + sumOfPoints);
        }
        return sumOfPoints;
    }

    public int processOptionClick(int optionIndex) {
        TriviaCardQuestion tcq = questions.get(currentQuestionIndex);
        int resultCode;
        if (tcq.checkAnswer(optionIndex)) {
            tcq.setState(TriviaCardConstants.Q_WON);
            resultCode = TriviaCardConstants.Q_WON;
        } else {
            switch (tcq.getState()) {
                case TriviaCardConstants.Q_NOT_PLAYED:
                    tcq.subtractPoints(TriviaCardConstants.POINTS_PER_FAILURE);
                    tcq.setState(TriviaCardConstants.Q_FAILURE_1);
                    resultCode = TriviaCardConstants.Q_FAILURE_1;
                    break;
                case TriviaCardConstants.Q_FAILURE_1:
                    tcq.subtractPoints(0);
                    tcq.setState(TriviaCardConstants.Q_FAILURE_2);
                    resultCode = TriviaCardConstants.Q_FAILURE_2;
                    break;
                default:
                    throw new IllegalStateException("TCQ.processOptionClick --> Unexpected state: " + tcq.getState());
            }
        }
        if (LOG) {
            Log.d(TAG, ".processOptionClick: optionIndex = " + optionIndex + " processed --> resultCode = " + resultCode);
        }
        return resultCode;
    }

    public void updateQuestionUsageStats() {
        List<String> qIDsToIncrementStats = new ArrayList<>();
        for (TriviaCardQuestion tcq: questions) {
            if (tcq != null && tcq.getState() == TriviaCardConstants.Q_WON) {
                String qId = tcq.getJsonQuestion().getId();
                triviaCardVM.incrementFreqByQiD(qId);
                qIDsToIncrementStats.add(qId);
            }
        }
        if (qIDsToIncrementStats.size() > 0) {
            triviaCardVM.incrementFreqByQiD(qIDsToIncrementStats);
        } else {
            if (LOG) {
                Log.d(TAG, ".updateQuestionUsageStats: nothing to update");
            }
        }
    }

    public void incrementCountImagesLoaded() {
        ++numOfImagesLoaded;
    }

    public int getCountImagesLoaded() {
        return numOfImagesLoaded;
    }

    public boolean hasNextQuestion() {
        return currentQuestionIndex + 1 < TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ;
    }

    public String getQuizId() { return jsonQuiz.getId(); }
    public String getQuizName() { return jsonQuiz.getTitle(); }
    public List<String> getQuestionIDs() { return jsonQuiz.getQuestion_ids(); }
    public TriviaCardQuestion getCurrentQuestion() { return questions.get(currentQuestionIndex); }
    public JSON_Quiz getJsonQuiz() { return jsonQuiz; }
}
