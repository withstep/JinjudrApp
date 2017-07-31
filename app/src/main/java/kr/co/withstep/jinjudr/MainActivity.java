package kr.co.withstep.jinjudr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebView webView;
    private ProgressBar progress;
    public static Handler MyHandler;
    final String myUrl = "http://www.jinjudr.or.kr/";

    private ValueCallback<Uri> filePathCallbackNormal;
    private ValueCallback<Uri[]> filePathCallbackLollipop;
    private final static int FILECHOOSER_NORMAL_REQ_CODE = 1;
    private final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Notification
        FirebaseMessaging.getInstance().subscribeToTopic("notice");
        FirebaseInstanceId.getInstance().getToken();

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

            progress = (ProgressBar) findViewById(R.id.progressBar);

            webView = (WebView) findViewById(R.id.webView);
            webView.setWebViewClient(new HelloWebViewClient());
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
                    i.setType("image/*");
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
                    i.setType("image/*");
                    // i.setType("video/*");
                    startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_LOLLIPOP_REQ_CODE);

                    return true;
                }
            });

            webView.loadUrl(myUrl);
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
    }

    //링크된 페이지가 우리의 웹뷰안에서 로드되게 하기
    //웹뷰 클라이언트 재정의(WebViewClient)
    private class HelloWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            view.loadUrl(url);
            return true;
        }
        // 페이지 로딩 시작시 호출
        @Override
        public void onPageStarted(WebView view,String url , Bitmap favicon){
            progress.setVisibility(View.VISIBLE);
        }
        //페이지 로딩 종료시 호출
        public void onPageFinished(WebView view,String Url){
            progress.setVisibility(View.GONE);
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
                filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallbackLollipop = null;
            }
        } else {
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop.onReceiveValue(null);
                filePathCallbackLollipop = null;
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
}
