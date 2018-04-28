package iot.project.glennncullen.shmartmirror;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotesActivity extends AppCompatActivity {

    // static objects
    static final String LOG_TAG = NotesActivity.class.getCanonicalName();
    private Handler handler;

    // layout objects
    Button notesLogoutBtn;
    Button postNoteBtn;
    EditText notesEditText;
    ListView notesListView;
    ArrayList<String> notesList = new ArrayList<>();
    ArrayList<String> notesKeyList = new ArrayList<>();
    ArrayAdapter<String> notesAdapter;

    // firebase objects
    FirebaseDatabase firebaseDB;
    DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Notes");

        firebaseDB = FirebaseDatabase.getInstance();
        databaseRef = firebaseDB.getReference();

        notesListView = (ListView) findViewById(R.id.notesListview);
        notesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, notesList);
        notesEditText = (EditText) findViewById(R.id.notesEditText);
        notesEditText.setHint("Write Note");
        notesListView.setAdapter(notesAdapter);

        postNoteBtn = (Button) findViewById(R.id.postNoteBtn);
        postNoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notesList.size() > 4) {
                    Toast.makeText(getApplicationContext(), "Only 5 Notes Allowed", Toast.LENGTH_SHORT).show();
                    notesEditText.setText("");
                } else {
                    String note = String.valueOf(notesEditText.getText());
                    if (!note.equals("")) {
                        databaseRef.push().setValue(note);
                        try {
                            handler.publish(new JSONObject().put("new", 1), "/iotappdev/pi/notes/new/");
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Unable to create JSONObject for new notes");
                            e.printStackTrace();
                        }
                        notesEditText.setText("");
                    }
                }
            }
        });

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                final AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
                builder.setMessage(item)
                        .setTitle("Delete")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(LOG_TAG, "\nKey:\t" + notesKeyList.get(position) + "\nValue:\t" + notesList.get(position));
                                firebaseDB.getReference(notesKeyList.get(position)).setValue(null);
                                try {
                                    handler.publish(new JSONObject().put("new", 1), "/iotappdev/pi/notes/new/");
                                } catch (JSONException e) {
                                    Log.e(LOG_TAG, "Unable to create JSONObject for new notes");
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notesList.clear();
                notesKeyList.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    notesKeyList.add(ds.getKey());
                    notesList.add(ds.getValue(String.class));
                }
                Log.i(LOG_TAG, "notesList:\t" + notesList.toString());
                notesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(LOG_TAG, "Firebase database error:\t" + databaseError.toException());
            }
        });

        notesLogoutBtn = (Button) findViewById(R.id.notesLogoutBtn);
        notesLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
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
     * disable interaction with activity
     */
    public void disableInteraction(){
        notesLogoutBtn.setEnabled(false);
        notesListView.setEnabled(false);
        notesEditText.setEnabled(false);
        postNoteBtn.setEnabled(false);
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
