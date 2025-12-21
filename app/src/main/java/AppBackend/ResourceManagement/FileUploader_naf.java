package AppBackend.ResourceManagement;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileUploader_naf {
    private static final String TAG = "FRACTAL_UPLOADER";

    public static void uploadCheckpoint(Context context, String laptopIp, File file) {
        new Thread(() -> {
            String urlString = "http://" + laptopIp + ":5000/upload/checkpoint";
            String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            try {
                Log.d(TAG, "Starting upload of: " + file.getName());
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // Content Wrapper for Multipart
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
                dos.writeBytes(lineEnd);

                // Write actual file bytes
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Response check
                int serverResponseCode = conn.getResponseCode();
                if (serverResponseCode == 200) {
                    Log.i(TAG, "UPLOAD SUCCESS: Checkpoint sent to server.");
                } else {
                    Log.e(TAG, "UPLOAD FAILED: Server returned " + serverResponseCode);
                }

                fis.close();
                dos.flush();
                dos.close();

            } catch (Exception e) {
                Log.e(TAG, "Upload Error: " + e.getMessage());
            }
        }).start();
    }
}