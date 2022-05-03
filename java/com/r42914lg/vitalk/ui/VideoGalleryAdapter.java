package com.r42914lg.arkados.vitalk.ui;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.r42914lg.arkados.vitalk.model.LocalVideo;
import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.model.VideoGalleryScanner;

public class VideoGalleryAdapter extends RecyclerView.Adapter<VideoGalleryAdapter.VideoGalleryViewHolder> {
    public static final String TAG = "LG> VideoGalleryAdapter";

    private final ThirdFragment thirdFragment;
    private final VideoGalleryScanner videoGalleryScanner;

    public static class VideoGalleryViewHolder extends RecyclerView.ViewHolder{
        protected ImageView videoThumbnail;

        public VideoGalleryViewHolder(View itemView) {
            super(itemView);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
        }
    }

    public VideoGalleryAdapter(VideoGalleryScanner videoGalleryScanner, ThirdFragment thirdFragment) {
        this.videoGalleryScanner = videoGalleryScanner;
        this.thirdFragment = thirdFragment;
    }

    @Override
    public VideoGalleryAdapter.VideoGalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_recycler_element, parent, false);
        return new VideoGalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(VideoGalleryAdapter.VideoGalleryViewHolder holder, int position) {
        LocalVideo localVideo = videoGalleryScanner.getVideoList().get(position);
        Bitmap thumbNailBmp = videoGalleryScanner.loadThumbnail(localVideo.uri);
        holder.videoThumbnail.setImageBitmap(thumbNailBmp);
        holder.videoThumbnail.setTag(localVideo);

        holder.videoThumbnail.setOnClickListener(view -> {
            AlertDialog dialog = new AlertDialog.Builder(thirdFragment.getContext()).create();
            dialog.setTitle(thirdFragment.getContext().getString(R.string.dialog_youtube_upload_title));
            dialog.setMessage(thirdFragment.getContext().getString(R.string.dialog_youtube_upload_text));
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", (dialog1, id) -> {
                LocalVideo video = (LocalVideo) view.getTag();
                thirdFragment.startVideoUploadNavigateToFirst(video);
                dialog1.cancel();
            });
            dialog.setOnDismissListener(DialogInterface::cancel);
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return videoGalleryScanner.getVideoList().size();
    }
}
