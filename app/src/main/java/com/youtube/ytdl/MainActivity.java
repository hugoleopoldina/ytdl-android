package com.youtube.ytdl;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo;
import com.github.kiulian.downloader.downloader.request.RequestSearchContinuation;
import com.github.kiulian.downloader.downloader.request.RequestSearchResult;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.SearchResultItem;
import com.github.kiulian.downloader.model.search.SearchResultPlaylistDetails;
import com.github.kiulian.downloader.model.search.SearchResultVideoDetails;
import com.youtube.ytdl.components.TextInput;
import com.youtube.ytdl.components.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    public static final Handler mainLoop = new Handler(Looper.getMainLooper());

    public static YoutubeDownloader downloader = new YoutubeDownloader();
    private boolean hasFound = false;
    private SearchResult result;
    private String query;

    private MyRecyclerViewAdapter recyclerViewAdapter;

    // search_pager.xml
    private EditText videoInputSearch;
    private RecyclerView recyclerView;
    private LinearLayout root;

    // main_activity.xml
    private ViewPager viewPager;
    private MyPagerAdapter pagerAdapter;

    private String playlistPattern = "https://www.youtube.com/playlist?list=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent downloadsService = new Intent(this, YoutubeDownloadService.class);
        startService(downloadsService);

        setContentView(R.layout.main_activity);

        viewPager = findViewById(R.id.viewPager);
        View searchPager = getLayoutInflater().inflate(R.layout.search_pager, null, false);
        View playlistPager = getLayoutInflater().inflate(R.layout.playlist_pager, null, false);

        ArrayList<View> views = new ArrayList<>();
        views.add(0, searchPager);
        views.add(1, playlistPager);

        pagerAdapter = new MyPagerAdapter(views);
        viewPager.setAdapter(pagerAdapter);

        videoInputSearch = ((TextInput) searchPager.findViewById(R.id.videoInputSearch)).edit_text;
        recyclerView     = searchPager.findViewById(R.id.recyclerView);
        root             = searchPager.findViewById(R.id.root);

        videoInputSearch.setText("https://www.youtube.com/playlist?list=PLaAHS_pJgi9NjW_6BOGfyrv5XMruCKQT9");

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewAdapter                      = new MyRecyclerViewAdapter(getSupportFragmentManager());

        Utils.setOnFocusInput(this, Arrays.asList(root, recyclerView), videoInputSearch);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);

        videoInputSearch.setOnFocusChangeListener(this);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                View lastItem = layoutManager.findViewByPosition(layoutManager.getItemCount() - 1);

                // Caso encontre o ultimo item no RecyclerView continue a pesquisa e adiciona
                // o resultado à lista LayoutManager
                if (lastItem != null && recyclerViewAdapter.getItemCount() >= 4) {
                    if (!hasFound) {

                        // Adiciona um ProgressBar para indicar carregamento
                        if (!MyRecyclerViewAdapter.hasProgressBar) {
                            MyRecyclerViewAdapter.hasProgressBar = true;

                            HashMap<String, Object> loading = new HashMap<>();
                            loading.put("type", VideoType.LOADING);
                            recyclerViewAdapter.addVideoInfo(loading);
                        }

                        new Thread(() -> {
                            RequestSearchContinuation request = new RequestSearchContinuation(result);
                            result = downloader.searchContinuation(request).data();
                            addResultToList(result);
                            recyclerViewAdapter.removeProgressBar();
                        }).start();

                        hasFound = true;
                    }
                } else if (hasFound) {
                    hasFound = false;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.search) {
            viewPager.setCurrentItem(0, false);
        }

        else if (id == R.id.playlist) {
            viewPager.setCurrentItem(1, false);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        int id = view.getId();

        if (!hasFocus) {
            if (id == videoInputSearch.getId()) {
                recyclerViewAdapter.cleanVideoInfo();

                query = ((EditText) view).getText().toString();
                Utils.disableVirtualKeyboard(view.getContext(), view);

                new Thread(() -> {
                    if (!MyRecyclerViewAdapter.hasProgressBar) {
                        MyRecyclerViewAdapter.hasProgressBar = true;

                        HashMap<String, Object> loading = new HashMap<>();
                        loading.put("type", VideoType.LOADING);
                        recyclerViewAdapter.addVideoInfo(loading);
                    }

                    RequestSearchResult request;

                    try {
                        request = new RequestSearchResult(query);
                        result = downloader.search(request).data();

                    } catch (NullPointerException err) {
                        request = new RequestSearchResult(query
                                .concat(" videos"));
                        result = downloader.search(request).data();
                    }

                    if (result != null) {
                        addResultToList(result);
                        recyclerViewAdapter.removeProgressBar();
                    }
                }).start();
            }
        }
    }

    private void addResultToList(SearchResult result) {
        new Thread(() -> {
            // Verificando se a pesquisa contém um padrão de playlist
            if (query.contains(playlistPattern)) {
                try {
                    HashMap<String, Object> data = new HashMap<>();

                    String playlistId = query.replace(playlistPattern, "");
                    RequestPlaylistInfo playlistReq = new RequestPlaylistInfo(playlistId);
                    Response<PlaylistInfo> playlistResp = downloader.getPlaylistInfo(playlistReq);

                    PlaylistInfo playlistInfo = playlistResp.data();
                    PlaylistVideoDetails videoInfo = playlistInfo.videos().get(0);
                    PlaylistDetails playlistDetails = playlistInfo.details();

                    data.put("title", playlistDetails.title());
                    data.put("author", playlistDetails.author());
                    data.put("thumbVideo", videoInfo.thumbnails().get(0));
                    data.put("videoId", playlistDetails.playlistId());
                    data.put("videoCount", String.valueOf(playlistDetails.videoCount()));
                    data.put("videos", playlistInfo.videos());
                    data.put("type", VideoType.PLAYLIST);

                    mainLoop.post(() -> recyclerViewAdapter.addVideoInfo(data));
                } catch (Exception err) {
                    mainLoop.post(() -> Toast.makeText(this,
                            "Playlist não encontrada", Toast.LENGTH_SHORT).show());
                    recyclerViewAdapter.removeProgressBar();
                }
            }

            else {
                List<SearchResultItem> items = result.items();

                if (!items.isEmpty()) {
                    for (SearchResultItem item : items) {
                        HashMap<String, Object> data = new HashMap<>();

                        String type = item.type().name();

                        if (type.equals("VIDEO")) {
                            SearchResultVideoDetails video = item.asVideo();

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
                        }

                        else if (type.equals("PLAYLIST")) {
                            SearchResultPlaylistDetails playlist = item.asPlaylist();
                            RequestPlaylistInfo req = new RequestPlaylistInfo(playlist.playlistId());
                            PlaylistInfo info = downloader.getPlaylistInfo(req).data();

                            data.put("title", playlist.title());
                            data.put("author", playlist.author());
                            data.put("thumbVideo", playlist.thumbnails().get(0));
                            data.put("videoId", playlist.playlistId());
                            data.put("videoCount", String.valueOf(playlist.videoCount()));
                            data.put("type", VideoType.PLAYLIST);
                            data.put("videos", info.videos());
                        }

                        if (!data.isEmpty()) {
                            mainLoop.post(() -> recyclerViewAdapter.addVideoInfo(data));
                        }
                    }
                }
            }
        }).start();
    }
}
