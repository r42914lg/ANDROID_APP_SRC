package com.r42914lg.arkados.triviacard.model;

import com.android.billingclient.api.Purchase;

import java.util.List;

public interface IDataLoaderListener {
    public void callbackPurchaseTokenLegitimate_200(Purchase purchase, long expiryTimeMillis, int paymentState, boolean autoRenewing);
    public void callbackPurchaseTokenLegitimate_500(Purchase purchase);
    public void callbackPurchaseTokenLegitimate_FAILURE(Purchase purchase);
    public void callbackActiveQuizzesList(List<JSON_Quiz> jsonQuizList);
    public void callbackActiveQuestionsGivenQuiz(List<JSON_Question> jsonQuestions);
    public void callbackImageById(JSON_Image jsonImage, TriviaCardQuestion triviaCardQuestion);
    public void callbackLoadImageFromFullPath(byte[] bytes, String key);
    public void callbackLoadIconFromFullPath(byte[] bytes, String key, JSON_Quiz jsonQuiz);
    public void callbackFirebaseAuthenticated();
    public void handleSchemaMismatch();
}
