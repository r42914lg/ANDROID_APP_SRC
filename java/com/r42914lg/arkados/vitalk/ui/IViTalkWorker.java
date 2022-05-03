package com.r42914lg.arkados.vitalk.ui;

public interface IViTalkWorker {
    enum Mode {SOURCE, RECORD, PREVIEW}
    Mode getMode();
    void onRecordButtonEnabledFlag(boolean aBoolean);
    void onRecordSessionEndedFlag(boolean aBoolean);
    void onFirebaseUploadFinishedFlag(boolean aBoolean);
    void navigateToWorkItems();
    void onYouTubePlayHit();
}
