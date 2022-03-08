package ipfs.gomobile.example;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ipfs.gomobile.android.IPFS;

final class DownloadTiming {
    private static final String TAG = "DownloadTiming";
    private final WeakReference<MainActivity> activityRef;
    private final int interval;
    private Thread runner;
    private Long seconds;

    DownloadTiming(MainActivity activity, int interval) {
        activityRef = new WeakReference<>(activity);
        this.interval = interval;
        seconds = 0L;
    }

    void start() {
        runner = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                final MainActivity activity = activityRef.get();
                if (activity == null || activity.isFinishing()) return;
                activity.runOnUiThread(() -> activity.updateDownloadTiming(seconds++, FetchFile.getSizeResult()));
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        runner.start();
    }


    void stop() {
        if (runner != null) {
            if (!runner.isInterrupted()) runner.interrupt();
            runner = null;
        }
    }
}
