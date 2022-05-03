package com.r42914lg.arkados.vitalk.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.r42914lg.arkados.vitalk.R;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import com.r42914lg.arkados.vitalk.model.WorkItemVideo;

import java.util.List;

public class WorkItemAdapter extends RecyclerView.Adapter<WorkItemAdapter.WorkItemViewHolder> implements View.OnClickListener {
    public static final String TAG = "LG> WorkItemAdapter";

    private final List<WorkItemVideo> videoList;
    private final FirstFragment firstFragment;

    public static class WorkItemViewHolder extends RecyclerView.ViewHolder{
        protected ImageView youTubeThumbnail;
        protected TextView youTubeId;
        protected TextView youTubeTitle;
        protected MaterialButton selectButton;
        protected MaterialButton previewButton;
        protected MaterialButton shareButton;
        protected MaterialButton favButton;

        public WorkItemViewHolder(View itemView) {
            super(itemView);
            youTubeThumbnail = itemView.findViewById(R.id.c_youtube_thumbnail);
            youTubeId = itemView.findViewById(R.id.c_youtube_id);
            youTubeTitle = itemView.findViewById(R.id.c_youtube_title);
            selectButton = itemView.findViewById(R.id.c_work_button);
            previewButton = itemView.findViewById(R.id.c_preview_button);
            shareButton = itemView.findViewById(R.id.c_share_button);
            favButton = itemView.findViewById(R.id.c_fav_button);
        }
    }

    public WorkItemAdapter(List<WorkItemVideo> videoList, FirstFragment firstFragment) {
        this.videoList = videoList;
        this.firstFragment = firstFragment;
    }

    @Override
    public WorkItemAdapter.WorkItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.work_item_recycler_row, parent,false);
        return new WorkItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(WorkItemAdapter.WorkItemViewHolder holder, @SuppressLint("RecyclerView") int position) {
        WorkItemVideo current = videoList.get(position);
        current.positionInAdapter = position;
        ViTalkVM viTalkVM = firstFragment.getViTalkPresenter().getViTalkVM();

        holder.previewButton.setEnabled(current.recordExists);
        holder.shareButton.setEnabled(current.recordExists);

        holder.previewButton.setOnClickListener(view -> {
            if (firstFragment.getViTalkPresenter().noGoogleSignIn()) {
                firstFragment.getViTalkPresenter().doGoogleSignIn();
                return;
            }
            firstFragment.getViTalkPresenter().processPreviewRequest(current.youTubeId);
        });

        holder.shareButton.setOnClickListener(view -> {
            if (firstFragment.getViTalkPresenter().noGoogleSignIn()) {
                firstFragment.getViTalkPresenter().doGoogleSignIn();
                return;
            }
            firstFragment.getViTalkPresenter().processShareRequest(current.youTubeId);
        });

        if (!viTalkVM.checkImageLoaded(current.youTubeId)) {
            holder.youTubeThumbnail.setImageResource(R.drawable.q_default);
        } else {
            holder.youTubeThumbnail.setImageBitmap(viTalkVM.lookupForBitmap(current.youTubeId));
        }

        holder.youTubeId.setText("YouTube ID: " + current.youTubeId);
        holder.youTubeTitle.setText(current.title == null ? current.youTubeId : current.title);
        holder.selectButton.setTag(current);
        holder.selectButton.setOnClickListener(this);

        holder.favButton.setTag(viTalkVM.checkIfFavorite(current.youTubeId));
        holder.favButton.setIconResource(viTalkVM.checkIfFavorite(current.youTubeId)?
                R.drawable.ic_baseline_favorite_24:R.drawable.ic_baseline_favorite_border_24);

        holder.favButton.setOnClickListener(view -> {
            if ((boolean) holder.favButton.getTag()) {
                holder.favButton.setIconResource(R.drawable.ic_baseline_favorite_border_24);
                viTalkVM.processFavoriteRemoved(((WorkItemVideo) holder.selectButton.getTag()).youTubeId);
            }  else {
                holder.favButton.setIconResource(R.drawable.ic_baseline_favorite_24);
                viTalkVM.processFavoriteAdded(((WorkItemVideo) holder.selectButton.getTag()).youTubeId);
            }
            holder.favButton.setTag(!(boolean) holder.favButton.getTag());
        });
    }

    @Override
    public void onClick(View v) {
        if (firstFragment.getViTalkPresenter().noGoogleSignIn()) {
            firstFragment.getViTalkPresenter().doGoogleSignIn();
            return;
        }

        WorkItemVideo current = (WorkItemVideo) v.getTag();

        if (current.recordExists) {

            AlertDialog dialog = new AlertDialog.Builder(firstFragment.getContext()).create();
            dialog.setTitle("Record exist");
            dialog.setMessage("Are you sure you want to overwrite?");
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    firstFragment.setVideoIdForWorkNavigateToSecond(current.youTubeId);
                    dialog.cancel();
                }
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "NO", new DialogInterface.OnClickListener() {
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

        } else {
            firstFragment.setVideoIdForWorkNavigateToSecond(current.youTubeId);
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public void deleteItem(int position) {
        String idToRemove = videoList.get(position).youTubeId;
        videoList.remove(position);
        notifyItemRemoved(position);
        firstFragment.getViTalkPresenter().getViTalkVM().onWorkItemDeleted(idToRemove);
    }

    public Context getContext() {
        return firstFragment.getContext();
    }
}

