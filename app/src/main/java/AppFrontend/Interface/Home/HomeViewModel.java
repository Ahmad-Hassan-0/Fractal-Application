package AppFrontend.Interface.Home;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

// FIX 1: Ensure this package and class name matches your Kotlin file exactly
import AppFrontend.Interface.Home.trainer_naf;


import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO;
import AppBackend.ResourceManagement.FileUploader_naf;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<Integer> trainingProgress = new MutableLiveData<>(0);
    private final MutableLiveData<ResourceManager_Live_DTO> liveStats = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("inactive");

    // FIX 2: Variable type must match the class name in the Kotlin file
    private final trainer_naf trainerEngine;
    private final ResourceManager_Live_DTO resourceManager;
    private static final String TAG = "FRACTAL_VM";

    public HomeViewModel(@NonNull Application application) {
        super(application);
        // FIX 3: Initialize using the correct class name
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
                statusMessage.postValue("Checking checkpoints...");
                trainerEngine.restoreWeights();

                // FIX 4: Callback must reference the correct class name
                trainerEngine.trainModel(new trainer_naf.TrainingCallback() {
                    @Override
                    public void onProgress(int percentage) {
                        trainingProgress.postValue(percentage);
                        statusMessage.postValue("Training: " + percentage + "%");
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
            }
        }).start();
    }

    public MutableLiveData<Integer> getTrainingProgress() { return trainingProgress; }
    public MutableLiveData<ResourceManager_Live_DTO> getLiveStats() { return liveStats; }
    public MutableLiveData<String> getStatusMessage() { return statusMessage; }
}

//package AppFrontend.Interface.Home;
//
//import android.app.Application;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.lifecycle.AndroidViewModel;
//import androidx.lifecycle.MutableLiveData;
//
//// The class name defined in your Kotlin file is ModelTrainer
//import AppFrontend.Interface.Home.ModelTrainer;
//
//import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class HomeViewModel extends AndroidViewModel {
//    private final MutableLiveData<Integer> trainingProgress = new MutableLiveData<>(0);
//    private final MutableLiveData<ResourceManager_Live_DTO> liveStats = new MutableLiveData<>();
//    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("inactive");
//
//    private final ModelTrainer trainerEngine;
//    private final ResourceManager_Live_DTO resourceManager;
//    private static final String TAG = "FRACTAL_VM";
//
//    public HomeViewModel(@NonNull Application application) {
//        super(application);
//        // Initialize the Kotlin ModelTrainer
//        trainerEngine = new ModelTrainer(application);
//        resourceManager = new ResourceManager_Live_DTO(application);
//
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
//            resourceManager.updateStatistics(application);
//            liveStats.postValue(resourceManager);
//        }, 0, 500, TimeUnit.MILLISECONDS);
//    }
//
//    public void runAILifecycle(String laptopIp) {
//        new Thread(() -> {
//            try {
//                statusMessage.postValue("Initializing...");
//                Log.d(TAG, "Step 1: Initializing Weights");
//                trainerEngine.initializeWeights();
//
//                statusMessage.postValue("Training...");
//                Log.d(TAG, "Step 2: Starting Training Pipeline");
//                trainerEngine.trainModel();
//
//                statusMessage.postValue("Inference...");
//                Log.d(TAG, "Step 3: Running Test Inference");
//                float[] dummyImage = new float[28 * 28];
//                for (int i = 0; i < dummyImage.length; i++) dummyImage[i] = 0.5f;
//                trainerEngine.inferModel(dummyImage);
//
//                statusMessage.postValue("Restoring...");
//                Log.d(TAG, "Step 4: Restoring Weights from Checkpoint");
//                trainerEngine.restoreWeights();
//
//                statusMessage.postValue("Complete");
//                Log.d(TAG, "AI Lifecycle Pipeline finished successfully");
//
//            } catch (Exception e) {
//                statusMessage.postValue("Error");
//                Log.e(TAG, "Pipeline failed: " + e.getMessage());
//            }
//        }).start();
//    }
//
//    // Getters for UI observation
//    public MutableLiveData<String> getStatusMessage() { return statusMessage; }
//    public MutableLiveData<ResourceManager_Live_DTO> getLiveStats() { return liveStats; }
//}