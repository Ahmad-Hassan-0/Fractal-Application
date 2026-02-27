package AppFrontend.Interface.Insights.DeviceInsights;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class DeviceInsightsViewModel extends AndroidViewModel {

    // Status Values
    private final MutableLiveData<Integer> batteryPercent = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isCharging = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isOnline = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isWifi = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isCellular = new MutableLiveData<>(false);

    // Text Stats
    private final MutableLiveData<String> deviceStandby = new MutableLiveData<>("--");
    private final MutableLiveData<String> networkStandby = new MutableLiveData<>("--");
    private final MutableLiveData<String> powerConsumption = new MutableLiveData<>("--");
    private final MutableLiveData<String> tempStorage = new MutableLiveData<>("--");
    private final MutableLiveData<String> bgTasks = new MutableLiveData<>("--");

    public DeviceInsightsViewModel(@NonNull Application application) {
        super(application);
        update_ui_stats(); // Fetch real data when ViewModel is created
    }

    // --- GETTERS ---
    public LiveData<Integer> getBatteryPercent() { return batteryPercent; }
    public LiveData<Boolean> getIsCharging() { return isCharging; }
    public LiveData<Boolean> getIsOnline() { return isOnline; }
    public LiveData<Boolean> getIsWifi() { return isWifi; }
    public LiveData<Boolean> getIsCellular() { return isCellular; }

    public LiveData<String> getDeviceStandby() { return deviceStandby; }
    public LiveData<String> getNetworkStandby() { return networkStandby; }
    public LiveData<String> getPowerConsumption() { return powerConsumption; }
    public LiveData<String> getTempStorage() { return tempStorage; }
    public LiveData<String> getBgTasks() { return bgTasks; }

    // --- ACTIONS ---
    public void button_optimize() {
        tempStorage.setValue("0.1");
        bgTasks.setValue("0");
    }

    public void update_ui_stats() {
        Context context = getApplication().getApplicationContext();

        // 1. Fetch Live Battery Status
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batPct = (int) ((level / (float) scale) * 100);
            batteryPercent.setValue(batPct);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            isCharging.setValue(isChargingNow);
        }

        // 2. Fetch Live Network Status
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean hasWifi = false;
        boolean hasCellular = false;
        boolean online = false;

        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    online = true; // Lit if either Wifi or Cellular is true

                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        hasWifi = true;
                    }
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        hasCellular = true;
                    }
                }
            }
        }

        isOnline.setValue(online);
        isWifi.setValue(hasWifi);
        isCellular.setValue(hasCellular);

        // Dummy text stats
        deviceStandby.setValue("129");
        networkStandby.setValue("2");
        powerConsumption.setValue("1.2");
        tempStorage.setValue("3.4");
        bgTasks.setValue("3");
    }
}