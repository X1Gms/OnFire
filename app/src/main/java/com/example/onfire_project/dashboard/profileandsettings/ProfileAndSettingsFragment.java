package com.example.onfire_project.dashboard.profileandsettings;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.onfire_project.databinding.FragmentProfileAndSettingsBinding;

public class ProfileAndSettingsFragment extends Fragment {

    private FragmentProfileAndSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileAndSettingsViewModel galleryViewModel =
                new ViewModelProvider(this).get(ProfileAndSettingsViewModel.class);

        binding = FragmentProfileAndSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textProfileAndSettings;
        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}