package iot.project.glennncullen.shmartmirror;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.Objects;
import java.util.UUID;


/*
Amazon Cognito Identity Pool Id:   "us-east-2:02b24f82-0972-4eb4-bcd2-83760190d1ef"

Partial Code for Connection to AWS IoT taken from:
https://github.com/awslabs/aws-sdk-android-samples/blob/master/AndroidPubSub/src/com/amazonaws/demo/androidpubsub/PubSubActivity.java

 */

public class MainActivity extends AppCompatActivity{

    // set log tag to underlying class name for debug
    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private Handler handler;

    EditText authNumTxt;
    Button mainLogoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Shmart Mirror Authorisation");

        mainLogoutBtn = (Button) findViewById(R.id.mainLogoutBtn);
        mainLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
            }
        });

        authNumTxt = (EditText) findViewById(R.id.authNumTxt);
        authNumTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(authNumTxt.getText().length() == 6){
                    JSONObject authJson = new JSONObject();
                    try {
                        authJson.put("auth", String.valueOf(authNumTxt.getText()));
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Unable to add to authJson");
                        e.printStackTrace();
                    }
                    handler.publish(authJson, "/iotappdev/pi/auth/");
                    authNumTxt.setText("");
                }
            }
        });

    } // END OF ON CREATE


    // update based on callback from pi
    public void update(JSONObject message){
        if(!message.has("auth")) return;
        try {
            if(message.get("auth").equals("authorised")){
                handler.changeAuthorisation();
    //            authNumTxt.setVisibility(View.INVISIBLE);
    //            findViewById(R.id.authNumLbl).setVisibility(View.INVISIBLE);
            }else{
                Log.i(LOG_TAG, "unable to authorise connection to mirror");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error with JSONObject on update for MainActivity");
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
        authNumTxt.setEnabled(false);
        mainLogoutBtn.setEnabled(false);
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
