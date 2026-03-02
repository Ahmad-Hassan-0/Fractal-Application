//package AppFrontend.Interface.Home;
//
//import android.app.Application;
//import android.content.Intent;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.core.content.ContextCompat;
//import androidx.lifecycle.AndroidViewModel;
//import androidx.lifecycle.MutableLiveData;
//
//import AppFrontend.Interface.Home.trainer_naf;
//import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO;
//import AppBackend.ResourceManagement.FileUploader_naf;
//import com.example.fractal.FractalTrainingService; // Import the new service
//
//import java.io.File;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class HomeViewModel extends AndroidViewModel {
//    private final MutableLiveData<Integer> trainingProgress = new MutableLiveData<>(0);
//    private final MutableLiveData<ResourceManager_Live_DTO> liveStats = new MutableLiveData<>();
//    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("inactive");
//
//    private final trainer_naf trainerEngine;
//    private final ResourceManager_Live_DTO resourceManager;
//    private static final String TAG = "FRACTAL_VM";
//
//    public HomeViewModel(@NonNull Application application) {
//        super(application);
//        trainerEngine = new trainer_naf(application);
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
//                // --- 1. START IMMORTAL FOREGROUND SERVICE ---
//                Intent startIntent = new Intent(getApplication(), FractalTrainingService.class);
//                startIntent.putExtra("PROGRESS", 0);
//                ContextCompat.startForegroundService(getApplication(), startIntent);
//
//                statusMessage.postValue("Checking checkpoints...");
//                trainerEngine.restoreWeights();
//
//                trainerEngine.trainModel(new trainer_naf.TrainingCallback() {
//                    @Override
//                    public void onProgress(int percentage) {
//                        trainingProgress.postValue(percentage);
//                        statusMessage.postValue("Training: " + percentage + "%");
//
//                        // --- 2. UPDATE NOTIFICATION PROGRESS ---
//                        Intent updateIntent = new Intent(getApplication(), FractalTrainingService.class);
//                        updateIntent.putExtra("PROGRESS", percentage);
//                        getApplication().startService(updateIntent);
//                    }
//
//                    @Override
//                    public void onLog(String message) {
//                        Log.d(TAG, "Engine: " + message);
//                    }
//                });
//
//                statusMessage.postValue("Training Completed");
//                Thread.sleep(500);
//
//                statusMessage.postValue("Saving weights...");
//                boolean saveSuccessful = trainerEngine.saveWeights();
//
//                if (saveSuccessful) {
//                    statusMessage.postValue("Uploading to server...");
//                    File ckptFile = new File(getApplication().getFilesDir(), "checkpoint.ckpt");
//
//                    if (ckptFile.exists() && ckptFile.length() > 0) {
//                        FileUploader_naf.uploadCheckpoint(getApplication(), laptopIp, ckptFile);
//                    }
//                }
//
//                float[] dummyInput = new float[784];
//                int result = trainerEngine.inferModel(dummyInput);
//
//                Thread.sleep(1000);
//                statusMessage.postValue("Inference Result: " + result);
//                statusMessage.postValue("Process Complete");
//
//            } catch (Exception e) {
//                Log.e(TAG, "Lifecycle Error: " + e.getMessage());
//                statusMessage.postValue("Error: " + e.getMessage());
//            } finally {
//                // --- 3. STOP SERVICE WHEN DONE OR ON CRASH ---
//                Intent stopIntent = new Intent(getApplication(), FractalTrainingService.class);
//                stopIntent.setAction("STOP_SERVICE");
//                getApplication().startService(stopIntent);
//            }
//        }).start();
//    }
//
//    public MutableLiveData<Integer> getTrainingProgress() { return trainingProgress; }
//    public MutableLiveData<ResourceManager_Live_DTO> getLiveStats() { return liveStats; }
//    public MutableLiveData<String> getStatusMessage() { return statusMessage; }
//}
package AppFrontend.Interface.Home;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO;
import AppBackend.ResourceManagement.ResourceManager.ResourceStatistics;
import AppBackend.LocalTrainingModule.TrainingExecutor.TrainingCallback;
import AppBackend.ResourceManagement.OperationControl.OperationControl;
import com.example.fractal.FractalTrainingService;
import com.example.fractal.Orchestrator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {

    // 1. Grab the Singleton Repository
    private final TrainingStateRepository repository = TrainingStateRepository.getInstance();

    private final MutableLiveData<ResourceManager_Live_DTO> liveStats = new MutableLiveData<>();
    private final ResourceManager_Live_DTO resourceManager;
    private static final String TAG = "FRACTAL_VM";

    public HomeViewModel(@NonNull Application application) {
        super(application);
        resourceManager = new ResourceManager_Live_DTO(application);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            resourceManager.updateStatistics(application);
            liveStats.postValue(resourceManager);
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void toggleAILifecycle() {
        if (!repository.isActive) {
            // 1. Turn ON (Enters Waiting state initially)
            repository.isActive = true;
            repository.isWaiting = true;
            repository.isPaused = false;
            repository.statusMessage.postValue("Initializing...");
            startPipelineThread();

        } else if (repository.isWaiting) {
            // 2. Cancel while WAITING
            repository.isActive = false;
            repository.isWaiting = false;
            repository.statusMessage.postValue("Process Cancelled");

        } else if (!repository.isPaused) {
            // 3. Pause while TRAINING
            repository.isPaused = true;
            repository.statusMessage.postValue("Training Paused");

        } else {
            // 4. Resume while PAUSED
            repository.isPaused = false;
            repository.statusMessage.postValue("Training Resumed...");
        }
    }

    private void startPipelineThread() {
        new Thread(() -> {
            try {
                Intent startIntent = new Intent(getApplication(), FractalTrainingService.class);
                startIntent.putExtra("PROGRESS", 0);
                ContextCompat.startForegroundService(getApplication(), startIntent);

                Orchestrator orchestrator = new Orchestrator(getApplication());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    orchestrator.executeTrainingPipeline(new TrainingCallback() {
                        @Override
                        public void onProgress(int percentage) {
                            repository.trainingProgress.postValue(percentage);
                            Intent updateIntent = new Intent(getApplication(), FractalTrainingService.class);
                            updateIntent.putExtra("PROGRESS", percentage);
                            getApplication().startService(updateIntent);
                        }

                        @Override
                        public void onValidationUpdate(@NonNull String result) {
                            ResourceStatistics stats = repository.detailedStats.getValue();
                            if (stats != null) {
                                stats.setInferenceTesting(result);
                                repository.detailedStats.postValue(stats);
                            }
                        }

                        @Override
                        public void onStatusUpdate(@NonNull String message) {
                            repository.statusMessage.postValue(message);
                        }

                        @Override
                        public void onEpochUpdate(int completedEpochs, int totalEpochs, float loss, @NonNull String timeLeft) {
                            ResourceStatistics currentStats = repository.detailedStats.getValue();
                            if (currentStats != null) {
                                currentStats.setEpochsCompleted(completedEpochs + " / " + totalEpochs);
                                currentStats.setEstimatedTimeLeft(timeLeft);

                                int perf = Math.max(0, 100 - (int)(loss * 100));
                                currentStats.setOverallPerformance(perf + "%");

                                repository.detailedStats.postValue(currentStats);
                            }
                        }

                        // --- NEW CALLBACK IMPLEMENTATIONS ---
                        @Override
                        public boolean isPaused() {
                            return repository.isPaused;
                        }

                        @Override
                        public boolean isCancelled() {
                            return !repository.isActive;
                        }

                        @Override
                        public void onWaitingStateChanged(boolean isWaiting) {
                            repository.isWaiting = isWaiting;
                        }

                        // --- SMART HARDWARE TRAP IMPLEMENTATION ---
                        @Override
                        public String checkLiveConditions() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                OperationControl opControl = new OperationControl(getApplication());
                                return opControl.getViolationMessage();
                            }
                            return null;
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Lifecycle Error: " + e.getMessage());
                repository.statusMessage.postValue("Error: " + e.getMessage());
            } finally {
                // Ensure everything resets if the thread naturally finishes or is cancelled
                repository.isActive = false;
                repository.isWaiting = false;
                repository.isPaused = false;
                repository.trainingProgress.postValue(0);

                Intent stopIntent = new Intent(getApplication(), FractalTrainingService.class);
                stopIntent.setAction("STOP_SERVICE");
                getApplication().startService(stopIntent);
            }
        }).start();
    }

    // 4. Return the Repository's LiveData to the UI
    public MutableLiveData<Integer> getTrainingProgress() { return repository.trainingProgress; }
    public MutableLiveData<String> getStatusMessage() { return repository.statusMessage; }
    public MutableLiveData<ResourceStatistics> getDetailedStats() { return repository.detailedStats; }
    public MutableLiveData<ResourceManager_Live_DTO> getLiveStats() { return liveStats; }
}