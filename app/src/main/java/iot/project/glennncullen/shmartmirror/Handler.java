package iot.project.glennncullen.shmartmirror;

/*
Amazon Cognito Identity Pool Id:   "us-east-2:02b24f82-0972-4eb4-bcd2-83760190d1ef"

Partial Code for Connection to AWS IoT taken from:
https://github.com/awslabs/aws-sdk-android-samples/blob/master/AndroidPubSub/src/com/amazonaws/demo/androidpubsub/PubSubActivity.java

 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.UUID;

public class Handler {

    private static Handler handler;
    private static Context context;
    private static AppCompatActivity activityInFocus;

    // set log tag to underlying class name for debug
    private static final String LOG_TAG = Handler.class.getCanonicalName();

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

    private static AWSIotClient myIotAndroidClient;
    private static AWSIotMqttManager myMQTTManager;
    private static String keystorePath;
    private static String keystoreName;
    private static String keystorePassword;

    private static KeyStore clientKeyStore = null;
    private static String certificateId;

    private static boolean isConnected;
    private static boolean isAuthorisedByMirror;

    /**
     * empty private constructor for
     * singleton
     */
    private Handler(){
    }

    /**
     * returns instance of handler singleton
     * or initiates singleton
     *
     * @param appContext Context of the application
     * @param currentActivity current activity calling the handler
     * @return instance of handler singleton
     */
    public static Handler getInstance(Context appContext, AppCompatActivity currentActivity){
        if(handler == null){
            handler = new Handler();
            context = appContext;
            activityInFocus = currentActivity;
            connectToMQTT();
        }
        context = appContext;
        activityInFocus = currentActivity;
        Log.i(LOG_TAG, "activityInFocus:\t" + activityInFocus.getClass().getSimpleName());
        return handler;
    }

    /**
     * set isAuthorisedByMirror to true or false
     */
    public static void changeAuthorisation(){
        isAuthorisedByMirror = !isAuthorisedByMirror;
    }

    /**
     * handle logout event
     */
    public static void logout(){
        isAuthorisedByMirror = false;
        activityInFocus.startActivity(new Intent(context ,MainActivity.class));
    }

    /**
     * publishes message param to
     * topic param
     *
     * @param message JSONObject being sent to publish
     * @param topic topic path to be published to
     */
    public static void publish(JSONObject message, String topic){
        if(isConnected){
            Log.i(LOG_TAG, "trying to publish\nTopic: \t" + topic + "\nMessage: \t" + message.toString());
            myMQTTManager.publishString(message.toString(), topic, AWSIotMqttQos.QOS0);
        }
    }


    /**
     * Subscribes to the topic specified in the
     * topic parameter. Calls Update method of
     * the activityInFocus which handles incoming
     * data
     *
     * @param topic topic path to subscribe to
     */
    private static void subscribeToTopic(String topic){
        try {
            myMQTTManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(final String topic, final byte[] data) {
                    activityInFocus.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject receivedJson = null;
                                try {
                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                    Log.i(LOG_TAG, "Sending:\t" + receivedJson.toString() + "\nTo:\t" + activityInFocus.getClass().getSimpleName() + "\nInstance:\t" + activityInFocus.toString());
                                    if(receivedJson.has("auth")) {
                                        ((MainActivity) activityInFocus).update(receivedJson);
                                    }else if(receivedJson.has("logout")){
                                        logout();
                                    }else if(isAuthorisedByMirror){
                                        switch(activityInFocus.getClass().getSimpleName()){
                                            case "NotesActivity":
                                                ((NotesActivity) activityInFocus).update(receivedJson);
                                                break;
                                            case "NewsActivity":
                                                ((NewsActivity) activityInFocus).update(receivedJson);
                                                break;
                                            case "WeatherActivity":
                                                ((WeatherActivity) activityInFocus).update(receivedJson);
                                                break;
                                            case "CameraAvtivity":
                                                ((CameraActivity) activityInFocus).update(receivedJson);
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                } catch (JSONException e) {
                                    Log.e(LOG_TAG, "Error creating Json for topic: " + topic, e);
                                }
                            } catch (UnsupportedEncodingException e) {
                                Log.e(LOG_TAG, "Received message encoding error", e);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, "unable to subscribe to: " + topic, e);
        }
    }

    /**
     * Subscribes to /iotappdev/displays/
     * handles incoming data and starts relevant activities
     * as user switches between displays on the mirror
     *
     * disables inetraction with current activity
     * while next activity is starting
     */
    private static void subscribeToDisplays(){
        try {
            myMQTTManager.subscribeToTopic("/iotappdev/display/", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(String topic, final byte[] data) {
                    activityInFocus.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject receivedJson = null;
                                try {
                                    receivedJson = new JSONObject(new String(data, "UTF-8"));
                                    switch((String) receivedJson.get("display")){
                                        case "NotesFeed":
                                            activityInFocus.startActivity(new Intent(context ,NotesActivity.class));
                                            break;
                                        case "WeatherFeed":
                                            activityInFocus.startActivity(new Intent(context ,WeatherActivity.class));
                                            break;
                                        case "NewsFeed":
                                            activityInFocus.startActivity(new Intent(context ,NewsActivity.class));
                                            break;
                                        case "CameraFeed":
                                            activityInFocus.startActivity(new Intent(context ,CameraActivity.class));
                                            break;
                                        default:
                                            break;
                                    }
                                    switch(activityInFocus.getClass().getSimpleName()){
                                        case "NotesActivity":
                                            ((NotesActivity) activityInFocus).disableInteraction();
                                            break;
                                        case "NewsActivity":
                                            ((NewsActivity) activityInFocus).disableInteraction();
                                            break;
                                        case "WeatherActivity":
                                            ((WeatherActivity) activityInFocus).disableInteraction();
                                            break;
                                        case "CameraAvtivity":
                                            ((CameraActivity) activityInFocus).disableInteraction();
                                            break;
                                        default:
                                            break;
                                    }
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
    }

    /**
     * Connects to AWS IoT
     */
    private static void connectToMQTT(){
        // unique client ID for AWS IoT is required
        String clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context, // context
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

        keystorePath = context.getFilesDir().getPath();
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
        try{
            myMQTTManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                    if (String.valueOf(status).equalsIgnoreCase("Connected")) {
                        isConnected = true;
                        Log.i(LOG_TAG, "is connected");
                        subscribeToTopic("/iotappdev/news/article/link/");
                        subscribeToTopic("/iotappdev/android/auth/");
                        subscribeToTopic("/iotappdev/weather/day/");
                        subscribeToTopic("/iotappdev/logout/");
                        subscribeToDisplays();
                    }
                }
            });
        }catch (Exception e){
            Log.e(LOG_TAG, "Error Connecting to client ID", e);
        }

    }

}
