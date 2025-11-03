package com.example.onfire_project.ui.startexercise;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StartExerciseViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public StartExerciseViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is start exercise fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}