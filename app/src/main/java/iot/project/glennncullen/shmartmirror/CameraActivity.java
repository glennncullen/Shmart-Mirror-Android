package iot.project.glennncullen.shmartmirror;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    static final String LOG_TAG = CameraActivity.class.getCanonicalName();

    private Handler handler;

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    ImageView cameraImageView;
    Button cameraLogoutBtn;
    Button cameraSaveBtn;
    Button cameraDiscardBtn;

    Bitmap currentPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Camera");

        cameraImageView = (ImageView) findViewById(R.id.cameraImageView);

        firebaseStorage = FirebaseStorage.getInstance();



        cameraSaveBtn = (Button) findViewById(R.id.cameraSaveBtn);
        cameraSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PermissionChecker.PERMISSION_GRANTED){
                    int MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
                    ActivityCompat.requestPermissions(CameraActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                }else{
//                    getApplicationContext().sendBroadcast(new Intent(
//                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
//                            Uri.parse(MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), currentPic, "test", "test"))
//                    ));
                    MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), currentPic, "test", "test");
                    Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                    cameraImageView.setImageDrawable(getApplicationContext().getDrawable(android.R.drawable.gallery_thumb));
                    cameraSaveBtn.setEnabled(false);
                    cameraDiscardBtn.setEnabled(false);
                }
            }
        });
        cameraSaveBtn.setEnabled(false);


        cameraDiscardBtn = (Button) findViewById(R.id.cameraDiscardBtn);
        cameraDiscardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Discarded", Toast.LENGTH_SHORT).show();
                cameraImageView.setImageDrawable(getApplicationContext().getDrawable(android.R.drawable.gallery_thumb));
                cameraSaveBtn.setEnabled(false);
                cameraDiscardBtn.setEnabled(false);
            }
        });
        cameraDiscardBtn.setEnabled(false);


        cameraLogoutBtn = (Button) findViewById(R.id.cameraLogoutBtn);
        cameraLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
            }
        });
    }// END OF CREATE

    // update based on callback from pi
    public void update(JSONObject message) {
        if(!message.has("pic_name")) return;
        try {
            storageReference = firebaseStorage.getReference().child((String) message.get("pic_name"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to decode jsonobject for update on camera activity");
            e.printStackTrace();
        }
        storageReference.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                currentPic = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                cameraImageView.setImageBitmap(Bitmap.createScaledBitmap(currentPic, currentPic.getWidth()/2, currentPic.getHeight()/2, false));
                cameraSaveBtn.setEnabled(true);
                cameraDiscardBtn.setEnabled(true);
                storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(LOG_TAG, "picture deleted from firebase");
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(LOG_TAG, "Failed to download byte array from firebase storage:\n" + e.toString());
            }
        });
    }

    @Override
    public void onBackPressed() {
    }

    /**
     * disable interaction with activity
     */
    public void disableInteraction(){
        cameraLogoutBtn.setEnabled(false);
        cameraDiscardBtn.setEnabled(false);
        cameraSaveBtn.setEnabled(false);

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