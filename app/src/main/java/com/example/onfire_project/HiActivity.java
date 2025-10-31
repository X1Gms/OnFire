package com.example.onfire_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class HiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hi);

        View rootView = findViewById(android.R.id.content);

        rootView.setOnClickListener(v -> {
            Intent intent = new Intent(HiActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
