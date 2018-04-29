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

    // string for debug
    static final String LOG_TAG = NewsActivity.class.getCanonicalName();

    // singleton Handler
    private Handler handler;

    // View components
    Button newsLogoutBtn;
    WebView newsWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // get instance of Handler singleton and set Title of actionbar to News
        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("News");

        // when newsLogoutBtn is clicked, disableInteraction() and logout()
        newsLogoutBtn = (Button) findViewById(R.id.newsLogoutBtn);
        newsLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
            }
        });

        // initialise webview component
        newsWebView = (WebView) findViewById(R.id.newsWebView);
        newsWebView.setWebViewClient(new WebViewClient());
        newsWebView.getSettings().setJavaScriptEnabled(true);

    }// END OF CREATE


    /**
     * if the message contains the field 'link' the take
     * the link and open it in the web view
     *
     * @param message json object from subscribe
     */
    public void update(JSONObject message){
        if(!message.has("link")) return;
        try {
            newsWebView.loadUrl((String) message.get("link"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to use JSONObject message for NewsActivity");
            e.printStackTrace();
        }
    }

    // on back button pressed, do nothing
    @Override
    public void onBackPressed(){
    }

    /**
     * disable interaction with activity
     */
    public void disableInteraction(){
        newsWebView.loadUrl("about:blank");
        newsLogoutBtn.setEnabled(false);
    }

    /**
     * handle logout event by publishing logout
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
