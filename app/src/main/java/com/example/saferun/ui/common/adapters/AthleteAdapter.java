package com.example.saferun.ui.common.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AthleteAdapter extends RecyclerView.Adapter<AthleteAdapter.AthleteViewHolder> {

    private final Context context;
    private final List<User> athletes;
    private final OnAthleteClickListener listener;
    private final DecimalFormat scoreFormat = new DecimalFormat("#.#");

    public interface OnAthleteClickListener {
        void onAthleteClick(User athlete);
    }

    public AthleteAdapter(Context context, List<User> athletes, OnAthleteClickListener listener) {
        this.context = context;
        this.athletes = athletes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AthleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_athlete, parent, false);
        return new AthleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AthleteViewHolder holder, int position) {
        User athlete = athletes.get(position);
        holder.bind(athlete);
    }

    @Override
    public int getItemCount() {
        return athletes.size();
    }

    public class AthleteViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView athleteImage;
        private final TextView athleteName;
        private final TextView athleteEmail;
        private final TextView performanceScore;
        private final MaterialButton viewDetailsButton;

        public AthleteViewHolder(@NonNull View itemView) {
            super(itemView);
            athleteImage = itemView.findViewById(R.id.athlete_image);
            athleteName = itemView.findViewById(R.id.athlete_name);
            athleteEmail = itemView.findViewById(R.id.athlete_email);
            performanceScore = itemView.findViewById(R.id.performance_score);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
        }

        public void bind(User athlete) {
            // Set athlete name
            athleteName.setText(athlete.getName());

            // Set athlete email
            athleteEmail.setText(athlete.getEmail());

            // Set performance score
            double score = athlete.getPerformanceScore();
            performanceScore.setText(scoreFormat.format(score));

            // Adjust color based on performance score
            if (score < 40) {
                performanceScore.setBackgroundResource(R.drawable.rounded_score_background_red);
            } else if (score < 70) {
                performanceScore.setBackgroundResource(R.drawable.rounded_score_background_orange);
            } else {
                performanceScore.setBackgroundResource(R.drawable.rounded_score_background);
            }

            // Load profile image
            if (!TextUtils.isEmpty(athlete.getProfilePic())) {
                Glide.with(context)
                        .load(athlete.getProfilePic())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(athleteImage);
            } else {
                athleteImage.setImageResource(R.drawable.ic_person);
            }

            // Set click listener
            viewDetailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAthleteClick(athlete);
                }
            });

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAthleteClick(athlete);
                }
            });
        }
    }
}