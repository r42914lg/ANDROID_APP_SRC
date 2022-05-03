package com.r42914lg.arkados.triviacard.model;

import android.util.Log;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class JSON_Question {
    public static final String TAG = "LG> JSON_Question";

    private String id;
    private String task_def;
    private String option_1;
    private String option_2;
    private String option_3;
    private String option_4;
    private String option_5;
    private String option_6;
    private int answer;
    private String image_id;
    private boolean active;

    private JSON_Question() {}

    public JSON_Question(String id, String task_def, String option_1, String option_2, String option_3, String option_4, String option_5, String option_6, int answer, String image_id, boolean active) {
        this.task_def = task_def;
        this.option_1 = option_1;
        this.option_2 = option_2;
        this.option_3 = option_3;
        this.option_4 = option_4;
        this.option_5 = option_5;
        this.option_6 = option_6;
        this.answer = answer;
        this.image_id = image_id;
        this.active = active;
        this.id = id;
    }

    public boolean assertIsValid() {
        boolean valueToReturn =  id != null && task_def != null && option_1 != null && option_2 != null
                && option_3 != null && option_4 != null && option_5 != null && option_6 != null
                && answer != 0 && image_id != null;
        if (LOG) {
            Log.d(TAG, ".assertIsValid --> " + valueToReturn);
        }
        return valueToReturn;
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return this.id; }
    public String getTask_def() {
        return task_def;
    }
    public String getOption_1() { return option_1; }
    public String getOption_2() { return option_2; }
    public String getOption_3() {
        return option_3;
    }
    public String getOption_4() {
        return option_4;
    }
    public String getOption_5() {
        return option_5;
    }
    public String getOption_6() {
        return option_6;
    }
    public int getAnswer() {
        return answer;
    }
    public String getImage_id() { return image_id; }
    public boolean getActive() { return active; }
}
