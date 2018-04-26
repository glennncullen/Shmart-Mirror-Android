package iot.project.glennncullen.shmartmirror;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    // static objects
    static final String LOG_TAG = NotesActivity.class.getCanonicalName();
    private Handler handler;

    // layout objects
    Button notesLogoutBtn;
    ListView notesListView;
    ArrayList<String> notesList;
    ArrayAdapter<String> notesAdapter;

    // firebase objects
    FirebaseDatabase firebaseDB;
    DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        handler = Handler.getInstance(getApplicationContext(), this);
        getSupportActionBar().setTitle("Notes");

        notesListView = (ListView) findViewById(R.id.notesListview);
        notesList = new ArrayList<String>();
        notesAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_expandable_list_item_1, notesList);

        notesLogoutBtn = (Button) findViewById(R.id.notesLogoutBtn);
        notesLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        firebaseDB = FirebaseDatabase.getInstance();
        databaseRef = firebaseDB.getReference("notes");
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    // update based on callback from pi
    public void update(JSONObject message){

    }

    @Override
    public void onBackPressed(){
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
