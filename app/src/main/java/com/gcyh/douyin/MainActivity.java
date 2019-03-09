package com.gcyh.douyin;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gcyh.douyin.http.retrofit.ApiCallback;
import com.gcyh.douyin.http.retrofit.ApiClient;
import com.gcyh.douyin.tools.CommonUtil;
import com.gcyh.douyin.tools.RxUtil;
import com.gcyh.douyin.tools.download.DownloadManager;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends Activity {

    @BindView(R.id.et_video)
    EditText etVideo;
    @BindView(R.id.btb_parse)
    Button btbParse;
    @BindView(R.id.tv_picture_url)
    TextView tvPictureUrl;
    @BindView(R.id.tv_video_url)
    TextView tvVideoUrl;
    @BindView(R.id.btb_download)
    Button btbDownload;
    @BindView(R.id.tv_result)
    TextView tvResult;
    public final int REQUEST_CODE_CONTACT = 101;
    private CompositeSubscription mCompositeSubscription;
    String downPath=Environment.getExternalStorageDirectory()+"/11DyDownload/";
    ProgressDialog dialog;
    Pattern pattern = Pattern.compile("(http://|ftp://|https://|www){0,1}[^\u4e00-\u9fa5\\s]*?\\.(com|net|cn|me|tw|fr)[^\u4e00-\u9fa5\\s]*");
    DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= 23) {
            String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }else {
                    initPath();
                }
            }
        }
        FileDownloader.setup(this);
        dialog=new ProgressDialog(this);
//        etVideo.setText("http://v.douyin.com/28XRKP/");
        downloadManager = DownloadManager.getInstance();
        downloadManager.setProgressListener(new DownloadManager.ProgressListener() {
            @Override
            public void progressChanged(long read, long contentLength, boolean done) {
                if (done){
                    if (dialog.isShowing()){
                        dialog.cancel();
                    }
                }else {
                    if (!dialog.isShowing()){
                        dialog.show();
                    }
                    dialog.setTitle("已完成"+read*100/contentLength+"%");
                }

            }
        });

    }

    @OnClick({R.id.btb_parse, R.id.tv_picture_url, R.id.tv_video_url, R.id.btb_download})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btb_parse:
                String contentText=etVideo.getText().toString().toString();
                String url= parseContent(contentText);
                Log.d("hjq",url);
                if (TextUtils.isEmpty(url)){
                    Toast.makeText(this,"无法获取视频地址",Toast.LENGTH_SHORT).show();
                }else {
                    getHtmlData(url);
                }
                break;
            case R.id.tv_picture_url:
                copyMsg(this,tvPictureUrl.getText().toString());
                Toast.makeText(this,"复制成功",Toast.LENGTH_SHORT).show();
                break;
            case R.id.tv_video_url:
                copyMsg(this,tvVideoUrl.getText().toString());
                Toast.makeText(this,"复制成功",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btb_download:
                String videoUrl=tvVideoUrl.getText().toString();
                if (TextUtils.isEmpty(videoUrl)){
                    Toast.makeText(this,"下载地址不能为空",Toast.LENGTH_SHORT).show();
                }else {
//                    downloadManager.start(videoUrl);
//                    videoUrl=videoUrl+"&device_platform=iphone";
//                    Log.e("hjq","video="+videoUrl);
                    String path=downPath+CommonUtil.dateToString(new Date(),"yyyyMMddhhmmss")+".mp4";
                    startDownload(videoUrl,path);
                }

                break;
        }
    }
    public String parseContent(String content) {
        Matcher matcher = pattern.matcher(content);
        String url="";
          while (matcher.find()) {
              url=matcher.group(0);
             System.out.println(matcher.group(0));
         }
         return url;
    }
    public String getHtmlData(String url) {
        addSubscription(RxUtil.createHttpObservable(ApiClient.getInstance().getApiStores1().getHtmlData(url)).subscribe(new ApiCallback<String>() {
            @Override
            public void onSuccess(String data) {
                int playAddrPos=data.indexOf("playAddr");
                String playAddr=data.substring(playAddrPos).split("\"")[1];
                playAddr=playAddr.replace("playwm","play");
                int coverPos=data.indexOf("cover");
                String cover=data.substring(coverPos).split("\"")[1];
                tvVideoUrl.setText(playAddr);
                tvPictureUrl.setText(cover);
            }

            @Override
            public void onFailure(String exception) {
                Toast.makeText(MainActivity.this,exception,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {

            }
        }));
        return url;
    }
    public void onUnsubscribe() {
        if (mCompositeSubscription != null && mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
    }

    public void addSubscription(Subscription subscription) {
        if (mCompositeSubscription == null) {
            mCompositeSubscription = new CompositeSubscription();
        }
        mCompositeSubscription.add(subscription);
    }
    @Override
    protected void onDestroy() {
        onUnsubscribe();
        super.onDestroy();
    }

    public static void copyMsg(Context context, String msg) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(msg);
    }
    public void initPath(){
        if (!new File(downPath).exists()){
            new File(downPath).mkdir();
        }
        tvResult.setText("下载地址："+downPath);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_CONTACT:
                for (int i=0;i< permissions.length;i++){
                    if (TextUtils.equals(permissions[i],Manifest.permission.WRITE_EXTERNAL_STORAGE)&&grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {
                        initPath();
                    }
                return;
            }
        }
    }
    public void startDownload(String url,String path){
        if (!dialog.isShowing()){
            dialog.setMessage("开始下载");
            dialog.show();
        }
        FileDownloader.getImpl().create(url)
                .setPath(path)
                .setListener(new FileDownloadListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        if (!dialog.isShowing()){
                            dialog.show();
                        }
                        dialog.setMessage("已完成"+soFarBytes*100/totalBytes+"%");
                    }

                    @Override
                    protected void blockComplete(BaseDownloadTask task) {
                    }

                    @Override
                    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        if (dialog.isShowing()){
                            dialog.cancel();
                        }
                        Toast.makeText(MainActivity.this,"下载完成",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        if (dialog.isShowing()){
                            dialog.cancel();
                        }
                        Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                    }
                }).start();
    }
}
