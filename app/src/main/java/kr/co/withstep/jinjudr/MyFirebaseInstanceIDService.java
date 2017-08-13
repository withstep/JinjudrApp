package kr.co.withstep.jinjudr;

import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by byungguchoi on 2017. 7. 28..
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + token);

        // 생성등록된 토큰을 개인 앱서버에 보내 저장해 두었다가 추가 뭔가를 하고 싶으면 할 수 있도록 한다.
//        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        String tokenAes = Security.encrypt("Android||" + token, getString(R.string.app_secret_key));

        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("token", tokenAes)
                .build();

        //request
        Request request = new Request.Builder()
                .url(getString(R.string.app_site)+"app/token.php")
                .post(body)
                .build();
        try {
            client.newCall(request).execute();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //noinspection deprecation
                CookieSyncManager.getInstance().sync();
            } else {
                // 롤리팝 이상에서는 CookieManager의 flush를 하도록 변경됨.
                CookieManager.getInstance().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}