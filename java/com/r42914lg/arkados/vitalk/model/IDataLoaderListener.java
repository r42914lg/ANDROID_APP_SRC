package com.r42914lg.arkados.vitalk.model;

import android.graphics.Bitmap;

public interface IDataLoaderListener {
    void callbackLoadImageFromURL(Bitmap youTubeImage, String youTubeId);
    void callbackVideoTileReceived(String title, String youTubeId);
    void callbackFirebaseAuthenticated();
    void onFirebaseUploadFailed(String fullPath);
    void onFirebaseUploadFinished(String youTubeId);
}
