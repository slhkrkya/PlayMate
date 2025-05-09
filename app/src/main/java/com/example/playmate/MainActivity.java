package com.example.playmate;

import android.content.Context;
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
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.example.playmate.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private DrawerLayout drawerLayout;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar'ı bağla
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Güvenlik: Şifreler düz metin olarak saklanmamalıdır. Üretimde EncryptedSharedPreferences veya benzeri güvenli depolama kullanın.
        // Firebase oturumu kontrolü ve SharedPreferences
        SharedPreferences prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("remember_me", false);

        // "Beni Hatırla" seçili değilse, oturumu tamamen kapat
        if (!rememberMe) {
            FirebaseAuth.getInstance().signOut();
            GoogleSignInClient googleClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
            );
            googleClient.signOut();
        }

        // Geçerli kullanıcı null ise login sayfasına yönlendir
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        if (user == null) {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build();
            navController.navigate(R.id.loginFragment, null, navOptions);
        } else {
            // Kullanıcı varsa header bilgilerini güncelle
            View headerView = navigationView.getHeaderView(0);
            ImageView imageViewProfile = headerView.findViewById(R.id.imageViewProfile);
            TextView textViewUsername = headerView.findViewById(R.id.textViewUsername);
            textViewUsername.setText("Hoş geldin, " + user.getDisplayName());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(imageViewProfile);
            } else {
                Glide.with(this).load(R.drawable.ic_defaultprofile).circleCrop().into(imageViewProfile);
            }
        }

        // AppBar ve Navigation Drawer bağlantısı
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.nav_profile, R.id.editProfileFragment
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Menü tıklamaları (örneğin çıkış)
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_logout) {
                // Firebase çıkışı
                FirebaseAuth.getInstance().signOut();
                // Google çıkışı
                GoogleSignInClient googleClient = GoogleSignIn.getClient(this,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                );
                googleClient.signOut();

                // SharedPreferences temizliği
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                // Login sayfasına geri dön ve backstack'i temizle
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build();
                navController.navigate(R.id.loginFragment, null, navOptions);

                drawerLayout.closeDrawers();
                Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show();
                return true;
            }

            // Diğer menü öğeleri için yönlendirme
            if (navController.getCurrentDestination().getId() == itemId) {
                navController.popBackStack();
            }
            navController.navigate(itemId);
            drawerLayout.closeDrawers();
            return true;
        });

        // login/register ekranlarındayken menüyü gizle
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hideMenu = destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.registerFragment;
            drawerLayout.setDrawerLockMode(hideMenu
                    ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    : DrawerLayout.LOCK_MODE_UNLOCKED);
            if (getSupportActionBar() != null) {
                if (hideMenu) getSupportActionBar().hide();
                else getSupportActionBar().show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}