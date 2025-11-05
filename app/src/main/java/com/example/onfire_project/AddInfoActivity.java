package com.example.onfire_project;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class AddInfoActivity extends AppCompatActivity {

    private EditText etFullName, etBirthDate, etWeight, etHeight;
    private Spinner spSex, spWeightUnit, spHeightUnit;
    private MaterialButton btnGo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info);

        initViews();
        setUpDateInput();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etBirthDate = findViewById(R.id.etAge);
        spSex = findViewById(R.id.spSex);
        spWeightUnit = findViewById(R.id.spWeightUnit);
        spHeightUnit = findViewById(R.id.spHeightUnit);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        btnGo = findViewById(R.id.btnGo);
    }

    private void setUpDateInput(){
        Calendar calendar = Calendar.getInstance();

        etBirthDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etBirthDate.setText(date);
                    },
                    year, month, day);

            datePickerDialog.show();
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select sex","Male", "Female", "Other"}
        );
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSex.setAdapter(sexAdapter);

        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"kg", "lb"}
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWeightUnit.setAdapter(weightAdapter);

        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"cm", "ft"}
        );
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHeightUnit.setAdapter(heightAdapter);
    }

    private void setupListeners() {
        btnGo.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String ageStr = etBirthDate.getText().toString().trim();
            String sex = (String) spSex.getSelectedItem();
            String wu = (String) spWeightUnit.getSelectedItem();
            String hu = (String) spHeightUnit.getSelectedItem();
            double w = parse(etWeight.getText().toString());
            double h = parse(etHeight.getText().toString());

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ageStr.isEmpty()) {
                Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid age format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (age < 10 || age > 100) {
                Toast.makeText(this, "Enter a valid age (10–100)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spSex.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select your sex", Toast.LENGTH_SHORT).show();
                return;
            }

            if (w <= 0) {
                Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show();
                return;
            }

            if (h <= 0) {
                Toast.makeText(this, "Please enter a valid height", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("lb".equals(wu)) w *= 0.45359237;
            if ("ft".equals(hu)) h *= 30.48;

            Toast.makeText(
                    this,
                    "Saved Successfully!",
                    Toast.LENGTH_LONG
            ).show();

            startActivity(new Intent(this, RequestPermissionsActivity.class));
        });
    }

    private double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
