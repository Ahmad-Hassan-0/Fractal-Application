package AppFrontend.Interface.Insights.ModelTraining;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ModelTrainingViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ModelTrainingViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Model fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }

    /// /////////

    public void updateModelTrainingGraph(){

    }
}