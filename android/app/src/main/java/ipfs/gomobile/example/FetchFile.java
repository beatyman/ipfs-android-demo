package ipfs.gomobile.example;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.zxing.common.StringUtils;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Random;

import ipfs.gomobile.android.IPFS;

final class FetchFile extends AsyncTask<Void, Void, String> {
    private static final String TAG = "FetchIPFSFile";

    private final WeakReference<MainActivity> activityRef;
    private boolean backgroundError;
    private static byte[] fetchedData;
    private String cid;
    private static volatile String sizeResult;
    private File file;

    FetchFile(MainActivity activity, String cid) {
        activityRef = new WeakReference<>(activity);
        this.cid = cid;
    }

    @Override
    protected void onPreExecute() {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;
        activity.displayStatusProgress(activity.getString(R.string.titleImageFetching));
    }

    @Override
    protected String doInBackground(Void... v) {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            cancel(true);
            return null;
        }

        IPFS ipfs = activity.getIpfs();

        try {
                fetchedData = ipfs.newRequest("cat")
                .withArgument(cid)
                .send();
            sizeResult = getPrintSize();
            if(fetchedData.length > 15 * 1024 * 1024) {
                createFileWithByte();
                return activity.getString(R.string.titleFetchedFile);
            }

//            Log.d(TAG, "fetched file data=" + MainActivity.bytesToHex(fetchedData));
            return activity.getString(R.string.titleFetchedImage);
        } catch (Exception err) {
            backgroundError = true;
            return MainActivity.exceptionToString(err);
        }
    }

    protected void onPostExecute(String result) {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        if (backgroundError) {
            activity.displayStatusError(activity.getString(R.string.titleImageFetchingErr), result);
            Log.e(TAG, "Ipfs image fetch error: " + result);
        } else {
            if ( fetchedData.length < 15 * 1024 * 1024) {
                activity.displayStatusSuccess("");
                // Put directly data through this way because of size limit with Intend
                DisplayImageActivity.fetchedData = fetchedData;

                Intent intent = new Intent(activity, DisplayImageActivity.class);
                intent.putExtra("Title", result);
                activity.startActivity(intent);
            } else {
                activity.displayStatusSuccess(file.getAbsolutePath());
            }
        }
    }

    public static String getSizeResult() {
        return sizeResult;
    }

    /**
     * 字节 转换为B MB GB
     * @param
     * @return
     */
    public static String getPrintSize(){
        //获取到的size为：1705230
        int GB = 1024 * 1024 * 1024;//定义GB的计算常量
        int MB = 1024 * 1024;//定义MB的计算常量
        int KB = 1024;//定义KB的计算常量
        DecimalFormat df = new DecimalFormat("0.00");//格式化小数
        String resultSize = "";
        int length = fetchedData.length;
        if (length / GB >= 1) {
            //如果当前Byte的值大于等于1GB
            resultSize = df.format(length / (float) GB) + "GB   ";
        } else if (length / MB >= 1) {
            //如果当前Byte的值大于等于1MB
            resultSize = df.format(length / (float) MB) + "MB   ";
        } else if (length / KB >= 1) {
            //如果当前Byte的值大于等于1KB
            resultSize = df.format(length / (float) KB) + "KB   ";
        } else {
            resultSize = length + "B   ";
        }
        return resultSize;

    }

    private void createFileWithByte() {
        /**
         * 创建File对象，其中包含文件所在的目录以及文件的命名
         */
        String fileName = "The-world-at-War.mp4";
        file = new File(Environment.getExternalStorageDirectory(),
            fileName);
        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()) {
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(fetchedData);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
}
