package com.example.saferun.ui.athlete;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AthleteRunSessionAdapter extends RecyclerView.Adapter<AthleteRunSessionAdapter.SessionViewHolder> {

    private final Context context;
    private final List<RunSession> sessions;
    private final OnSessionClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnSessionClickListener {
        void onSessionClick(RunSession session);
    }

    public AthleteRunSessionAdapter(Context context, List<RunSession> sessions, OnSessionClickListener listener) {
        this.context = context;
        this.sessions = sessions;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
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

    public class SessionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final TextView distanceTextView;
        private final TextView durationTextView;
        private final TextView detailsTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleTextView = itemView.findViewById(R.id.session_title);
            dateTextView = itemView.findViewById(R.id.session_date);
            distanceTextView = itemView.findViewById(R.id.session_distance);
            durationTextView = itemView.findViewById(R.id.session_duration);
            detailsTextView = itemView.findViewById(R.id.session_details);
        }

        public void bind(RunSession session) {
            // Set title
            titleTextView.setText(session.getTitle());

            // Set date
            if (session.getDate() != null) {
                dateTextView.setText(dateFormat.format(session.getDate()));
            } else {
                dateTextView.setText("No date");
            }

//            // Set distance
//            distanceTextView.setText(String.format(Locale.getDefault(), "%.1f km", session.getDistance()));
//
//            // Set duration
//            durationTextView.setText(String.format(Locale.getDefault(), "%d min", session.getDuration()));

            detailsTextView.setText(String.format(Locale.getDefault(), "%.1f km • %d min", session.getDistance(), session.getDuration()));


            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });
        }
    }
}