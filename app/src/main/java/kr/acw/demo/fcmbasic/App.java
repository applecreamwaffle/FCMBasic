package kr.acw.demo.fcmbasic;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by hmj on 2019-02-25.
 *
 * @since 0.1
 */
public class App extends Application {

    private static String TAG = App.class.getSimpleName();

    public static String LAT = "LAT";
    public static String LNG = "LNG";
    public static String APP_MINUTES = "APP_MINUTES";
    public static String INIT = "INIT";

    private double DEFAULT_LAT = 37.4986585;
    private double DEFAULT_LNG = 127.0560179;

    private GoogleApiHelper googleApiHelper;
    private static App mInstance;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        sharedPreferences = getBaseContext().getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE);
        boolean isInit = sharedPreferences.getBoolean("INIT", true);
        Log.d(TAG, "onCreate() isInit? " + isInit);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isInit) {
            Calendar c = Calendar.getInstance(); // 처음 실행했을 때 시간을 저장해놓고, 후에 한시간마다 체크할 때 사용할 예정임
            int min = c.getTime().getMinutes();
            editor.putString(LAT, String.valueOf(DEFAULT_LAT));
            editor.putString(LNG, String.valueOf(DEFAULT_LNG));
            editor.putInt(APP_MINUTES, min);
            editor.putBoolean(INIT, false);
            editor.commit();
        }

        mInstance = this;
    }


    public static synchronized App getInstance() {
        return mInstance;
    }

    public static SharedPreferences getSharedPreferences() {
        return getInstance().sharedPreferences;
    }

}
