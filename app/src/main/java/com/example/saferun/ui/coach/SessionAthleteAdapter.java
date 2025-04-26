package com.example.saferun.ui.coach;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SessionAthleteAdapter extends RecyclerView.Adapter<SessionAthleteAdapter.AthleteViewHolder> {

    private static final String TAG = "SessionAthleteAdapter";
    private Context context;
    private List<User> athletes;
    private Map<String, Boolean> selectedAthletes;

    public SessionAthleteAdapter(Context context, List<User> athletes) {
        this.context = context;
        this.athletes = athletes != null ? athletes : new ArrayList<>();
        this.selectedAthletes = new HashMap<>();

        Log.d(TAG, "Adapter initialized with " + this.athletes.size() + " athletes");

        // Initialize all athletes as unselected
        for (User athlete : this.athletes) {
            selectedAthletes.put(athlete.getUid(), false);
        }
    }

    @NonNull
    @Override
    public AthleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating new ViewHolder");
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_athlete, parent, false);
        return new AthleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AthleteViewHolder holder, int position) {
        User athlete = athletes.get(position);
        Log.d(TAG, "Binding athlete at position " + position + ": " + athlete.getName());

        holder.nameTextView.setText(athlete.getName());

        if (!TextUtils.isEmpty(athlete.getProfilePic())) {
            Glide.with(context)
                    .load(athlete.getProfilePic())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_person);
        }

        // Set checkbox state
        boolean isSelected = selectedAthletes.get(athlete.getUid()) != null ?
                selectedAthletes.get(athlete.getUid()) : false;
        holder.selectCheckBox.setChecked(isSelected);
        Log.d(TAG, "Athlete " + athlete.getName() + " selection state: " + isSelected);

        // Handle the click event
        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.selectCheckBox.isChecked();
            holder.selectCheckBox.setChecked(newState);
            selectedAthletes.put(athlete.getUid(), newState);
            Log.d(TAG, "Clicked athlete " + athlete.getName() + ", new state: " + newState);
        });

        // Handle the checkbox click event
        holder.selectCheckBox.setOnClickListener(v -> {
            boolean newState = holder.selectCheckBox.isChecked();
            selectedAthletes.put(athlete.getUid(), newState);
            Log.d(TAG, "Checkbox clicked for " + athlete.getName() + ", new state: " + newState);
        });
    }

    @Override
    public int getItemCount() {
        return athletes.size();
    }

    public void updateAthletes(List<User> newAthletes) {
        Log.d(TAG, "Updating athletes list. Old size: " + this.athletes.size() +
                ", New size: " + (newAthletes != null ? newAthletes.size() : 0));

        // Make a completely fresh copy of the list to avoid reference issues
        this.athletes = new ArrayList<>();
        if (newAthletes != null) {
            this.athletes.addAll(newAthletes);

            // Initialize selection state for new athletes
            for (User athlete : this.athletes) {
                if (!selectedAthletes.containsKey(athlete.getUid())) {
                    selectedAthletes.put(athlete.getUid(), false);
                }
            }
        }

        Log.d(TAG, "After update, adapter list contains " + this.athletes.size() + " athletes");
        notifyDataSetChanged();
        Log.d(TAG, "Athletes list updated, notifyDataSetChanged called");
    }

    public Map<String, Boolean> getSelectedAthletes() {
        return selectedAthletes;
    }

    public List<String> getSelectedAthleteIds() {
        List<String> selectedIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedAthletes.entrySet()) {
            if (entry.getValue()) {
                selectedIds.add(entry.getKey());
            }
        }
        Log.d(TAG, "Getting selected athlete IDs: " + selectedIds.size() + " athletes selected");
        return selectedIds;
    }

    static class AthleteViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView nameTextView;
        CheckBox selectCheckBox;

        public AthleteViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                profileImageView = itemView.findViewById(R.id.athlete_image);
                nameTextView = itemView.findViewById(R.id.athlete_name);
                selectCheckBox = itemView.findViewById(R.id.select_checkbox);

                if (profileImageView == null) Log.e("AthleteViewHolder", "profileImageView is null");
                if (nameTextView == null) Log.e("AthleteViewHolder", "nameTextView is null");
                if (selectCheckBox == null) Log.e("AthleteViewHolder", "selectCheckBox is null");
            } catch (Exception e) {
                Log.e("AthleteViewHolder", "Error initializing views: " + e.getMessage(), e);
            }
        }
    }
}