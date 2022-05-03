package com.r42914lg.arkados.triviacard.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.r42914lg.arkados.triviacard.R;
import com.r42914lg.arkados.triviacard.model.ITriviaCardPresenter;
import com.r42914lg.arkados.triviacard.model.JSON_Quiz;

import java.text.MessageFormat;
import java.util.List;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> implements View.OnClickListener {
    public static final String TAG = "LG> QuizAdapter";

    private Context context;
    private final List<JSON_Quiz> quizzesList;
    protected final ITriviaCardPresenter presenter;
    private static final int SHOW_MENU = 1;
    private static final int HIDE_MENU = 2;

    public static class QuizViewHolder extends RecyclerView.ViewHolder{
        protected ImageView quizIcon;
        protected TextView quizTitle;
        protected TextView quizDescription;
        protected TextView quizScore;
        protected TextView quizLastPlayedDate;
        protected MaterialButton quizSelect;
        protected MaterialButton favButton;
        protected MaterialButton newButton;

        public QuizViewHolder(View itemView) {
            super(itemView);
            quizIcon = itemView.findViewById(R.id.quiz_icon);
            quizTitle = itemView.findViewById(R.id.quiz_title);
            quizDescription = itemView.findViewById(R.id.quiz_desc);
            quizScore = itemView.findViewById(R.id.quiz_score);
            quizLastPlayedDate = itemView.findViewById(R.id.quiz_last_played);
            quizSelect = itemView.findViewById(R.id.quiz_play);
            favButton = itemView.findViewById(R.id.fav_button);
            newButton = itemView.findViewById(R.id.new_button);
        }
    }

    public QuizAdapter(List<JSON_Quiz> quizzesList, Context context, ITriviaCardPresenter presenter) {
        this.quizzesList = quizzesList;
        this.presenter = presenter;
        this.context = context;
    }

    @Override
    public QuizAdapter.QuizViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.quiz_recycler_row, parent,false);
        return new QuizViewHolder(v);
    }

    @Override
    public void onBindViewHolder(QuizAdapter.QuizViewHolder holder, int position) {
        JSON_Quiz current = quizzesList.get(position);

        current.setPositionInAdapter(position);
        if (LOG) {
            Log.d(TAG, ".onBindViewHolder position --> " + position + " quizID --> " + current.getId() + " highScore --> " + current.getHighScore());
        }

        if (!presenter.getViewModel().checkImageLoaded(current.getIcon_full_path())) {
            holder.quizIcon.setImageResource(R.drawable.q_default);
        } else {
            holder.quizIcon.setImageBitmap(presenter.getViewModel().lookupForBitmap(current.getIcon_full_path()));
        }
        holder.quizTitle.setText(current.getTitle());
        holder.quizDescription.setText(current.getDescription());
        holder.quizScore.setText(current.getHighScore() > 0 ? MessageFormat.format(context.getString(R.string.adapter_best_score_text), current.getHighScore()) : null);
        holder.quizLastPlayedDate.setText(current.getLastPlayedDate() != null ? MessageFormat.format(context.getString(R.string.adapter_last_played_text), current.getLastPlayedDate()) : null);
        holder.quizSelect.setTag(current.getId());
        holder.quizSelect.setOnClickListener(this);

        holder.favButton.setTag(presenter.getViewModel().checkIfFavorite(current.getId()));
        holder.favButton.setIconResource(presenter.getViewModel().checkIfFavorite(current.getId())?
                R.drawable.ic_baseline_favorite_24:R.drawable.ic_baseline_favorite_border_24);
        holder.favButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((boolean) holder.favButton.getTag()) {
                    holder.favButton.setIconResource(R.drawable.ic_baseline_favorite_border_24);
                    presenter.getViewModel().processFavoriteRemoved((String) holder.quizSelect.getTag());
                }  else {
                    holder.favButton.setIconResource(R.drawable.ic_baseline_favorite_24);
                    presenter.getViewModel().processFavoriteAdded((String) holder.quizSelect.getTag());
                }
                holder.favButton.setTag(!(boolean) holder.favButton.getTag());
            }
        });

        holder.newButton.setVisibility(current.isIs_new() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        presenter.processQuizSelected((String) v.getTag());
    }

    @Override
    public int getItemCount() {
        return quizzesList.size();
    }
}