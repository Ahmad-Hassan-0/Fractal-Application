package AppFrontend.Interface.Home;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import AppFrontend.Interface.Home.trainer_naf;
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO;
import AppBackend.ResourceManagement.FileUploader_naf;
import com.example.fractal.FractalTrainingService; // Import the new service

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<Integer> trainingProgress = new MutableLiveData<>(0);
    private final MutableLiveData<ResourceManager_Live_DTO> liveStats = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("inactive");

    private final trainer_naf trainerEngine;
    private final ResourceManager_Live_DTO resourceManager;
    private static final String TAG = "FRACTAL_VM";

    public HomeViewModel(@NonNull Application application) {
        super(application);
        trainerEngine = new trainer_naf(application);
        resourceManager = new ResourceManager_Live_DTO(application);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            resourceManager.updateStatistics(application);
            liveStats.postValue(resourceManager);
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void runAILifecycle(String laptopIp) {
        new Thread(() -> {
            try {
                // --- 1. START IMMORTAL FOREGROUND SERVICE ---
                Intent startIntent = new Intent(getApplication(), FractalTrainingService.class);
                startIntent.putExtra("PROGRESS", 0);
                ContextCompat.startForegroundService(getApplication(), startIntent);

                statusMessage.postValue("Checking checkpoints...");
                trainerEngine.restoreWeights();

                trainerEngine.trainModel(new trainer_naf.TrainingCallback() {
                    @Override
                    public void onProgress(int percentage) {
                        trainingProgress.postValue(percentage);
                        statusMessage.postValue("Training: " + percentage + "%");

                        // --- 2. UPDATE NOTIFICATION PROGRESS ---
                        Intent updateIntent = new Intent(getApplication(), FractalTrainingService.class);
                        updateIntent.putExtra("PROGRESS", percentage);
                        getApplication().startService(updateIntent);
                    }

                    @Override
                    public void onLog(String message) {
                        Log.d(TAG, "Engine: " + message);
                    }
                });

                statusMessage.postValue("Training Completed");
                Thread.sleep(500);

                statusMessage.postValue("Saving weights...");
                boolean saveSuccessful = trainerEngine.saveWeights();

                if (saveSuccessful) {
                    statusMessage.postValue("Uploading to server...");
                    File ckptFile = new File(getApplication().getFilesDir(), "checkpoint.ckpt");

                    if (ckptFile.exists() && ckptFile.length() > 0) {
                        FileUploader_naf.uploadCheckpoint(getApplication(), laptopIp, ckptFile);
                    }
                }

                float[] dummyInput = new float[784];
                int result = trainerEngine.inferModel(dummyInput);

                Thread.sleep(1000);
                statusMessage.postValue("Inference Result: " + result);
                statusMessage.postValue("Process Complete");

            } catch (Exception e) {
                Log.e(TAG, "Lifecycle Error: " + e.getMessage());
                statusMessage.postValue("Error: " + e.getMessage());
            } finally {
                // --- 3. STOP SERVICE WHEN DONE OR ON CRASH ---
                Intent stopIntent = new Intent(getApplication(), FractalTrainingService.class);
                stopIntent.setAction("STOP_SERVICE");
                getApplication().startService(stopIntent);
            }
        }).start();
    }

    public MutableLiveData<Integer> getTrainingProgress() { return trainingProgress; }
    public MutableLiveData<ResourceManager_Live_DTO> getLiveStats() { return liveStats; }
    public MutableLiveData<String> getStatusMessage() { return statusMessage; }
}