package com.example.onfire_project.dashboard.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.onfire_project.R;
import com.example.onfire_project.data.DataRepository;
import com.example.onfire_project.data.FirebaseManager;
import com.example.onfire_project.data.models.Atividade;
import com.example.onfire_project.data.models.Pessoa;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private TextView tvWelcomeUser, tvActivityTime, tvDistanceValue, tvDurationValue, tvCaloriesValue;
    private TextView tvSteps, tvKilometers, tvStepsGoal, tvTimeGoal;
    private ProgressBar progressSteps, progressTime;

    private FirebaseManager firebaseManager;
    private DataRepository repo;
    private Pessoa currentUser;

    private static final int ONU_STEPS = 59900;
    private static final int ONU_MINUTES = 150;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvWelcomeUser   = root.findViewById(R.id.tv_welcome_user);
        tvActivityTime  = root.findViewById(R.id.tvActivityTime);
        tvDistanceValue = root.findViewById(R.id.tvDistanceValue);
        tvDurationValue = root.findViewById(R.id.tvDurationValue);
        tvCaloriesValue = root.findViewById(R.id.tvCaloriesValue);
        tvSteps         = root.findViewById(R.id.tv_steps);
        tvKilometers    = root.findViewById(R.id.tv_kilometers);
        tvStepsGoal     = root.findViewById(R.id.tv_steps_goal);
        tvTimeGoal      = root.findViewById(R.id.tvTimeGoal);
        progressSteps   = root.findViewById(R.id.progress_steps);
        progressTime    = root.findViewById(R.id.progressTime);

        firebaseManager = new FirebaseManager();
        repo = new DataRepository(requireContext());

        loadUserFromFirebase();

        return root;
    }

    private void loadUserFromFirebase() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final String uid = fbUser.getUid();

        firebaseManager.getUserInfo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Pessoa p = snap.getValue(Pessoa.class);
                if (p != null && p.email != null) {
                    currentUser = p;
                    repo.savePessoa(p);
                    updateWelcomeText(p.nome);
                    updateONUProgress(true);
                } else {
                    Pessoa local = repo.getPessoaByEmail(fbUser.getEmail());
                    if (local != null) {
                        currentUser = local;
                        updateWelcomeText(local.nome);
                        updateONUProgress(true);
                    } else {
                        updateWelcomeText("Guest");
                        showEmptyLastActivity();
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWelcomeText(String name) {
        if (name != null)
            tvWelcomeUser.setText("Hello, " + name + "!");
        else
            tvWelcomeUser.setText("Hello!");
    }

    /**
     * Lê as atividades da Firebase, sincroniza com SQLite e atualiza o dashboard.
     */

    private void showEmptyLastActivity() {
        tvActivityTime.setText("--");
        tvDistanceValue.setText("0.00 km");
        tvDurationValue.setText("--:--:--");
        tvCaloriesValue.setText("0 kcal");

        updateONUProgress(false);
    }
        /**
     * Calcula o progresso semanal (steps, km, minutos) e atualiza os cards ONU
     */
        private void updateONUProgress(boolean updateLastActivity) {

            List<Atividade> atividades = repo.getAllAtividades(currentUser.email);
            int steps = 0;
            double km = 0;
            int totalSeconds = 0;

            for (Atividade a : atividades) {
                steps += a.pacos;
                km += a.distancia;

                // Parse time "HH:mm:ss"
                if (a.tempo != null && !a.tempo.isEmpty()) {
                    String[] parts = a.tempo.split(":");
                    if (parts.length == 3) {
                        int hours = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        int seconds = Integer.parseInt(parts[2]);
                        totalSeconds += hours * 3600 + minutes * 60 + seconds;
                    }
                }
            }

            int totalMinutes = totalSeconds / 60;

            // ✅ Prevent crash when no activities exist
            if (updateLastActivity && atividades != null && !atividades.isEmpty()) {
                Atividade primeiraAtividade = atividades.get(0);

                tvDistanceValue.setText(String.format(Locale.getDefault(), "%.2f km", primeiraAtividade.distancia));
                tvDurationValue.setText(primeiraAtividade.tempo != null ? primeiraAtividade.tempo : "--:--:--");
                tvCaloriesValue.setText(String.format(Locale.getDefault(), "%d kcal", primeiraAtividade.calorias));
            } else if (updateLastActivity) {
                // Optional: handle case with no activities
                tvDistanceValue.setText("0.00 km");
                tvDurationValue.setText("--:--:--");
                tvCaloriesValue.setText("0 kcal");
            }

            // --- Atualiza os cards da secção ONU ---
            tvSteps.setText(String.format(Locale.getDefault(), "%d", steps));
            tvKilometers.setText(String.format(Locale.getDefault(), "%.2f", km));

            progressSteps.setMax(ONU_STEPS);
            progressSteps.setProgress(Math.min(steps, ONU_STEPS));

            progressTime.setMax(ONU_MINUTES);
            progressTime.setProgress(Math.min(totalMinutes, ONU_MINUTES));

            tvStepsGoal.setText(String.format(Locale.getDefault(), "%d / %d steps", steps, ONU_STEPS));
            tvTimeGoal.setText(String.format(Locale.getDefault(), "%d / %d min", totalMinutes, ONU_MINUTES));
        }
}
