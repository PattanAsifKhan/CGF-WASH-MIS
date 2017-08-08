package in.collectivegood.dbsibycgf.gis;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import in.collectivegood.dbsibycgf.R;

public class MapActivity extends AppCompatActivity {
TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Intent intent=getIntent();
        String Text=intent.getExtras().getString("code").toLowerCase();
        DatabaseReference gis = FirebaseDatabase.getInstance().getReference("gis").child(Text);
        gis.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Location value = dataSnapshot.getValue(Location.class);
                directions(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        textView=(TextView)findViewById(R.id.textformap);
        textView.setText(Text);


    }
    public void directions(Location location) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location.getLat() + "," + location.getLon());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
}
