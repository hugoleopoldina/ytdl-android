package com.youtube.ytdl;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {
    private ArrayList<HashMap<String, Object>> videoInfoList = new ArrayList<>();
    public static boolean hasProgressBar = false;
    private final FragmentManager fragmentManager;

    public MyRecyclerViewAdapter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root = null;

        switch (viewType) {
            case VideoType.VIDEO:
                root = inflater.inflate(R.layout.video_info, parent, false);
                break;

            case VideoType.LOADING:
                root = inflater.inflate(R.layout.loading, parent, false);
                break;

            case VideoType.PLAYLIST:
                root = inflater.inflate(R.layout.playlist_info, parent, false);
                break;
        }

        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View videoInfo = holder.getView();

        if ( videoInfo.getId() == R.id.video_info || videoInfo.getId() == R.id.playlist_info) {
            // Dados para do DialogFragment
            HashMap<String, Object> dialogData = new HashMap<>();

            // Views
            ImageView thumbVideo = videoInfo.findViewById(R.id.thumbnailVideo);
            TextView title       = videoInfo.findViewById(R.id.title);
            TextView author      = videoInfo.findViewById(R.id.author);
            TextView videoTime   = videoInfo.findViewById(R.id.videoTime);

            // Dados
            String thumbVideoData  = (String) videoInfoList.get(position).get("thumbVideo");
            String titleData       = (String) videoInfoList.get(position).get("title");
            String authorData      = (String) videoInfoList.get(position).get("author");
            String videoId         = (String) videoInfoList.get(position).get("videoId");
            int viewType           =  (int)   videoInfoList.get(position).get("type");

            Glide.with(videoInfo.getContext()).load(thumbVideoData).into(thumbVideo);
            title.setText(titleData);
            author.setText(authorData);

            if (videoInfo.getId() == R.id.video_info) {
                String videoTimeData   = (String) videoInfoList.get(position).get("videoTime");
                videoTime.setText(videoTimeData);
            }

            else if (videoInfo.getId() == R.id.playlist_info) {
                TextView vVideoCount  = videoInfo.findViewById(R.id.videoCount);
                String videoCountData = (String) videoInfoList.get(position).get("videoCount");

                vVideoCount.setText(videoCountData.concat(" Vídeos"));

                List<PlaylistVideoDetails> videos = (List<PlaylistVideoDetails>)
                        videoInfoList.get(position).get("videos");
                dialogData.put("videos", videos);
            }

            dialogData.put("title", titleData);
            dialogData.put("videoId", videoId);
            dialogData.put("author", authorData);
            dialogData.put("thumbVideo", thumbVideoData);

            videoInfo.setOnClickListener(view -> {
                new DownloadDialog(dialogData, viewType)
                        .show(fragmentManager, "Dialog");
            });
        }
    }

    @Override
    public int getItemCount() {
        return videoInfoList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View view;

        public ViewHolder(View view) {
            super(view);

            this.view = view;
        }

        private View getView() {
            return view;
        }
    }

    @Override
    // Retorna um tipo de view ao ViewHolder atual, o tipo da view esta é tratada
    // em onCreateViewHolder
    public int getItemViewType(int position) {
        int viewType = (int) videoInfoList.get(position).get("type");
        return viewType;
    }

    public void addVideoInfo(HashMap<String, Object> videoInfo) {
        videoInfoList.add(videoInfo);
        MainActivity.mainLoop.post(this::notifyDataSetChanged);
    }

    public void cleanVideoInfo() {
        videoInfoList.clear();
        MainActivity.mainLoop.post(this::notifyDataSetChanged);
    }

    public void removeProgressBar() {
        for (int index = 0; index < videoInfoList.size(); index++) {
            int type = (int) videoInfoList.get(index).get("type");

            if  (type == VideoType.LOADING) {
                videoInfoList.remove(videoInfoList.get(index));
                hasProgressBar = false;
                break;
            }
        }
        MainActivity.mainLoop.post(this::notifyDataSetChanged);
    }
}
