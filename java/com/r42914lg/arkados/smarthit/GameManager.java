package com.r42914lg.arkados.smarthit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class GameManager {

    protected class GameParameters {
        protected int gameType;
        protected int gameTempoMillis;
        protected int gameTime;

        protected GameParameters(int gameType, int gameTempo) {
            this.gameType = gameType;
            this.gameTime = 0;
            switch (gameTempo) {
                case TEMPO_SLOW:
                    this.gameTempoMillis = activityRef.getResources().getInteger(R.integer.ROUND_TEMPO_DIFF_1);
                    break;
                case TEMPO_FAST:
                    this.gameTempoMillis = activityRef.getResources().getInteger(R.integer.ROUND_TEMPO_DIFF_2);
                    break;
                default:
                    throw new IllegalStateException("Wrong gameTempoCode: " + gameTempo);
            }
        }
    }

    public static final String CURRENT_LIST_INDEX_KEY = "currentListIndex";
    public static final String GAME_TIME_KEY = "gameTime";

    public static final int INIT_LIST_SIZE = 5;

    public static final int BASIC_SLOW = 11;
    public static final int BASIC_FAST = 12;
    public static final int OVERDRAFT_SLOW = 21;
    public static final int OVERDRAFT_FAST = 22;
    public static final int SHUFFLE_SLOW = 31;
    public static final int SHUFFLE_FAST = 32;
    public static final int SEQUENCE_SLOW = 41;
    public static final int SEQUENCE_FAST = 42;

    public static final int TEMPO_SLOW = 1;
    public static final int TEMPO_FAST = 2;

    public static final int TYPE_BASIC = 1;
    public static final int TYPE_OVERDRAFT = 2;
    public static final int TYPE_SHUFFLE = 3;
    public static final int TYPE_SEQUENCE = 4;

    private final SHBoardActivity activityRef;
    private final List<GameParameters> gameStepsList;
    private int currentListIndex;
    private SHGameLogic currentStepRef;

    public static int getSequenceCodeByUserInput(int gameType, int gameTempo) {
        return (gameType * 10) + gameTempo;
    }

    public GameManager(SHBoardActivity activityRef, int gameSequenceCode, Bundle savedInstanceState) {
        this(activityRef, gameSequenceCode);
        currentListIndex = savedInstanceState.getInt(CURRENT_LIST_INDEX_KEY);
        if (currentListIndex > -1) {
            currentListIndex--;
        }
        for (int i = 0; i < gameStepsList.size(); i++) {
            gameStepsList.get(i).gameTime = savedInstanceState.getInt(GAME_TIME_KEY + i);
        }
    }

    public GameManager(SHBoardActivity activityRef, int gameSequenceCode) {
        this.activityRef = activityRef;
        gameStepsList =  new ArrayList<>(INIT_LIST_SIZE);
        switch (gameSequenceCode) {
            case BASIC_SLOW:
                gameStepsList.add(new GameParameters(TYPE_BASIC, TEMPO_SLOW));
                break;
            case BASIC_FAST:
                gameStepsList.add(new GameParameters(TYPE_BASIC, TEMPO_FAST));
                break;
            case OVERDRAFT_SLOW:
                gameStepsList.add(new GameParameters(TYPE_OVERDRAFT, TEMPO_SLOW));
                break;
            case OVERDRAFT_FAST:
                gameStepsList.add(new GameParameters(TYPE_OVERDRAFT, TEMPO_FAST));
                break;
            case SHUFFLE_SLOW:
                gameStepsList.add(new GameParameters(TYPE_SHUFFLE, TEMPO_SLOW));
                break;
            case SHUFFLE_FAST:
                gameStepsList.add(new GameParameters(TYPE_SHUFFLE, TEMPO_FAST));
                break;
            case SEQUENCE_SLOW:
                gameStepsList.add(new GameParameters(TYPE_BASIC, TEMPO_SLOW));
                gameStepsList.add(new GameParameters(TYPE_OVERDRAFT, TEMPO_SLOW));
                gameStepsList.add(new GameParameters(TYPE_SHUFFLE, TEMPO_SLOW));
                break;
            case SEQUENCE_FAST:
                gameStepsList.add(new GameParameters(TYPE_BASIC, TEMPO_FAST));
                gameStepsList.add(new GameParameters(TYPE_OVERDRAFT, TEMPO_FAST));
                gameStepsList.add(new GameParameters(TYPE_SHUFFLE, TEMPO_FAST));
                break;
            default:
                throw new IllegalStateException("Wrong gameSequenceCode: " + gameSequenceCode);
        }
        this.currentListIndex = -1;
        if (gameSequenceCode >= SEQUENCE_SLOW) {
            activityRef.initAdInterstitial();
        }
    }

    public boolean hasNextStep() {
        return ((currentListIndex != -1) && (currentListIndex < gameStepsList.size() - 1)
                || (currentListIndex == -1) && (gameStepsList.size() > 0));
    }

    public void setCurrentGameTime(int time) {
        gameStepsList.get(currentListIndex).gameTime = time;
    }

    public int getCurrentGameTime() {
        return gameStepsList.get(currentListIndex).gameTime;
    }

    public int[] getAllTimesArray() {
        int[] allTimeArray = new int[TYPE_SEQUENCE];
        if (gameStepsList.size() == TYPE_SHUFFLE) {
            for (int i = 0; i < gameStepsList.size(); i++) {
                allTimeArray[i] = gameStepsList.get(i).gameTime;
                allTimeArray[TYPE_SEQUENCE - 1] += allTimeArray[i];
            }
        } else {
            allTimeArray[gameStepsList.get(0).gameType - 1] = gameStepsList.get(0).gameTime;
        }
        return allTimeArray;
    }

    public CharSequence getCurrentStepName() {
        return getGameTypeName(gameStepsList.get(currentListIndex).gameType);
    }

    public CharSequence getCurrentStepTempoName() {
        return getGameTempoName(gameStepsList.get(currentListIndex).gameTempoMillis);
    }

    public CharSequence getNextStepName() {
        return getGameTypeName(gameStepsList.get(currentListIndex + 1).gameType);
    }

    public void iterateToNextStep() {
        currentListIndex++;
        currentStepRef = null;
    }

    public SHGameLogic getCurrentStepRef() {
        if (currentStepRef == null) {
            switch (gameStepsList.get(currentListIndex).gameType) {
                case TYPE_BASIC:
                    currentStepRef = new SHGameLogicBasic(activityRef,
                            gameStepsList.get(currentListIndex).gameTempoMillis);
                    break;
                case TYPE_OVERDRAFT:
                    currentStepRef = new SHGameLogicOverdraft(activityRef,
                            gameStepsList.get(currentListIndex).gameTempoMillis);
                    break;
                case TYPE_SHUFFLE:
                    currentStepRef = new SHGameLogicShuffle(activityRef,
                            gameStepsList.get(currentListIndex).gameTempoMillis);
                    break;
                default:
                    throw new IllegalStateException("Wrong gameTypeCode: " +
                            gameStepsList.get(currentListIndex).gameType);
            }
        }
        return currentStepRef;
    }

    private CharSequence getGameTypeName(int gameType) {
        switch (gameType) {
            case TYPE_BASIC:
                return activityRef.getText(R.string.type_basic);
            case TYPE_OVERDRAFT:
                return activityRef.getText(R.string.type_overdraft);
            case TYPE_SHUFFLE:
                return activityRef.getText(R.string.type_shuffle);
        }
        return null;
    }

    private CharSequence getGameTempoName(int gameTempo) {
        if (gameTempo == activityRef.getResources().getInteger(R.integer.ROUND_TEMPO_DIFF_1)) {
            return activityRef.getText(R.string.difficulty_1);
        } else if (gameTempo == activityRef.getResources().getInteger(R.integer.ROUND_TEMPO_DIFF_2)) {
            return activityRef.getText(R.string.difficulty_2);
        } else {
            return null;
        }
    }

    protected void saveGameState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_LIST_INDEX_KEY, currentListIndex);
        for (int i = 0; i < gameStepsList.size(); i++) {
            outState.putInt(GAME_TIME_KEY + i, gameStepsList.get(i).gameTime);
        }
    }
}
