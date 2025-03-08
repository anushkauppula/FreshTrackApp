package com.example.freshtrack;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivityHome extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup Toolbar and Navigation Drawer Toggle
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();  // Store the ID to compare
        
        if (itemId == R.id.nav_profile) {
            Toast.makeText(this, "Profile Selected", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_settings) {
            Toast.makeText(this, "Settings Selected", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_notifications) {
            Toast.makeText(this, "Notifications Selected", Toast.LENGTH_SHORT).show();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}