package kr.acw.demo.fcmbasic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by hmj on 2019-02-20.
 *
 * @since 0.1
 */
public class SplashsceenActivity extends AppCompatActivity {
    private static final String TAG = SplashsceenActivity.class.getSimpleName();

    private final int SPLASH_DISPLAY_LENGTH = 3000; // 3s

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashsceenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
