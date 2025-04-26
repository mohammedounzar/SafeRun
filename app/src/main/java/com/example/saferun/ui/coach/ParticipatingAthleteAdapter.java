package com.example.saferun.ui.coach;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.saferun.R;
import com.example.saferun.data.model.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipatingAthleteAdapter extends RecyclerView.Adapter<ParticipatingAthleteAdapter.AthleteViewHolder> {

    private final Context context;
    private final List<User> athletes;
    private final OnAthleteClickListener listener;

    public interface OnAthleteClickListener {
        void onAthleteClick(User athlete);
    }

    public ParticipatingAthleteAdapter(Context context, List<User> athletes, OnAthleteClickListener listener) {
        this.context = context;
        this.athletes = athletes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AthleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participating_athlete, parent, false);
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

    class AthleteViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView athleteImage;
        private final TextView athleteName;
        private final TextView statusTextView;
        private final ImageView statusIndicator;

        public AthleteViewHolder(@NonNull View itemView) {
            super(itemView);
            athleteImage = itemView.findViewById(R.id.athlete_image);
            athleteName = itemView.findViewById(R.id.athlete_name);
            statusTextView = itemView.findViewById(R.id.status_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAthleteClick(athletes.get(position));
                }
            });
        }

        public void bind(User athlete) {
            // Set athlete name
            athleteName.setText(athlete.getName());

            // Set status text and indicator based on session status
            String status = athlete.getSessionStatus();
            if (status != null) {
                switch (status) {
                    case "assigned":
                        statusTextView.setText("Not Started");
                        statusIndicator.setImageResource(R.drawable.status_circle_gray);
                        break;
                    case "active":
                        statusTextView.setText("Running");
                        statusIndicator.setImageResource(R.drawable.status_circle_green);
                        break;
                    case "completed":
                        statusTextView.setText("Completed");
                        statusIndicator.setImageResource(R.drawable.status_circle_blue);
                        break;
                    default:
                        statusTextView.setText(status);
                        statusIndicator.setImageResource(R.drawable.status_circle_gray);
                        break;
                }
            } else {
                statusTextView.setText("Unknown");
                statusIndicator.setImageResource(R.drawable.status_circle_gray);
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
        }
    }
}