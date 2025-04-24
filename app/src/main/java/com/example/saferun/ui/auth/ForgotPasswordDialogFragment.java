package com.example.saferun.ui.auth;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.saferun.R;
import com.example.saferun.data.repository.UserRepository;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordDialogFragment extends DialogFragment {

    private static final String ARG_EMAIL = "email";

    private TextInputLayout emailInputLayout;
    private EditText emailEditText;
    private UserRepository userRepository;

    public static ForgotPasswordDialogFragment newInstance(String email) {
        ForgotPasswordDialogFragment fragment = new ForgotPasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = UserRepository.getInstance();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Inflate and set the layout for the dialog
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_forgot_password, null);
        builder.setView(view);

        // Set dialog title
        builder.setTitle(R.string.forgot_password_title);

        // Initialize views
        emailInputLayout = view.findViewById(R.id.forgot_password_email_layout);
        emailEditText = view.findViewById(R.id.forgot_password_email);

        // Pre-fill email if provided
        if (getArguments() != null) {
            String email = getArguments().getString(ARG_EMAIL);
            if (!TextUtils.isEmpty(email)) {
                emailEditText.setText(email);
            }
        }

        // Add buttons
        builder.setPositiveButton(R.string.send, null); // Set listener later to prevent auto-dismiss
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        // Set the positive button click listener after dialog creation
        // This prevents automatic dismiss on error
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = emailEditText.getText().toString().trim();

                // Validate email
                if (TextUtils.isEmpty(email)) {
                    emailInputLayout.setError(getString(R.string.error_field_required));
                    return;
                } else if (!isEmailValid(email)) {
                    emailInputLayout.setError(getString(R.string.error_invalid_email));
                    return;
                }

                // Reset error
                emailInputLayout.setError(null);

                // Send password reset email
                userRepository.resetPassword(email, new UserRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(),
                                R.string.password_reset_email_sent,
                                Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        return dialog;
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }
}