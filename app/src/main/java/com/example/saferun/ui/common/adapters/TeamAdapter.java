package com.example.saferun.ui.common.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.Team;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {

    private final Context context;
    private final List<Team> teams;
    private final OnTeamClickListener listener;

    public interface OnTeamClickListener {
        void onTeamClick(Team team);
    }

    public TeamAdapter(Context context, List<Team> teams, OnTeamClickListener listener) {
        this.context = context;
        this.teams = teams;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.bind(team);
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    public class TeamViewHolder extends RecyclerView.ViewHolder {
        private final ImageView teamIcon;
        private final TextView teamName;
        private final TextView teamDescription;
        private final TextView athleteCount;
        private final MaterialButton viewTeamButton;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            teamIcon = itemView.findViewById(R.id.team_icon);
            teamName = itemView.findViewById(R.id.team_name);
            teamDescription = itemView.findViewById(R.id.team_description);
            athleteCount = itemView.findViewById(R.id.athlete_count);
            viewTeamButton = itemView.findViewById(R.id.view_team_button);
        }

        public void bind(Team team) {
            // Set team name
            teamName.setText(team.getName());

            // Set team description
            if (team.getDescription() != null && !team.getDescription().isEmpty()) {
                teamDescription.setText(team.getDescription());
                teamDescription.setVisibility(View.VISIBLE);
            } else {
                teamDescription.setVisibility(View.GONE);
            }

            // Set athlete count
            int count = team.getAthleteCount();
            athleteCount.setText(count + (count == 1 ? " Athlete" : " Athletes"));

            // Set click listeners
            viewTeamButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTeamClick(team);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTeamClick(team);
                }
            });
        }
    }
}