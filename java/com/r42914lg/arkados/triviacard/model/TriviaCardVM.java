package com.r42914lg.arkados.triviacard.model;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.os.HandlerCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.billingclient.api.Purchase;
import com.r42914lg.arkados.triviacard.R;

import static android.content.Context.MODE_PRIVATE;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_ACTIVE;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_CANCELLED;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_EXPIRED;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_GRACE;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_ON_HOLD;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_PAUSED;

import java.time.LocalDate;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TriviaCardVM extends AndroidViewModel {
    public static final String TAG = "LG> TriviaCardVM";

    private final ExecutorService executorService;
    private final Handler mainThreadHandler;

    private final FirebaseHelper firebaseHelper;
    private final LocalStorageHelper localStorageHelper;

    private final BitmapFactory.Options options;
    private final Map<String, Bitmap> imagesMap;
    private int imageReqWidth;
    private int imageReqHeight;
    private int iconReqWidth;
    private int iconReqHeight;

    private final Set<String> favoriteQuizIDs;
    private List<JSON_Quiz> jsonQuizList;
    private List<JSON_Quiz> jsonQuizListLocal;
    private TriviaCardQuiz currentQuiz;

    private boolean quizzesListRequestInProgress;
    private boolean isOnlineAtStart;
    private boolean checkInternetFlag;
    private boolean firebaseIsAuthenticatedFlag;
    private boolean hasPendingRequestForOnlineList;
    private boolean disableLoadQuiz;

    private String currentPurchaseToken;

    private final MutableLiveData<Quiz_VM_EVENT> quizLiveData;
    private final MutableLiveData<UI_VM_EVENT> uiLiveData;
    private final MutableLiveData<String> toolBarTitleLiveData;
    private final MutableLiveData<String> needAcknowledgeFlagLiveData;

    public TriviaCardVM(Application application) {
        super(application);

        checkInternetFlag = true;

        executorService = Executors.newFixedThreadPool(10);
        mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        IDataLoaderListener listener = new IDataLoaderListener() {
            @Override
            public void callbackPurchaseTokenLegitimate_200(Purchase purchase, long expiryTimeMillis, int paymentState, boolean autoRenewing) {
                if (LOG) {
                    Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 [expiry:paymentState:autoRenewing]: " +  expiryTimeMillis + ":" + paymentState + ":" + autoRenewing);
                }

                localStorageHelper.storeExpirationDate(expiryTimeMillis);

                if (expiryTimeMillis  >= System.currentTimeMillis() && paymentState == 1 && autoRenewing) { // ACTIVE
                    notifyUIHideAdsMenu(true);
                    if ((purchase != null && !purchase.isAcknowledged()) || !localStorageHelper.checkIfPurchaseTokenAcknowledged()) {
                        needAcknowledgeFlagLiveData.setValue("TRUE");
                    }
                    notifyUIShowToast(application.getString(R.string.toast_sub_verified_successfully));
                    localStorageHelper.storeSubscriptionState(SUBSCRIPTION_STATE_ACTIVE);

                    if (LOG) {
                        Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 --> ACTIVE");
                    }
                } else if (expiryTimeMillis  >= System.currentTimeMillis() && paymentState == 1 && !autoRenewing) { // CANCELLED
                    notifyUIHideAdsMenu(true);
                    localStorageHelper.storeSubscriptionState(SUBSCRIPTION_STATE_CANCELLED);
                    if (localStorageHelper.checkExpireApproaching() && localStorageHelper.checkIfReminderNotCalledToday()) {
                        localStorageHelper.markReminderTodayCalled();
                        localStorageHelper.storeSubscriptionValidationDate(false);
                        notifyUIShowFixSubscriptionDialog(application.getString(R.string.dialog_sub_about__to_expire_title),
                                application.getString(R.string.dialog_sub_about_to_expire_text));
                    }

                    if (LOG) {
                        Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 --> CANCELLED");
                    }
                } else if (expiryTimeMillis  >= System.currentTimeMillis() && paymentState == 0 && autoRenewing) { // IN GRACE
                    notifyUIHideAdsMenu(true);
                    localStorageHelper.storeSubscriptionState(SUBSCRIPTION_STATE_GRACE);
                    if (localStorageHelper.checkIfReminderNotCalledToday()) {
                        localStorageHelper.markReminderTodayCalled();
                        localStorageHelper.storeSubscriptionValidationDate(false);
                        notifyUIShowFixSubscriptionDialog(application.getString(R.string.dialog_sub_in_grace_title),
                                application.getString(R.string.dialog_sub_in_grace_text));
                    }

                    if (LOG) {
                        Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 --> IN GRACE");
                    }
                } else if (expiryTimeMillis  < System.currentTimeMillis() && paymentState == 0 && autoRenewing) { // ON HOLD
                    notifyUIHideAdsMenu(false);
                    notifyUIDisableAdsMenu(false, true);
                    localStorageHelper.storeSubscriptionState(SUBSCRIPTION_STATE_ON_HOLD);

                    if (LOG) {
                        Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 --> ON HOLD");
                    }
                } else if (expiryTimeMillis  < System.currentTimeMillis() && paymentState == 1 && autoRenewing) { // PAUSED
                    notifyUIHideAdsMenu(false);
                    notifyUIDisableAdsMenu(false, true);
                    localStorageHelper.storeSubscriptionState(SUBSCRIPTION_STATE_PAUSED);

                    if (LOG) {
                        Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 --> PAUSED");
                    }
                } else { // EXPIRED
                    notifyUIHideAdsMenu(false);
                    notifyUIDisableAdsMenu(false, true);
                    localStorageHelper.storeSubscriptionState(SUBSCRIPTION_STATE_EXPIRED);

                    if (LOG) {
                        Log.d(TAG, ".callbackPurchaseTokenLegitimate_200 --> EXPIRED");
                    }
                }
            }

            @Override
            public void callbackPurchaseTokenLegitimate_500(Purchase purchase) {
                currentPurchaseToken = null;
                clearStoredQuiz();
                notifyUIHideAdsMenu(false);
                notifyUIDisableAdsMenu(false, true);
                notifyUIShowToast(application.getString(R.string.toast_sub_not_valid));

                if (LOG) {
                    Log.d(TAG, ".callbackPurchaseTokenLegitimate_500");
                }
            }

            @Override
            public void callbackPurchaseTokenLegitimate_FAILURE(Purchase purchase) {
                notifyUIHideAdsMenu(true);
                notifyUIShowToast(application.getString(R.string.toast_cannot_verify));

                if (LOG) {
                    Log.d(TAG, ".callbackPurchaseTokenLegitimate_FAILURE");
                }
            }

            @Override
            public void callbackActiveQuizzesList(List<JSON_Quiz> jsonQuizList) {
                if (LOG) {
                    Log.d(TAG, ".callbackActiveQuizzesList: quizzes received");
                }
                TriviaCardVM.this.jsonQuizList = jsonQuizList;
                Quiz_VM_EVENT event = quizLiveData.getValue();
                if (quizzesListRequestInProgress) {
                    event.setQuizListLoadedFlag(true);
                    quizLiveData.setValue(event);
                    quizzesListRequestInProgress = false;
                }
                if (crossCheckFavorites()) {
                    notifyFavoritesExist(true);
                }
            }

            @Override
            public void callbackActiveQuestionsGivenQuiz(List<JSON_Question> jsonQuestions) {
                if (LOG) {
                    Log.d(TAG, ".callbackActiveQuestionsGivenQuiz: quizInProgress = " + currentQuiz.checkIfQuizInProgress());
                }
                if (currentQuiz.checkIfQuizInProgress()) {
                    currentQuiz.setJsonQuestions(jsonQuestions);
                }
            }

            @Override
            public void callbackImageById(JSON_Image jsonImage, TriviaCardQuestion triviaCardQuestion) {
                if (LOG) {
                    Log.d(TAG, ".callbackImageById: quizInProgress = " + currentQuiz.checkIfQuizInProgress());
                }
                if (currentQuiz.checkIfQuizInProgress()) {
                    triviaCardQuestion.setJsonImage(jsonImage);
                }
            }

            @Override
            public void callbackLoadImageFromFullPath(byte[] bytes, String key) {
                imagesMap.put(key, decodeSampledBitmapFromResource(bytes, imageReqWidth, imageReqHeight));
                doCallbackLoadImageFromFullPath();
            }

            @Override
            public void callbackLoadIconFromFullPath(byte[] bytes, String key, JSON_Quiz jsonQuiz) {
                if (LOG) {
                    Log.d(TAG, ".callbackLoadIconFromFullPath");
                }
                imagesMap.put(key, decodeSampledBitmapFromResource(bytes, iconReqWidth, iconReqHeight));
                Quiz_VM_EVENT event = quizLiveData.getValue();
                event.setQuizForIconLoaded(jsonQuiz);
                quizLiveData.setValue(event);
            }

            @Override
            public void callbackFirebaseAuthenticated() {
                firebaseIsAuthenticatedFlag = true;
                if (hasPendingRequestForOnlineList) {
                    firebaseHelper.subscribeForQuizzesList(TriviaCardVM.this);
                }
            }

            @Override
            public void handleSchemaMismatch() {
                Quiz_VM_EVENT event = quizLiveData.getValue();
                event.setSchemaMismatchIdentifiedFlag(true);
                quizLiveData.setValue(event);
            }
        };

        localStorageHelper = new LocalStorageHelper(application, listener);
        firebaseHelper = new FirebaseHelper(localStorageHelper, listener);
        options = new BitmapFactory.Options();
        imagesMap =  new Hashtable<>();

        if (localStorageHelper.getPreferences() == null) {
            localStorageHelper.setSharedPreferences(application.getSharedPreferences("sharedPrefs", MODE_PRIVATE));
            if (LOG) {
                Log.d(TAG, ": shared preferences set");
            }
        }

        quizLiveData = new MutableLiveData<>();
        quizLiveData.setValue(new Quiz_VM_EVENT());

        uiLiveData =  new MutableLiveData<>();
        uiLiveData.setValue(new UI_VM_EVENT());

        toolBarTitleLiveData = new MutableLiveData<>();

        needAcknowledgeFlagLiveData = new MutableLiveData<>();
        needAcknowledgeFlagLiveData.setValue("FALSE");

        favoriteQuizIDs = localStorageHelper.loadFavorites();

        currentPurchaseToken = localStorageHelper.getPurchaseTokenStored();
        if (currentPurchaseToken != null) {
            notifyUIDisableAdsMenu(true, true);
            if (localStorageHelper.checkIfPurchaseTokenValid()) {
                notifyUIHideAdsMenu(true);
            } else {
                firebaseHelper.checkPurchaseTokenLegitimate(localStorageHelper.getSkuStored(), currentPurchaseToken, null);
            }
        } else {
            notifyUIHideAdsMenu(false);
            notifyUIDisableAdsMenu(false,true);
        }

        if (LOG) {
            Log.d(TAG, ": in constructor end");
        }
    }

    public void requestCheckGPDevAPIPurchaseTokenLegitimate(Purchase purchase, boolean  calledFromMainThread) {
        currentPurchaseToken = purchase.getPurchaseToken();
        if (!currentPurchaseToken.equals(localStorageHelper.getPurchaseTokenStored()) || !localStorageHelper.checkIfPurchaseTokenValid()) {
            if (!currentPurchaseToken.equals(localStorageHelper.getPurchaseTokenStored())) {
                localStorageHelper.clearPurchaseToken();
                localStorageHelper.storePurchaseToken(purchase.getSkus().get(0), purchase.getPurchaseToken());
            }
            firebaseHelper.checkPurchaseTokenLegitimate(purchase);
            notifyUIDisableAdsMenu(true, calledFromMainThread);

        }
    }

    public boolean isDisableLoadQuiz() { return disableLoadQuiz; }
    public void setDisableLoadQuiz(boolean disableLoadQuiz) { this.disableLoadQuiz = disableLoadQuiz; }

    public String getCurrentPurchaseToken() {
        return currentPurchaseToken;
    }

    public MutableLiveData<String> getNeedAcknowledgeFlagLiveData() {
        return needAcknowledgeFlagLiveData;
    }

    public int getSubscriptionState() {
        return localStorageHelper.getSubscriptionState();
    }

    private void notifyUIShowFixSubscriptionDialog(String title, String message) {
        UI_VM_EVENT event = uiLiveData.getValue();
        event.setDialogText(title, message);
        event.setType(UI_VM_EVENT.TYPE_GOOGLE_DIALOG);
        uiLiveData.setValue(event);

        if (LOG) {
            Log.d(TAG, ".notifyUIShowFixSubscriptionDialog");
        }
    }

    private void notifyUIShowToast(String text) {
        UI_VM_EVENT event = uiLiveData.getValue();
        event.setToastText(text);
        event.setType(UI_VM_EVENT.TYPE_TOAST);
        uiLiveData.setValue(event);

        if (LOG) {
            Log.d(TAG, ".notifyUIShowToast");
        }
    }

    public void markPurchaseTokenAcknowledged(boolean acknowledgedFlag) {
        localStorageHelper.markPurchaseTokenAcknowledged(acknowledgedFlag);
    }

    private void notifyUIHideAdsMenu(boolean hideFlag) {
        UI_VM_EVENT event = uiLiveData.getValue();
        event.setHideAds(hideFlag);
        event.setType(UI_VM_EVENT.TYPE_ADS_STATE);
        uiLiveData.setValue(event);
    }

    private void notifyUIDisableAdsMenu(boolean disableFlag, boolean  calledFromMainThread) {
        UI_VM_EVENT event = uiLiveData.getValue();
        event.setDisableAds(disableFlag);
        event.setType(UI_VM_EVENT.TYPE_ADS_STATE);
        if (calledFromMainThread) {
            uiLiveData.setValue(event);
        } else {
            uiLiveData.postValue(event);
        }
    }

    public void setFavoritesChecked(boolean favoritesChecked) {
        UI_VM_EVENT event = uiLiveData.getValue();
        event.setFavoritesChecked(favoritesChecked);
        event.setType(UI_VM_EVENT.TYPE_FAVORITES_STATE);
        uiLiveData.setValue(event);
    }

    private void notifyFavoritesExist(boolean doEnable) {
        if (LOG) {
            Log.d(TAG, ".notifyFavoritesExist -->  Num of favorites == " + favoriteQuizIDs.size());
        }

        UI_VM_EVENT event = uiLiveData.getValue();
        event.setEnableFavorites(doEnable);
        if (!doEnable) {
            event.setFavoritesChecked(false);
        }
        event.setType(UI_VM_EVENT.TYPE_FAVORITES_STATE);
        uiLiveData.setValue(event);
    }

    public boolean checkIfFavorite(String quizId) {
        return favoriteQuizIDs.contains(quizId);
    }

    public boolean crossCheckFavorites() {
        if (jsonQuizListLocal != null) {
            for (JSON_Quiz jsonQuiz : jsonQuizListLocal) {
                if (favoriteQuizIDs.contains(jsonQuiz.getId())) {
                    return true;
                }
            }
        }
        if (jsonQuizList != null) {
            for (JSON_Quiz jsonQuiz : jsonQuizList) {
                if (favoriteQuizIDs.contains(jsonQuiz.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void processFavoriteAdded(String quizId) {
        favoriteQuizIDs.add(quizId);
        localStorageHelper.storeFavorites(favoriteQuizIDs);
        notifyFavoritesExist(true);
    }

    public void processFavoriteRemoved(String quizId)  {
        favoriteQuizIDs.remove(quizId);
        localStorageHelper.storeFavorites(favoriteQuizIDs);
        notifyFavoritesExist(crossCheckFavorites());
    }

    public Set<String> getFavoriteQuizIDs() {
        return favoriteQuizIDs;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        firebaseHelper.onClear();
        localStorageHelper.onClear();
    }

    public void setBitmapDimensions(int imageReqWidth,int imageReqHeight, int iconReqWidth,int iconReqHeight) {
        this.imageReqWidth = imageReqWidth;
        this.imageReqHeight = imageReqHeight;
        this.iconReqWidth = iconReqWidth;
        this.iconReqHeight = iconReqHeight;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
    public Handler getMainThreadHandler() {
        return mainThreadHandler;
    }
    public boolean isJsonMismatchIdentified() { return quizLiveData.getValue().isJsonMismatchIdentifiedFlag(); }
    public boolean isOnlineAtStart() {
        return isOnlineAtStart;
    }
    public void setOnlineAtStart(boolean onlineAtStart) { isOnlineAtStart = onlineAtStart; }
    public boolean isCheckInternetFlag() { return checkInternetFlag; }
    public void setCheckInternetFlag(boolean checkInternetFlag) { this.checkInternetFlag = checkInternetFlag; }
    public boolean isQuizzesListRequestInProgress() {
        return quizzesListRequestInProgress;
    }
    public void setQuizzesListRequestInProgress(boolean quizzesListRequestInProgress) { this.quizzesListRequestInProgress = quizzesListRequestInProgress; }
    public LiveData<Quiz_VM_EVENT> getLiveQuizEvent() { return quizLiveData; }
    public LiveData<UI_VM_EVENT> getLiveUIEvent() { return uiLiveData; }
    public LiveData<String> getLiveToolBarTitle() { return toolBarTitleLiveData; }
    public List<JSON_Quiz> getJsonQuizList() { return jsonQuizList; }
    public List<JSON_Quiz> getJsonQuizListLocal() { return jsonQuizListLocal; }

    public void requestJsonQuizListLoad() {
        quizLiveData.getValue().setQuizListLoadedFlag(false);
        if (firebaseIsAuthenticatedFlag) {
            firebaseHelper.subscribeForQuizzesList(this);
        } else {
            hasPendingRequestForOnlineList = true;
        }
    }

    public void requestJsonQuizListLocalLoad() {
        quizLiveData.getValue().setQuizListLocalLoadedFlag(false);
        jsonQuizListLocal = localStorageHelper.getLocalQuizzesList(executorService, mainThreadHandler);
        quizLiveData.getValue().setQuizListLocalLoadedFlag(true);
        if (crossCheckFavorites()) {
            notifyFavoritesExist(true);
        }
    }

    public void requestToolBarTitleUpdate(String text) {
        toolBarTitleLiveData.setValue(text);
    }

    public Bitmap lookupForBitmap(String imageId) {
        return imagesMap.get(imageId);
    }

    public boolean checkImageLoaded(String imageId) {
        return imagesMap.containsKey(imageId);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromResource(byte[] bytes, int reqWidth, int reqHeight) {
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    public TriviaCardQuiz getCurrentQuiz() {
        return currentQuiz;
    }

    public void setCurrentQuiz(TriviaCardQuiz currentQuiz) {
        this.currentQuiz = currentQuiz;
    }

    public void resetImageLoadedCount() {
        quizLiveData.getValue().setImagesLoadedFlag(false);
    }

    public void resetOnlineQuizList() {
        if (jsonQuizList != null) {
            jsonQuizList.clear();
        }
        jsonQuizList = null;
    }

    public JSON_Quiz findQuizByIdInList(String quizIdChosen) {
        if (jsonQuizList != null) {
            for (JSON_Quiz jsonQuiz : jsonQuizList) {
                if (jsonQuiz.getId().equals(quizIdChosen)) {
                    return jsonQuiz;
                }
            }
        }
        if (jsonQuizListLocal != null) {
            for (JSON_Quiz jsonQuiz : jsonQuizListLocal) {
                if (jsonQuiz.getId().equals(quizIdChosen)) {
                    return jsonQuiz;
                }
            }
        }
        return null;
    }

    public void loadLocalImageFromFullPath(String imageId) {
        if (!checkImageLoaded(imageId)) {
            if (LOG) {
                Log.d(TAG, ".loadLocalImageFromFullPath image --> " + imageId + " ...calling localStorageHelper.loadLocalImageFromFullPath");
            }
            localStorageHelper.loadLocalImageFromFullPath(imageId, executorService, mainThreadHandler);
        } else {
            if (LOG) {
                Log.d(TAG, ".loadLocalImageFromFullPath image --> " + imageId + " is already loaded");
            }
            doCallbackLoadImageFromFullPath();
        }
    }

    public void subscribeForLoadImageFromFullPath(String imageId, TriviaCardQuestion triviaCardQuestion) {
        if (!checkImageLoaded(imageId)) {
            firebaseHelper.subscribeForLoadImageFromFullPath(imageId, triviaCardQuestion);
        } else {
            doCallbackLoadImageFromFullPath();
        }
    }

    private void doCallbackLoadImageFromFullPath() {
        if (currentQuiz.checkIfQuizInProgress()) {
            currentQuiz.incrementCountImagesLoaded();
            Quiz_VM_EVENT event = quizLiveData.getValue();
            event.setImagesLoadedFlag(true);
            quizLiveData.setValue(event);
        }
        if (LOG) {
            Log.d(TAG, ".callbackLoadImageFromFullPath quiz in progress = " + currentQuiz.checkIfQuizInProgress() + "images loaded count = " + currentQuiz.getCountImagesLoaded());
        }
    }

    public boolean checkIfRequiresRewarded(JSON_Quiz quiz) {
        LocalDate storedDate = localStorageHelper.readNewDateByQid(quiz.getId());
        return storedDate == null || storedDate.isBefore(LocalDate.parse(quiz.getDate_new()));
    }

    public void storeRewardedShownForQuiz(String quizId)  {
        localStorageHelper.storeNewDateByQid(quizId);
    }

    public int checkStoredQuizAvailable() { return localStorageHelper.checkStoredQuizAvailable(); }
    public JSON_Image getLocalImageById(String imageId) { return localStorageHelper.getLocalImageById(imageId); }
    public List<JSON_Question> getLocalActiveQuestionsGivenQuiz(JSON_Quiz json_quiz) { return localStorageHelper.getLocalActiveQuestionsGivenQuiz(json_quiz);}
    public void subscribeForActiveQuestionsGivenQuiz(TriviaCardQuiz triviaCardQuiz) { firebaseHelper.subscribeForActiveQuestionsGivenQuiz(triviaCardQuiz);}
    public int readFreqByQiD(String questionId) { return localStorageHelper.readFreqByQiD(questionId); }
    public void incrementFreqByQiD(String questionId) { localStorageHelper.incrementFreqByQiD(questionId); }
    public void incrementFreqByQiD(List<String> questionIDs) { firebaseHelper.incrementFreqByQiD(questionIDs); }
    public int storePointsForQuiz(String quizId, int pointsValue) { return localStorageHelper.storePointsForQuiz(quizId, pointsValue); }
    public void clearStoredQuiz() { localStorageHelper.clearStoredQuiz(); }
    public void storeQuiz(JSON_Quiz json_quiz, int questionIndex, int pointsValue) { localStorageHelper.storeQuiz(json_quiz, questionIndex, pointsValue); }
    public void loadStoredQuiz() { localStorageHelper.loadStoredQuiz(this); }
    public void subscribeForImageById(String imageId, TriviaCardQuestion triviaCardQuestion) { firebaseHelper.subscribeForImageById(imageId, triviaCardQuestion); }
}
