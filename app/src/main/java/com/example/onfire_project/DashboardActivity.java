package com.example.onfire_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.onfire_project.databinding.ActivityDashboardBinding;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private NavigationView navViewMain;
    private DrawerLayout drawer;
    private NavController navController;
    private MenuItem lastSelectedMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDashboardBinding binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarDashboard.toolbar);

        drawer = binding.drawerLayout;
        navViewMain = binding.navViewMain;
        NavigationView navViewBottom = binding.navViewBottom;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_history, R.id.nav_profile_and_settings)
                .setOpenableLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_dashboard);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Configurar manualmente a navegação sem usar NavigationUI para o NavigationView
        setupCustomNavigation();

        navViewBottom.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_sign_out) {
                handleSignOut();
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });

        // Definir o item inicial como selecionado
        if (savedInstanceState == null) {
            setSelectedMenuItem(R.id.nav_dashboard);
        }
    }

    private void setupCustomNavigation() {
        navViewMain.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            drawer.closeDrawer(GravityCompat.START);

            if (id == R.id.nav_start_exercise) {
                // Redirecionar para a Activity de exercício SEM marcar como selecionado
                Intent intent = new Intent(DashboardActivity.this, RunningActivity.class);
                startActivity(intent);
                return true;
            } else {
                // Para fragments, navegar e marcar como selecionado
                try {
                    navController.navigate(id);
                    setSelectedMenuItem(id);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        // Listener para atualizar a seleção quando navegar pelos fragments
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            // Só atualiza a seleção se for um destino válido no menu
            if (destinationId == R.id.nav_dashboard ||
                    destinationId == R.id.nav_history ||
                    destinationId == R.id.nav_profile_and_settings) {
                setSelectedMenuItem(destinationId);
            }
        });
    }

    private void setSelectedMenuItem(int menuItemId) {
        Menu menu = navViewMain.getMenu();

        // Desselecionar todos os itens primeiro
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setChecked(false);
        }

        // Selecionar apenas o item especificado
        MenuItem selectedItem = menu.findItem(menuItemId);
        if (selectedItem != null) {
            selectedItem.setChecked(true);
            lastSelectedMenuItem = selectedItem;
        }
    }

    private void handleSignOut() {
        Toast.makeText(this, getString(R.string.sign_out_message), Toast.LENGTH_SHORT).show();
        // Implementação do sign out
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Quando voltar da RunningActivity, restaurar a seleção anterior
        if (lastSelectedMenuItem != null) {
            lastSelectedMenuItem.setChecked(true);
        } else {
            setSelectedMenuItem(R.id.nav_dashboard);
        }
    }
}