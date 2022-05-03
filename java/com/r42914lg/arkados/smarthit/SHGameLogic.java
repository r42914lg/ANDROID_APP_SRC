package com.r42914lg.arkados.smarthit;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class SHGameLogic {
    public static final String LAST_TICK_MILLIS_KEY = "lastTickInMilliseconds";
    public static final String GAME_POINTS_KEY = "gamePoints";
    public static final String SECOND_NEXT_FRUIT_KEY = "secondNextFruit";
    public static final String NEXT_FRUIT_KEY = "nextFruit";
    public static final String CURRENT_FRUIT_KEY = "currentFruit";
    public static final String OPEN_COUNT_KEY = "openCount";
    public static final String BUTTON_PREFIX_KEY = "b";
    public static final String FLAVOUR_SET_SIZE_KEY = "flavourSetSize";
    public static final String FLAVOUR_SET_PREFIX_KEY = "flavourSet";
    public static final String VALUE_SET_SIZE_KEY = "valueSetSize";
    public static final String VALUE_SET_PREFIX_KEY = "valueSet";

    private final List<Integer> valueSet;
    private final List<Integer> flavourSet;

    public int openCount;
    protected int currentFruit;
    protected int nextFruit;
    protected int secondNextFruit;
    protected int gamePoints;
    protected long lastTickInMilliseconds;
    protected final int tempo;
    private final int maxOpenFruits;

    protected final SHBoardActivity shBoardActivity;
    protected final Random rng = new Random();
    protected final SHImageButton[][] playButtons;

    @SuppressLint("ClickableViewAccessibility")
    protected SHGameLogic(SHBoardActivity boardActivity, int tempoInSeconds) {
        this.shBoardActivity = boardActivity;
        this.tempo = tempoInSeconds;

        if (tempoInSeconds == shBoardActivity.getResources().getInteger(R.integer.ROUND_TEMPO_DIFF_1))
            this.maxOpenFruits = shBoardActivity.getResources().getInteger(R.integer.MAX_OPEN_FRUITS_DIFF_1);
        else if (tempoInSeconds == shBoardActivity.getResources().getInteger(R.integer.ROUND_TEMPO_DIFF_2))
            this.maxOpenFruits = shBoardActivity.getResources().getInteger(R.integer.MAX_OPEN_FRUITS_DIFF_2);
        else
            throw new IllegalStateException("Unexpected tempo: " + tempoInSeconds);

        int[] ints = shBoardActivity.getResources().getIntArray(R.array.VALUE_SET);
        this.valueSet = new ArrayList<>(ints.length);
        this.flavourSet = new ArrayList<>(ints.length);
        this.playButtons = new SHImageButton[shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT)][shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT)];

        for (int i=0; i < shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT); i++) {
            for (int j=0; j < shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT); j++) {
                playButtons[i][j] = shBoardActivity.findViewById(
                        shBoardActivity.getResources().
                                getIdentifier("button_" + i + j, "id",
                                        shBoardActivity.getPackageName()));
                playButtons[i][j].setOnTouchListener(boardActivity);
                playButtons[i][j].setShGameLogic(this);
                shBoardActivity.sizePlayButton(playButtons[i][j], 1);
            }
        }
    }

    public void initializeGameState() {
        int[] ints = shBoardActivity.getResources().getIntArray(R.array.VALUE_SET);
        for (int i : ints) {
            valueSet.add(i);
        }

        for (int i=0; i < shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT); i++) {
            for (int j=0; j < shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT); j++) {
                renewButton(playButtons[i][j]);
                playButtons[i][j].redraw();
            }
        }
        currentFruit = getRandomFruit(true, -1);
        nextFruit = getRandomFruit(true,-1);
        secondNextFruit = getRandomFruit(true,-1);
        shBoardActivity.updateScoreView(gamePoints);
        shBoardActivity.updateFruitsView(SHImageButton.getFlavourResID(currentFruit),
                SHImageButton.getFlavourResID(nextFruit),
                SHImageButton.getFlavourResID(secondNextFruit));
    }

    public int getGamePoints() {
        return gamePoints;
    }
    public int getMaxOpenFruits() {
        return maxOpenFruits;
    }

    protected void checkWin(SHImageButton shImageButton) {
        shBoardActivity.updateScoreView(gamePoints);
        if (currentFruit == shImageButton.getFlavour()) {
            shImageButton.setThumb(true);
        } else {
            shImageButton.setThumb(false);
            valueSet.add(shImageButton.getValue());
        }
        flavourSet.remove((Integer) shImageButton.getFlavour());
        shImageButton.redraw();
    }

    public boolean reValidateButtonsAndCheckIfGameOver(long chronometerTime) {
        if (gamePoints >= shBoardActivity.getResources().getInteger(R.integer.TARGET_SCORE)) {
            return true;
        } else {
            for (int i = 0; i < shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT); i++) {
                for (int j = 0; j < shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT); j++) {
                    if (!playButtons[i][j].isThumbDown() && playButtons[i][j].checkIfExpired(chronometerTime)) {
                        processExpired(playButtons[i][j]);
                    }
                    if (playButtons[i][j].adjustBackgroundToDurationLeft()) {
                        playButtons[i][j].redraw();
                    }
                    if (playButtons[i][j].isThumbDown()) {
                        passThruAndRenew(playButtons[i][j], -1);
                    }
                }
            }
            if (lastTickInMilliseconds == 0) {
                lastTickInMilliseconds = chronometerTime;
            }
            if (chronometerTime - lastTickInMilliseconds >= tempo) {
                currentFruit = nextFruit;
                nextFruit = secondNextFruit;
                lastTickInMilliseconds = chronometerTime;
                secondNextFruit = getRandomFruit(true, secondNextFruit);
                shBoardActivity.updateFruitsView(SHImageButton.getFlavourResID(currentFruit),
                        SHImageButton.getFlavourResID(nextFruit),
                        SHImageButton.getFlavourResID(secondNextFruit));
            }
            return false;
        }
    }

    protected void processExpired(SHImageButton shImageButton) {
        shImageButton.setThumb(false);
        flavourSet.remove((Integer) shImageButton.getFlavour());
        valueSet.add(shImageButton.getValue());
        shImageButton.redraw();
        shBoardActivity.updateScoreView(gamePoints);
    }

    protected boolean passThruAndRenew(SHImageButton shImageButton, int overDraftValue) {
        if (shImageButton.checkPassThruCount() <= 0) {
            if (overDraftValue != -1) {
                renewButton(shImageButton, overDraftValue);
                shImageButton.redraw();
            } else {
                if (!valueSet.isEmpty()) {
                    renewButton(shImageButton);
                    shImageButton.redraw();
                }
            }
            return true;
        } else {
            shImageButton.decrementPassThruCount();
            return false;
        }
    }

    protected void renewButton(SHImageButton shImageButton) {
        renewButton(shImageButton, getRandomValue());
    }

    protected void renewButton(SHImageButton shImageButton, int overDraftValue) {
        int fruitToSet = getRandomFruit(false, -1);
        int durationToSet = getRandomDuration(shImageButton.getNumOfFailures(), overDraftValue);
        flavourSet.add(fruitToSet);
        shImageButton.setAttributes(
                durationToSet,
                fruitToSet,
                overDraftValue
        );
    }

    protected int getRandomValue() {
        int valueToReturn;
        Collections.shuffle(valueSet);
        valueToReturn = valueSet.get(0);
        valueSet.remove((Integer) valueToReturn);
        return valueToReturn;
    }
    protected int getRandomDuration(int numOfFailures, int value) {
        if (value == shBoardActivity.getResources().getInteger(R.integer.MAX_VALUE)) {
            return shBoardActivity.getResources().getInteger(R.integer.DURATION_VALUE_1);
        }

        int toReturn;
        int bound = Math.max((shBoardActivity.getResources().getInteger(R.integer.DURATION_COUNT) - numOfFailures), 1);
        int randomValue  = rng.nextInt(bound) + 1;

        switch (randomValue) {
            case 1:
                toReturn = shBoardActivity.getResources().getInteger(R.integer.DURATION_VALUE_1);
                break;
            case 2:
                toReturn = shBoardActivity.getResources().getInteger(R.integer.DURATION_VALUE_2);
                break;
            case 3:
                toReturn = shBoardActivity.getResources().getInteger(R.integer.DURATION_VALUE_3);
                break;
            case 4:
                toReturn = shBoardActivity.getResources().getInteger(R.integer.DURATION_VALUE_4);
                break;
            default:
                throw new IllegalStateException("Check num of failures: " + numOfFailures);
        }

        return toReturn;
    }

    protected int getRandomFruit(boolean isTarget, int currentValue) {
        int fruitToReturn = rng.nextInt(shBoardActivity.getResources().getInteger(R.integer.FRUITS_COUNT)) + 1;
        if (isTarget) {
            if (flavourSet.isEmpty()) {
                fruitToReturn = currentValue;
            } else {
                Collections.shuffle(flavourSet);
                fruitToReturn = flavourSet.get(0);
            }
        }
        return fruitToReturn;
    }

    protected void saveGameState(@NonNull Bundle outState) {
        outState.putInt(OPEN_COUNT_KEY, openCount);
        outState.putInt(CURRENT_FRUIT_KEY, currentFruit);
        outState.putInt(NEXT_FRUIT_KEY, nextFruit);
        outState.putInt(SECOND_NEXT_FRUIT_KEY, secondNextFruit);
        outState.putInt(GAME_POINTS_KEY, gamePoints);
        outState.putLong(LAST_TICK_MILLIS_KEY, lastTickInMilliseconds);

        for (int i = 0; i < shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT); i++) {
            for (int j = 0; j < shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT); j++) {
                outState.putParcelable(BUTTON_PREFIX_KEY + i + ""+ j, playButtons[i][j].shButtonState);
            }
        }

        outState.putInt(FLAVOUR_SET_SIZE_KEY, flavourSet.size());
        for (int i = 0; i < flavourSet.size(); i++) {
            outState.putInt(FLAVOUR_SET_PREFIX_KEY + i, flavourSet.get(i));
        }

        outState.putInt(VALUE_SET_SIZE_KEY, valueSet.size());
        for (int i = 0; i < valueSet.size(); i++) {
            outState.putInt(VALUE_SET_PREFIX_KEY + i, valueSet.get(i));
        }
    }

    protected void restoreGameState(@NonNull Bundle savedInstanceState) {
        lastTickInMilliseconds = savedInstanceState.getLong(LAST_TICK_MILLIS_KEY);
        gamePoints = savedInstanceState.getInt(GAME_POINTS_KEY);
        secondNextFruit = savedInstanceState.getInt(SECOND_NEXT_FRUIT_KEY);
        nextFruit = savedInstanceState.getInt(NEXT_FRUIT_KEY);
        currentFruit = savedInstanceState.getInt(CURRENT_FRUIT_KEY);
        openCount = savedInstanceState.getInt(OPEN_COUNT_KEY);

        shBoardActivity.updateScoreView(gamePoints);
        shBoardActivity.updateFruitsView(SHImageButton.getFlavourResID(currentFruit),
                SHImageButton.getFlavourResID(nextFruit),
                SHImageButton.getFlavourResID(secondNextFruit));

        for (int i = 0; i < shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT); i++) {
            for (int j = 0; j < shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT); j++) {
                playButtons[i][j].shButtonState = savedInstanceState.getParcelable(BUTTON_PREFIX_KEY + i + "" + j);
                playButtons[i][j].redraw();
            }
        }

        int flavourSetSize = savedInstanceState.getInt(FLAVOUR_SET_SIZE_KEY);
        for (int i = 0; i < flavourSetSize; i++) {
            flavourSet.add(savedInstanceState.getInt(FLAVOUR_SET_PREFIX_KEY + i));
        }

        int valueSetSize = savedInstanceState.getInt(VALUE_SET_SIZE_KEY);
        for (int i = 0; i < valueSetSize; i++) {
            valueSet.add(savedInstanceState.getInt(VALUE_SET_PREFIX_KEY + i));
        }
    }
}
