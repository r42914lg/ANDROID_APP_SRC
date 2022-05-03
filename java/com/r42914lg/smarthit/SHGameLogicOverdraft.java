package com.r42914lg.arkados.smarthit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class SHGameLogicOverdraft extends SHGameLogic {
    public static final String OVERDRAFT_SET_SIZE_KEY = "overdraftSetSize";
    public static final String OVERDRAFT_SET_PREFIX_KEY = "overdraftSet";

    protected final List<Integer> overdraftSet;

    public SHGameLogicOverdraft (SHBoardActivity boardActivity, int tempoInSeconds) {
        super(boardActivity, tempoInSeconds);
        int[] ints = shBoardActivity.getResources().getIntArray(R.array.VALUE_SET);
        this.overdraftSet = new ArrayList<>(ints.length);
    }

    @Override
    public void checkWin(SHImageButton shImageButton) {
        if (currentFruit == shImageButton.getFlavour()) {
            gamePoints += shImageButton.getValue();
        } else {
            gamePoints -= shImageButton.getValue();
            overdraftSet.add(shImageButton.getValue());
        }
        super.checkWin(shImageButton);
    }

    @Override
    protected void processExpired(SHImageButton shImageButton) {
        if (currentFruit != shImageButton.getFlavour()) {
            gamePoints -= shImageButton.getValue();
            overdraftSet.add(shImageButton.getValue());
        }
        super.processExpired(shImageButton);
    }

    @Override
    public boolean reValidateButtonsAndCheckIfGameOver(long chronometerTime) {
        if (super.reValidateButtonsAndCheckIfGameOver(chronometerTime)) {
            return true;
        } else {
            for (int i = 0; i < shBoardActivity.getResources().getInteger(R.integer.ROWS_COUNT); i++) {
                for (int j = 0; j < shBoardActivity.getResources().getInteger(R.integer.COLUMNS_COUNT); j++) {
                    if (!overdraftSet.isEmpty()
                            && playButtons[i][j].getShowState() == SHImageButton.BUTTON_STATE_SHOW_THUMB
                            && !playButtons[i][j].isThumbDown()) {
                        if (super.passThruAndRenew(playButtons[i][j], overdraftSet.get(0))) {
                            overdraftSet.remove(0);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void saveGameState(@NonNull @NotNull Bundle outState) {
        super.saveGameState(outState);
        outState.putInt(OVERDRAFT_SET_SIZE_KEY, overdraftSet.size());
        for (int i = 0; i < overdraftSet.size(); i++) {
            outState.putInt(OVERDRAFT_SET_PREFIX_KEY + i, overdraftSet.get(i));
        }
    }

    @Override
    protected void restoreGameState(@NonNull @NotNull Bundle savedInstanceState) {
        super.restoreGameState(savedInstanceState);
        int overdraftSetSize = savedInstanceState.getInt(OVERDRAFT_SET_SIZE_KEY);
        for (int i = 0; i < overdraftSetSize; i++) {
            overdraftSet.add(savedInstanceState.getInt(OVERDRAFT_SET_PREFIX_KEY + i));
        }
    }
}
