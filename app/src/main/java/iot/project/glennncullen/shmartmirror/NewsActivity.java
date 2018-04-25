package iot.project.glennncullen.shmartmirror;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.support.v7.widget.Toolbar;

public class NewsActivity extends AppCompatActivity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    WebView newsWebView;
    String link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        getSupportActionBar().setTitle("News");

        Intent intent = getIntent();
        if (intent != null) {
            link = intent.getStringExtra("link");
        }

        newsWebView = (WebView) findViewById(R.id.newsWebView);
        newsWebView.setWebViewClient(new WebViewClient());
        newsWebView.getSettings().setJavaScriptEnabled(true);
        newsWebView.loadUrl(link);
    }

    @Override
    public void onBackPressed(){
    }
}
