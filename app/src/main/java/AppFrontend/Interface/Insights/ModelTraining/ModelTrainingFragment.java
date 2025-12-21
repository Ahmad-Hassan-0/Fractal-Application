package AppFrontend.Interface.Insights.ModelTraining;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fractal.databinding.FragmentModelBinding;

public class ModelTrainingFragment extends Fragment {

    private FragmentModelBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ModelTrainingViewModel notificationsViewModel =
                new ViewModelProvider(this).get(ModelTrainingViewModel.class);

        binding = FragmentModelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textModel;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /// ///////////

    public void update_ui_stats(){

    }
}