package kr.acw.demo.fcmbasic.immortal;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import kr.acw.demo.fcmbasic.App;
import kr.acw.demo.fcmbasic.MainActivity;
import kr.acw.demo.fcmbasic.R;

/**
 * Immortal Service
 */
public class ImmortalService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = ImmortalService.class.getSimpleName();

    // [START Variables]
    public static Intent serviceIntent = null;
    private Thread thread;

    // 위치서비스
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest = LocationRequest.create();
    private Location currentLocation;

    // 알람 설정 - 한시간마다 이전 미세먼지 수치와 현재 미세먼지 수치를 비교하여 더 나빠져있으면 Notification 발생시킨다 (쿠폰 발행)
    private BroadcastReceiver alarmReceiver;
    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;

    //37.4986585,127.0560179
    // [END Variables]

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                // 이벤트 처리
                long when = System.currentTimeMillis();
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


                NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                        context).setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle("Nexo water")
                        .setContentText("Message")
                        .setWhen(when)
                        .setContentIntent(pendingIntent);
                notificationManager.notify(68, mNotifyBuilder.build());
                //AlarmManager 재 등록
                alarmEveryHour();
            }
        };

        registerReceiver(alarmReceiver, new IntentFilter("AlarmService"));
        alarmEveryHour();

        locationRequest.setInterval(15 * 60 * 1000); // 15m
        locationRequest.setFastestInterval(5 * 60 * 1000); // 5m
        locationRequest.setMaxWaitTime(60 * 60 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    // 매시간 체크하기 위한 알람 설정
    public void alarmEveryHour() {
        Log.d(TAG, "alarmEveryHour()");
        Intent intent = new Intent("AlarmService");
        alarmPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

        // get a Calendar and set the time to 14:00:00
        int init_min = App.getSharedPreferences().getInt(App.APP_MINUTES, -1);
        Log.d(TAG, "alarmEveryHour() init_min? " + init_min);

        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, startTime.getTime().getHours() + 1);
        if (init_min != -1) {
            startTime.set(Calendar.MINUTE, init_min);
        } else {
            startTime.set(Calendar.MINUTE, 0);
        }

        // get a Calendar at the current time
        Calendar now = Calendar.getInstance();
        long time;

        if (now.before(startTime)) {
            time = startTime.getTimeInMillis();
        } else {
            startTime.add(Calendar.DATE, 1);
            time = startTime.getTimeInMillis();
        }

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //API 19 이상 API 23미만
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, alarmPendingIntent);
            } else {
                //API 19미만
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, alarmPendingIntent);
            }
        } else {
            //API 23 이상
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmPendingIntent);
        }
    }

    // 서비스가 종료되었을 때 다시 살리기 위한 알람 설정
    protected void setAlarmTimer() {
        Log.d(TAG, "setAlarmTimer()");
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, 1);

        Intent intent = new Intent(ImmortalService.this, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        manager.set(AlarmManager.RTC, c.getTimeInMillis(), alarmPendingIntent);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //API 19 이상 API 23미만
                manager.setExact(AlarmManager.RTC, c.getTimeInMillis(), sender);
            } else {
                //API 19미만
                manager.set(AlarmManager.RTC, c.getTimeInMillis(), sender);
            }
        } else {
            //API 23 이상
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC, c.getTimeInMillis(), sender);
        }
    }

    public void showToast(final Application application, final String msg) {
        Handler h = new Handler(application.getMainLooper());
        h.post(() -> Toast.makeText(application, msg, Toast.LENGTH_LONG).show());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceIntent = intent;
        Log.d(TAG, "START SERVICE");
        showToast(getApplication(), "START SERVICE");

        thread = new Thread(() -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("aa hh:mm:ss");
            boolean run = true;
            while (run) {
                try {
                    //Thread.sleep(1000 * 60 * 1); // 1 minute
                    Thread.sleep(3 * 60 * 1000); // 3min
                    Date date = new Date();

                    String lat = App.getSharedPreferences().getString(App.LAT, null);
                    String lng = App.getSharedPreferences().getString(App.LNG, null);

                    if (lat != null && lng != null) {
                        Log.d(TAG, simpleDateFormat.format(date) + ", lat: " + lat + " , lng: " + lng);
                        showToast(getApplication(), simpleDateFormat.format(date) + ", lat: " + lat + " , lng: " + lng);
                    } else {
                        Log.d(TAG, simpleDateFormat.format(date));
                        showToast(getApplication(), simpleDateFormat.format(date));
                    }
                } catch (InterruptedException ex) {
                    run = false;
                    ex.printStackTrace();
                }
            }
        });
        thread.start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        alarmManager.cancel(alarmPendingIntent);
        unregisterReceiver(alarmReceiver);
        alarmReceiver = null;

        if (googleApiClient.isConnecting()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.unregisterConnectionCallbacks(this);
            googleApiClient.unregisterConnectionFailedListener(this);
            googleApiClient.disconnect();
        }
        googleApiClient = null;
        locationRequest = null;
//        currentLocation = null;
//        if (App.getGoogleApiHelper().isConnected()) {
//            App.getGoogleApiHelper().disconnect();
//        }

        serviceIntent = null;
        setAlarmTimer();
        Thread.currentThread().interrupt();

        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission Error");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

        Log.i(TAG, "Connected to Google API " + ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION));
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed()");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.i(TAG, "onLocationChanged() " + location);
            currentLocation = location;
            SharedPreferences.Editor editor = App.getSharedPreferences().edit();
            editor.putString(App.LAT, String.valueOf(currentLocation.getLatitude()));
            editor.putString(App.LNG, String.valueOf(currentLocation.getLongitude()));
            editor.commit();
        }
    }


}
