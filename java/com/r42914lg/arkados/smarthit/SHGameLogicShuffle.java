package com.r42914lg.arkados.smarthit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

public class SHGameLogicShuffle extends SHGameLogicOverdraft {
    public static final String LAST_SHUFFLE_TIME_KEY = "lastShuffleTime";
    private long lastShuffleTime;

    public SHGameLogicShuffle(SHBoardActivity boardActivity, int tempoInSeconds) {
        super(boardActivity, tempoInSeconds);
    }

    @Override
    public boolean reValidateButtonsAndCheckIfGameOver(long chronometerTime) {
        if (lastShuffleTime == 0) {
            lastShuffleTime = chronometerTime;
        }
        if (super.reValidateButtonsAndCheckIfGameOver(chronometerTime)) {
            return true;
        } else {
            if ((chronometerTime - lastShuffleTime) > shBoardActivity.getResources().getInteger(R.integer.SHUFFLE_AFTER_MILLIS)) {
                doShuffle();
                lastShuffleTime = chronometerTime;
            }
            return false;
        }
    }

    private void doShuffle() {
        for (int i = shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT) - 1; i >= 0; i--) {
            for (int j = shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT) - 1; j >= 0; j--) {
                int m = rng.nextInt(i + 1);
                int n = rng.nextInt(j + 1);
                SHButtonState temp = playButtons[i][j].getState();
                playButtons[i][j].setState(playButtons[m][n].getState());
                playButtons[m][n].setState(temp);
                playButtons[i][j].redraw();
            }
        }
    }

    @Override
    protected void saveGameState(@NonNull @NotNull Bundle outState) {
        super.saveGameState(outState);
        outState.putLong(LAST_SHUFFLE_TIME_KEY, lastShuffleTime);
    }

    @Override
    protected void restoreGameState(@NonNull @NotNull Bundle savedInstanceState) {
        super.restoreGameState(savedInstanceState);
        lastShuffleTime = savedInstanceState.getLong(LAST_SHUFFLE_TIME_KEY);
    }
}
