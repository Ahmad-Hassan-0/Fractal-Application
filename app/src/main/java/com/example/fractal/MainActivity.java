package com.example.fractal;

import android.animation.TimeInterpolator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.fractal.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        NavigationUI.setupWithNavController(navView, navController);

        // --- 1. INITIAL STATE SCALING (FIXED) ---
        navView.post(() -> {
            // Check if we are currently on the Get Started screen
            int currentDest = navController.getCurrentDestination() != null ?
                    navController.getCurrentDestination().getId() : -1;

            // ONLY force home selection if we are NOT on the Get Started screen
            // This prevents the app from auto-skipping the onboarding
            if (currentDest != R.id.navigation_get_started) {
                navView.setSelectedItemId(R.id.navigation_home);
            }

            // Apply scaling to the currently selected item
            int selectedId = navView.getSelectedItemId();
            for (int i = 0; i < navView.getMenu().size(); i++) {
                int itemId = navView.getMenu().getItem(i).getItemId();
                View itemView = navView.findViewById(itemId);
                if (itemView != null) {
                    boolean isSelected = (itemId == selectedId);
                    float scale = isSelected ? 1.4f : 1.0f;

                    itemView.setScaleX(scale);
                    itemView.setScaleY(scale);
                    itemView.setAlpha(isSelected ? 1.0f : 0.7f);

                    View labelGroup = itemView.findViewById(com.google.android.material.R.id.navigation_bar_item_labels_group);
                    if (labelGroup != null) {
                        labelGroup.setScaleX(1.0f / scale);
                        labelGroup.setScaleY(1.0f / scale);
                    }
                }
            }
        });

        // --- 2. FLUID ICON SCALING LISTENER ---
        navView.setOnItemSelectedListener(item -> {
            for (int i = 0; i < navView.getMenu().size(); i++) {
                int itemId = navView.getMenu().getItem(i).getItemId();
                View itemView = navView.findViewById(itemId);

                if (itemView != null) {
                    boolean isSelected = (itemId == item.getItemId());
                    float containerScale = isSelected ? 1.4f : 1.0f;

                    TimeInterpolator interpolator = isSelected ?
                            new OvershootInterpolator(1.5f) : new DecelerateInterpolator();

                    itemView.animate()
                            .scaleX(containerScale)
                            .scaleY(containerScale)
                            .alpha(isSelected ? 1.0f : 0.7f)
                            .setDuration(350)
                            .setInterpolator(interpolator)
                            .start();

                    View labelGroup = itemView.findViewById(com.google.android.material.R.id.navigation_bar_item_labels_group);
                    if (labelGroup != null) {
                        float inverseScale = 1.0f / containerScale;
                        labelGroup.animate()
                                .scaleX(inverseScale)
                                .scaleY(inverseScale)
                                .setDuration(350)
                                .setInterpolator(interpolator)
                                .start();
                    }
                }
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        // --- 3. DESTINATION CHANGE LISTENER ---
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            View customHeader = findViewById(R.id.custom_header);

            if (destination.getId() == R.id.navigation_get_started) {
                if (customHeader != null) customHeader.setVisibility(View.GONE);
                navView.setVisibility(View.GONE);
            } else {
                // --- FIX: FORCE SELECTION & SCALE WHEN ENTERING MAIN APP ---
                if (destination.getId() == R.id.navigation_home) {
                    navView.getMenu().findItem(R.id.navigation_home).setChecked(true);

                    // We call post to ensure the view is ready before we scale it
                    navView.post(() -> {
                        View homeItem = navView.findViewById(R.id.navigation_home);
                        if (homeItem != null) {
                            // Scale UP Home
                            homeItem.setScaleX(1.4f);
                            homeItem.setScaleY(1.4f);
                            homeItem.setAlpha(1.0f);

                            // Inverse scale Home text
                            View label = homeItem.findViewById(com.google.android.material.R.id.navigation_bar_item_labels_group);
                            if (label != null) {
                                label.setScaleX(1.0f / 1.4f);
                                label.setScaleY(1.0f / 1.4f);
                            }

                            // Scale DOWN others
                            int[] others = {R.id.navigation_device, R.id.navigation_model};
                            for (int id : others) {
                                View otherItem = navView.findViewById(id);
                                if (otherItem != null) {
                                    otherItem.setScaleX(1.0f);
                                    otherItem.setScaleY(1.0f);
                                    otherItem.setAlpha(0.7f);
                                }
                            }
                        }
                    });
                }

                // Fade in the bars smoothly
                if (customHeader != null && customHeader.getVisibility() != View.VISIBLE) {
                    customHeader.setAlpha(0f);
                    customHeader.setVisibility(View.VISIBLE);
                    customHeader.animate().alpha(1f).setDuration(500).start();
                }

                if (navView.getVisibility() != View.VISIBLE) {
                    navView.setAlpha(0f);
                    navView.setVisibility(View.VISIBLE);
                    navView.animate().alpha(1f).setDuration(500).start();
                }
            }
        });
    }
}