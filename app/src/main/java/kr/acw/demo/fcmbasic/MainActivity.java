package kr.acw.demo.fcmbasic;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.acw.demo.fcmbasic.immortal.ImmortalService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * [ REFERENCES ]
 * - https://developer.android.com/reference/android/webkit/WebSettings.html
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
//    private static final String WEB_URL = "https://www.google.com/";//"https://www.hyundai.com/kr/ko/vehicles/nexo/specifications"; // WebView Default URL
    private static final String WEB_URL = "https://www.google.co.kr/maps/";//"https://www.hyundai.com/kr/ko/vehicles/nexo/specifications"; // WebView Default URL
    private static final int REQUEST_PERMISSIONS_CODE = 34;

    private HttpConnection httpConnection = HttpConnection.getInstance();

    //    @BindView(R.id.subscribeButton) Button subscribeButton;
//    @BindView(R.id.logTokenButton) Button logTokenButton;
//    @BindView(R.id.startServiceButton) Button startServiceButton;
//    @BindView(R.id.stopServiceButton) Button stopServiceButton;
    @BindView(R.id.baseWebView)
    WebView baseWebView;
    @BindView(R.id.progress_horizontal)
    ProgressBar progressBar;
    @OnClick(R.id.refreshButton)
    public void refreshButtonClick(View view) {
        String lat = App.getSharedPreferences().getString("LAT", "");
        String lng = App.getSharedPreferences().getString("LNG", "");
        baseWebView.loadUrl(WEB_URL + "@" + lat + "," + lng + ",15z?hl=ko");
//        @37.5002273,127.068094,15z?hl=ko
        Log.i(TAG, "url? " + baseWebView.getUrl());
    }

    private String currentToken;
    private Intent serviceIntent;
    private boolean isGPSStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getString(R.string.nexo_notification_channel_id);
            String channelName = getString(R.string.nexo_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }
        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]

        // [START Get token]
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(
                new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        currentToken = task.getResult().getToken();
                    }
                }
        );
        Log.d(TAG, "- FCM currentToken: " + currentToken);
        // [END Get token]

        new GPSUtil(this).turnGPSOn(new GPSUtil.OnGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                isGPSStatus = isGPSEnable;
            }
        });

        Log.d(TAG, "isGPSStatus: " + isGPSStatus);

//        Calendar calendar = Calendar.getInstance();
//        Intent intent = new Intent(MainActivity.this, EveryHourAlarmReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        AlarmManager am = (AlarmManager)this.getSystemService(this.ALARM_SERVICE);
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 30000, pendingIntent);

        // Call
        //initService();
        initWebViewSettings();
    }


    private void sendAppInfo() {
        new Thread() {
            @Override
            public void run() {
                httpConnection.requestWebServer("", "", "", appInfoCallback);
            }
        }.start();
    }

    private final Callback appInfoCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.d(TAG, "appInfoCallback Fail T_T");
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String body = response.body().string();
            Log.d(TAG, "appInfoCallback Success ^_^ " + body);
        }
    };

    private void initService() {
        Log.d(TAG, "initService() " + ImmortalService.serviceIntent);
        if (ImmortalService.serviceIntent == null) {
            serviceIntent = new Intent(this, ImmortalService.class);
            startService(serviceIntent);
        } else {
            serviceIntent = ImmortalService.serviceIntent;
            //Toast.makeText(getApplicationContext(), "already", Toast.LENGTH_SHORT).show();
        }
    }

    private void initWebViewSettings() {
        String lat = App.getSharedPreferences().getString("LAT", "");
        String lng = App.getSharedPreferences().getString("LNG", "");

        // [START Webview Settings]
        progressBar.setMax(100);
        progressBar.setProgress(1);

        baseWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        baseWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
        baseWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }
        });
        baseWebView.setNetworkAvailable(true);
        baseWebView.getSettings().setJavaScriptEnabled(true);
        baseWebView.getSettings().setDomStorageEnabled(true);
        baseWebView.getSettings().setSupportZoom(true);
//        baseWebView.getSettings().setBuiltInZoomControls(true);
        baseWebView.loadUrl(WEB_URL);
//        baseWebView.loadUrl(WEB_URL + "?lat=" + lat + "&lng=" + lng + "&");
        baseWebView.loadUrl(WEB_URL + "@" + lat + "," + lng + ",15z?hl=ko");
        Log.i(TAG, "url? " + baseWebView.getUrl());

        // [END Webview Settings]
    }

    /**
     * Google Play Services 를 사용할 수 있는지에 대한 여부
     * @return
     */
    public boolean isGooglePlayServicesAvailable() {
        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "@@@ onRequestPermissionsResult");
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CODE: {
                if (grantResults.length <= 0) {
                    Log.i(TAG, "User interaction was cancelled.");
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // 허용
                    Log.i(TAG, "PERMISSION RESULT : YAY!");
                    //initService();
                } else { // 거부
                    Log.i(TAG, "PERMISSION RESULT : BOO!");
                    showSnackbar(R.string.permission_denied_explanation,
                            R.string.settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Build intent that displays the App settings screen.
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });

                }
            }
            return;
        }
    }

    /**
     * 네트워크 상태 체크
     * @param dialogInterface
     * @return
     */
    private boolean callPromptInternetConnection(DialogInterface dialogInterface){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
Log.i(TAG, "callPromptInternetConnection() networkInfo ? " + networkInfo);
        if (networkInfo == null || networkInfo.isConnected() == false) {
            connectPromptInternet();
            return false;
        }

        if (dialogInterface != null) {
            dialogInterface.dismiss();
        }

        if (checkPermissions()) {
            initService();
        } else {
            requestPermissions();
        }

        return true;
    }

    private void connectPromptInternet( ){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.title_alert_no_intenet));
        builder.setMessage(getString(R.string.msg_alert_no_internet));

        String positiveText = getString(R.string.btn_label_refresh);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callPromptInternetConnection(dialog)) {
                          if (checkPermissions()) {
                              baseWebView.loadUrl(baseWebView.getUrl().toString()); // WebView Reload!
                              initService();
                          } else {
                              requestPermissions();
                          }
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean checkPermissions() {
        int pm1 = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int pm2 = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

        return pm1 == PackageManager.PERMISSION_GRANTED && pm2 == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request permissions.
     */
    private void requestPermissions() {
        boolean rationale1 = ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        boolean rationale2 = ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (rationale1 || rationale2) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_PERMISSIONS_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_CODE);
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener clickListener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), clickListener)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() : isGooglePlayServicesAvailable() ? " + isGooglePlayServicesAvailable());
        // 구글플레이서비스를 사용할 수 있는지 여부 판단
        if (isGooglePlayServicesAvailable()) {
            callPromptInternetConnection(null);
        } else {
            Toast.makeText(getApplicationContext(),getString(R.string.no_google_playservice_available), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (everyHourAlarmReceiver != null) {
//            unregisterReceiver(everyHourAlarmReceiver);
//            everyHourAlarmReceiver = null;
//        }

        if (serviceIntent != null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
    }

}
