package com.rs_games.resonance.core;

import com.mojang.realmsclient.util.task.DownloadTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ResonanceDownloadManager {
    // Состояния для нашей выездной панели
    public static String currentFileName = "";
    public static long bytesPerSecond = 0;
    private static boolean downloading = false;
    public static float globalProgress = 0.0f;
    public static String downloadSpeed = "0 KB/s";
    public static String currentTask = "Ожидание...";
    // Список задач из JSON
    private static final List<DownloadTask> queue = new ArrayList<>();

    public static void startChain() {
        if (downloading) return;
        downloading = true;
        runNextTask();
    }

    private static void runNextTask() {
        if (queue.isEmpty()) {
            downloading = false;
            return;
        }

        DownloadTask task = queue.remove(0);
        currentFileName = task.name;

    }

    public static boolean isDownloading() { return downloading; }
    public static class DownloadTask {
        public String name;
        public String url;
        public String path;

        public DownloadTask(String name, String url, String path) {
            this.name = name;
            this.url = url;
            this.path = path;
        }
    }
}
