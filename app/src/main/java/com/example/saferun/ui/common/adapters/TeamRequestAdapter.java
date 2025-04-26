package com.example.saferun.ui.common.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.TeamRequest;
import com.example.saferun.util.DateTimeUtil;

import java.util.List;

public class TeamRequestAdapter extends RecyclerView.Adapter<TeamRequestAdapter.TeamRequestViewHolder> {

    private static final String TAG = "TeamRequestAdapter";

    private Context context;
    private List<TeamRequest> requests;
    private OnRequestActionListener listener;
    private boolean isAthlete; // True for athlete view (with accept/reject buttons), false for coach view

    public interface OnRequestActionListener {
        void onAcceptRequest(TeamRequest request);
        void onRejectRequest(TeamRequest request);
    }

    public TeamRequestAdapter(Context context, List<TeamRequest> requests, boolean isAthlete, OnRequestActionListener listener) {
        this.context = context;
        this.requests = requests;
        this.isAthlete = isAthlete;
        this.listener = listener;

        Log.d(TAG, "Adapter created with " + (requests != null ? requests.size() : 0) + " requests, isAthlete: " + isAthlete);
    }

    @NonNull
    @Override
    public TeamRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                isAthlete ? R.layout.item_team_request_athlete : R.layout.item_team_request_coach,
                parent,
                false
        );
        return new TeamRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamRequestViewHolder holder, int position) {
        TeamRequest request = requests.get(position);

        if (isAthlete) {
            // Athlete view shows coach name
            holder.nameTextView.setText(request.getCoachName());
            Log.d(TAG, "Binding coach request: " + request.getCoachName() + ", status: " + request.getStatus());

            // Only show action buttons for pending requests
            if (request.isPending()) {
                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setVisibility(View.VISIBLE);
                holder.statusTextView.setVisibility(View.GONE);
            } else {
                holder.acceptButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
                holder.statusTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setText(capitalizeFirstLetter(request.getStatus()));
            }
        } else {
            // Coach view shows athlete name
            holder.nameTextView.setText(request.getAthleteName());
            holder.statusTextView.setText(capitalizeFirstLetter(request.getStatus()));

            Log.d(TAG, "Binding athlete request: " + request.getAthleteName() + ", status: " + request.getStatus());
        }

        // Set timestamp
        if (request.getTimestamp() != null && holder.timestampTextView != null) {
            String formattedTime = DateTimeUtil.formatTimestamp(request.getTimestamp());
            holder.timestampTextView.setText(formattedTime);
            Log.d(TAG, "Setting timestamp: " + formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void updateData(List<TeamRequest> newRequests) {
        Log.d(TAG, "Updating data with " + (newRequests != null ? newRequests.size() : 0) + " requests");
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    class TeamRequestViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView statusTextView;
        TextView timestampTextView;
        Button acceptButton;
        Button rejectButton;

        public TeamRequestViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);

            if (isAthlete) {
                timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
                acceptButton = itemView.findViewById(R.id.accept_button);
                rejectButton = itemView.findViewById(R.id.reject_button);

                if (acceptButton != null && rejectButton != null) {
                    acceptButton.setOnClickListener(v -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && listener != null) {
                            Log.d(TAG, "Accept button clicked for position: " + position);
                            listener.onAcceptRequest(requests.get(position));
                        }
                    });

                    rejectButton.setOnClickListener(v -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && listener != null) {
                            Log.d(TAG, "Reject button clicked for position: " + position);
                            listener.onRejectRequest(requests.get(position));
                        }
                    });
                }
            }
        }
    }
}