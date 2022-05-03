package com.r42914lg.arkados.triviacard.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class JSON_Image {
    public static final String TAG = "LG> JSON_Image";

    private String id;
    private String image_full_path;
    private List<Integer> cell_values;

    private JSON_Image() {}

    public JSON_Image(String id, String image_full_path, JSONArray values) {
        this.id = id;
        this.image_full_path = image_full_path;
        cell_values = new ArrayList<Integer>(values.length());
        for (int i = 0; i < values.length(); i++) {
            try {
                cell_values.add((Integer) values.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean assertIsValid() {
        boolean valueToReturn = id != null && image_full_path != null && cell_values != null;
        if (LOG) {
            Log.d(TAG, ".assertIsValid --> " + valueToReturn);
        }
        return valueToReturn;
    }

    public void setId(String id) { this.id = id; }
    public String getImage_full_path() { return image_full_path; }
    public List<Integer> getCell_values() { return cell_values; }
}
