package com.r42914lg.arkados.smarthit;

import android.os.Parcel;
import android.os.Parcelable;

public class SHButtonState implements Parcelable {
    protected int passThruCount;
    protected int numOfFailures;
    protected int currentBackgroundColor;
    protected int currentPNGResID;
    protected int currentValueResID;
    protected int durationLeft;
    protected long timeFruitWasOpen;
    protected int flavour;
    protected int showState;
    protected int value;
    protected boolean thumbUp;

    protected SHButtonState() {}

    protected SHButtonState(Parcel in) {
        passThruCount = in.readInt();
        numOfFailures = in.readInt();
        currentBackgroundColor = in.readInt();
        currentPNGResID = in.readInt();
        currentValueResID = in.readInt();
        durationLeft = in.readInt();
        timeFruitWasOpen = in.readLong();
        flavour = in.readInt();
        showState = in.readInt();
        value = in.readInt();
        thumbUp = in.readByte() != 0;
    }

    public static final Creator<SHButtonState> CREATOR = new Creator<SHButtonState>() {
        @Override
        public SHButtonState createFromParcel(Parcel in) {
            return new SHButtonState(in);
        }

        @Override
        public SHButtonState[] newArray(int size) {
            return new SHButtonState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(passThruCount);
        dest.writeInt(numOfFailures);
        dest.writeInt(currentBackgroundColor);
        dest.writeInt(currentPNGResID);
        dest.writeInt(currentValueResID);
        dest.writeInt(durationLeft);
        dest.writeLong(timeFruitWasOpen);
        dest.writeInt(flavour);
        dest.writeInt(showState);
        dest.writeInt(value);
        dest.writeByte((byte) (thumbUp ? 1 : 0));
    }
}

