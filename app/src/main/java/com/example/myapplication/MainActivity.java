package com.example.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.service_downloader.ServiceDownloader;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private LinearProgressIndicator progressIndicator;
    private Button buttonOpenFile;
    private String fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressIndicator = findViewById(R.id.linearProgressIndicator);
        EditText editTextLink = findViewById(R.id.editTextLink);
        Button buttonDownload = findViewById(R.id.button_download);
        buttonOpenFile = findViewById(R.id.button_open_file);

        buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    if (checkPermissions()) {
//                        String url = editTextLink.getText().toString();
//                        startDownloadService(url);
//                    } else {
//                        requestPermissions();
//                    }
//                } else {
                    String url = editTextLink.getText().toString();
                    startDownloadService(url);
//                }
            }
        });

        buttonOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        // Register the BroadcastReceiver to receive progress updates and download completion
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceDownloader.ACTION_PROGRESS_UPDATE);
        filter.addAction(ServiceDownloader.ACTION_DOWNLOAD_COMPLETE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(progressReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(progressReceiver, filter);
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    private void startDownloadService(String fileUrl) {
        Intent serviceIntent = new Intent(this, ServiceDownloader.class);
        serviceIntent.putExtra("fileUrl", fileUrl);
        serviceIntent.putExtra("fileName", "downloaded_file");
        startService(serviceIntent);
    }

    private void openFile() {
        if (fileUri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(fileUri), "application/octet-stream");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No file to open", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServiceDownloader.ACTION_PROGRESS_UPDATE)) {
                int progress = intent.getIntExtra(ServiceDownloader.EXTRA_PROGRESS, 0);
                progressIndicator.setProgress(progress);
            } else if (intent.getAction().equals(ServiceDownloader.ACTION_DOWNLOAD_COMPLETE)) {
                fileUri = intent.getStringExtra(ServiceDownloader.EXTRA_FILE_URI);
                buttonOpenFile.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Download complete", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                String url = ((EditText) findViewById(R.id.editTextLink)).getText().toString();
                startDownloadService(url);
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
