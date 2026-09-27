package com.youtube.ytdl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.youtube.ytdl.components.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DownloadDialog extends DialogFragment {

    private final String embedBase = "<iframe width=100% height=200 src=https://www.youtube.com/" +
            "embed/videoId title=YouTube video player frameborder=0 allow=accelerometer; " +
            "autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share " +
            "allowfullscreen></iframe>";

    private final HashMap<String, Object> downloadData;

    private String thumbVideo;
    private String author;
    private String videoId;
    private String title;
    private int viewType;

    private TextView vProgressText;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");

            if (message.equals("onDownloading")) {
                int downloadCount = intent.getIntExtra("downloadCount", 0);

                MainActivity.mainLoop.post(() -> {
                    vProgressText.setText("Em progresso: " +
                            String.valueOf(downloadCount).concat(" Item(s)"));
                });
            }

            else if (message.equals("onFinished")) {
                MainActivity.mainLoop.post(() -> {
                    Toast.makeText(getContext(), "Download concluído", Toast.LENGTH_SHORT).show();
                    vProgressText.setText("0 Item(s)");
                });
            }
        }
    };

    public DownloadDialog(HashMap<String, Object> downloadData, int viewType) {
        this.downloadData = downloadData;
        this.viewType = viewType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle sis) {
        View view = null;

        if (viewType == VideoType.VIDEO) {
            view = inflater.inflate(R.layout.video_content, viewGroup, false);
        }

        else if (viewType == VideoType.PLAYLIST) {
            view = inflater.inflate(R.layout.playlist_content, viewGroup, false);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle sis) {
        vProgressText = view.findViewById(R.id.progressText);

        if (viewType == VideoType.VIDEO) {
            videoId    = (String) downloadData.get("videoId");
            thumbVideo = (String) downloadData.get("thumbVideo");
            author     = (String) downloadData.get("author");
            title      = (String) downloadData.get("title");

            IntentFilter filter = new IntentFilter("dialogVideo");
            getContext().registerReceiver(receiver, filter);

            Button vDownloadAudio = view.findViewById(R.id.downloadAudio);
            Button vDownloadVideo = view.findViewById(R.id.downloadVideo);
            TextView vTitle       = view.findViewById(R.id.title);
            WebView embed         = view.findViewById(R.id.embed);

            Log.d("VIDEO_ID", videoId);

            vTitle.setText(title);
            embed.setWebChromeClient(new WebChromeClient());
            embed.getSettings().setJavaScriptEnabled(true);
            embed.loadData(embedBase.replace("videoId", videoId),
                    "text/html", "utf-8");

            vDownloadAudio.setOnClickListener(v -> {
                if (Utils.checkStoragePermission(getContext(), getActivity())) {
                    Intent intent = new Intent("downloadService");

                    intent.putExtra("dialogType", "video");
                    intent.putExtra("type", YoutubeDownload.DownloadType.MP3);
                    intent.putExtra("message", "download");
                    intent.putExtra("thumb", thumbVideo);
                    intent.putExtra("videoId", videoId);
                    intent.putExtra("filename", title);
                    intent.putExtra("author", author);

                    getContext().sendBroadcast(intent);
                }
            });

            vDownloadVideo.setOnClickListener(v -> {

                if (Utils.checkStoragePermission(getContext(), getActivity())) {
                    Intent intent = new Intent("downloadService");

                    intent.putExtra("dialogType", "video");
                    intent.putExtra("type", YoutubeDownload.DownloadType.MP4);
                    intent.putExtra("message", "download");
                    intent.putExtra("thumb", -1);
                    intent.putExtra("videoId", videoId);
                    intent.putExtra("filename", title);
                    intent.putExtra("author", -1);

                    getContext().sendBroadcast(intent);
                }
            });
        }

        else if (viewType == VideoType.PLAYLIST) {
            RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
            Button downloadAudio = view.findViewById(R.id.downloadAudio);

            MyRecyclerViewAdapter recyclerViewAdapter = new MyRecyclerViewAdapter(getChildFragmentManager());
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

            recyclerView.setAdapter(recyclerViewAdapter);
            recyclerView.setLayoutManager(layoutManager);

            IntentFilter filter = new IntentFilter("dialogPlaylist");
            getContext().registerReceiver(receiver, filter);

            List<PlaylistVideoDetails> videos =
                    (List<PlaylistVideoDetails>) downloadData.get("videos");

            for (PlaylistVideoDetails video : videos) {
                HashMap<String, Object> data = new HashMap<>();

                int time = video.lengthSeconds();
                int minutes = time / 60;
                int secondsF = time % 60;
                int minutesF = minutes % 60;
                int hoursF = minutes / 60;

                String videoTime = String.format(Locale.getDefault() ,
                        "%02d:%02d:%02d", hoursF, minutesF, secondsF);

                data.put("title", video.title());
                data.put("author", video.author());
                data.put("thumbVideo", video.thumbnails().get(0));
                data.put("videoId", video.videoId());
                data.put("type", VideoType.VIDEO);
                data.put("videoTime", videoTime);

                recyclerViewAdapter.addVideoInfo(data);
            }

            downloadAudio.setOnClickListener(v -> {
                if (Utils.checkStoragePermission(getContext(), getActivity())) {
                    Intent downloadService = new Intent("downloadService");
                    downloadService.putExtra("message", "download");

                    for (PlaylistVideoDetails video : videos) {
                        downloadService.putExtra("dialogType", "playlist");
                        downloadService.putExtra("type", YoutubeDownload.DownloadType.MP3);
                        downloadService.putExtra("thumb", video.thumbnails().get(0));
                        downloadService.putExtra("videoId", video.videoId());
                        downloadService.putExtra("filename", video.title());
                        downloadService.putExtra("author", video.author());
                        getContext().sendBroadcast(downloadService);
                    }
                }
            });
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        getContext().unregisterReceiver(receiver);
    }
}
