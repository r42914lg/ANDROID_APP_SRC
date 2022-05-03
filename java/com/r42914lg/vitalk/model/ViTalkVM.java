package com.r42914lg.arkados.vitalk.model;

import static android.content.Context.MODE_PRIVATE;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.r42914lg.arkados.vitalk.R;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViTalkVM extends AndroidViewModel {
    public static final String TAG = "LG> ViTalkVM";

    private String youtubeVideoId;
    private String dataSource;
    private boolean isOnline;
    private boolean firebaseAuthenticated;

    private final Map<String, Bitmap> imagesMap;

    private final LocalStorageHelper localStorageHelper;
    private final FirebaseHelper firebaseHelper;
    private final Set<String> favoriteIDs;
    private final List<WorkItemVideo> workItemVideoList;

    private final MutableLiveData<String> toastLiveData;
    private final MutableLiveData<String> youtubeVideoIdLiveData;
    private final MutableLiveData<Boolean> progressBarFlagLiveData;
    private final MutableLiveData<Boolean> recordSessionEndedFlagLiveData;
    private final MutableLiveData<FavoritesEvent> favoritesLiveData;
    private final MutableLiveData<RetryDialogEvent> retryDialogEventMutableLiveData;
    private final MutableLiveData<TerminateDialogEvent> terminateDialogEventMutableLiveData;
    private final MutableLiveData<Boolean> workItemsLoadedFlagLiveData;
    private final MutableLiveData<Integer> invalidateItemAtPositionLiveData;
    private final MutableLiveData<Boolean> firebaseUploadFinishedLiveData;
    private final MutableLiveData<GoogleSignInAccount> googleSignInLiveData;
    private final MutableLiveData<String> liveToolBarTitle;

    private final ExecutorService executorService;
    private final Handler mainThreadHandler;

    private final Application application;

    public ViTalkVM(@NonNull Application application) {
        super(application);

        this.application = application;

        executorService = Executors.newFixedThreadPool(10);
        mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        IDataLoaderListener listener = new IDataLoaderListener() {
            @Override
            public void callbackLoadImageFromURL(Bitmap youTubeImage, String youTubeId) {
                if (LOG) {
                    Log.d(TAG, ".callbackLoadImageFromURL ID --> " + youTubeId);
                }

                imagesMap.put(youTubeId, youTubeImage);
                int positionInAdapter = lookUpForPositionInAdapter(youTubeId);
                if (positionInAdapter >= 0) {
                    invalidateItemAtPositionLiveData.setValue(positionInAdapter);
                }
            }

            @Override
            public void callbackVideoTileReceived(String title, String youTubeId) {
                if (LOG) {
                    Log.d(TAG, ".callbackVideoTileReceived ID --> " + youTubeId);
                }

                int index = lookUpForIndexInList(youTubeId);
                if (index != -1) {
                    workItemVideoList.get(index).title = title;

                    int positionInAdapter = lookUpForPositionInAdapter(youTubeId);
                    if (positionInAdapter >= 0) {
                        invalidateItemAtPositionLiveData.setValue(positionInAdapter);
                    }
                }
            }

            @Override
            public void callbackFirebaseAuthenticated() {
                if (LOG) {
                    Log.d(TAG, ".callbackFirebaseAuthenticated");
                }

                firebaseAuthenticated = true;
            }

            @Override
            public void onFirebaseUploadFailed(String fullPath) {
                if (LOG) {
                    Log.d(TAG, ".onFirebaseUploadFailed to FULL PATH --> " + fullPath);
                }

                progressBarFlagLiveData.setValue(false);
                retryDialogEventMutableLiveData.setValue(new RetryDialogEvent(application.getString(R.string.dialog_upload_failed_title), application.getString(R.string.dialog_upload_failed_text)));
            }

            @Override
            public void onFirebaseUploadFinished(String youTubeId) {
                if (LOG) {
                    Log.d(TAG, ".onFirebaseUploadFinished ID --> " +  youTubeId);
                }

                setRecordExistFlag(youTubeId, true);
                localStorageHelper.storeWorkItems(workItemVideoList);
                progressBarFlagLiveData.setValue(false);
                notifyUIShowToast(application.getString(R.string.toast_upload_finished));
                firebaseUploadFinishedLiveData.setValue(true);
            }
        };

        youtubeVideoIdLiveData = new MutableLiveData<>();
        progressBarFlagLiveData = new MutableLiveData<>();
        recordSessionEndedFlagLiveData = new MutableLiveData<>();
        favoritesLiveData = new MutableLiveData<>();
        favoritesLiveData.setValue(new FavoritesEvent());
        workItemsLoadedFlagLiveData = new MutableLiveData<>();
        invalidateItemAtPositionLiveData = new MutableLiveData<>();
        firebaseUploadFinishedLiveData = new MutableLiveData<>();
        toastLiveData = new MutableLiveData<>();
        retryDialogEventMutableLiveData = new MutableLiveData<>();
        terminateDialogEventMutableLiveData = new MutableLiveData<>();
        googleSignInLiveData = new MutableLiveData<>();
        liveToolBarTitle = new MutableLiveData<>();

        firebaseHelper = new FirebaseHelper(listener);

        localStorageHelper = new LocalStorageHelper(listener);
        if (localStorageHelper.getPreferences() == null) {
            localStorageHelper.setSharedPreferences(application.getSharedPreferences("sharedPrefs", MODE_PRIVATE));
        }

        imagesMap =  new Hashtable<>();

        workItemVideoList = localStorageHelper.loadWorkItems();

        workItemsLoadedFlagLiveData.setValue(true);
        favoriteIDs = localStorageHelper.loadFavorites();

        if (LOG) {
            Log.d(TAG, " View Model instance created");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        firebaseHelper.onClear();
        localStorageHelper.onClear();

        if (LOG) {
            Log.d(TAG, ".onCleared");
        }
    }

    public Set<String> getFavoriteIDs() {
        return favoriteIDs;
    }

    private void notifyFavoritesExist(boolean doEnable) {
        FavoritesEvent event = favoritesLiveData.getValue();
        event.setEnableFavorites(doEnable);
        if (!doEnable) {
            event.setFavoritesChecked(false);
        }
        favoritesLiveData.setValue(event);
    }

    public void setGoogleAccount(GoogleSignInAccount credential) {
        if (LOG) {
            Log.d(TAG, ".setGoogleAccount --> " + credential.getDisplayName());
        }

        googleSignInLiveData.setValue(credential);
        liveToolBarTitle.setValue(application.getString(R.string.first_fragment_label).concat(" - ").concat(credential.getDisplayName()));
    }

    public void requestToolbarUpdate()  {
        GoogleSignInAccount account = googleSignInLiveData.getValue();
        if (account != null) {
            liveToolBarTitle.setValue(application.getString(R.string.first_fragment_label).concat(" - ").concat(account.getDisplayName()));
        }
    }

    public boolean checkIfFavorite(String quizId) {
        return favoriteIDs.contains(quizId);
    }

    public boolean crossCheckFavorites() {
        if (workItemVideoList != null) {
            for (WorkItemVideo workItemVideo : workItemVideoList) {
                if (favoriteIDs.contains(workItemVideo.youTubeId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void processFavoriteAdded(String quizId) {
        favoriteIDs.add(quizId);
        localStorageHelper.storeFavorites(favoriteIDs);
        notifyFavoritesExist(true);
    }

    public void processFavoriteRemoved(String quizId)  {
        favoriteIDs.remove(quizId);
        localStorageHelper.storeFavorites(favoriteIDs);
        notifyFavoritesExist(crossCheckFavorites());
    }

    public void setFavoritesChecked(boolean favoritesChecked) {
        FavoritesEvent event = favoritesLiveData.getValue();
        event.setFavoritesChecked(favoritesChecked);
        favoritesLiveData.setValue(event);
    }

    public void onPermissionsCheckPassed() {}
    public void onPermissionsCheckFailed() {
        terminateDialogEventMutableLiveData.setValue(new TerminateDialogEvent(application.getString(R.string.dialog_terminate_no_permissions_title), application.getString(R.string.dialog_terminate_no_permissions_text)));
    }

    public void setNetworkStatus(boolean isOnline) {
        this.isOnline = isOnline;
        if (isOnline) {
            loadYoutubeThumbnailsAndTitles();
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void onYouTubePlayerReady() {
        youtubeVideoIdLiveData.setValue(youtubeVideoId);
    }

    public void onVideoCued() {
        progressBarFlagLiveData.setValue(false);
    }

    public void onVideIdSelected(String youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
        setRecordExistFlag(youtubeVideoId, false);
        progressBarFlagLiveData.setValue(true);
    }

    public void onRecordSessionEnded(String dataSource) {
        if (LOG) {
            Log.d(TAG, ".onRecordSessionEnded");
        }

        recordSessionEndedFlagLiveData.setValue(true);
        if (!firebaseAuthenticated) {
            terminateDialogEventMutableLiveData.setValue(new TerminateDialogEvent(application.getString(R.string.dialog_terminate_no_firebase_title), application.getString(R.string.dialog_terminate_no_firebase_text)));
        }
        firebaseHelper.uploadAudioWithId(youtubeVideoId, googleSignInLiveData.getValue().getId(),dataSource);
        progressBarFlagLiveData.setValue(true);
    }

    public List<WorkItemVideo> getWorkItemVideoList() {
        return workItemVideoList;
    }

    public void addYouTubeIdToWorkItems(String youtubeVideoId) {
        if (lookUpForPositionInAdapter(youtubeVideoId) == -2) {
            if (LOG) {
                Log.d(TAG, ".addYouTubeIdToWorkItems: ID --> " + youtubeVideoId);
            }

            workItemVideoList.add(new WorkItemVideo(false, youtubeVideoId));
            localStorageHelper.loadImageFromURL(youtubeVideoId, executorService, mainThreadHandler);
            localStorageHelper.queryYouTubeTitleFromURL(youtubeVideoId, executorService, mainThreadHandler);
            localStorageHelper.storeWorkItems(workItemVideoList);
            workItemsLoadedFlagLiveData.setValue(true);
        } else {
            if (LOG) {
                Log.d(TAG, ".addYouTubeIdToWorkItems: Video in the list, delete first... ID --> " + youtubeVideoId);
            }

            notifyUIShowToast(application.getString(R.string.toast_video_exists) + youtubeVideoId);
        }
    }

    public void onWorkItemDeleted(String  youtubeVideoId) {
        int index = lookUpForIndexInList(youtubeVideoId);
        workItemVideoList.remove(index);
        localStorageHelper.storeWorkItems(workItemVideoList);
    }

    public String getGoogleAccId() {
        return googleSignInLiveData.getValue().getId();
    }

    public Bitmap lookupForBitmap(String imageId) {
        return imagesMap.get(imageId);
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public boolean checkImageLoaded(String imageId) {
        return imagesMap.containsKey(imageId);
    }

    public String getCurrentYoutubeId() {
        return youtubeVideoId;
    }

    private int lookUpForPositionInAdapter(String youTubeId) {
        for (int i = 0; i < workItemVideoList.size(); i++) {
            if (workItemVideoList.get(i).youTubeId.equals(youTubeId)) {
                return workItemVideoList.get(i).positionInAdapter;
            }
        }
        return -2;
    }

    private int lookUpForIndexInList(String youTubeId) {
        for (int i = 0; i < workItemVideoList.size(); i++) {
            if (workItemVideoList.get(i).youTubeId.equals(youTubeId)) {
                return i;
            }
        }
        return -1;
    }

    private void notifyUIShowToast(String text) {
        toastLiveData.setValue(text);
    }

    private void loadYoutubeThumbnailsAndTitles() {
        if (isOnline && workItemVideoList != null && !workItemVideoList.isEmpty()) {
            for (WorkItemVideo workItemVideo : workItemVideoList) {
                if (!imagesMap.containsKey(workItemVideo.youTubeId)) {
                    localStorageHelper.loadImageFromURL(workItemVideo.youTubeId, executorService, mainThreadHandler);
                    localStorageHelper.queryYouTubeTitleFromURL(workItemVideo.youTubeId, executorService, mainThreadHandler);
                }
            }
        }
    }

    private void setRecordExistFlag(String youtubeVideoId, boolean flag) {
        for (WorkItemVideo workItemVideo : workItemVideoList) {
            if (workItemVideo.youTubeId.equals(youtubeVideoId)) {
                workItemVideo.recordExists = flag;
                return;
            }
        }
    }

    public MutableLiveData<Boolean> getProgressBarFlagLiveData() { return progressBarFlagLiveData; }
    public MutableLiveData<Boolean> getRecordSessionEndedFlagLiveData() { return recordSessionEndedFlagLiveData; }
    public MutableLiveData<FavoritesEvent> getFavoritesLiveData() { return favoritesLiveData; }
    public MutableLiveData<Boolean> getWorkItemsLoadedFlagLiveData() { return workItemsLoadedFlagLiveData; }
    public MutableLiveData<Integer> getInvalidateItemAtPositionLiveData() { return invalidateItemAtPositionLiveData; }
    public MutableLiveData<Boolean>  getFirebaseUploadFinishedLiveData() { return firebaseUploadFinishedLiveData; }
    public MutableLiveData<String> getToastLiveData() { return toastLiveData; }
    public MutableLiveData<RetryDialogEvent> getDialogEventMutableLiveData() { return retryDialogEventMutableLiveData; }
    public MutableLiveData<TerminateDialogEvent> getTerminateDialogEventMutableLiveData() { return terminateDialogEventMutableLiveData; }
    public MutableLiveData<GoogleSignInAccount> getGoogleSignInLiveData() { return googleSignInLiveData; }
    public MutableLiveData<String> getLiveToolBarTitle() { return  liveToolBarTitle; }
}
