package ipfs.gomobile.example;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;

import ipfs.gomobile.android.IPFS;

final class StartIPFS extends AsyncTask<Void, Void, String> {
    private static final String TAG = "StartIPFS";

    private final WeakReference<MainActivity> activityRef;
    private boolean backgroundError;

    StartIPFS(MainActivity activity) {
        activityRef = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected String doInBackground(Void... v) {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            cancel(true);
            return null;
        }

        try {
            IPFS ipfs = new IPFS(activity.getApplicationContext());
            // 恢复默认配置
//            ipfs.setConfig(null);
            // 修改配置信息
            setConfig(ipfs);
            // 重启配置
            ipfs.start();

            ArrayList<JSONObject> jsonList = ipfs.newRequest("id").sendToJSONList();

            activity.setIpfs(ipfs);
            return jsonList.get(0).getString("ID");
        } catch (Exception err) {
            backgroundError = true;
            return MainActivity.exceptionToString(err);
        }
    }

    private void setConfig(IPFS ipfs) throws Exception {
        JSONObject config = new JSONObject("{\"Datastore\":{\"StorageMax\":\"10GB\",\"StorageGCWatermark\":90,\"GCPeriod\":\"1h\",\"Spec\":{\"mounts\":[{\"child\":{\"path\":\"blocks\",\"shardFunc\":\"/repo/flatfs/shard/v1/next-to-last/2\",\"sync\":true,\"type\":\"flatfs\"},\"mountpoint\":\"/blocks\",\"prefix\":\"flatfs.datastore\",\"type\":\"measure\"},{\"child\":{\"compression\":\"none\",\"path\":\"datastore\",\"type\":\"levelds\"},\"mountpoint\":\"/\",\"prefix\":\"leveldb.datastore\",\"type\":\"measure\"}],\"type\":\"mount\"},\"HashOnRead\":false,\"BloomFilterSize\":0},\"Addresses\":{\"Swarm\":[\"/ip4/0.0.0.0/tcp/0\",\"/ip6/::/tcp/0\",\"/ip4/0.0.0.0/udp/0/quic\",\"/ip6/::/udp/0/quic\",\"/ble/Qmeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\"],\"Announce\":null,\"AppendAnnounce\":null,\"NoAnnounce\":null,\"API\":null,\"Gateway\":null},\"Mounts\":{\"IPFS\":\"/ipfs\",\"IPNS\":\"/ipns\",\"FuseAllowOther\":false},\"Discovery\":{\"MDNS\":{\"Enabled\":true,\"Interval\":10}},\"Routing\":{\"Type\":\"dhtclient\"},\"Ipns\":{\"RepublishPeriod\":\"\",\"RecordLifetime\":\"\",\"ResolveCacheSize\":128},\"Bootstrap\":[\"/ip4/103.44.247.16/tcp/14001/p2p/12D3KooWQs95gjCsCVZ2kszC9eguWAakHHUTRAkeH4HRqgYnzm74\"],\"Gateway\":{\"HTTPHeaders\":null,\"RootRedirect\":\"\",\"Writable\":false,\"PathPrefixes\":null,\"APICommands\":null,\"NoFetch\":false,\"NoDNSLink\":false,\"PublicGateways\":null},\"API\":{\"HTTPHeaders\":{\"Access-Control-Allow-Credentials\":[\"true\"],\"Access-Control-Allow-Headers\":[\"Authorization\"],\"Access-Control-Allow-Methods\":[\"PUT\",\"GET\",\"POST\",\"OPTIONS\"],\"Access-Control-Allow-Origin\":[\"*\"],\"Access-Control-Expose-Headers\":[\"Location\"]}},\"Swarm\":{\"AddrFilters\":null,\"DisableBandwidthMetrics\":false,\"DisableNatPortMap\":false,\"RelayClient\":{},\"RelayService\":{},\"Transports\":{\"Network\":{},\"Security\":{},\"Multiplexers\":{}},\"ConnMgr\":{\"Type\":\"basic\",\"LowWater\":100,\"HighWater\":200,\"GracePeriod\":\"20s\"}},\"AutoNAT\":{},\"Pubsub\":{\"Router\":\"\",\"DisableSigning\":false},\"Peering\":{\"Peers\":null},\"DNS\":{\"Resolvers\":null},\"Migration\":{\"DownloadSources\":null,\"Keep\":\"\"},\"Provider\":{\"Strategy\":\"\"},\"Reprovider\":{\"Interval\":\"12h\",\"Strategy\":\"all\"},\"Experimental\":{\"FilestoreEnabled\":false,\"UrlstoreEnabled\":false,\"GraphsyncEnabled\":false,\"Libp2pStreamMounting\":false,\"P2pHttpProxy\":false,\"StrategicProviding\":false,\"AcceleratedDHTClient\":false},\"Plugins\":{\"Plugins\":null},\"Pinning\":{\"RemoteServices\":null},\"Internal\":{}}");
        // 读取默认Identity
        JSONObject identity = ipfs.getConfigKey("Identity");
        // 添加到新的配置文件中
        ipfs.setConfig(config.put("Identity", identity));
    }

    /**
     * 检查网络是否可用
     *
     * @param paramContext
     * @return
     */
    public static boolean checkEnable(Context paramContext) {
        boolean i = false;
        NetworkInfo localNetworkInfo = ((ConnectivityManager) paramContext
            .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if ((localNetworkInfo != null) && (localNetworkInfo.isAvailable()))
            return true;
        return false;
    }

    /**
     * 将ip的整数形式转换成ip形式
     *
     * @param ipInt
     * @return
     */
    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    /**
     * 获取当前ip地址
     *
     * @param context
     * @return
     */
    public static String getLocalIpAddress(Context context) {
        try {

            WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            return int2ip(i);
        } catch (Exception ex) {
            return " 获取IP出错鸟!!!!请保证是WIFI,或者请重新打开网络!\n" + ex.getMessage();
        }
        // return null;
    }

    //GPRS连接下的ip
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }
    protected void onPostExecute(String result) {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        if (backgroundError) {
            activity.displayPeerIDError(result);
            Log.e(TAG, "IPFS start error: " + result);
        } else {
            activity.displayPeerIDResult(result);
            Log.i(TAG, "Your PeerID is: " + result);
        }
    }
}
