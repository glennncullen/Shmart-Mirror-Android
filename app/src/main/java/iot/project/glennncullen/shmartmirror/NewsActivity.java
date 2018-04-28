package iot.project.glennncullen.shmartmirror;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.support.v7.widget.Toolbar;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class NewsActivity extends AppCompatActivity {

    static final String LOG_TAG = NewsActivity.class.getCanonicalName();

    private Handler handler;

    Button newsLogoutBtn;

    WebView newsWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("News");

        newsLogoutBtn = (Button) findViewById(R.id.newsLogoutBtn);
        newsLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
            }
        });

        newsWebView = (WebView) findViewById(R.id.newsWebView);
        newsWebView.setWebViewClient(new WebViewClient());
        newsWebView.getSettings().setJavaScriptEnabled(true);

    }


    // update based on callback from pi
    public void update(JSONObject message){
        try {
            newsWebView.loadUrl((String) message.get("link"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to use JSONObject message for NewsActivity");
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed(){
    }

    /**
     * disable interaction with activity
     */
    public void disableInteraction(){
        newsLogoutBtn.setEnabled(false);
    }

    /**
     * handle logout event
     */
    private void logout(){
        try {
            handler.publish(new JSONObject().put("logout", 1), "/iotappdev/logout/");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to create JSONObject for logout");
            e.printStackTrace();
        }
    }
}
