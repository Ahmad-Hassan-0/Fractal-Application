package AppFrontend.Interface.Home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fractal.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Click Listener for Diamond
        binding.diamondToggleButton.setOnClickListener(v -> {
            button_toggleTraining();
        });

        return root;
    }

    public void button_toggleTraining() {
        boolean currentStatus = binding.diamondToggleButton.isTraining();

        if (!currentStatus) {
            Log.d("TrainingAction", "Starting Training Simulation...");
            binding.diamondToggleButton.animateLoading(true);
        } else {
            Log.d("TrainingAction", "Stopping Training...");
            binding.diamondToggleButton.animateLoading(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}