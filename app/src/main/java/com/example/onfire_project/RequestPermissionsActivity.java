package com.example.onfire_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class RequestPermissionsActivity extends AppCompatActivity {

    private static final int REQ_ACTIVITY = 200;
    private static final int REQ_STORAGE = 201;
    private static final int REQ_LOCATION = 202;
    private static final int REQ_BACKGROUND = 203;

    // Estágios sequenciais
    private static final int STAGE_ACTIVITY = 0;
    private static final int STAGE_STORAGE = 1;
    private static final int STAGE_LOCATION = 2;
    private static final int STAGE_BACKGROUND = 3;
    private static final int STAGE_DONE = 4;

    private int currentStage = STAGE_ACTIVITY;
    private Button btnPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permissions);

        btnPermissions = findViewById(R.id.btnPermissions);

        // Verificar se já tem todas as permissões necessárias
        if (hasAllRequiredPermissions()) {
            goToDashboard();
            return;
        }

        btnPermissions.setOnClickListener(v -> {
            currentStage = STAGE_ACTIVITY;
            requestNextMissingPermission();
        });
    }

    /**
     * Verifica se todas as permissões obrigatórias estão concedidas
     */
    private boolean hasAllRequiredPermissions() {
        // Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

//        // Storage
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED ||
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                        != PackageManager.PERMISSION_GRANTED) {
//            return false;
//        }

        // Location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    /**
     * Fluxo sequencial para pedir permissões em falta
     */
    private void requestNextMissingPermission() {
        switch (currentStage) {
            case STAGE_ACTIVITY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                                REQ_ACTIVITY);
                        return;
                    }
                }
                // Já tem ou não precisa → próximo estágio
//                currentStage = STAGE_STORAGE;
                currentStage = STAGE_LOCATION;
                requestNextMissingPermission();
                break;

//            case STAGE_STORAGE:
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                        != PackageManager.PERMISSION_GRANTED ||
//                        ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                                != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(this,
//                            new String[]{
//                                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//                            },
//                            REQ_STORAGE);
//                    return;
//                }
//                currentStage = STAGE_LOCATION;
//                requestNextMissingPermission();
//                break;

            case STAGE_LOCATION:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            REQ_LOCATION);
                    return;
                }
                currentStage = STAGE_BACKGROUND;
                requestNextMissingPermission();
                break;

            case STAGE_BACKGROUND:
                // Background location é OPICIONAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQ_BACKGROUND);
                        return;
                    }
                }
                currentStage = STAGE_DONE;
                requestNextMissingPermission();
                break;

            case STAGE_DONE:
                // Todas as permissões processadas
                if (hasAllRequiredPermissions()) {
                    goToDashboard();
                } else {
                    Toast.makeText(this, "Permissões obrigatórias em falta. Tente novamente.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    /**
     * Processa as respostas das permissões
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) {
            // Usuário cancelou
            Toast.makeText(this, "Solicitação cancelada", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case REQ_ACTIVITY:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //currentStage = STAGE_STORAGE;
                    currentStage = STAGE_LOCATION;
                } else {
                    Toast.makeText(this, "Permissão de reconhecimento de atividade é obrigatória", Toast.LENGTH_LONG).show();
                    return;
                }
                break;

//            case REQ_STORAGE:
//                boolean storageGranted = true;
//                for (int result : grantResults) {
//                    if (result != PackageManager.PERMISSION_GRANTED) {
//                        storageGranted = false;
//                        break;
//                    }
//                }
//                if (storageGranted) {
//                    currentStage = STAGE_LOCATION;
//                } else {
//                    Toast.makeText(this, "Permissões de armazenamento são obrigatórias", Toast.LENGTH_LONG).show();
//                    return;
//                }
//                break;

            case REQ_LOCATION:
                boolean locationGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        locationGranted = false;
                        break;
                    }
                }
                if (locationGranted) {
                    currentStage = STAGE_BACKGROUND;
                } else {
                    Toast.makeText(this, "Permissões de localização são obrigatórias", Toast.LENGTH_LONG).show();
                    return;
                }
                break;

            case REQ_BACKGROUND:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Localização em segundo plano não concedida. A app funcionará sem ela.", Toast.LENGTH_SHORT).show();
                }
                currentStage = STAGE_DONE;
                break;
        }

        // Continuar para próxima permissão
        requestNextMissingPermission();
    }

    /**
     * Avança para a DashboardActivity
     */
    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish(); // Fecha esta activity
    }
}