package com.example.saferun.ui.coach;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AthleteRequestAdapter extends RecyclerView.Adapter<AthleteRequestAdapter.AthleteViewHolder> {

    private static final String TAG = "AthleteRequestAdapter";

    private Context context;
    private List<User> athletes;
    private OnAthleteRequestListener listener;
    private Map<String, String> requestStatusMap; // Map athlete ID to request status

    public interface OnAthleteRequestListener {
        void onSendRequest(User athlete);
    }

    public AthleteRequestAdapter(Context context, List<User> athletes, OnAthleteRequestListener listener) {
        this.context = context;
        this.athletes = athletes != null ? athletes : new ArrayList<>();
        this.listener = listener;
        this.requestStatusMap = new HashMap<>();

        Log.d(TAG, "Adapter created with " + this.athletes.size() + " athletes");
    }

    @NonNull
    @Override
    public AthleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_athlete_for_request, parent, false);
        return new AthleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AthleteViewHolder holder, int position) {
        User athlete = athletes.get(position);

        // Set athlete name
        holder.nameTextView.setText(athlete.getName());
        Log.d(TAG, "Binding athlete: " + athlete.getName());

        // Set profile image if available
        if (athlete.getProfilePic() != null && !athlete.getProfilePic().isEmpty()) {
            Picasso.get()
                    .load(athlete.getProfilePic())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(holder.profileImageView);
        }

        // Set request status if available
        String status = requestStatusMap.get(athlete.getUid());
        if (status != null) {
            Log.d(TAG, "Status for athlete " + athlete.getName() + ": " + status);
            if (status.equals("pending")) {
                holder.statusTextView.setText("Request pending");
                holder.statusTextView.setTextColor(context.getResources().getColor(R.color.accent_color));
                holder.sendRequestButton.setText("Pending");
                holder.sendRequestButton.setEnabled(false);
            } else if (status.equals("accepted")) {
                holder.statusTextView.setText("Already in your team");
                holder.statusTextView.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                holder.sendRequestButton.setText("Added");
                holder.sendRequestButton.setEnabled(false);
            } else if (status.equals("rejected")) {
                holder.statusTextView.setText("Request declined");
                holder.statusTextView.setTextColor(context.getResources().getColor(android.R.color.holo_red_light));
                holder.sendRequestButton.setText("Invite Again");
                holder.sendRequestButton.setEnabled(true);
            }
        } else {
            holder.statusTextView.setText("No request sent");
            holder.statusTextView.setTextColor(context.getResources().getColor(R.color.text_secondary));
            holder.sendRequestButton.setText("Invite");
            holder.sendRequestButton.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return athletes.size();
    }

    public void updateData(List<User> newAthletes) {
        Log.d(TAG, "Updating data with " + (newAthletes != null ? newAthletes.size() : 0) + " athletes");
        this.athletes = newAthletes;
        notifyDataSetChanged();
    }

    public void updateRequestStatus(String athleteId, String status) {
        Log.d(TAG, "Updating status for athlete " + athleteId + " to " + status);
        requestStatusMap.put(athleteId, status);

        // Find position of athlete in the list and update only that item
        for (int i = 0; i < athletes.size(); i++) {
            if (athletes.get(i).getUid().equals(athleteId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setRequestStatuses(Map<String, String> statusMap) {
        Log.d(TAG, "Setting status map with " + statusMap.size() + " entries");
        this.requestStatusMap = statusMap;
        notifyDataSetChanged();
    }

    class AthleteViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView;
        TextView statusTextView;
        Button sendRequestButton;

        public AthleteViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImageView = itemView.findViewById(R.id.profile_image);
            nameTextView = itemView.findViewById(R.id.name_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);
            sendRequestButton = itemView.findViewById(R.id.send_request_button);

            sendRequestButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    User athlete = athletes.get(position);
                    Log.d(TAG, "Send request button clicked for athlete: " + athlete.getName());
                    listener.onSendRequest(athlete);

                    // Update UI immediately to show pending status
                    updateRequestStatus(athlete.getUid(), "pending");
                }
            });
        }
    }
}