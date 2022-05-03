package com.r42914lg.arkados.triviacard.model;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class JSON_Quiz {
    public static final String TAG = "LG> JSON_Quiz";

    private String id;
    private String title;
    private String description;
    private List<String> question_ids;
    private String icon_full_path;
    private boolean active;
    private boolean is_new;
    private String date_new;
    private boolean isLocal;
    private int highScore;
    private String lastPlayedDate;
    private int positionInAdapter = -1;

    private  JSON_Quiz() {}

    protected JSON_Quiz(String id, String title, String description, List<String> question_ids, String icon_full_path, boolean active, boolean is_new, String date_new) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.question_ids = question_ids;
        this.icon_full_path = icon_full_path;
        this.active = active;
        this.is_new = is_new;
        this.date_new = date_new;
    }

    public boolean assertIsValid() {
        boolean valueToReturn = id != null && title != null && description != null && question_ids != null && icon_full_path != null;

        LocalDate date = null;
        try {
            date = LocalDate.parse(date_new);
        } catch (DateTimeParseException dtpe) {
            if (LOG) {
                Log.d(TAG, ".assertIsValid --> WRONG DATA_NEW, check .json" + dtpe.getMessage());
            }
        }

        if (is_new && date == null) {
            valueToReturn = false;
        }

        if (LOG) {
            Log.d(TAG, ".assertIsValid --> Quiz ID = " + id + " " + valueToReturn);
        }
        return valueToReturn;
    }

    public boolean isIs_new() { return is_new; }
    public String getDate_new() { return date_new; }
    public void setLastPlayedDate(String lastPlayedDate) { this.lastPlayedDate = lastPlayedDate; }
    public void setHighScore(int highScore) { this.highScore = highScore; }
    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public void setIsLocal(boolean isLocal) { this.isLocal = isLocal; }
    public boolean isLocal() { return isLocal; }
    public void setPositionInAdapter(int positionInAdapter) { this.positionInAdapter = positionInAdapter; }
    public int getPositionInAdapter() { return positionInAdapter; }

    public String getLastPlayedDate() { return lastPlayedDate; }
    public int getHighScore() { return highScore; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getQuestion_ids() {return question_ids; }
    public String getIcon_full_path() { return icon_full_path; }
    public boolean getActive() { return active; }
}
