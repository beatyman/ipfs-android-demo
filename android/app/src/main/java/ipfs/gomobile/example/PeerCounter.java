package ipfs.gomobile.example;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import ipfs.gomobile.android.IPFS;

final class PeerCounter {
    private static final String TAG = "PeerCounter";
    private final WeakReference<MainActivity> activityRef;
    private final int interval;
    private Thread runner;

    PeerCounter(MainActivity activity, int interval) {
        activityRef = new WeakReference<>(activity);
        this.interval = interval;
    }

    void start() {
        runner = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                updatePeerCount();
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        runner.start();
    }

    private void updatePeerCount() {
        final MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        try {
            IPFS ipfs = activity.getIpfs();

            ArrayList<JSONObject> jsonList = ipfs.newRequest("swarm/peers")
                .withOption("verbose", false)
                .withOption("streams", false)
                .withOption("latency", false)
                .withOption("direction", false)
                .sendToJSONList();

            JSONArray peerList = jsonList.get(0).getJSONArray("Peers");

            HashSet<String> peers = new HashSet<>();
            for (int i = 0; i < peerList.length(); i++) {
                JSONObject peer = (JSONObject) peerList.get(i);
                peers.add(peer.getString("Peer"));
            }

            final int count = peers.size();

            activity.runOnUiThread(() -> activity.updatePeerCount(count));
        } catch (JSONException err) {
            // Should only occurs if the peer list is empty, don't log an error
        } catch (Exception err) {
            Log.e(TAG, err.toString());
        }
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }


    void stop() {
        if (runner != null) {
            if (!runner.isInterrupted()) runner.interrupt();
            runner = null;
        }
    }
}
