package com.r42914lg.arkados.smarthit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.Toast;

import java.text.MessageFormat;

@SuppressLint("AppCompatCustomView")
public class SHImageButton extends ImageButton {
    public static final int DURATION_VALUE_1_COLOR = R.drawable.background_value1;
    public static final int DURATION_VALUE_2_COLOR = R.drawable.background_value2;
    public static final int DURATION_VALUE_3_COLOR = R.drawable.background_value3;
    public static final int DURATION_VALUE_4_COLOR = R.drawable.background_value4;

    public static final int FRUIT_1_RES_ID = R.drawable.apple;
    public static final int FRUIT_2_RES_ID = R.drawable.apricot;
    public static final int FRUIT_3_RES_ID = R.drawable.banana;
    public static final int FRUIT_4_RES_ID = R.drawable.cherry;
    public static final int FRUIT_5_RES_ID = R.drawable.kiwi;
    public static final int FRUIT_6_RES_ID = R.drawable.lemon;
    public static final int FRUIT_7_RES_ID = R.drawable.pear;
    public static final int FRUIT_8_RES_ID = R.drawable.strawberry;
    public static final int FRUIT_9_RES_ID = R.drawable.tomato;

    public static final int VALUE_1_RES_ID = R.drawable.digit1;
    public static final int VALUE_2_RES_ID = R.drawable.digit2;
    public static final int VALUE_3_RES_ID = R.drawable.digit3;
    public static final int VALUE_4_RES_ID = R.drawable.digit4;
    public static final int VALUE_5_RES_ID = R.drawable.digit5;
    public static final int VALUE_6_RES_ID = R.drawable.digit6;
    public static final int VALUE_7_RES_ID = R.drawable.digit7;
    public static final int VALUE_8_RES_ID = R.drawable.digit8;
    public static final int VALUE_9_RES_ID = R.drawable.digit9;

    public static final int THUMB_UP_RES_ID = R.drawable.thumb_up;
    public static final int THUMB_DOWN_RES_ID = R.drawable.thumb_down;

    private static final int BUTTON_STATE_SHOW_FRUIT = 0;
    private static final int BUTTON_STATE_SHOW_VALUE = 1;
    public static final int BUTTON_STATE_SHOW_THUMB = 2;

    private SHGameLogic shGameLogic;
    protected SHButtonState shButtonState;

    public SHImageButton(Context context) {
        super(context);
        shButtonState = new SHButtonState();
    }
    public SHImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        shButtonState = new SHButtonState();
    }
    public SHImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        shButtonState = new SHButtonState();
    }

    public static int getFlavourResID(int flavour) {
        int flavourResID;
        switch (flavour) {
            case 1:
                flavourResID = FRUIT_1_RES_ID;
                break;
            case 2:
                flavourResID = FRUIT_2_RES_ID;
                break;
            case 3:
                flavourResID = FRUIT_3_RES_ID;
                break;
            case 4:
                flavourResID = FRUIT_4_RES_ID;
                break;
            case 5:
                flavourResID = FRUIT_5_RES_ID;
                break;
            case 6:
                flavourResID = FRUIT_6_RES_ID;
                break;
            case 7:
                flavourResID = FRUIT_7_RES_ID;
                break;
            case 8:
                flavourResID = FRUIT_8_RES_ID;
                break;
            case 9:
                flavourResID = FRUIT_9_RES_ID;
                break;
            default:
                throw new IllegalStateException("Unexpected FRUIT: " + flavour);
        }
        return flavourResID;
    }

    public int checkPassThruCount() { return shButtonState.passThruCount; }
    public int getFlavour() {
        return shButtonState.flavour;
    }
    public int getValue() {
        return shButtonState.value;
    }
    public boolean isThumbDown() { return !shButtonState.thumbUp; }
    public int getNumOfFailures() { return shButtonState.numOfFailures; }
    public int getShowState () { return shButtonState.showState; }
    public SHButtonState getState() { return shButtonState; }
    public void setState(SHButtonState shButtonState) { this.shButtonState = shButtonState; }

    public void setShGameLogic(SHGameLogic shGameLogic) {
        this.shGameLogic = shGameLogic;
    }

    private boolean adjustBackgroundBasedOnDuration(int durationToShow) {
        int oldBackgroundColor = shButtonState.currentBackgroundColor;
        if (durationToShow <= getResources().getInteger(R.integer.DURATION_VALUE_1))
            shButtonState.currentBackgroundColor = DURATION_VALUE_1_COLOR;
        else if (durationToShow <= getResources().getInteger(R.integer.DURATION_VALUE_2))
            shButtonState.currentBackgroundColor = DURATION_VALUE_2_COLOR;
        else if (durationToShow <= getResources().getInteger(R.integer.DURATION_VALUE_3))
                shButtonState.currentBackgroundColor = DURATION_VALUE_3_COLOR;
        else if (durationToShow <= getResources().getInteger(R.integer.DURATION_VALUE_4))
                    shButtonState.currentBackgroundColor = DURATION_VALUE_4_COLOR;
        return oldBackgroundColor != shButtonState.currentBackgroundColor;
    }

    public boolean adjustBackgroundToDurationLeft() {
        return adjustBackgroundBasedOnDuration(shButtonState.durationLeft);
    }

    public void setAttributes(int durationToShow,int flavour, int value) {
        shButtonState.flavour = flavour;
        shButtonState.value = value;
        shButtonState.thumbUp = true;
        shButtonState.showState = BUTTON_STATE_SHOW_VALUE;
        shButtonState.passThruCount = getResources().getInteger(R.integer.PASS_THRU_COUNT);

        adjustBackgroundBasedOnDuration(durationToShow);
        shButtonState.durationLeft = durationToShow;
        shButtonState.currentPNGResID = getFlavourResID(flavour);

        switch (value) {
            case 1:
                shButtonState.currentValueResID = VALUE_1_RES_ID;
                break;
            case 2:
                shButtonState.currentValueResID = VALUE_2_RES_ID;
                break;
            case 3:
                shButtonState.currentValueResID = VALUE_3_RES_ID;
                break;
            case 4:
                shButtonState.currentValueResID = VALUE_4_RES_ID;
                break;
            case 5:
                shButtonState.currentValueResID = VALUE_5_RES_ID;
                break;
            case 6:
                shButtonState.currentValueResID = VALUE_6_RES_ID;
                break;
            case 7:
                shButtonState.currentValueResID = VALUE_7_RES_ID;
                break;
            case 8:
                shButtonState.currentValueResID = VALUE_8_RES_ID;
                break;
            case 9:
                shButtonState.currentValueResID = VALUE_9_RES_ID;
                break;
            default:
                throw new IllegalStateException("Unexpected VALUE: " + value);
        }
    }

    public boolean checkIfExpired (long chronometerTime) {
        if (shButtonState.showState == BUTTON_STATE_SHOW_FRUIT) {
            shButtonState.durationLeft -= chronometerTime - shButtonState.timeFruitWasOpen;
            shButtonState.timeFruitWasOpen = chronometerTime;
        }
        return shButtonState.durationLeft <= 0;
    }

    public void decrementPassThruCount() {
        --shButtonState.passThruCount;
    }

    public void setThumb(boolean isThumbUp) {
        if (!isThumbUp) {
            shButtonState.numOfFailures++;
        }
        shButtonState.thumbUp = isThumbUp;
        shButtonState.showState = BUTTON_STATE_SHOW_THUMB;
        --shGameLogic.openCount;
    }

    public void redraw() {
        switch (shButtonState.showState) {
            case BUTTON_STATE_SHOW_FRUIT:
                setBackgroundResource(shButtonState.currentBackgroundColor);
                setImageResource(shButtonState.currentPNGResID);
                break;
            case BUTTON_STATE_SHOW_VALUE:
                setBackgroundResource(shButtonState.currentBackgroundColor);
                setImageResource(shButtonState.currentValueResID);
                break;
            case BUTTON_STATE_SHOW_THUMB:
                setBackgroundResource(R.drawable.background_empty);
                setImageResource((shButtonState.thumbUp) ? THUMB_UP_RES_ID : THUMB_DOWN_RES_ID);
                break;
        }
    }

    public void processClick(long chronometerTime) {
        if (shButtonState.showState != BUTTON_STATE_SHOW_THUMB) {
            if (shButtonState.showState == BUTTON_STATE_SHOW_FRUIT) {
                shButtonState.showState = BUTTON_STATE_SHOW_VALUE;
                shButtonState.durationLeft -= chronometerTime - shButtonState.timeFruitWasOpen;
                --shGameLogic.openCount;
            } else {
                if (shGameLogic.openCount < shGameLogic.getMaxOpenFruits()) {
                    shButtonState.showState = BUTTON_STATE_SHOW_FRUIT;
                    shButtonState.timeFruitWasOpen = chronometerTime;
                    ++shGameLogic.openCount;
                } else {
                    Toast.makeText(
                            getContext(),
                            MessageFormat.format(String.valueOf(getResources().getText(R.string.openExceedDialog)), shGameLogic.getMaxOpenFruits()),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
            redraw();
        }
    }

    public void processDoubleClick() {
        if (shButtonState.showState == BUTTON_STATE_SHOW_FRUIT) {
            shGameLogic.checkWin(this);
            shButtonState.showState = BUTTON_STATE_SHOW_THUMB;
        }
    }
}