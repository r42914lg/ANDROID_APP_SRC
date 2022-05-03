package com.r42914lg.arkados.vitalk.model;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoGalleryScanner {
        private final Activity activity;
        List<LocalVideo> videoList;

        public VideoGalleryScanner (Activity activity) {
            this.activity = activity;
            videoList = new ArrayList<>();

            Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);

            String[] projection = new String[] {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE
            };
            String selection = MediaStore.Video.Media.DURATION + " <= ?";
            String[] selectionArgs = new String[] {String.valueOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES))};
            String sortOrder = MediaStore.Video.Media.DATE_ADDED + " ASC";

            try (Cursor cursor = activity.getApplicationContext().getContentResolver().query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            )) {
                // Cache column indices.
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    int duration = cursor.getInt(durationColumn);
                    int size = cursor.getInt(sizeColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    videoList.add(new LocalVideo(contentUri, name, duration, size));
                }
            }
        }

        public List<LocalVideo> getVideoList() {
            return videoList;
        }

        public Bitmap loadThumbnail(Uri contentUri) {
            Bitmap thumbnail = null;
            try {
                thumbnail = activity.getApplicationContext().getContentResolver().loadThumbnail(contentUri, new Size(640, 480), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return thumbnail;
        }
    }
