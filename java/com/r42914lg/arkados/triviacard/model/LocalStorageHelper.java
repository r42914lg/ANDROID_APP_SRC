package com.r42914lg.arkados.triviacard.model;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_IMAGES;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_LOCAL_FILE_NAME;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_QUESTIONS;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_QUIZZES;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_ACTIVE;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_CANCELLED;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_STATE_GRACE;

public class LocalStorageHelper {
    public static final String TAG = "LG> LocalStorageHelper";

    private IDataLoaderListener dataLoaderListener;
    private SharedPreferences preferences;
    private JSONObject obj;
    private Application application;

    public LocalStorageHelper(Application application, IDataLoaderListener dataLoaderListener) {
        this.application = application;
        this.dataLoaderListener = dataLoaderListener;
        if (LOG) {
            Log.d(TAG, ".setDataLoaderListener: listener set " + dataLoaderListener);
        }

        if (LOG) {
            Log.d(TAG, ": instance created");
        }
    }

    public void onClear() {
        dataLoaderListener = null;
        preferences = null;
        application =  null;
    }

    public void setSharedPreferences(SharedPreferences preferences) { this.preferences = preferences; }
    protected SharedPreferences getPreferences() {
        return preferences;
    }

    public void init() {
        if (obj == null) {
            try {
                obj = new JSONObject(loadJSONFromAsset(JSON_LOCAL_FILE_NAME));
                if (LOG) {
                    Log.d(TAG, ".init: local JSON read from asset");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public List<JSON_Question> getLocalActiveQuestionsGivenQuiz(JSON_Quiz json_quiz) {
        List<JSON_Question> resultToReturn = new ArrayList<>();
        try {
            init();
            JSONObject questionsRoot = obj.getJSONObject(JSON_QUESTIONS);
            Iterator<String> questions = questionsRoot.keys();

            while (questions.hasNext()){
                String id = questions.next();
                if (json_quiz.getQuestion_ids().contains(id)) {
                    JSONObject question = questionsRoot.getJSONObject(id);
                    if (question.getBoolean("active")) {
                        JSON_Question json_question = new JSON_Question(
                                id,
                                question.getString("task_def"),
                                question.getString("option_1"),
                                question.getString("option_2"),
                                question.getString("option_3"),
                                question.getString("option_4"),
                                question.getString("option_5"),
                                question.getString("option_6"),
                                question.getInt("answer"),
                                question.getString("image_id"),
                                question.getBoolean("active")
                        );
                        resultToReturn.add(json_question);

                        if (LOG) {
                            Log.d(TAG, ".getLocalActiveQuestionsGivenQuiz: Question ID  = " + id + "added to Question list");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            resultToReturn = null;
        }
        return resultToReturn;
    }

    public JSON_Image getLocalImageById(String imageId) {
        JSON_Image resultToReturn;
        try {
            init();
            JSONObject imagesRoot = obj.getJSONObject(JSON_IMAGES);
            JSONObject image = imagesRoot.getJSONObject(imageId);
            resultToReturn = new JSON_Image(imageId, image.getString("image_full_path"), image.getJSONArray("cell_values"));

            if (LOG) {
                Log.d(TAG, ".getLocalImageById: imageId  = " + imageId + " JSON created");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            resultToReturn = null;
        }
        return resultToReturn;
    }

    public void loadLocalImageFromFullPath(String imageName, Executor executor, Handler resultHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = readFileFromAsset(imageName);

                if (LOG) {
                    Log.d(TAG, ".loadLocalImageFromFullPath: imageName  = " + imageName + " read finished on thread, before callback call");
                }

                resultHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dataLoaderListener.callbackLoadImageFromFullPath(buffer, imageName);
                    }
                });
            }
        });
    }

    public void loadLocalIconFromFullPath(String imageName, Executor executor, Handler resultHandler, JSON_Quiz jsonQuiz) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = readFileFromAsset(imageName);

                if (LOG) {
                    Log.d(TAG, ".loadLocalImageFromFullPath: imageName  = " + imageName + " read finished on thread, before callback call");
                }

                resultHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dataLoaderListener.callbackLoadIconFromFullPath(buffer, imageName, jsonQuiz);
                    }
                });
            }
        });
    }

    public  List<JSON_Quiz> getLocalQuizzesList(Executor executor, Handler resultHandler) {
        List<JSON_Quiz> resultToReturn = new ArrayList<>();
        try {
            init();
            JSONObject quizzesRoot = obj.getJSONObject(JSON_QUIZZES);
            Iterator<String> quizzes = quizzesRoot.keys();

            while (quizzes.hasNext()){
                String id = quizzes.next();
                JSONObject quiz = quizzesRoot.getJSONObject(id);
                JSONArray array = quiz.getJSONArray("question_ids");
                if (quiz.getBoolean("active")) {
                    List<String> questionIDs = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        questionIDs.add(array.getString(i));
                    }
                    JSON_Quiz jsonQuiz = new JSON_Quiz(
                            id,
                            quiz.getString("title"),
                            quiz.getString("description"),
                            questionIDs,
                            quiz.getString("icon_full_path"),
                            quiz.getBoolean("active"),
                            quiz.getBoolean("is_new"),
                            quiz.getString("date_new")
                    );
                    jsonQuiz.setIsLocal(true);
                    jsonQuiz.setHighScore(readPointsByQiD(id));
                    jsonQuiz.setLastPlayedDate(readLastPlayedDateByQiD(id));
                    loadLocalIconFromFullPath(jsonQuiz.getIcon_full_path(), executor, resultHandler, jsonQuiz);
                    resultToReturn.add(jsonQuiz);

                    if (LOG) {
                        Log.d(TAG, ".getLocalQuizzesList: quiz ID  = " + id + " added to local quizzes list");
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            resultToReturn = null;
        }
        return resultToReturn;
    }

    public int readFreqByQiD(String questionId) {
        int valueToReturn = preferences.getInt(questionId, 0);
        if (LOG) {
            Log.d(TAG, ".readFreqByQiD: questionId  = " + questionId + " Freq value --> " + valueToReturn);
        }
        return valueToReturn;
    }

    public int readPointsByQiD(String quizId) {
        int valueToReturn = preferences.getInt(quizId + "POINTS", 0);
        if (LOG) {
            Log.d(TAG, ".readPointsByQiD: quizId  = " + quizId + " Points value --> " + valueToReturn);
        }
        return valueToReturn;
    }

    public String readLastPlayedDateByQiD(String quizId) {
        String valueToReturn = preferences.getString(quizId +  "DATE", null);
        if (LOG) {
            Log.d(TAG, ".readLastPlayedDateByQiD: quizId  = " + quizId + " Date --> " + valueToReturn);
        }
        return valueToReturn;
    }

    public void storeNewDateByQid(String quizId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(quizId + "NEW_DATE" , LocalDate.now().toString());
        editor.apply();

        if (LOG) {
            Log.d(TAG, ".storeNewDateByQid: questionId  = " + quizId);
        }
    }

    public LocalDate readNewDateByQid(String quizId) {
        String dateStr = preferences.getString(quizId + "NEW_DATE", null);
        LocalDate valueToReturn = dateStr == null ? null : LocalDate.parse(dateStr);
        if (LOG) {
            Log.d(TAG, ".readNewDateByQid: quizId  = " + quizId + " New Date value --> " + valueToReturn);
        }
        return valueToReturn;
    }

    public void incrementFreqByQiD(String questionId) {
        SharedPreferences.Editor editor = preferences.edit();
        int prevVal = readFreqByQiD(questionId);
        editor.putInt(questionId,  prevVal + 1);
        editor.apply();

        if (LOG) {
            Log.d(TAG, ".incrementFreqByQiD: questionId  = " + questionId + " value = " + (prevVal + 1));
        }
    }

    public int storePointsForQuiz(String quizId, int pointsValue) {
        SharedPreferences.Editor editor = preferences.edit();
        int oldValue = readPointsByQiD(quizId);
        int valueToReturn = -1;

        if (oldValue < pointsValue) {
            editor.putInt(quizId + "POINTS", pointsValue);
            valueToReturn = pointsValue;
        }

        editor.putString(quizId + "DATE", LocalDate.now().toString());
        editor.apply();

        if (LOG) {
            Log.d(TAG, ".storePointsForQuiz: quizId  = " + quizId + " old value = " + oldValue + " current = " + pointsValue + " date played updated = " +  LocalDate.now().toString());
        }

        return valueToReturn;
    }

    public int checkStoredQuizAvailable() {
        int valueToReturn = 1;
        if (preferences.getInt("CURRENT_QUESTION_INDEX", -1) == -1) {
            valueToReturn = -1;
        } else {
            if (preferences.getBoolean("IS_LOCAL", true)) {
                valueToReturn = 0;
            }
        }

        if (LOG) {
            Log.d(TAG, ".checkStoredQuizAvailable: return value  = " + valueToReturn + " no data (-1), local (0), not local (1)");
        }

        return valueToReturn;
    }

    public void clearStoredQuiz() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("SAVED_QUIZ");
        editor.remove("TITLE");
        editor.remove("DESCRIPTION");
        editor.remove("CURRENT_QUESTION_INDEX");
        editor.remove("IS_LOCAL");
        editor.remove("POINTS_INTERIM");
        for (int i = 0; i < NUM_OF_QUESTIONS_IN_QUIZ; i++) {
            editor.remove("QUESTION_ID_" + i);
        }
        editor.apply();

        if (LOG) {
            Log.d(TAG, ".clearStoredQuiz: data cleared");
        }
    }

    public void storeQuiz(JSON_Quiz json_quiz, int questionIndex, int pointsValue) {
        clearStoredQuiz();

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("SAVED_QUIZ", json_quiz.getId());
        editor.putString("TITLE", json_quiz.getTitle());
        editor.putString("DESCRIPTION", json_quiz.getDescription());
        editor.putString("ICON", json_quiz.getIcon_full_path());
        editor.putBoolean("IS_NEW", json_quiz.isIs_new());
        editor.putString("DATE_NEW", json_quiz.getDate_new());
        editor.putInt("CURRENT_QUESTION_INDEX", questionIndex);
        editor.putBoolean("IS_LOCAL", json_quiz.isLocal());
        editor.putInt("POINTS_INTERIM", pointsValue);

        for (int i = questionIndex; i < NUM_OF_QUESTIONS_IN_QUIZ; i++) {
            editor.putString("QUESTION_ID_" + i, json_quiz.getQuestion_ids().get(i));

        }
        editor.apply();

        if (LOG) {
            Log.d(TAG, ".storeQuiz: (JSON_Quiz, questionIndex, pointsValue) = (" + json_quiz.getId() + ", " + questionIndex + ", " + pointsValue + ")");
        }
    }

    public void loadStoredQuiz(TriviaCardVM triviaCardVM) {
        String quizID = preferences.getString("SAVED_QUIZ", null);
        String title = preferences.getString("TITLE", null);
        String description = preferences.getString("DESCRIPTION", null);
        String iconFullPath = preferences.getString("ICON", null);
        int questionIndex = preferences.getInt("CURRENT_QUESTION_INDEX", -1);
        int pointsValue = preferences.getInt("POINTS_INTERIM", -1);
        boolean isLocal = preferences.getBoolean("IS_LOCAL", true);
        boolean isNew = preferences.getBoolean("IS_NEW", false);
        String dateNew = preferences.getString("DATE_NEW", null);

        List<String> qIDs = new ArrayList<>();
        for (int i = 0; i < NUM_OF_QUESTIONS_IN_QUIZ; i++) {
            if (i < questionIndex) {
                qIDs.add(null);
            } else {
                String id = preferences.getString("QUESTION_ID_" + i, null);
                if (id == null) {
                    throw new IllegalStateException("LG>> loadStoredQuiz() >> null values for qID in prefs");
                }
                qIDs.add(id);

                if (LOG) {
                    Log.d(TAG, ".loadStoredQuiz: question ID added = " + id);
                }
            }
        }
        if (questionIndex == -1 || pointsValue == -1 ) {
            throw new IllegalStateException("LG>> loadStoredQuiz() >> qIndex = " + questionIndex + " points = " + pointsValue);
        }

        JSON_Quiz jsonQuiz = new JSON_Quiz(quizID, title, description, qIDs, iconFullPath,true,isNew, dateNew);
        jsonQuiz.setIsLocal(isLocal);

        new TriviaCardQuizFromPrefs(questionIndex, pointsValue, jsonQuiz, triviaCardVM);
    }

    public Set<String> loadFavorites() {
        Set<String> valueToReturn = new HashSet<>(preferences.getStringSet("FAVORITES", new HashSet<>()));
        if (LOG) {
            Log.d(TAG, ".loadFavorites: " + valueToReturn.toString());
        }
        return valueToReturn;
    }

    public void storeFavorites(Set<String>  favorites) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("FAVORITES");
        editor.putStringSet("FAVORITES", favorites);
        editor.apply();
        if (LOG) {
            Log.d(TAG, ".storeFavorites: " + favorites.toString());
        }
    }

    public void storeSubscriptionState(int state) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("SUBSCRIPTION_STATE", state);
        editor.apply();

        storeSubscriptionValidationDate(true);
    }

    public int getSubscriptionState() {
        return preferences.getInt("SUBSCRIPTION_STATE", -1);
    }

    public void storePurchaseToken(String skuId, String purchaseToken) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("SKU_ID", skuId);
        editor.putString("PURCHASE_TOKEN", purchaseToken);
        editor.putString("PURCHASE_TOKEN_VALID_DATE", (LocalDate.now().minusDays(1)).toString());
        editor.putBoolean("PURCHASE_TOKEN_ACKNOWLEDGED", false);
        editor.apply();
    }

    public void storeExpirationDate(long timeInMilliseconds) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("EXPIRATION_TIME_IN_MILLISECONDS", timeInMilliseconds);
        editor.apply();
    }

    public boolean checkExpireApproaching() {
        long expireDate = preferences.getLong("EXPIRATION_TIME_IN_MILLISECONDS", 0);
        long daysToExpire = (expireDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
        return daysToExpire > 0 && daysToExpire <= 3;
    }

    public void markReminderTodayCalled() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("LAST_REMINDER_DATE", LocalDate.now().toString());
        editor.apply();
    }

    public boolean checkIfReminderNotCalledToday() {
        String date = preferences.getString("LAST_REMINDER_DATE", null);
        return date == null || !date.equals(LocalDate.now().toString());
    }

    public void markPurchaseTokenAcknowledged(boolean acknowledgedFlag) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("PURCHASE_TOKEN_ACKNOWLEDGED", acknowledgedFlag);
        editor.apply();
    }

    public void clearPurchaseToken() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("SKU_ID");
        editor.remove("PURCHASE_TOKEN");
        editor.remove("SUBSCRIPTION_VALIDATION_DATE");
        editor.remove("SUBSCRIPTION_STATE");
        editor.remove("PURCHASE_TOKEN_ACKNOWLEDGED");
        editor.remove("EXPIRATION_TIME_IN_MILLISECONDS");
        editor.remove("LAST_REMINDER_DATE");
        editor.apply();
    }

    public String getPurchaseTokenStored() {
        return preferences.getString("PURCHASE_TOKEN", null);
    }

    public String getSkuStored() {
        return preferences.getString("SKU_ID", null);
    }

    public void storeSubscriptionValidationDate(boolean markAsToday) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("SUBSCRIPTION_VALIDATION_DATE", (markAsToday ? LocalDate.now() : LocalDate.now().minusDays(1)).toString());
        editor.apply();
    }

    public boolean checkIfPurchaseTokenValid() {
        String date = preferences.getString("SUBSCRIPTION_VALIDATION_DATE", null);
        int state = preferences.getInt("SUBSCRIPTION_STATE", -1);
        return (state == SUBSCRIPTION_STATE_ACTIVE || state == SUBSCRIPTION_STATE_CANCELLED || state == SUBSCRIPTION_STATE_GRACE)
                && date != null && date.equals(LocalDate.now().toString());
    }

    public boolean checkIfPurchaseTokenAcknowledged() {
        return preferences.getBoolean("PURCHASE_TOKEN_ACKNOWLEDGED",false);
    }

    private String loadJSONFromAsset(String fullPath) {
        String json;
        byte[] buffer = readFileFromAsset(fullPath);
        json = new String(buffer, StandardCharsets.UTF_8);
        if (LOG) {
            Log.d(TAG, ".loadJSONFromAsset: JSON read from asset SUCCESS");
        }
        return json;
    }

    private byte[] readFileFromAsset(String fullPath) {
        byte[] buffer;
        try {
            InputStream is = application.getAssets().open(fullPath);
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            buffer = null;
        }
        return buffer;
    }
}
