package com.example.playmate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private DrawerLayout drawerLayout;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar'ı ayarla
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Header içeriğini ayarla
        View headerView = navigationView.getHeaderView(0);
        ImageView imageViewProfile = headerView.findViewById(R.id.imageViewProfile);
        TextView textViewUsername = headerView.findViewById(R.id.textViewUsername);

        // Test verileri – Firebase ile değiştireceğiz
        textViewUsername.setText("Hoş geldin, Oyuncu!");
        Glide.with(this)
                .load(R.drawable.ic_defaultprofile)
                .circleCrop()
                .into(imageViewProfile);

        // NavController'ı güvenli şekilde al
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Beni Hatırla kontrolü
        SharedPreferences prefs = getSharedPreferences("login_prefs", 0);
        boolean rememberMe = prefs.getBoolean("remember_me", false);
        if (rememberMe && FirebaseAuth.getInstance().getCurrentUser() != null) {
            navController.navigate(R.id.homeFragment);
        }

        // AppBarConfiguration DrawerLayout ile tanımlanıyor
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.nav_profile, R.id.editProfileFragment
        )
                .setOpenableLayout(drawerLayout)
                .build();

        // Toolbar ve NavigationView NavController ile eşleştiriliyor
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Menüdeki öğeleri dinle (ör. Logout)
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int currentDestinationId = navController.getCurrentDestination().getId();

            if (itemId == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();

                SharedPreferences.Editor editor = getSharedPreferences("login_prefs", 0).edit();
                editor.putBoolean("remember_me", false);
                editor.apply();

                navController.navigate(R.id.loginFragment);
                drawerLayout.closeDrawers();
                Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show();
                return true;
            }

            // Eğer tıklanan item zaten açık olan fragment ise önce popBackStack yap
            if (itemId == currentDestinationId) {
                navController.popBackStack(); // önce çık
                navController.navigate(itemId); // sonra tekrar git
            } else {
                navController.navigate(itemId);
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // Login ve Register ekranlarında menüyü gizle
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment || destination.getId() == R.id.registerFragment) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                if (getSupportActionBar() != null) getSupportActionBar().hide();
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                if (getSupportActionBar() != null) getSupportActionBar().show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}