package com.example.saferun.ui.athlete;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying run session history items in a RecyclerView
 * for the athlete to see their previous sessions
 */
public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {

    private final Context context;
    private final List<RunSession> sessions;
    private final SimpleDateFormat dateFormat;

    public SessionHistoryAdapter(Context context, List<RunSession> sessions) {
        this.context = context;
        this.sessions = sessions;
        this.dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_history, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        RunSession session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView sessionTitle;
        private final TextView sessionDate;
        private final TextView sessionDetails;
        private final TextView sessionStatus;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionTitle = itemView.findViewById(R.id.session_title);
            sessionDate = itemView.findViewById(R.id.session_date);
            sessionDetails = itemView.findViewById(R.id.session_details);
            sessionStatus = itemView.findViewById(R.id.session_status);
        }

        public void bind(RunSession session) {
            // Set session title
            sessionTitle.setText(session.getTitle());

            // Set session date
            if (session.getDate() != null) {
                sessionDate.setText(dateFormat.format(session.getDate()));
            } else {
                sessionDate.setText("No date");
            }

            // Set session details (distance and duration)
            String details = String.format(Locale.getDefault(), "%.1f km • %d min",
                    session.getDistance(), session.getDuration());
            sessionDetails.setText(details);

            // Set session status with appropriate styling
            String status = capitalizeFirstLetter(session.getStatus());
            sessionStatus.setText(status);

            // Apply different background colors based on status
            if ("Completed".equals(status)) {
                sessionStatus.setBackgroundResource(R.drawable.rounded_status_background);
            } else if ("Active".equals(status)) {
                sessionStatus.setBackgroundResource(R.drawable.rounded_status_background_green);
            } else {
                sessionStatus.setBackgroundResource(R.drawable.rounded_score_background_orange);
            }
        }

        private String capitalizeFirstLetter(String text) {
            if (TextUtils.isEmpty(text)) {
                return "";
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
    }
}