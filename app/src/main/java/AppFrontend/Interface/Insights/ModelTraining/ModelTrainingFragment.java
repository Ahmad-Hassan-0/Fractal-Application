package AppFrontend.Interface.Insights.ModelTraining;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.fractal.databinding.FragmentModelBinding;

public class ModelTrainingFragment extends Fragment {

    private FragmentModelBinding binding;
    private ModelTrainingViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(this).get(ModelTrainingViewModel.class);
        binding = FragmentModelBinding.inflate(inflater, container, false);

        // Connect ViewModel text to UI
        viewModel.getText().observe(getViewLifecycleOwner(), binding.textModel::setText);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Start any training listeners here
        update_ui_stats();
    }

    /**
     * Logic for updating the UI based on training progress.
     */
    public void update_ui_stats() {
        if (binding != null) {
            // The TrainingWaveView animates itself via postInvalidateOnAnimation().
            // If you wanted to stop it, you could add a 'setRunning(false)' method.
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}