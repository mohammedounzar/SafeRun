package com.example.saferun.ui.coach;

import android.content.Context;
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

public class RunSessionAdapter extends RecyclerView.Adapter<RunSessionAdapter.SessionViewHolder> {

    private final Context context;
    private final List<RunSession> sessions;
    private final SimpleDateFormat dateFormat;

    public RunSessionAdapter(Context context, List<RunSession> sessions) {
        this.context = context;
        this.sessions = sessions;
        this.dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_run_session, parent, false);
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
        private final TextView titleText;
        private final TextView dateText;
        private final TextView detailsText;
        private final TextView statusText;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.session_title);
            dateText = itemView.findViewById(R.id.session_date);
            detailsText = itemView.findViewById(R.id.session_details);
            statusText = itemView.findViewById(R.id.session_status);
        }

        public void bind(RunSession session) {
            titleText.setText(session.getTitle());

            if (session.getDate() != null) {
                dateText.setText(dateFormat.format(session.getDate()));
            } else {
                dateText.setText("No date available");
            }

            detailsText.setText(String.format(Locale.getDefault(),
                    "%.1f km • %d min",
                    session.getDistance(),
                    session.getDuration()));

            statusText.setText(capitalizeFirstLetter(session.getStatus()));

            // Set status color based on session status
            int statusColor;
            switch (session.getStatus()) {
                case "completed":
                    statusColor = context.getResources().getColor(R.color.success_color);
                    break;
                case "active":
                    statusColor = context.getResources().getColor(R.color.active_color);
                    break;
                default:
                    statusColor = context.getResources().getColor(R.color.accent_color);
                    break;
            }
            statusText.setTextColor(statusColor);
        }

        private String capitalizeFirstLetter(String input) {
            if (input == null || input.isEmpty()) {
                return input;
            }
            return input.substring(0, 1).toUpperCase() + input.substring(1);
        }
    }
}