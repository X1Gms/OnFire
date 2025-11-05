package com.example.onfire_project.ui.profileandsettings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileAndSettingsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ProfileAndSettingsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is profile and settings fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}