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

        // 1. Observe System Stats (CPU, RAM, Temp)
        homeViewModel.getLiveStats().observe(getViewLifecycleOwner(), stats -> {
            binding.processorUsagePercentage.setText("Processor Usage: " + stats.getCpuPercentage() + "%");
            binding.ramUsagePercentage.setText("Ram Usage: " + stats.getRamPercentage() + "%");
            binding.systemTempratureDegree.setText("System Temperature: " + stats.getTemperature() + "°C");
            binding.computationUsagePercentage.setText("Computation Usage: 100%");
        });

        // 2. Observe Training Progress (Updates Diamond View stroke/fill)
        homeViewModel.getTrainingProgress().observe(getViewLifecycleOwner(), percent -> {
            binding.diamondToggleButton.setProgress(percent);
        });

        // 3. Observe Status Messages (UI Text updates)
        homeViewModel.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            binding.textView.setText(msg);
        });

        // 4. Click Trigger for AI Lifecycle
        binding.diamondToggleButton.setOnClickListener(v -> {

            String laptopIp = "192.168.0.101";

            Log.d(TAG, "Diamond Clicked. Checking local data files...");

            // Check if files exist in internal storage
            File imgFile = new File(requireContext().getFilesDir(), "train_images_server.bin");
            File lblFile = new File(requireContext().getFilesDir(), "train_labels_server.bin");

            if (!imgFile.exists() || !lblFile.exists()) {
                // Files missing -> Trigger Download from Python Server
                binding.textView.setText("Data missing. Downloading...");
                Log.i(TAG, "Files not found. Initiating server download...");

                DataDownloader_naf.downloadFiles(getContext(), laptopIp, new DataDownloader_naf.DownloadListener() {
                    @Override
                    public void onDownloadFinished() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.i(TAG, "Download complete. Starting AI Lifecycle.");
                                binding.textView.setText("Download Success. Starting AI...");
                                homeViewModel.runAILifecycle();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.e(TAG, "Download failed: " + error);
                                binding.textView.setText("Server Error: " + error);
                            });
                        }
                    }
                });
            } else {
                // Files exist -> Run AI Lifecycle immediately
                Log.i(TAG, "Local files found. Starting AI Lifecycle.");
                homeViewModel.runAILifecycle();
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