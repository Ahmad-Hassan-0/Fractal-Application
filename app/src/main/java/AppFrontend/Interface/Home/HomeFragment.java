package AppFrontend.Interface.Home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.fractal.databinding.FragmentHomeBinding;
import java.io.File;
import AppBackend.ResourceManagement.DataDownloader_naf;

public class HomeFragment extends Fragment {
    private static final String TAG = "FRACTAL_HOME";
    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        homeViewModel.getLiveStats().observe(getViewLifecycleOwner(), stats -> {
            binding.processorUsagePercentage.setText("Processor Usage: " + stats.getCpuPercentage() + "%");
            binding.ramUsagePercentage.setText("Ram Usage: " + stats.getRamPercentage() + "%");
            binding.systemTempratureDegree.setText("System Temperature: " + stats.getTemperature() + "°C");
            binding.computationUsagePercentage.setText("Computation Usage: 100%");
        });

        homeViewModel.getTrainingProgress().observe(getViewLifecycleOwner(), percent -> {
            binding.diamondToggleButton.setProgress(percent);
        });

        homeViewModel.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            binding.textView.setText(msg);
        });

        binding.diamondToggleButton.setOnClickListener(v -> {
            String laptopIp = "192.168.0.102"; // Ensure this matches your server IP
//            String laptopIp = "10.120.154.12";
//            String laptopIp = "192.168.43.76";
            // Check for ALL three required files
            File imgFile = new File(requireContext().getFilesDir(), "train_images_server.bin");
            File lblFile = new File(requireContext().getFilesDir(), "train_labels_server.bin");
            File modelFile = new File(requireContext().getFilesDir(), "model_server.tflite");

            if (!imgFile.exists() || !lblFile.exists() || !modelFile.exists()) {
                binding.textView.setText("Resources missing. Syncing...");

                DataDownloader_naf.downloadFiles(getContext(), laptopIp, new DataDownloader_naf.DownloadListener() {
                    @Override
                    public void onDownloadFinished() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.textView.setText("Sync Complete. Starting AI...");
//                                homeViewModel.runAILifecycle();
                                homeViewModel.runAILifecycle(laptopIp);
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.textView.setText("Sync Error: " + error);
                            });
                        }
                    }
                });
            } else {
//                homeViewModel.runAILifecycle();
                homeViewModel.runAILifecycle(laptopIp);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}