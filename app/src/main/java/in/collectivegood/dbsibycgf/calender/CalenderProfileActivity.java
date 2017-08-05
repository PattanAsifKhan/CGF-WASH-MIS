package in.collectivegood.dbsibycgf.calender;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import in.collectivegood.dbsibycgf.R;
import in.collectivegood.dbsibycgf.database.CCDbHelper;
import in.collectivegood.dbsibycgf.database.CCRecord;
import in.collectivegood.dbsibycgf.database.DbHelper;
import in.collectivegood.dbsibycgf.database.Schemas;
import in.collectivegood.dbsibycgf.gallery.GalleryCCListActivity;
import in.collectivegood.dbsibycgf.gallery.GalleryMainActivity;
import in.collectivegood.dbsibycgf.support.InfoProvider;
import in.collectivegood.dbsibycgf.support.UserTypes;

public class CalenderProfileActivity extends AppCompatActivity {
    ArrayAdapter<CCRecord> ccRecordArrayAdapter;
    ArrayList<CCRecord> cclist;
    ListView listView;
    String selectedstate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calender_profile);
        String email = InfoProvider.getCCData(this, Schemas.CCDatabaseEntry.EMAIL);
        final DatabaseReference user_type = FirebaseDatabase.getInstance()
                .getReference("user_types")
                .child(email.replaceAll("\\.", "(dot)"));
        user_type.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value.equals(UserTypes.USER_TYPE_CC)) {
                    openCalendar(InfoProvider.getCCState(CalenderProfileActivity.this), InfoProvider.getCcUID(CalenderProfileActivity.this));
                    finish();
                } else {
                    admin();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void admin() {
        cclist = new ArrayList<>();
        ccRecordArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cclist);
        listView = (ListView) findViewById(R.id.cclist);
        Spinner spinner = (Spinner) findViewById(R.id.spinnerstates);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    selectedstate = "TL";
                } else if (position == 2) {
                    selectedstate = "AP";
                } else {
                    return;
                }
                CCDbHelper dbHelper = new CCDbHelper(new DbHelper(CalenderProfileActivity.this));
                Cursor read = dbHelper.read(null, null);
                cclist.clear();
                while (read.moveToNext()) {
                    String Uid = read.getString(read.getColumnIndexOrThrow(Schemas.CCDatabaseEntry.UID));
                    String Name = read.getString(read.getColumnIndexOrThrow(Schemas.CCDatabaseEntry.NAME));
                    String Phone = read.getString(read.getColumnIndexOrThrow(Schemas.CCDatabaseEntry.PHONE));
                    String Email = read.getString(read.getColumnIndexOrThrow(Schemas.CCDatabaseEntry.EMAIL));
                    String ProjectCoordinator = read.getString(read.getColumnIndexOrThrow(Schemas.CCDatabaseEntry.PROJECT_COORDINATOR));
                    String ccState = InfoProvider.getCCState(CalenderProfileActivity.this, Uid);
                    if (ccState.equalsIgnoreCase(selectedstate)) {
                        CCRecord ccRecord = new CCRecord(Uid, Name, Phone, Email, ProjectCoordinator);
                        cclist.add(ccRecord);
                    }
                }
                read.close();
                ccRecordArrayAdapter.notifyDataSetChanged();
                listView.setAdapter(ccRecordArrayAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String uid = cclist.get(position).getUid();
                openCalendar(selectedstate, uid);
            }
        });

    }

    private void openCalendar(String state, String Uid) {
        Intent intent = new Intent(CalenderProfileActivity.this, CalendarActivity.class);
        intent.putExtra("state", state);
        intent.putExtra("uid", Uid);
        startActivity(intent);

    }

}
