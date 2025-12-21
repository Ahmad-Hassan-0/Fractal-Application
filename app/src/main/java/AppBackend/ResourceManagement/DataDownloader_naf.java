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

    // This is the interface your Fragment is looking for
    public interface DownloadListener {
        void onDownloadFinished();
        void onError(String error);
    }

    /**
     * This method matches the call in your Fragment:
     * DataDownloader_naf.downloadFiles(getContext(), laptopIp, listener)
     */
    public static void downloadFiles(Context context, String laptopIp, DownloadListener listener) {
        new Thread(() -> {
            try {
                // Construct URLs based on the Python Server routes
                String baseUrl = "http://" + laptopIp + ":5000/download/";
                String imagesUrl = baseUrl + "images";
                String labelsUrl = baseUrl + "labels";

                Log.d(TAG, "Starting download from: " + baseUrl);

                // Download both files to internal storage
                downloadFile(context, imagesUrl, "train_images_server.bin");
                downloadFile(context, labelsUrl, "train_labels_server.bin");

                Log.i(TAG, "Downloads complete.");
                listener.onDownloadFinished();

            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + e.getMessage());
                listener.onError(e.getMessage());
            }
        }).start();
    }

    private static File downloadFile(Context context, String urlStr, String fileName) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000); // 10 seconds timeout
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Server returned HTTP " + connection.getResponseCode());
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