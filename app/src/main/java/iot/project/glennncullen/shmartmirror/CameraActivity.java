package iot.project.glennncullen.shmartmirror;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    static final String LOG_TAG = CameraActivity.class.getCanonicalName();

    private Handler handler;

    Button cameraLogoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Camera");

        cameraLogoutBtn = (Button) findViewById(R.id.cameraLogoutBtn);
        cameraLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
            }
        });
    }

    // update based on callback from pi
    public void update(JSONObject message) {

    }

    @Override
    public void onBackPressed() {
    }

    /**
     * disable interaction with activity
     */
    public void disableInteraction(){
        cameraLogoutBtn.setEnabled(false);
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