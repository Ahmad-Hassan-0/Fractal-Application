package AppFrontend.Interface.Insights.DeviceInsights;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.List;

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

    // Live Listeners & Internal State
    private BroadcastReceiver batteryReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Handler pollingHandler;
    private Runnable pollingRunnable;

    private int liveVoltage_mV = 0; // Millivolts

    // SharedPreferences for persistent Network Time
    private final SharedPreferences prefs;
    private static final String PREFS_NAME = "FractalDevicePrefs";
    private static final String KEY_NET_START_TIME = "network_start_time";

    public DeviceInsightsViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        startLiveUpdates();
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

    // --- REAL OPTIMIZATION ACTION ---
    public void button_optimize() {
        Context context = getApplication().getApplicationContext();

        // 1. Clear Temp Storage (Cache)
        clearCacheFolder(context.getCacheDir());
        clearCacheFolder(context.getExternalCacheDir());

        // 2. Kill other background processes (Requires KILL_BACKGROUND_PROCESSES permission)
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = context.getPackageManager();
        if (am != null && pm != null) {
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            String myPackage = context.getPackageName();
            for (ApplicationInfo packageInfo : packages) {
                if (!packageInfo.packageName.equals(myPackage)) {
                    am.killBackgroundProcesses(packageInfo.packageName);
                }
            }
        }

        // 3. Request Java Garbage Collection to free RAM
        System.gc();

        // Force an immediate UI update
        updateStats();
    }

    private void startLiveUpdates() {
        Context context = getApplication().getApplicationContext();

        // 1. Continuous Battery Listener
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryPercent.setValue((int) ((level / (float) scale) * 100));

                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging.setValue(status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL);

                liveVoltage_mV = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            }
        };
        context.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // 2. Continuous Network Listener
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {

                        // If we don't have a saved start time, save the exact moment we came online!
                        long savedTime = prefs.getLong(KEY_NET_START_TIME, 0);
                        if (savedTime == 0) {
                            prefs.edit().putLong(KEY_NET_START_TIME, System.currentTimeMillis()).apply();
                        }

                        isOnline.postValue(true);
                        isWifi.postValue(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                        isCellular.postValue(caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    // Wipe the saved time because we lost connection
                    prefs.edit().putLong(KEY_NET_START_TIME, 0).apply();

                    isOnline.postValue(false);
                    isWifi.postValue(false);
                    isCellular.postValue(false);
                }
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(networkCallback);
            }

            // Push initial state immediately in case the callback takes a moment
            Network activeNetwork = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activeNetwork = cm.getActiveNetwork();
            }
            if (activeNetwork != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    long savedTime = prefs.getLong(KEY_NET_START_TIME, 0);
                    if (savedTime == 0) {
                        prefs.edit().putLong(KEY_NET_START_TIME, System.currentTimeMillis()).apply();
                    }
                    isOnline.setValue(true);
                    isWifi.setValue(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                    isCellular.setValue(caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                } else {
                    prefs.edit().putLong(KEY_NET_START_TIME, 0).apply();
                }
            } else {
                prefs.edit().putLong(KEY_NET_START_TIME, 0).apply();
            }
        }

        // 3. Continuous Polling Loop (Every 2 seconds)
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                updateStats();
                pollingHandler.postDelayed(this, 2000);
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void updateStats() {
        Context context = getApplication().getApplicationContext();

        // A. Real Device Standby Time (System Uptime)
        long uptimeMillis = SystemClock.elapsedRealtime();
        double uptimeHours = uptimeMillis / (1000.0 * 60.0 * 60.0);
        deviceStandby.setValue(String.format("%.1f", uptimeHours));

        // B. Persistent Network Standby Time
        long savedStartTime = prefs.getLong(KEY_NET_START_TIME, 0);
        if (Boolean.TRUE.equals(isOnline.getValue()) && savedStartTime > 0) {
            // Compare current real-world time to the saved time
            long netTimeMillis = System.currentTimeMillis() - savedStartTime;
            double netTimeHours = netTimeMillis / (1000.0 * 60.0 * 60.0);
            networkStandby.setValue(String.format("%.2f", netTimeHours));
        } else {
            networkStandby.setValue("0.00");
        }

        // C. Real Power Consumption (Current * Voltage = Power)
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            long currentNow_uA = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            double current_mA = Math.abs(currentNow_uA) / 1000.0;
            double voltage_V = liveVoltage_mV / 1000.0;
            double power_mW = current_mA * voltage_V;

            powerConsumption.setValue(String.format("%.1f", power_mW));
        }

        // D. Real Temp Storage (Cache Size)
        long cacheBytes = getFolderSize(context.getCacheDir()) + getFolderSize(context.getExternalCacheDir());
        double cacheMB = cacheBytes / (1024.0 * 1024.0);
        tempStorage.setValue(String.format("%.2f", cacheMB));

        // E. Real Background Tasks
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            int processCount = runningProcesses != null ? runningProcesses.size() : 0;
            bgTasks.setValue(String.valueOf(processCount));
        }
    }

    private long getFolderSize(File file) {
        long size = 0;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        size += getFolderSize(child);
                    }
                }
            } else {
                size += file.length();
            }
        }
        return size;
    }

    private void clearCacheFolder(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        clearCacheFolder(child);
                    } else {
                        child.delete();
                    }
                }
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Context context = getApplication().getApplicationContext();
        if (batteryReceiver != null) context.unregisterReceiver(batteryReceiver);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null) cm.unregisterNetworkCallback(networkCallback);
        if (pollingHandler != null && pollingRunnable != null) pollingHandler.removeCallbacks(pollingRunnable);
    }
}