package ipfs.gomobile.example;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import ipfs.gomobile.android.IPFS;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private IPFS ipfs;

    private TextView ipfsTitle;
    private ProgressBar ipfsStartingProgress;
    private TextView ipfsResult;

    private TextView peerCounter;

    private TextView onlineTitle;
    private TextView offlineTitle;
    private Button xkcdButton;
    private Button shareButton;
    private Button fetchButton;
    private TextView ipfsStatus;
    private ProgressBar ipfsProgress;
    private TextView ipfsError;

    private PeerCounter peerCounterUpdater;
    private TextView ipAddress;
    private TextView timing;
    private DownloadTiming downloadTiming;

    // 测试button
    private Button button1_10M;
    private Button button2_50M;
    private Button button3_100M;
    private Button button4_200M;
    private Button button5_500M;
    private Button button6_231M;


    void setIpfs(IPFS ipfs) {
        this.ipfs = ipfs;
    }

    IPFS getIpfs() {
        return ipfs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipfsTitle = findViewById(R.id.ipfsTitle);
        ipfsStartingProgress = findViewById(R.id.ipfsStartingProgress);
        ipfsResult = findViewById(R.id.ipfsResult);

        peerCounter = findViewById(R.id.peerCounter);

        onlineTitle = findViewById(R.id.onlineTitle);
        offlineTitle = findViewById(R.id.offlineTitle);
        xkcdButton = findViewById(R.id.xkcdButton);
        shareButton = findViewById(R.id.shareButton);
        fetchButton = findViewById(R.id.fetchButton);
        ipfsStatus = findViewById(R.id.ipfsStatus);
        ipfsProgress = findViewById(R.id.ipfsProgress);
        ipfsError = findViewById(R.id.ipfsError);
        ipAddress = findViewById(R.id.ipAddress);
        timing = findViewById(R.id.timing);

        requestMyPermissions();
        if (ContextCompat.checkSelfPermission(
            getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            new StartIPFS(this).execute();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, R.string.ble_permissions_explain,
                Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        final MainActivity activity = this;

        ActivityResultLauncher<String[]> selectFileResultLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            result -> {
                if (result != null) {
                    Log.d(TAG, String.format("onActivityResult: GetContent: Uri=%s", result));
                    new ShareFile(activity, result).execute();
                }
            });

        xkcdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(xkcdButton.getContext(), R.string.random_xkcd_disable,
                    Toast.LENGTH_LONG).show();
//                new FetchRandomXKCD(activity).execute();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFileResultLauncher.launch(new String[]{"image/*"});
            }
        });

        fetchButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(activity);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan a QR code");
            integrator.setOrientationLocked(false);
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
            new IntentIntegrator(activity).initiateScan();
        });

        button1_10M = findViewById(R.id.button);
        button2_50M = findViewById(R.id.button2);
        button3_100M = findViewById(R.id.button3);
        button4_200M = findViewById(R.id.button4);
        button5_500M = findViewById(R.id.button5);
        button6_231M = findViewById(R.id.button6);

        HashMap<Button, String> downloadFile = new HashMap<>();
        downloadFile.put(button1_10M, "QmaJ6kN9fW3TKpVkpf1NuW7cjhHjNp5Jwr3cQuHzsoZWkJ");
        downloadFile.put(button2_50M, "Qme9ZiuG2zpL1qtFNvWKYkj7MYzeHH9b4qrgeYJMUDRjpu");
        downloadFile.put(button3_100M, "Qmca3PNFKuZnYkiVv1FpcV1AfDUm4qCSHoYjPTBqDAsyk8");
        downloadFile.put(button4_200M, "QmXo4G557RDJQfVjg9fKW5RJkUyPJ3YS6WGEw934ZcQeVT");
        downloadFile.put(button5_500M, "QmV7q5aTmvZtGWja4wpodiUTEpBVWYFkQGRQ8PmJMDPG62");
        downloadFile.put(button6_231M, "QmTjMw5CB4ZRwAaLo7V44wyAYiPYaYW7gMNXh4GaUpMgmp");

        downloadFile.forEach((key, value) -> {
            key.setOnClickListener(v -> {
                new FetchFile(MainActivity.this, value).execute();
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // QR Code scan result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                new FetchFile(this, result.getContents()).execute();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] strPerm,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, strPerm, grantResults);

        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new StartIPFS(this).execute();
        } else {
            Toast.makeText(this, R.string.ble_permissions_denied,
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (peerCounterUpdater != null) {
            peerCounterUpdater.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (peerCounterUpdater != null) {
            peerCounterUpdater.start();
        }
    }

    void displayPeerIDError(String error) {
        ipfsTitle.setTextColor(Color.RED);
        ipfsResult.setTextColor(Color.RED);

        ipfsTitle.setText(getString(R.string.titlePeerIDErr));
        ipfsResult.setText(error);
        ipfsStartingProgress.setVisibility(View.INVISIBLE);
    }

    void displayPeerIDResult(String peerID) {
        ipfsTitle.setText(getString(R.string.titlePeerID));
        ipfsResult.setText(peerID);
        ipAddress.setText(getString(R.string.ipAddress, StartIPFS.getLocalIpAddress(getApplicationContext())));
        ipfsStartingProgress.setVisibility(View.INVISIBLE);

        updatePeerCount(0);
        peerCounter.setVisibility(View.VISIBLE);
        onlineTitle.setVisibility(View.VISIBLE);
        offlineTitle.setVisibility(View.VISIBLE);
        xkcdButton.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.VISIBLE);
        fetchButton.setVisibility(View.VISIBLE);
        ipAddress.setVisibility(View.VISIBLE);

        button1_10M.setVisibility(View.VISIBLE);
        button2_50M.setVisibility(View.VISIBLE);
        button3_100M.setVisibility(View.VISIBLE);
        button4_200M.setVisibility(View.VISIBLE);
        button5_500M.setVisibility(View.VISIBLE);
//        button6_231M.setVisibility(View.VISIBLE);

        peerCounterUpdater = new PeerCounter(this, 3000);
        peerCounterUpdater.start();
    }

    void updateDownloadTiming(Long seconds, String fileSize) {
        timing.setText(getString(R.string.timing, seconds, fileSize));
    }


    void updatePeerCount(int count) {
        peerCounter.setText(getString(R.string.titlePeerCon, count));
    }

    void displayStatusProgress(String text) {
        ipfsStatus.setTextColor(ipfsTitle.getCurrentTextColor());
        ipfsStatus.setText(text);
        ipfsStatus.setVisibility(View.VISIBLE);
        ipfsError.setVisibility(View.INVISIBLE);
        ipfsProgress.setVisibility(View.VISIBLE);

        timing.setVisibility(View.VISIBLE);
        downloadTiming = new DownloadTiming(this, 1000);
        downloadTiming.start();

        xkcdButton.setAlpha(0.5f);
        xkcdButton.setClickable(false);
        shareButton.setAlpha(0.5f);
        shareButton.setClickable(false);
        fetchButton.setAlpha(0.5f);
        fetchButton.setClickable(false);
    }

    @SuppressLint("SetTextI18n")
    void displayStatusSuccess(String filePath) {
        if (!filePath.isEmpty()) {
            ipfsStatus.setText("Download file success! file path: " + filePath);
        } else {
            ipfsStatus.setVisibility(View.INVISIBLE);
        }
        ipfsProgress.setVisibility(View.INVISIBLE);
        downloadTiming.stop();

        xkcdButton.setAlpha(1);
        xkcdButton.setClickable(true);
        shareButton.setAlpha(1);
        shareButton.setClickable(true);
        fetchButton.setAlpha(1);
        fetchButton.setClickable(true);
    }

    void displayStatusError(String title, String error) {
        ipfsStatus.setTextColor(Color.RED);
        ipfsStatus.setText(title);

        ipfsProgress.setVisibility(View.INVISIBLE);
        ipfsError.setVisibility(View.VISIBLE);
        ipfsError.setText(error);

        xkcdButton.setAlpha(1);
        xkcdButton.setClickable(true);
        shareButton.setAlpha(1);
        shareButton.setClickable(true);
        fetchButton.setAlpha(1);
        fetchButton.setClickable(true);
    }

    static String exceptionToString(Exception error) {
        String string = error.getMessage();

        if (error.getCause() != null) {
            string += ": " + error.getCause().getMessage();
        }

        return string;
    }

    public static String bytesToHex(byte[] bytes) {
        final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    private void requestMyPermissions() {

        if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有写SD权限");
        }
        if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有读SD权限");
        }
    }

}
