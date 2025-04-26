package com.example.saferun.ui.coach;

import android.content.Context;
import android.content.Intent;
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
import com.example.saferun.ui.coach.AthleteDetailActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SessionAthleteListAdapter extends RecyclerView.Adapter<SessionAthleteListAdapter.AthleteViewHolder> {

    private Context context;
    private List<User> athletes;

    public SessionAthleteListAdapter(Context context, List<User> athletes) {
        this.context = context;
        this.athletes = athletes;
    }

    @NonNull
    @Override
    public AthleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_athlete_detail, parent, false);
        return new AthleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AthleteViewHolder holder, int position) {
        User athlete = athletes.get(position);

        // Set athlete name
        holder.nameTextView.setText(athlete.getName());

        // Set athlete email
        holder.statusTextView.setText("Assigned"); // Default status

        // Load profile image
        if (!TextUtils.isEmpty(athlete.getProfilePic())) {
            Glide.with(context)
                    .load(athlete.getProfilePic())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_person);
        }

        // Set click listener to navigate to athlete details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AthleteDetailActivity.class);
            intent.putExtra("athlete_id", athlete.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return athletes.size();
    }

    static class AthleteViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView nameTextView;
        TextView statusTextView;

        public AthleteViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.athlete_image);
            nameTextView = itemView.findViewById(R.id.athlete_name);
            statusTextView = itemView.findViewById(R.id.athlete_status);
        }
    }
}