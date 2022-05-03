package com.r42914lg.arkados.triviacard.model;

import androidx.annotation.NonNull;

public class Quiz_VM_EVENT {
    private boolean schemaMismatchIdentifiedFlag;
    private boolean quizListLoadedFlag;
    private boolean quizListLocalLoadedFlag;
    private boolean anotherImageLoadedFlag;
    private JSON_Quiz jsonQuiz;

    public boolean isQuizListLoadedFlag() { return quizListLoadedFlag; }
    public boolean checkImageLoadedFlag() { return anotherImageLoadedFlag; }
    public JSON_Quiz getQuizForIconLoaded() { return jsonQuiz; }
    public boolean isJsonMismatchIdentifiedFlag() { return schemaMismatchIdentifiedFlag; }

    public void setSchemaMismatchIdentifiedFlag(boolean jsonMismatchIdentifiedFlag) { this.schemaMismatchIdentifiedFlag = jsonMismatchIdentifiedFlag; }
    public void setQuizListLoadedFlag(boolean quizListLoadedFlag) { this.quizListLoadedFlag = quizListLoadedFlag; }
    public void setQuizForIconLoaded(JSON_Quiz jsonQuiz) { this.jsonQuiz = jsonQuiz; }
    public void setImagesLoadedFlag(boolean anotherImageLoadedFlag) { this.anotherImageLoadedFlag = anotherImageLoadedFlag; }
    public void setQuizListLocalLoadedFlag(boolean quizListLocalLoadedFlag) { this.quizListLocalLoadedFlag = quizListLocalLoadedFlag; }

    @NonNull
    @Override
    public String toString() {
        return "TriviaCardVM_EVENT{" +
                "jsonMismatchIdentifiedFlag=" + schemaMismatchIdentifiedFlag +
                ", quizListLoadedFlag=" + quizListLoadedFlag +
                ", quizListLocalLoadedFlag=" + quizListLocalLoadedFlag +
                ", imagesLoadedFlag=" + anotherImageLoadedFlag +
                ", jsonQuiz=" + jsonQuiz +
                '}';
    }
}
