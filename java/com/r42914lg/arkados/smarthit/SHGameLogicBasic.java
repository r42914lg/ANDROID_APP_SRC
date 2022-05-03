package com.r42914lg.arkados.smarthit;

public class SHGameLogicBasic extends SHGameLogic {

    public SHGameLogicBasic(SHBoardActivity boardActivity, int tempoInSeconds) {
        super(boardActivity, tempoInSeconds);
    }

    @Override
    public void checkWin(SHImageButton shImageButton) {
        if (currentFruit == shImageButton.getFlavour()) {
            gamePoints += shImageButton.getValue();
        }
        super.checkWin(shImageButton);
    }

}
