package kr.acw.demo.fcmbasic;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by hmj on 2019-02-26.
 *
 * @since 0.1
 */
public class HttpConnection {
    private static String TAG = HttpConnection.class.getSimpleName();

    private static HttpConnection instance = new HttpConnection();
    private OkHttpClient client;

    public static HttpConnection getInstance() {

        return instance;
    }

    private HttpConnection() {
        this.client = new OkHttpClient();
    }

    public void requestWebServer(String url, String param1, String param2, Callback callback) {
        RequestBody body = new FormBody.Builder()
                .add("param1", param1)
                .add("param2", param2)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

}
