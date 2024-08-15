package com.example.myapplication.service_downloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServiceDownloader extends Service {

    public static final String CHANNEL_ID = "ServiceDownloaderChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_PROGRESS_UPDATE = "com.example.myapplication.PROGRESS_UPDATE";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.example.myapplication.DOWNLOAD_COMPLETE";
    public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";
    public static final String EXTRA_FILE_URI = "EXTRA_FILE_URI";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String fileUrl = intent.getStringExtra("fileUrl");
        String fileName = intent.getStringExtra("fileName");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading file")
                .setContentText(fileName)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        new DownloadFileTask().execute(fileUrl, fileName);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, File> {
        @Override
        protected File doInBackground(String... params) {
            String fileUrl = params[0];
            String fileName = params[1];
            File outputFile = null;

            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                outputFile = new File(getExternalFilesDir(null), fileName);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                long totalFileSize = connection.getContentLength();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    publishProgress((int) ((totalBytesRead * 100) / totalFileSize));
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return outputFile;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            Intent progressIntent = new Intent(ACTION_PROGRESS_UPDATE);
            progressIntent.putExtra(EXTRA_PROGRESS, progress[0]);
            sendBroadcast(progressIntent);
        }

        @Override
        protected void onPostExecute(File file) {
            if (file != null) {
                Uri fileUri = FileProvider.getUriForFile(ServiceDownloader.this, "com.example.myapplication.fileprovider", file);
                Intent completeIntent = new Intent(ACTION_DOWNLOAD_COMPLETE);
                completeIntent.putExtra(EXTRA_FILE_URI, fileUri.toString());
                sendBroadcast(completeIntent);

                Notification notification = new NotificationCompat.Builder(ServiceDownloader.this, CHANNEL_ID)
                        .setContentTitle("Download complete")
                        .setContentText(file.getName())
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();

                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, notification);
                }
            }

            stopForeground(true);
            stopSelf();
        }
    }
}
