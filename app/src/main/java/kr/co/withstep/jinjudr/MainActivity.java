package kr.co.withstep.jinjudr;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private WebView webView;
    private ProgressBar progress;
    public static Handler MyHandler;
    public String myUrl;

    private ValueCallback<Uri> filePathCallbackNormal;
    private ValueCallback<Uri[]> filePathCallbackLollipop;
    private final static int FILECHOOSER_NORMAL_REQ_CODE = 1;
    private final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2;

    @Override
    public void onStart() {
        super.onStart();
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        cookieManager.setAcceptCookie(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieSyncManager.createInstance(this);
        }

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConn = ni.isConnected();

        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        boolean isMobileConn = ni.isConnected();

        if (!isWifiConn && !isMobileConn) {
            Toast.makeText(this, "인터넷에 접속되어 있지 않습니다!", Toast.LENGTH_SHORT)
                    .show();
            finish();//액티비티 종료
        } else {

            setContentView(R.layout.activity_main);

            //Notification
            FirebaseMessaging.getInstance().subscribeToTopic("jinjudr");
            // Token Inserts
            myUrl = getString(R.string.app_site) + "app/index.php";

            String token = FirebaseInstanceId.getInstance().getToken();
//            Log.d(TAG, "Token : " + token);
            if (token != null) {
                String tokenAes = Security.encrypt("Android||" + token, getString(R.string.app_secret_key));
                myUrl = myUrl + "?token=" + tokenAes;
                Log.d(TAG, "Token URL : " + myUrl);
            }


            progress = (ProgressBar) findViewById(R.id.progressBar);

            webView = (WebView) findViewById(R.id.webView);
            webView.setWebViewClient(new MyWebViewClient());
            webView.getSettings().setDefaultTextEncodingName("UTF-8");
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            // Enable pinch to zoom without the zoom buttons
            webView.getSettings().setBuiltInZoomControls(true);
            // Enable pinch to zoom without the zoom buttons
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                // Hide the zoom controls for HONEYCOMB+
                webView.getSettings().setDisplayZoomControls(false);
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                webView.getSettings().setTextZoom(100);

            webView.setWebChromeClient(new WebChromeClient() {
                // For Android < 3.0
                public void openFileChooser( ValueCallback<Uri> uploadMsg) {
//                    Log.d("MainActivity", "3.0 <");
                    openFileChooser(uploadMsg, "");
                }
                // For Android 3.0+
                public void openFileChooser( ValueCallback<Uri> uploadMsg, String acceptType) {
//                    Log.d("MainActivity", "3.0+");
                    filePathCallbackNormal = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    // i.setType("video/*");
                    startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_NORMAL_REQ_CODE);
                }
                // For Android 4.1+
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
//                    Log.d("MainActivity", "4.1+");
                    openFileChooser(uploadMsg, acceptType);
                }

                // For Android 5.0+
                public boolean onShowFileChooser(
                        WebView webView, ValueCallback<Uri[]> filePathCallback,
                        WebChromeClient.FileChooserParams fileChooserParams) {
//                    Log.d("MainActivity", "5.0+");
                    if (filePathCallbackLollipop != null) {
                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }
                    filePathCallbackLollipop = filePathCallback;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    // i.setType("video/*");
                    startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_LOLLIPOP_REQ_CODE);

                    return true;
                }
            });

            webView.setDownloadListener(new DownloadListener() {

                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                    try {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setMimeType(mimeType);
                        request.addRequestHeader("User-Agent", userAgent);
                        request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
                        request.setDescription("DownloadFile");
                        String fileName = URLUtil.guessFileName(url,contentDisposition,mimeType);
                        fileName = fileName.replaceAll("\"", "");
                        request.setTitle(fileName);
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                         Toast.makeText(getApplicationContext(), "다운로드 시작중..", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {

                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Should we show an explanation?
                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                 Toast.makeText(getBaseContext(), "첨부파일 다운로드를 위해\n동의가 필요합니다.", Toast.LENGTH_LONG).show();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        110);
                            } else {
                                 Toast.makeText(getBaseContext(), "첨부파일 다운로드를 위해\n동의가 필요합니다.", Toast.LENGTH_LONG).show();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        110);
                            }
                        }
                    }
                }
            });

            // FCM 에서 넘어온 URL 주소로 변경
            String url = myUrl;
            if (getIntent().getExtras() != null) {
                url = getIntent().getExtras().getString("url");
            }

            webView.loadUrl(url);
        }

        MyHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            if(msg.what == 0)
            {
                EXITBack = false;
            }
            }
        };


        this.buttonControl();
    }
    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieSyncManager.getInstance().stopSync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieSyncManager.getInstance().startSync();
        }
    }

    //링크된 페이지가 우리의 웹뷰안에서 로드되게 하기
    //웹뷰 클라이언트 재정의(WebViewClient)
    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view,String url , Bitmap favicon){
            progress.setVisibility(View.VISIBLE);
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "Token : " + token);
        }
        //페이지 로딩 종료시 호출
        public void onPageFinished(WebView view,String Url){

            progress.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //noinspection deprecation
                CookieSyncManager.getInstance().sync();
            } else {
                // 롤리팝 이상에서는 CookieManager의 flush를 하도록 변경됨.
                CookieManager.getInstance().flush();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == FILECHOOSER_NORMAL_REQ_CODE) {
                if (filePathCallbackNormal == null) return ;
                Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                filePathCallbackNormal.onReceiveValue(result);
                filePathCallbackNormal = null;
            } else if (requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE) {
                if (filePathCallbackLollipop == null) return ;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                }
                filePathCallbackLollipop = null;
            }
        } else {
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop.onReceiveValue(null);
                filePathCallbackLollipop = null;
            }
        }

    }

    private static boolean EXITBack = false;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(!EXITBack) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                } else {
                    Toast.makeText(MainActivity.this, "'뒤로'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
                    EXITBack = true;
                    MyHandler.sendEmptyMessageDelayed(0, 1000 * 2);
                    return false;
                }
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    protected void buttonControl() {
        // Buttons
        ImageButton back = (ImageButton) this.findViewById(R.id.back);
        ImageButton forward = (ImageButton) this.findViewById(R.id.forward);
        ImageButton refresh = (ImageButton) this.findViewById(R.id.refresh);
        ImageButton home = (ImageButton) this.findViewById(R.id.home);

        back.setOnClickListener(this);
        forward.setOnClickListener(this);
        refresh.setOnClickListener(this);
        home.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.back :
                if(!EXITBack) {
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        Toast.makeText(MainActivity.this, "마지막 페이지 입니다.", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.forward :
                if (webView.canGoForward()) {
                    webView.goForward();
                }
                break;
            case R.id.refresh :
                webView.reload();
                break;
            case R.id.home :
                webView.loadUrl(myUrl);
                break;
        }
    }
}
