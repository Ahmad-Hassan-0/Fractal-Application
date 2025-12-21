package AppBackend.ResourceManagement;

import android.content.Context;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DataDownloader_naf {

    private static final String TAG = "FRACTAL_DOWNLOADER";

    public interface DownloadListener {
        void onDownloadFinished();
        void onError(String error);
    }

    public static void downloadFiles(Context context, String laptopIp, DownloadListener listener) {
        new Thread(() -> {
            try {
                String baseUrl = "http://" + laptopIp + ":5000/download/";
                String imagesUrl = baseUrl + "images";
                String labelsUrl = baseUrl + "labels";
                String modelUrl = baseUrl + "model"; // New endpoint for TFLite model

                Log.d(TAG, "Starting full sync from: " + baseUrl);

                // 1. Download Images
                downloadFile(context, imagesUrl, "train_images_server.bin");
                // 2. Download Labels
                downloadFile(context, labelsUrl, "train_labels_server.bin");
                // 3. Download Model
                downloadFile(context, modelUrl, "model_server.tflite");

                Log.i(TAG, "All files (Images, Labels, Model) downloaded successfully.");
                listener.onDownloadFinished();

            } catch (Exception e) {
                Log.e(TAG, "Sync failed: " + e.getMessage());
                listener.onError(e.getMessage());
            }
        }).start();
    }

    private static File downloadFile(Context context, String urlStr, String fileName) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Server Error (" + fileName + "): " + connection.getResponseCode());
        }

        File file = new File(context.getFilesDir(), fileName);
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(file)) {

            byte[] data = new byte[8192];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
        } finally {
            connection.disconnect();
        }
        return file;
    }
}