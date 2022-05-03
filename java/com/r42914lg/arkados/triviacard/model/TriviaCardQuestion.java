package com.r42914lg.arkados.triviacard.model;

import android.util.Log;

import com.r42914lg.arkados.triviacard.TriviaCardConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class TriviaCardQuestion {
    public static final String TAG = "LG> TriviaCardQuestion";

    private final TriviaCardVM triviaCardVM;
    private final JSON_Question jsonQuestion;
    private final ArrayList<Integer> transcoding = new ArrayList<>(TriviaCardConstants.NUM_OF_OPTIONS);
    private final int ordIndexInQuiz;
    private JSON_Image jsonImage;
    private int points;
    private int state;
    private final boolean isLocal;

    public TriviaCardQuestion(JSON_Question jsonQuestion, int ordIndexInQuiz, boolean isLocal, TriviaCardVM triviaCardVM) {
        this.triviaCardVM = triviaCardVM;
        this.jsonQuestion = jsonQuestion;
        this.ordIndexInQuiz = ordIndexInQuiz;
        this.isLocal = isLocal;

        init();
    }

    private void init() {
        points = TriviaCardConstants.POINTS_PER_QUESTION;
        state = TriviaCardConstants.Q_NOT_PLAYED;

        transcoding.add(jsonQuestion.getAnswer());
        populateOtherOptions();
        Collections.shuffle(transcoding);

        if (isLocal) {
            this.jsonImage = triviaCardVM.getLocalImageById(jsonQuestion.getImage_id());
            triviaCardVM.loadLocalImageFromFullPath(jsonImage.getImage_full_path());
        } else {
            triviaCardVM.subscribeForImageById(jsonQuestion.getImage_id(), this);
        }

        if (LOG) {
            Log.d(TAG, " instance created: qid-ordIndexInQuiz-isLocal = " + jsonQuestion.getId() + "-" + ordIndexInQuiz + "-" + isLocal);
        }
    }

    public void setJsonImage(JSON_Image jsonImage) {
        this.jsonImage = jsonImage;
        triviaCardVM.subscribeForLoadImageFromFullPath(jsonImage.getImage_full_path(), this);
    }

    public void subtractPoints(int positiveValue) {
        if (positiveValue > 0) {
            points -= positiveValue;
        } else {
            points = 0;
        }
    }

    public boolean checkImageLoaded() {
        return jsonImage != null && triviaCardVM.checkImageLoaded(jsonImage.getImage_full_path());
    }

    public String getImageFullPath() {
        return jsonImage.getImage_full_path();
    }

    public void subtractPointsByCellIndex(int cellIndex) {
        points -= jsonImage.getCell_values().get(cellIndex);
        jsonImage.getCell_values().set(cellIndex, 0);
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getPoints() { return points; }

    public boolean checkAnswer(int optionIndex) {
        return transcoding.get(optionIndex - 1) == jsonQuestion.getAnswer();
    }

    public String getTaskDefinition() {
        return jsonQuestion.getTask_def();
    }

    public int getState() { return state; }

    public List<Integer> getCellValues() { return jsonImage.getCell_values(); }

    public int getOrdIndexInQuiz() { return ordIndexInQuiz; }

    public JSON_Question getJsonQuestion() { return jsonQuestion; }

    public String getOptionText(int optionIndex) {
        int option = transcoding.get(optionIndex - 1);
        switch (option) {
            case 1:
                return jsonQuestion.getOption_1();
            case 2:
                return jsonQuestion.getOption_2();
            case 3:
                return jsonQuestion.getOption_3();
            case 4:
                return jsonQuestion.getOption_4();
            case 5:
                return jsonQuestion.getOption_5();
            case 6:
                return jsonQuestion.getOption_6();
            default:
                throw new IllegalStateException("Wrong option value: " + option);
        }
    }

    private void populateOtherOptions() {
        Random rng = new Random();
        while (transcoding.size() < TriviaCardConstants.NUM_OF_OPTIONS) {
            int random = rng.nextInt(TriviaCardConstants.NUM_OF_OPTIONS_JSON - 1) + 1;
            if (!transcoding.contains(random)) {
                transcoding.add(random);
            }
        }
    }
}
