package iot.project.glennncullen.shmartmirror;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class NotesActivity extends AppCompatActivity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        getSupportActionBar().setTitle("Notes");
    }

    @Override
    public void onBackPressed(){
    }
}
