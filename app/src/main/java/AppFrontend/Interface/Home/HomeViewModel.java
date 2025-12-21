package AppFrontend.Interface.Home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.frac_exp_20.trainer_naf;
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<Integer> trainingProgress = new MutableLiveData<>(0);
    private final MutableLiveData<ResourceManager_Live_DTO> liveStats = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("inactive");

    private final trainer_naf trainer;
    private final ResourceManager_Live_DTO resourceManager;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        trainer = new trainer_naf(application);
        resourceManager = new ResourceManager_Live_DTO(application);

        // System Stats Polling (Every 0.5s)
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            resourceManager.updateStatistics(application);
            liveStats.postValue(resourceManager);
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void runAILifecycle() {
        new Thread(() -> {
            // 1. Restore
            statusMessage.postValue("Checking checkpoints...");
            trainer.restoreWeights();

            // 2. Train
            trainer.trainModel(new trainer_naf.TrainingCallback() {
                @Override
                public void onProgress(int percentage) {
                    trainingProgress.postValue(percentage);
                    // Update text with percentage
                    statusMessage.postValue("Training: " + percentage + "%");
                }

                @Override
                public void onLog(String message) {
                    // Internal logs handled in trainer_naf
                }
            });

            // 3. Training Completed (Immediate update after loop finishes)
            statusMessage.postValue("Training Completed");

            // 4. Infer
            float[] testImg = new float[784];
            int result = trainer.inferModel(testImg);

            // 5. Save
            trainer.saveWeights();

            // Final display after a short delay so user sees "Training Completed"
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            statusMessage.postValue("Inference Result: " + result);
            statusMessage.postValue("Training Completed");

        }).start();
    }

    public MutableLiveData<Integer> getTrainingProgress() { return trainingProgress; }
    public MutableLiveData<ResourceManager_Live_DTO> getLiveStats() { return liveStats; }
    public MutableLiveData<String> getStatusMessage() { return statusMessage; }
}