package com.example.onfire_project.dashboard.profileandsettings;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.onfire_project.databinding.FragmentProfileAndSettingsBinding;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class ProfileAndSettingsFragment extends Fragment {

    private FragmentProfileAndSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileAndSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setupSpinners();
        setupDatePicker();
        setupClicks();

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Select sex","Male", "Female", "Other"}
        );
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spSex.setAdapter(sexAdapter);

        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"kg", "lb"}
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spWeightUnit.setAdapter(weightAdapter);

        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"cm", "ft"}
        );
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spHeightUnit.setAdapter(heightAdapter);
    }

    private void setupDatePicker() {
        binding.etAge.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(requireContext(), (datePicker, y, m, d) -> {
                binding.etAge.setText(d + "/" + (m + 1) + "/" + y);
            }, year, month, day).show();
        });
    }

    private void setupClicks() {
        binding.btnSaveInfo.setOnClickListener(v -> savePersonalInfo());
        binding.btnSaveNewPassword.setOnClickListener(v -> savePassword());
        binding.tvForgot.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Redirect to password recovery", Toast.LENGTH_SHORT).show()
        );
        binding.ivSyncNow.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Syncing data...", Toast.LENGTH_SHORT).show()
        );
    }

    private void savePersonalInfo() {
        String name = binding.etFullName.getText().toString().trim();
        String birthDate = binding.etAge.getText().toString().trim();
        String weightStr = binding.etWeight.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString().trim();

        if (name.isEmpty()) {
            toast("Enter your full name");
            return;
        }
        if (birthDate.isEmpty()) {
            toast("Enter your birth date");
            return;
        }
        if (binding.spSex.getSelectedItemPosition() == 0) {
            toast("Select your sex");
            return;
        }
        if (weightStr.isEmpty() || Double.parseDouble(weightStr) <= 0) {
            toast("Enter a valid weight");
            return;
        }
        if (heightStr.isEmpty() || Double.parseDouble(heightStr) <= 0) {
            toast("Enter a valid height");
            return;
        }

        toast("Personal info saved ✅");
    }

    private void savePassword() {
        String oldPass = binding.etCurrentPass.getText().toString().trim();
        String newPass = binding.etNewPass.getText().toString().trim();

        if (oldPass.isEmpty()) {
            toast("Enter your current password");
            return;
        }

        if (newPass.isEmpty()) {
            toast("Enter your new password");
            return;
        }

        // ✅ Usa a validação completa
        String validationError = validatePassword(newPass);
        if (validationError != null) {
            toast(validationError);
            return;
        }

        toast("Password updated ✅");
    }

    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must have at least 8 characters.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number.";
        }
        if (!password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/`~|\\\\].*")) {
            return "Password must contain at least one special character.";
        }
        return null;
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
