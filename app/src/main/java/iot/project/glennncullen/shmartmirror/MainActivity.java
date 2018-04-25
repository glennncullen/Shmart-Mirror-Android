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
import java.util.UUID;


/*
Amazon Cognito Identity Pool Id:   "us-east-2:02b24f82-0972-4eb4-bcd2-83760190d1ef"

Partial Code for Connection to AWS IoT taken from:
https://github.com/awslabs/aws-sdk-android-samples/blob/master/AndroidPubSub/src/com/amazonaws/demo/androidpubsub/PubSubActivity.java

 */

public class MainActivity extends AppCompatActivity{

    // set log tag to underlying class name for debug
    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // IoT endpoint
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3oazwlb9g85vu.iot.us-east-2.amazonaws.com";

    private static final String COGNITO_POOL_ID = "us-east-2:02b24f82-0972-4eb4-bcd2-83760190d1ef";
    // AWS IoT policy
    private static final String AWS_IOT_POLICY_NAME = "farringtonsnostril";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_2;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    AWSIotClient myIotAndroidClient;
    AWSIotMqttManager myMQTTManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;

    JSONObject testJson = new JSONObject();

    boolean isConnected;
    boolean isAuthorisedByMirror;

    EditText authNumTxt;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authNumTxt = (EditText) findViewById(R.id.authNumTxt);
        isAuthorisedByMirror = false;


        //
        // initialise states for first publish here
        //



        // unique client ID for AWS IoT is required
        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        myMQTTManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // sends ping every 10 second, will recognise disconnects
        myMQTTManager.setKeepAlive(10);

        // in the event of sudden loss of connect, last will and testament is published
        myMQTTManager.setMqttLastWillAndTestament(
                new AWSIotMqttLastWillAndTestament("/lwt/",
                        "Connection Lost Suddenly", AWSIotMqttQos.QOS0)
        );

        // IoT Client to create certificate
        myIotAndroidClient = new AWSIotClient(credentialsProvider);
        myIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // load cert/key from keystore
        try{
            if(AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)){
                if(AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath, keystoreName, keystorePassword)){
                    Log.i(LOG_TAG, "Certificate " + certificateId + " found in keystore");
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId, keystorePath, keystoreName, keystorePassword);
                }else{
                    Log.i(LOG_TAG, "key/cert " + certificateId + " NOT found in keystore");
                }
            }else{
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " NOT found");
            }
        }catch (Exception e){
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }


        // create new key and ccertificate if none already created
        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                myIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        myIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }


        // connect to clientId for AWS IoT MQTT
        // subscribe to relevant topics
        // start listener threads and define functionality for incoming Json
        try{
            myMQTTManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                    if(String.valueOf(status).equalsIgnoreCase("Connected")) {
                        isConnected = true;
                        Log.i(LOG_TAG, "is connected");
                        // connect to /iotappdev/TEST/
                        try {
                            myMQTTManager.subscribeToTopic("/iotappdev/TEST/", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject receivedJson = null;
                                                try {
                                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                                    Toast.makeText(getBaseContext(), (String) receivedJson.get("TEST"), Toast.LENGTH_SHORT).show();
                                                } catch (JSONException e) {
                                                    Log.e(LOG_TAG, "Error creating Json with TEST data", e);
                                                }
                                            } catch (UnsupportedEncodingException e) {
                                                Log.e(LOG_TAG, "Received message encoding error", e);
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "unable to subscribe to /iotappdev/TEST/", e);
                        }
                        // connect to /iotappdev/display/
                        try {
                            myMQTTManager.subscribeToTopic("/iotappdev/display/", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject receivedJson = null;
                                                try {
                                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                                    Toast.makeText(getBaseContext(), (String) receivedJson.get("display"), Toast.LENGTH_SHORT).show();
                                                } catch (JSONException e) {
                                                    Log.e(LOG_TAG, "Error creating Json with display data", e);
                                                }
                                            } catch (UnsupportedEncodingException e) {
                                                Log.e(LOG_TAG, "Received message encoding error", e);
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "unable to subscribe to /iotappdev/display/", e);
                        }
                        // connect to /iotappdev/auth/
                        try {
                            myMQTTManager.subscribeToTopic("/iotappdev/android/auth/", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject receivedJson = null;
                                                try {
                                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                                    Log.i(LOG_TAG, receivedJson.toString());
                                                    Toast.makeText(getBaseContext(), (String) receivedJson.get("auth"), Toast.LENGTH_SHORT).show();
                                                    if(receivedJson.get("auth").equals("authorised")){
                                                        isAuthorisedByMirror = true;
                                                        authNumTxt.setVisibility(View.INVISIBLE);
                                                        findViewById(R.id.authNumLbl).setVisibility(View.INVISIBLE);
                                                    }else{
                                                        Log.i(LOG_TAG, "unable to authorise connection to mirror");
                                                    }
                                                } catch (JSONException e) {
                                                    Log.e(LOG_TAG, "Error creating Json with auth data", e);
                                                }
                                            } catch (UnsupportedEncodingException e) {
                                                Log.e(LOG_TAG, "Received message encoding error", e);
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "unable to subscribe to /iotappdev/auth/", e);
                        }
                        // connect to /iotappdev/news/article/link/
                        try {
                            myMQTTManager.subscribeToTopic("/iotappdev/news/article/link/", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject receivedJson = null;
                                                try {
                                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                                    if(isAuthorisedByMirror) {
//                                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse((String) receivedJson.get("link"))));
                                                        startActivity(new Intent(getApplicationContext() ,NewsActivity.class).putExtra("link", (String) receivedJson.get("link")));
                                                    }
                                                } catch (JSONException e) {
                                                    Log.e(LOG_TAG, "Error creating Json with article link data", e);
                                                }
                                            } catch (UnsupportedEncodingException e) {
                                                Log.e(LOG_TAG, "Received message encoding error", e);
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "unable to subscribe to /iotappdev/test/", e);
                        }
                        // connect to /iotappdev/weather/day/
                        try {
                            myMQTTManager.subscribeToTopic("/iotappdev/weather/day/", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject receivedJson = null;
                                                try {
                                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                                    Log.i(LOG_TAG, receivedJson.toString());
                                                    if(isAuthorisedByMirror) {
                                                        startActivity(new Intent(getApplicationContext(), WeatherActivity.class).putExtra("weather", (String) receivedJson.toString()));
                                                    }
                                                } catch (JSONException e) {
                                                    Log.e(LOG_TAG, "Error creating Json with weather data", e);
                                                }
                                            } catch (UnsupportedEncodingException e) {
                                                Log.e(LOG_TAG, "Received message encoding error", e);
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "unable to subscribe to /iotappdev/test/", e);
                        }
                    }
                }
            });
        }catch (Exception e){
            Log.e(LOG_TAG, "Error Connecting to client ID", e);
        }

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
                    authPublish(String.valueOf(authNumTxt.getText()));
                    authNumTxt.setText("");
                }
            }
        });

    } // END OF ON CREATE



//  METHODS TO PUBLISH TO AWS IOT MQTT

    private void authPublish(String num){
        if(isConnected){
            Log.i(LOG_TAG, "trying to publish to /iotappdev/pi/auth/ with: " + num);
            JSONObject authJson = new JSONObject();
            try {
                authJson.put("auth", String.valueOf(num));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Unable to add to authJson");
                e.printStackTrace();
            }
            myMQTTManager.publishString(authJson.toString(), "/iotappdev/pi/auth/", AWSIotMqttQos.QOS0);
        }
    }

    @Override
    public void onBackPressed(){
    }
}
