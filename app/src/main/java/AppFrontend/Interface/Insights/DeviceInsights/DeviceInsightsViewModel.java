package AppFrontend.Interface.Insights.DeviceInsights;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeviceInsightsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public DeviceInsightsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Device fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }


    //-----------

    public void button_optimize(){

    }

    public void update_ui_stats(){

    }
}