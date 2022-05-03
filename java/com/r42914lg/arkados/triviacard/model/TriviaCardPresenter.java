package com.r42914lg.arkados.triviacard.model;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.APP_PACKAGE_NAME;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_SKU;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_ON_HOLD;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_PAUSED;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.android.billingclient.api.SkuDetails;
import com.facebook.appevents.AppEventsLogger;
import com.r42914lg.arkados.triviacard.R;
import com.r42914lg.arkados.triviacard.TriviaCardConstants;
import com.r42914lg.arkados.triviacard.ui.AdDialogFragment;
import com.r42914lg.arkados.triviacard.ui.FirstFragment;
import com.r42914lg.arkados.triviacard.ui.ICoreFrameTriviaCardUI;
import com.r42914lg.arkados.triviacard.ui.IQuestionViewTriviaCardUI;
import com.r42914lg.arkados.triviacard.ui.IQuizChooserTriviaCardUI;
import com.r42914lg.arkados.triviacard.ui.SecondFragment;
import com.r42914lg.arkados.triviacard.util.BillingAssistant;
import com.r42914lg.arkados.triviacard.util.CheckInternetHelper;
import com.r42914lg.arkados.triviacard.util.IOnlineStatusListener;
import com.r42914lg.arkados.triviacard.util.ITimeoutListener;
import com.r42914lg.arkados.triviacard.util.TimeoutTracker;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

public class TriviaCardPresenter implements ITriviaCardPresenter {
    public static final String TAG = "LG> TriviaCardPresenter";

    private final BillingAssistant billingAssistant;
    private final CheckInternetHelper checkInternetHelper;
    private final TriviaCardVM triviaCardVM;

    AppCompatActivity activity;
    private ICoreFrameTriviaCardUI iCoreFrame;
    private IQuizChooserTriviaCardUI iQuizChooser;
    private IQuestionViewTriviaCardUI iQuestionView;
    private Fragment currentFragment;

    private int displayIsShort;
    private int imageWidth;

    private final AppEventsLogger logger;

    public TriviaCardPresenter(TriviaCardVM triviaCardVM, AppCompatActivity activity) {
        this.checkInternetHelper = new CheckInternetHelper();
        this.billingAssistant = new BillingAssistant(activity, triviaCardVM);
        this.triviaCardVM = triviaCardVM;
        this.activity = activity;
        this.logger = AppEventsLogger.newLogger(activity);

        if (LOG) {
            Log.d(TAG, " instance created");
        }
    }

    public void setCurrentFragment(Fragment fragment) {
        this.currentFragment = fragment;
        if (LOG) {
            Log.d(TAG, ".setCurrentFragment: Fragment --> " + fragment);
        }
    }

    @Override
    public TriviaCardVM getViewModel() {
        return triviaCardVM;
    }

    @Override
    public BillingAssistant getBillingAssistant() {
        return billingAssistant;
    }

    @Override
    public void onAdsButtonClicked() {
        int state = triviaCardVM.getSubscriptionState();
        if (state == SUBSCRIPTION_STATE_ON_HOLD || state == SUBSCRIPTION_STATE_PAUSED) {
            showGooglePlayDeepLinkDialog(activity.getString(R.string.restore_sub_dialog_title), state == SUBSCRIPTION_STATE_ON_HOLD ?
                    activity.getString(R.string.restore_sub_dialog_txt_hold) : activity.getString(R.string.restore_sub_dialog_txt_paused));
        } else {
            SkuDetails skuDetails = billingAssistant.getSubscriptionSKU();
            if (skuDetails != null) {
                showBillingDialog(activity.getString(R.string.dialog_buy_sub_title),
                        MessageFormat.format(String.valueOf(activity.getString(R.string.dialog_buy_sub_text)),
                                parseISO860(skuDetails.getSubscriptionPeriod()), skuDetails.getPrice()), skuDetails);
            }  else {
                showGooglePlayDeepLinkDialog(activity.getString(R.string.dialog_google_login_title), activity.getString(R.string.dialog_google_login_text));
            }
        }
    }

    public void shutDownBillingClient() {
        billingAssistant.shutDownBillingClient();
    }

    public int getImageWidth() {
        if (imageWidth == 0) {
            if (iCoreFrame.getOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
                if (checkShortDisplayFlag() != TriviaCardConstants.DISPLAY_NORMAL) {
                    imageWidth = iCoreFrame.getDisplayWidth() - iCoreFrame.getStatusBarHeight();
                } else {
                    imageWidth = iCoreFrame.getDisplayWidth() - iCoreFrame.getAppBarHeight() - iCoreFrame.getStatusBarHeight();
                }
            } else {
                imageWidth = iCoreFrame.getDisplayWidth();
            }
        }
        return imageWidth;
    }

    public int getButtonPixelSize() {
        return (getImageWidth() - TriviaCardConstants.CELLS_COUNT - 1 - TriviaCardConstants.MARGIN_PD) / TriviaCardConstants.CELLS_COUNT;
    }

    @Override
    public int checkShortDisplayFlag() {
        if (displayIsShort == 0) {
            if (iCoreFrame.getDisplayHeight() < TriviaCardConstants.SHORT_DISPLAY_SIZE_PIXELS) {
                displayIsShort = TriviaCardConstants.DISPLAY_LOW_RES;
            } else if ( (float) iCoreFrame.getDisplayHeight() / (float) iCoreFrame.getDisplayWidth() < TriviaCardConstants.SHORT_DISPLAY_RATION) {
                displayIsShort = TriviaCardConstants.DISPLAY_SHORT;
            } else {
                displayIsShort = TriviaCardConstants.DISPLAY_NORMAL;
            }
        }
        return displayIsShort;
    }

    public void initCoreFrame(ICoreFrameTriviaCardUI iCoreFrame) {
        this.iCoreFrame = iCoreFrame;

        checkInternetHelper.setOnlineStatusListener((Activity) iCoreFrame, new IOnlineStatusListener() {
            @Override
            public void callbackInternetStatusChecked(boolean isOnline) {
                if (triviaCardVM ==  null)
                    return;

                triviaCardVM.setOnlineAtStart(isOnline);
                triviaCardVM.setCheckInternetFlag(!isOnline);

                if (isOnline && triviaCardVM.getJsonQuizList() == null) {
                    requestOnlineQuizzesList();
                    if (currentFragment  instanceof SecondFragment) {
                        triviaCardVM.requestToolBarTitleUpdate(((Activity) iCoreFrame).getResources().getString(R.string.second_fragment_label));
                    }
                } else {
                    if (iCoreFrame != null) {
                        iCoreFrame.showToast(activity.getString(R.string.toast_no_internet), Toast.LENGTH_SHORT);
                    }
                    if (currentFragment  instanceof SecondFragment) {
                        triviaCardVM.requestToolBarTitleUpdate(((Activity) iCoreFrame).getResources().getString(R.string.second_fragment_label_offline));
                    }
                }

                int storedQuizStatus = triviaCardVM.checkStoredQuizAvailable();
                if (!triviaCardVM.isDisableLoadQuiz() &&
                        (storedQuizStatus == 0 ||
                                (storedQuizStatus == 1 && triviaCardVM.isOnlineAtStart() && !triviaCardVM.isJsonMismatchIdentified()))) {
                    showLoadQuizDialog();
                    triviaCardVM.setDisableLoadQuiz(true);
                }
            }
        });

        triviaCardVM.getLiveQuizEvent().observe((AppCompatActivity) iCoreFrame, new Observer<Quiz_VM_EVENT>() {
            @Override
            public void onChanged(Quiz_VM_EVENT event) {
                if (LOG) {
                    Log.d(TAG, ".OBSERVE. processing EVENT " + event.toString());
                }
                if (event.isJsonMismatchIdentifiedFlag()) {
                    if (LOG) {
                        Log.d(TAG, ".OBSERVE. processing EVENT JSON mismatch");
                    }
                    TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
                    if (currentQuiz != null && !currentQuiz.isLocal()) {
                        currentQuiz.setQuizIsInProgress(false);
                        clearQuestionView();
                    }
                    iCoreFrame.showToast(activity.getString(R.string.toast_json_mismatch), Toast.LENGTH_LONG);
                    if (iQuizChooser != null) {
                        initQuizChooser(iQuizChooser);
                    }
                }
                if (event.isQuizListLoadedFlag() && triviaCardVM.isQuizzesListRequestInProgress()) {
                    if (LOG) {
                        Log.d(TAG, ".OBSERVE. processing EVENT Quiz List Loaded");
                    }
                    if (triviaCardVM.isQuizzesListRequestInProgress()) {
                        if (iQuizChooser != null) {
                            iQuizChooser.addRowsToAdapter(triviaCardVM.getJsonQuizList(), false);
                        }
                        iCoreFrame.stopProgressBar();
                        triviaCardVM.setQuizzesListRequestInProgress(false);
                    }
                }
                if (event.checkImageLoadedFlag()) {
                    if (LOG) {
                        Log.d(TAG, ".OBSERVE. processing EVENT Question image loaded");
                    }
                    int imagesLoaded = triviaCardVM.getCurrentQuiz().getCountImagesLoaded();
                    if (currentFragment instanceof FirstFragment) {
                        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
                        iCoreFrame.showProgressBar((imagesLoaded * 100) / TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ);
                        if (currentQuiz.checkIfQ1NotShownYet() && currentQuiz.checkIfImageLoaded(currentQuiz.getCurrentQuestionIndex())) {
                            currentQuiz.setQ1NotShownYet(false);
                            updateQuestionView();
                        }
                    }
                    if (imagesLoaded == TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ) {
                        iCoreFrame.stopProgressBar();
                        triviaCardVM.resetImageLoadedCount();
                    }
                }
                if (event.getQuizForIconLoaded() != null && iQuizChooser != null) {
                    if (LOG) {
                        Log.d(TAG, ".OBSERVE. processing EVENT Quiz icon loaded");
                    }
                    if (event.getQuizForIconLoaded().getPositionInAdapter() != -1) {
                        iQuizChooser.notifyAdapterIconLoaded(event.getQuizForIconLoaded().getPositionInAdapter());
                    }
                }
            }
        });

        triviaCardVM.getLiveUIEvent().observe((AppCompatActivity) iCoreFrame, new Observer<UI_VM_EVENT>() {
            @Override
            public void onChanged(UI_VM_EVENT ui_vm_event) {
                if (ui_vm_event.getEventType() ==  UI_VM_EVENT.TYPE_GOOGLE_DIALOG)  {
                    showGooglePlayDeepLinkDialog(ui_vm_event.getDialogTitle(), ui_vm_event.getDialogMessage());
                }
            }
        });

        if (triviaCardVM.isCheckInternetFlag()) {
            triviaCardVM.setCheckInternetFlag(false);
            checkInternetHelper.requestAsyncCheck(triviaCardVM.getExecutorService(), triviaCardVM.getMainThreadHandler());
        }

        if (triviaCardVM.getJsonQuizListLocal() == null) {
            triviaCardVM.requestJsonQuizListLocalLoad();
        }

        imageWidth = 0;
    }

    public void initQuizChooser(IQuizChooserTriviaCardUI iQuizChooser) {
        this.iQuizChooser = iQuizChooser;

        iCoreFrame.showFavoriteIcon(true);

        iCoreFrame.stopProgressBar();
        iCoreFrame.updateFabAction(TriviaCardConstants.FAB_ACTION_REFRESH);

        if (!triviaCardVM.isJsonMismatchIdentified()) {
            if (triviaCardVM.isCheckInternetFlag()) {
                triviaCardVM.setCheckInternetFlag(false);
                triviaCardVM.resetOnlineQuizList();
                checkInternetHelper.requestAsyncCheck(triviaCardVM.getExecutorService(), triviaCardVM.getMainThreadHandler());
            }
        } else {
            triviaCardVM.resetOnlineQuizList();
            triviaCardVM.requestToolBarTitleUpdate(((Activity) iCoreFrame).getResources().getString(R.string.second_fragment_label_json));
        }

        iQuizChooser.addRowsToAdapter(triviaCardVM.getJsonQuizListLocal(), true);
        iQuizChooser.addRowsToAdapter(triviaCardVM.getJsonQuizList(), false);
    }

    public void initQuestionView(IQuestionViewTriviaCardUI iQuestionView) {
        this.iQuestionView = iQuestionView;
        imageWidth = 0;

        iCoreFrame.showFavoriteIcon(false);

        if (triviaCardVM == null) {
            clearQuestionView();
            return;
        }

        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();

        if (currentQuiz != null && currentQuiz.checkIfQuizInProgress()) {
            triviaCardVM.requestToolBarTitleUpdate(currentQuiz.getQuizName());
        }
        if (currentQuiz != null && currentQuiz.checkIfQuizInProgress()
                && currentQuiz.checkIfQ1NotShownYet() && currentQuiz.checkIfImageLoaded(currentQuiz.getCurrentQuestionIndex())) {

            currentQuiz.setQ1NotShownYet(false);
            updateQuestionView();

            if (triviaCardVM.getCurrentQuiz().getCountImagesLoaded() == TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ) {
                iCoreFrame.stopProgressBar();
                triviaCardVM.resetImageLoadedCount();
            }
        } else if (currentQuiz != null && currentQuiz.checkIfQuizInProgress() && !currentQuiz.checkIfQ1NotShownYet()) {
            updateQuestionView();
            switch (currentQuiz.getCurrentQuestion().getState()) {
                case TriviaCardConstants.Q_WON:
                    iQuestionView.openAllCells();
                    break;
                case TriviaCardConstants.Q_FAILURE_2:
                    iQuestionView.lockQuestionView();
                    break;
            }
        } else {
            clearQuestionView();
        }

        iCoreFrame.updateFabAction(TriviaCardConstants.FAB_ACTION_SKIP);

        if (triviaCardVM.isJsonMismatchIdentified() && currentQuiz != null && !currentQuiz.isLocal()) {
            navigateFirstToSecond();
        }
    }

    public void detachView(ICoreFrameTriviaCardUI view) { iCoreFrame = null; }
    public void detachView(IQuestionViewTriviaCardUI view) { iQuestionView = null; }
    public void detachView(IQuizChooserTriviaCardUI view) { iQuizChooser = null; }

    @Override
    public void requestFinish() {
        iCoreFrame.requestFinishActivity();
    }

    public void processCellOpen(int cellIndex) {
        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        currentQuiz.getCurrentQuestion().subtractPointsByCellIndex(cellIndex - 1);
        iQuestionView.updateLocalPoints(""+currentQuiz.getCurrentQuestion().getPoints());
    }

    public void processFabClick() {
        if (currentFragment instanceof FirstFragment) {
            processQuestionNext();
        } else {
            if (triviaCardVM.getJsonQuizList() != null && triviaCardVM.getJsonQuizListLocal() != null) {
                iCoreFrame.showToast(activity.getString(R.string.toast_up_to_date), Toast.LENGTH_SHORT);
            } else {
                if (triviaCardVM.getJsonQuizList() == null) {
                    if (triviaCardVM.isJsonMismatchIdentified()) {
                        iCoreFrame.showToast(activity.getString(R.string.toast_json_mismatch), Toast.LENGTH_LONG);
                    } else {
                        checkInternetHelper.requestAsyncCheck(triviaCardVM.getExecutorService(), triviaCardVM.getMainThreadHandler());
                    }
                }
                if (triviaCardVM.getJsonQuizListLocal() == null) {
                    triviaCardVM.requestJsonQuizListLocalLoad();
                }
            }
        }
    }

    public void processQuizSelected(String quizId) {
        JSON_Quiz quizSelected = triviaCardVM.findQuizByIdInList(quizId);

        if (!triviaCardVM.getLiveUIEvent().getValue().checkDisableAds()
                && quizSelected.isIs_new()
                && triviaCardVM.checkIfRequiresRewarded(quizSelected)) {

            AdDialogFragment dialog = AdDialogFragment.newInstance();
            dialog.setAdDialogInteractionListener(
                    new AdDialogFragment.AdDialogInteractionListener() {
                        @Override
                        public void onShowAd() {
                            Log.d(TAG, "The rewarded interstitial ad is starting.");
                            iCoreFrame.showRewardedInterstitialAd(quizSelected);
                        }

                        @Override
                        public void onCancelAd() {
                            Log.d(TAG, "The rewarded interstitial ad was skipped before it starts.");
                        }
                    });

            dialog.show(currentFragment.getActivity().getSupportFragmentManager(), "AdDialogFragment");

        } else {
            doSelectQuizAndNavigateToFirst(quizSelected);
        }
    }

    public void doSelectQuizAndNavigateToFirst(JSON_Quiz quizSelected) {
        new TriviaCardQuiz(quizSelected, triviaCardVM);
        navigateToFirstAndCheckQuizDataLoaded();
        logMetaEvent("quiz-started-" + quizSelected.getId());
    }

    private void navigateToFirstAndCheckQuizDataLoaded() {
        if (triviaCardVM == null || iCoreFrame ==  null)
            return;

        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        navigateSecondToFirst();
        iCoreFrame.stopProgressBar();
        iCoreFrame.showProgressBar(0);
        triviaCardVM.setDisableLoadQuiz(true);

        TimeoutTracker tracker = new TimeoutTracker(triviaCardVM.getExecutorService(), triviaCardVM.getMainThreadHandler());
        tracker.setTimeoutListener(new ITimeoutListener() {
            @Override
            public void callbackWaitTimeExpired(int questionIndex) {
                if (currentQuiz == null)
                    return;

                if (currentQuiz.checkIfImageLoaded(currentQuiz.getCurrentQuestionIndex())) {
                    if (LOG) {
                        Log.d(TAG, ".processQuizSelected.callbackWaitTimeExpired: 1st Q loaded");
                    }
                } else {
                    if (LOG) {
                        Log.d(TAG, ".processQuizSelected.callbackWaitTimeExpired: 1st Q NOT loaded, terminating quiz");
                    }
                    clearQuestionView();
                    iCoreFrame.stopProgressBar();
                    currentQuiz.setQuizIsInProgress(false);
                    if (!triviaCardVM.isJsonMismatchIdentified()) {
                        iCoreFrame.showToast(activity.getString(R.string.toast_check_internet), Toast.LENGTH_SHORT);
                    }
                    triviaCardVM.setCheckInternetFlag(true);
                    navigateFirstToSecond();
                }
            }
        });
        tracker.start(5000, -1);
    }

    private void processQuestionNext() {
        if (triviaCardVM == null || iCoreFrame ==  null || triviaCardVM.getCurrentQuiz().checkIfQ1NotShownYet())
            return;

        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        if (currentQuiz.hasNextQuestion()) {
            iCoreFrame.updateFabAction(TriviaCardConstants.FAB_ACTION_SKIP);
            if (currentQuiz.iterateToNextQuestion()) {
                updateQuestionView();
                if (!triviaCardVM.getLiveUIEvent().getValue().checkDisableAds() && currentQuiz.getCurrentQuestionIndex() % 4 == 0) {
                    iCoreFrame.showInterstitialAd();
                }
            } else {
                iCoreFrame.showToast(activity.getString(R.string.toast_wait_for_image), Toast.LENGTH_SHORT);
                if (!currentQuiz.isLocal()) {
                    TimeoutTracker tracker = new TimeoutTracker(triviaCardVM.getExecutorService(), triviaCardVM.getMainThreadHandler());
                    tracker.setTimeoutListener(new ITimeoutListener() {
                        @Override
                        public void callbackWaitTimeExpired(int questionIndex) {
                            if (!currentQuiz.checkIfImageLoaded(questionIndex)) {
                                if (LOG) {
                                    Log.d(TAG, ".processQuestionNext.callbackWaitTimeExpired: next Image NOT loaded, terminating quiz");
                                }
                                clearQuestionView();
                                currentQuiz.setQuizIsInProgress(false);
                                iCoreFrame.stopProgressBar();
                                iCoreFrame.showToast(activity.getString(R.string.toast_check_internet), Toast.LENGTH_SHORT);
                                triviaCardVM.setCheckInternetFlag(true);
                                navigateFirstToSecond();
                            } else {
                                if (LOG) {
                                    Log.d(TAG, ".processQuestionNext.callbackWaitTimeExpired: next Image loaded");
                                }
                            }
                        }
                    });
                    tracker.start(5000, currentQuiz.getCurrentQuestionIndex() +1);
                }
            }
        } else {
            currentQuiz.setQuizIsInProgress(false);
            updateQuizScoreInChooser(triviaCardVM.storePointsForQuiz(currentQuiz.getQuizId(), currentQuiz.getSumOfPlayedQuestionsPoints()));
            showEndOfQuizDialog();
            currentQuiz.updateQuestionUsageStats();
            logMetaEvent("quiz-finished-" + currentQuiz.getQuizId());
        }
    }

    public void processOption(int optionIndex) {
        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        int resultCode = currentQuiz.processOptionClick(optionIndex);
        switch (resultCode) {
            case TriviaCardConstants.Q_FAILURE_1:
                iQuestionView.updateLocalPoints(""+currentQuiz.getCurrentQuestion().getPoints());
                iQuestionView.animateOptionButton(optionIndex, TriviaCardConstants.ANIMATE_OPTION_FAIL_1);
                break;
            case TriviaCardConstants.Q_FAILURE_2:
                iQuestionView.updateLocalPoints(""+currentQuiz.getCurrentQuestion().getPoints());
                iQuestionView.animateOptionButton(optionIndex, TriviaCardConstants.ANIMATE_OPTION_FAIL_2);
                iQuestionView.lockQuestionView();
                if (!currentQuiz.hasNextQuestion()) {
                    currentQuiz.setQuizIsInProgress(false);
                }
                iCoreFrame.updateFabAction(currentQuiz.getCurrentQuestionIndex() + 1 == TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ?
                        TriviaCardConstants.FAB_ACTION_FINISH:TriviaCardConstants.FAB_ACTION_NEXT);
                break;
            case TriviaCardConstants.Q_WON:
                iQuestionView.updateTotalPoints(""+currentQuiz.getSumOfPlayedQuestionsPoints());
                iQuestionView.animateOptionButton(optionIndex, TriviaCardConstants.ANIMATE_OPTION_WIN);
                iQuestionView.openAllCells();
                if (!currentQuiz.hasNextQuestion()) {
                    currentQuiz.setQuizIsInProgress(false);
                }
                iCoreFrame.updateFabAction(currentQuiz.getCurrentQuestionIndex() + 1 == TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ?
                        TriviaCardConstants.FAB_ACTION_FINISH:TriviaCardConstants.FAB_ACTION_NEXT);
                break;
            default:
                throw new IllegalStateException("TCP.processOption --> Unexpected value: " + resultCode);
        }
    }

    private void clearQuestionView() {
        if (iQuestionView == null) {
            return;
        }
        iQuestionView.updateOptionsText(null, null, null, null);
        iQuestionView.updateQuestionTaskView(null);
        iQuestionView.updateTotalPoints(null);
        iQuestionView.updateLocalPoints(null);
        iQuestionView.updateQuestionIndexView(null);
        iQuestionView.updatePictureView(null);
        for (int i = 0; i < TriviaCardConstants.CELLS_COUNT * TriviaCardConstants.CELLS_COUNT; i++) {
            iQuestionView.updateCellView(i + 1, -1);
        }
        if (LOG) {
            Log.d(TAG, ".clearQuestionView DONE");
        }
    }

    private void updateQuestionView() {
        if (iQuestionView == null) {
            return;
        }

        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        TriviaCardQuestion currentQuestion = currentQuiz.getCurrentQuestion();
        iQuestionView.updateOptionsText(currentQuestion.getOptionText(1),
                currentQuestion.getOptionText(2),
                currentQuestion.getOptionText(3),
                currentQuestion.getOptionText(4));

        iQuestionView.updateQuestionIndexView("" + currentQuestion.getOrdIndexInQuiz());
        iQuestionView.updateQuestionTaskView(currentQuestion.getTaskDefinition());
        iQuestionView.updateLocalPoints("" + currentQuestion.getPoints());
        iQuestionView.updateTotalPoints("" + currentQuiz.getSumOfPlayedQuestionsPoints());
        iQuestionView.updatePictureView(triviaCardVM.lookupForBitmap(currentQuestion.getImageFullPath()));

        List<Integer> cellValues = currentQuestion.getCellValues();
        for (int i = 0; i < TriviaCardConstants.CELLS_COUNT * TriviaCardConstants.CELLS_COUNT; i++) {
            iQuestionView.updateCellView(i + 1, cellValues.get(i));
        }

        if (LOG) {
            Log.d(TAG, ".updateQuestionView DONE");
        }
    }

    private void showGooglePlayDeepLinkDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(currentFragment.getContext()).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(R.string.dialog_yes),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(
                        "https://play.google.com/store/account/subscriptions?sku="
                                + SUBSCRIPTION_SKU + "&package=" + APP_PACKAGE_NAME));
                intent.setPackage("com.android.vending");
                currentFragment.startActivity(intent);
                dialog.cancel();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void showBillingDialog(String title, String message, SkuDetails skuDetails) {
        AlertDialog dialog = new AlertDialog.Builder(currentFragment.getContext()).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getText(R.string.dialog_yes),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                billingAssistant.launchPurchaseFlow(skuDetails);
                dialog.cancel();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getText(R.string.dialog_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void showEndOfQuizDialog() {
        AlertDialog dialog = new AlertDialog.Builder(currentFragment.getContext()).create();
        dialog.setTitle(activity.getString(R.string.dialog_quiz_finished_title));
        dialog.setMessage(MessageFormat.format(activity.getString(R.string.dialog_quiz_finish_text), triviaCardVM.getCurrentQuiz().getSumOfPlayedQuestionsPoints()));
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getText(R.string.dialog_ok),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                navigateFirstToSecond();
                dialog.cancel();
                iCoreFrame.askRatings();
            }
        });
        dialog.show();
    }

    private void showLoadQuizDialog() {
        AlertDialog dialog = new AlertDialog.Builder(currentFragment.getContext()).create();
        dialog.setTitle(activity.getString(R.string.dialog_load_quiz_title));
        dialog.setMessage(activity.getString(R.string.dialog_load_quiz_text));
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(R.string.dialog_yes),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                loadGame();
                triviaCardVM.clearStoredQuiz();
                dialog.cancel();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.dialog_no),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                triviaCardVM.clearStoredQuiz();
                dialog.cancel();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                triviaCardVM.clearStoredQuiz();
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void requestOnlineQuizzesList() {
        if (triviaCardVM ==  null || iCoreFrame ==  null)
            return;

        triviaCardVM.setQuizzesListRequestInProgress(true);
        triviaCardVM.requestJsonQuizListLoad();
        iCoreFrame.showProgressBar(-1);

        TimeoutTracker tracker = new TimeoutTracker(triviaCardVM.getExecutorService(), triviaCardVM.getMainThreadHandler());
        tracker.setTimeoutListener(new ITimeoutListener() {
            @Override
            public void callbackWaitTimeExpired(int questionIndex) {
                if (triviaCardVM.getJsonQuizList() == null) {
                    if (LOG) {
                        Log.d(TAG, ".requestOnlineQuizzesList.callbackWaitTimeExpired: quizzes online list NOT YET loaded");
                    }
                    iCoreFrame.stopProgressBar();
                    triviaCardVM.setQuizzesListRequestInProgress(false);
                } else {
                    if (LOG) {
                        Log.d(TAG, ".requestOnlineQuizzesList.callbackWaitTimeExpired: quizzes online list HAS BEEN loaded");
                    }
                }
            }
        });
        tracker.start(10000, -1);
    }

    public void saveGame() {
        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        if (currentQuiz != null && currentQuiz.checkIfQuizInProgress()) {
            triviaCardVM.storeQuiz(currentQuiz.getJsonQuiz(),
                    currentQuiz.getCurrentQuestionIndex(), currentQuiz.getSumOfPlayedQuestionsPoints());

            if (LOG) {
                Log.d(TAG, ".saveGame: saving for quiz ID = " + currentQuiz.getJsonQuiz());
            }
        }
    }

    public void loadGame() {
        if (LOG) {
            Log.d(TAG, ".loadGame");
        }

        triviaCardVM.loadStoredQuiz();
        navigateToFirstAndCheckQuizDataLoaded();
    }

    private void updateQuizScoreInChooser(int highScore) {
        TriviaCardQuiz currentQuiz = triviaCardVM.getCurrentQuiz();
        int index;

        List<JSON_Quiz> jsonQuizList = triviaCardVM.getJsonQuizList();
        if (jsonQuizList != null) {
            index = jsonQuizList.indexOf(currentQuiz.getJsonQuiz());
            if (index != -1) {
                if (highScore != -1) {
                    jsonQuizList.get(index).setHighScore(highScore);
                }
                jsonQuizList.get(index).setLastPlayedDate(LocalDate.now().toString());
            }
        }

        List<JSON_Quiz> jsonQuizListLocal = triviaCardVM.getJsonQuizListLocal();
        if (jsonQuizListLocal != null) {
            index = jsonQuizListLocal.indexOf(currentQuiz.getJsonQuiz());
            if (index != -1) {
                if (highScore != -1) {
                    jsonQuizListLocal.get(index).setHighScore(highScore);
                }
                jsonQuizListLocal.get(index).setLastPlayedDate(LocalDate.now().toString());
            }
        }
    }

    private void logMetaEvent(String event) {
        logger.logEvent(event);
    }

    private String parseISO860(String ISO860text)  {
        int numPart = Integer.parseInt(ISO860text.substring(1, ISO860text.length() - 1));
        String textPart;
        switch (ISO860text.charAt(ISO860text.length() - 1))  {
            case 'D':
                textPart = " day";
                break;
            case 'W':
                textPart = " week";
                break;
            case 'M':
                textPart = " month";
                break;
            case 'Y':
                textPart = " year";
                break;
            default:
                textPart = ISO860text;
        }
        return  (numPart > 1) ? numPart + textPart + "s" : numPart + textPart;
    }

    private void navigateFirstToSecond() {
        if (iQuestionView != null) {
            iQuestionView.navigateToNextFragment();
        }
    }

    private void navigateSecondToFirst() {
        if (iQuizChooser != null) {
            iQuizChooser.navigateToNextFragment();
        }
    }
}

