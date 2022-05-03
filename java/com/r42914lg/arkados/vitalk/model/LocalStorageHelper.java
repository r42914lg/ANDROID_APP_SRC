package com.r42914lg.arkados.vitalk.model;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class LocalStorageHelper {
    public static final String TAG = "LG> LocalStorageHelper";

    private SharedPreferences preferences;
    private final IDataLoaderListener dataLoaderListener;

    public LocalStorageHelper(IDataLoaderListener dataLoaderListener) {
        this.dataLoaderListener = dataLoaderListener;

        if (LOG) {
            Log.d(TAG, " instance created");
        }
    }

    public void setSharedPreferences(SharedPreferences preferences) { this.preferences = preferences; }
    protected SharedPreferences getPreferences() {
        return preferences;
    }
    public void onClear() {
        preferences = null;
    }

    public List<WorkItemVideo> loadWorkItems() {
        List<WorkItemVideo> workItemVideoList = new CopyOnWriteArrayList<>();//new ArrayList<>();
        Set<String> youTubeIDs = preferences.getStringSet("YOUTUBE_IDs", new HashSet<>());
        for (String youTubeId : youTubeIDs) {
            workItemVideoList.add(new WorkItemVideo(preferences.getBoolean(youTubeId + "_HAS_RECORD", false), youTubeId));
        }
        if (LOG) {
            Log.d(TAG, ".loadWorkItems: items loaded --> " + workItemVideoList.size());
        }
        return workItemVideoList;
    }

    public void storeWorkItems(List<WorkItemVideo> workItemVideoList) {
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> youTubeIDs = new HashSet<>();
        for (WorkItemVideo workItemVideo : workItemVideoList) {
            youTubeIDs.add(workItemVideo.youTubeId);
            editor.remove(workItemVideo.youTubeId + "_HAS_RECORD");
        }

        editor.remove("YOUTUBE_IDs");
        editor.putStringSet("YOUTUBE_IDs", youTubeIDs);

        for (WorkItemVideo workItemVideo : workItemVideoList) {
            editor.putBoolean(workItemVideo.youTubeId + "_HAS_RECORD", workItemVideo.recordExists);
        }

        editor.apply();

        if (LOG) {
            Log.d(TAG, ".storeWorkItems: items stored --> " + workItemVideoList.size());
        }
    }

    public Set<String> loadFavorites() {
        return new HashSet<>(preferences.getStringSet("FAVORITE_IDs", new HashSet<>()));
    }

    public void storeFavorites(Set<String>  favorites) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("FAVORITE_IDs");
        editor.putStringSet("FAVORITE_IDs", favorites);
        editor.apply();
    }

    public void loadImageFromURL(String youTubeId, Executor executor, Handler resultHandler) {
        executor.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(new URL("https://img.youtube.com/vi/" + youTubeId + "/0.jpg")
                        .openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            Bitmap finalBitmap = bitmap;
            if (finalBitmap !=  null) {
                resultHandler.post(() -> dataLoaderListener.callbackLoadImageFromURL(finalBitmap, youTubeId));
            }
        });

        if (LOG) {
            Log.d(TAG, ".loadImageFromURL: accessing URL in a background thread for youTubeId --> " + youTubeId);
        }
    }

    public void queryYouTubeTitleFromURL(String youTubeId, Executor executor, Handler resultHandler) {
        String stringUrl = "https://www.youtube.com/oembed?url=youtube.com/watch?v=" + youTubeId + "&format=json";

        executor.execute(() -> {

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String title = null;

            try {
                URL url = new URL(stringUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                String result = buffer.toString();
                JSONObject jsonResponse = new JSONObject(result);
                title = jsonResponse.getString("title");

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (title !=  null) {
                String finalTitle = title;
                resultHandler.post(() -> dataLoaderListener.callbackVideoTileReceived(finalTitle, youTubeId));
            }
        });

        if (LOG) {
            Log.d(TAG, ".queryYouTubeTitleFromURL: accessing URL in a background thread for youTubeId --> " + youTubeId);
        }
    }
}
