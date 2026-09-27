package com.youtube.ytdl;

import android.os.Environment;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFmpegExecution;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.youtube.ytdl.components.Utils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.concurrent.Executor;

public class YoutubeDownload {
    private final File downloadsDir;
    private final File mainDir;
    private final File musicsDir;
    private final File videosDir;

    private YoutubeProgressCallback<File> callback;
    private YoutubeDownloader downloader;
    private String filename;
    private String videoId;
    private String author;
    private String thumb;
    private int type;

    public YoutubeDownload(String videoId) {
        this.videoId = videoId;

        downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mainDir = new File(downloadsDir, "YTDL");
        musicsDir = new File(mainDir, "Musicas");
        videosDir = new File(mainDir, "Videos");

        videosDir.mkdir();
        musicsDir.mkdir();
    }

    public YoutubeDownload setType(int type) {
        this.type = type;
        return this;
    }

    public YoutubeDownload setDownloader(YoutubeDownloader downloader) {
        this.downloader = downloader;
        return this;
    }

    public YoutubeDownload setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public YoutubeDownload setAuthor(String author) {
        this.author = author;
        return this;
    }

    public YoutubeDownload setThumb(String thumb) {
        this.thumb = thumb;
        return this;
    }

    public YoutubeDownload setCallback(YoutubeProgressCallback<File> callback) {
        this.callback = callback;
        return this;
    }

    public void download() {
        RequestVideoInfo requestVideoInfo = new RequestVideoInfo(videoId);
        Response<VideoInfo> response = downloader.getVideoInfo(requestVideoInfo);
        VideoInfo videoInfo = response.data();

        if (videoInfo == null) {
            return;
        }

        Format format = null;
        File targetDir = null;

        if (type == DownloadType.MP3) {
            format = videoInfo.bestAudioFormat();
            targetDir = musicsDir;
        }

        else if (type == DownloadType.MP4) {
            format = videoInfo.bestVideoWithAudioFormat();
            targetDir = videosDir;
        }

        RequestVideoFileDownload videoFileDownload = new RequestVideoFileDownload(format)
                    .callback(callback)
                    .renameTo(filename)
                    .saveTo(targetDir)
                    .overwriteIfExists(true);

        Response<File> downloadResponse = downloader.downloadVideoFile(videoFileDownload);

        if (type == DownloadType.MP3 && Configs.metadataVideo) {
            new Thread(() -> {
                File outputFile = downloadResponse.data();

                File mp3File = new File(musicsDir, outputFile.getName()
                        .replace(".m4a", ".mp3"));

                String[] cmd = {
                        "-i", outputFile.getAbsolutePath(),
                        "-acodec", "libmp3lame",
                        "-ab", "128k",
                        "-ar", "44100",
                        "-y", mp3File.getAbsolutePath()
                };

                FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
                    if (returnCode == 0) {
                        new Thread(() -> {
                            try {
                                AudioFile audioFile = AudioFileIO.read(mp3File);
                                Tag tag = audioFile.getTag();

                                tag.setField(FieldKey.ALBUM, "YTDL");
                                tag.setField(FieldKey.ARTIST, author);
                                tag.setField(Utils.getArtwork(thumb));

                                AudioFileIO.write(audioFile);
                                outputFile.delete();
                            } catch (Exception err) {
                                err.printStackTrace();
                            }
                        }).start();
                    }
                });
            }).start();
        }
    }

    public static class DownloadType {
        public static final int MP3 = 0;
        public static final int MP4 = 1;
    }
}
