package com.youtube.ytdl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class YoutubeDownloadService extends Service {
    private NotificationCompat.Builder notification;
    private NotificationManagerCompat manager;

    private static int downloadCount = 0;
    private static int progressMax = 100;
    private Intent dialogIntent;

    private static ThreadPoolExecutor executor = (ThreadPoolExecutor)
            Executors.newFixedThreadPool(2);

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");

            if (message.equals("download")) {
                downloadCount++;

                Runnable runnable = () -> {
                    int type = intent.getIntExtra("type", -1);
                    String dialogType = intent.getStringExtra("dialogType");
                    String filename = intent.getStringExtra("filename");
                    String videoId = intent.getStringExtra("videoId");
                    String author = intent.getStringExtra("author");
                    String thumb = intent.getStringExtra("thumb");

                    if (dialogIntent == null) {
                        if (dialogType.equals("video")) {
                            dialogIntent = new Intent("dialogVideo");
                        }

                        else {
                            dialogIntent = new Intent("dialogPlaylist");
                        }
                    }

                    new YoutubeDownload(videoId)
                            .setDownloader(MainActivity.downloader)
                            .setCallback(new DownloadCallback())
                            .setFilename(filename)
                            .setAuthor(author)
                            .setThumb(thumb)
                            .setType(type)
                            .download();
                };

                executor.execute(runnable);
            }
        }
    };

    @Override
    public void onCreate() {
        manager = createNotificationChannel();
        notification = createNotification();

        IntentFilter intent = new IntentFilter("downloadService");
        registerReceiver(receiver, intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private NotificationCompat.Builder createNotification() {
        return new NotificationCompat.Builder(this, "channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText("Download em progresso")
                .setContentTitle("Download")
                .setOnlyAlertOnce(true)
                .setOngoing(true);
    }

    private NotificationManagerCompat createNotificationChannel() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationChannel channel = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("channel","main",
                    NotificationManager.IMPORTANCE_HIGH);
        }

        if (channel != null) {
            notificationManager.createNotificationChannel(channel);
        }

        return notificationManager;
    }

    private void notifyy(NotificationManagerCompat manager, Notification notification) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(0, notification);
        }
    }

    private class DownloadCallback implements YoutubeProgressCallback<File> {
        @Override
        public void onDownloading(int progress) {
            notification.setContentText("Em progresso: " + downloadCount + " item(s)");
            notification.setContentTitle("Download");
            notification.setOngoing(true);

            notifyy(manager, notification.build());

            dialogIntent.putExtra("message", "onDownloading");
            dialogIntent.putExtra("downloadCount", downloadCount);
            sendBroadcast(dialogIntent);
        }

        @Override
        public void onFinished(File data) {
            if (downloadCount <= 1) {
                MainActivity.mainLoop.postDelayed(() -> {
                    notification.setContentText("Download concluído");
                    notification.setOngoing(false);
                    notifyy(manager, notification.build());
                }, 1000);

                dialogIntent.putExtra("message", "onFinished");
                sendBroadcast(dialogIntent);
            }

            downloadCount--;
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println("DownloadCallback error: " + throwable.getMessage());
        }
    }
}
