package in.collectivegood.dbsibycgf;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import in.collectivegood.dbsibycgf.database.CCDbHelper;
import in.collectivegood.dbsibycgf.database.CCRecord;
import in.collectivegood.dbsibycgf.database.DbHelper;
import in.collectivegood.dbsibycgf.database.Schemas;
import in.collectivegood.dbsibycgf.database.SchoolDbHelper;
import in.collectivegood.dbsibycgf.database.SchoolRecord;
import in.collectivegood.dbsibycgf.discussion.DiscussionActivity;
import in.collectivegood.dbsibycgf.profiles.CCProfileActivity;

public class InitializingActivity extends AppCompatActivity {

    private static final String TAG = "Initializing";
    private StorageReference reference;
    private SchoolDbHelper schoolDbHelper;
    private CCDbHelper ccDbHelper;
    private Intent intent;
    private ProgressDialog dialog;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initializing);

        intent = new Intent(InitializingActivity.this, DiscussionActivity.class);
        schoolDbHelper = new SchoolDbHelper(new DbHelper(this));
        ccDbHelper = new CCDbHelper(new DbHelper(this));

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.loading_data));
        dialog.setProgressPercentFormat(null);
        dialog.setProgressNumberFormat(null);
        dialog.setCancelable(false);
        dialog.show();

        file = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name) + "/list.csv");
        FirebaseStorage storage = FirebaseStorage.getInstance();
        reference = storage.getReference("list.csv");
        if (file.exists()) {
            final long localTime = file.lastModified();
            reference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    long onlineTime = storageMetadata.getCreationTimeMillis();
                    if (localTime < onlineTime) {
                        downloadFile();
                    } else {
                        try {
                            readFile(file);
                            setName();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            try {
                File folder = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name));
                boolean mkdirs = true;
                if (!folder.exists()) {
                    mkdirs = folder.mkdirs();
                }
                if (mkdirs) {
                    if (file.createNewFile()) {
                        downloadFile();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadFile() {
        reference.getFile(file)
                .addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        try {
                            readFile(file);
                            setName();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void readFile(File file) throws IOException {
        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        BufferedReader reader = new BufferedReader(new InputStreamReader(buf));
        reader.readLine();
        while (true) {
            String s = reader.readLine();
            if (s != null) {
                String[] record = s.split(",");
                Log.d(TAG, "onComplete: " + Arrays.toString(record));

                String block = record[1];
                String village = record[2];
                String schoolCode = record[3];
                String schoolName = record[4];
                String schoolEmail = record[5];
                String schoolDistrict = record[6];
                String schoolState = record[7];
                String ccName = record[8].trim();
                String ccEmail = record[9];
                String ccUid = record[10];
                String projectCoordinator = record[11];
                String ccPhone = record[12];

                SchoolRecord schoolRecord = new SchoolRecord(schoolCode, block, village,
                        schoolName, schoolEmail, schoolState, schoolDistrict, ccUid);
                Cursor school = schoolDbHelper.read(Schemas.SchoolDatabaseEntry.CODE, schoolCode);
                if (school.getCount() <= 0) {
                    school.close();
                    schoolDbHelper.insert(schoolRecord);
                }
                school.close();

                CCRecord ccRecord = new CCRecord(ccUid, ccName, ccPhone, ccEmail, projectCoordinator);
                Cursor cc = ccDbHelper.read(Schemas.CCDatabaseEntry.UID, ccUid);
                if (cc.getCount() <= 0) {
                    cc.close();
                    ccDbHelper.insert(ccRecord);
                }
                cc.close();

            } else {
                break;
            }
        }
        buf.close();
    }

    public void setName() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        Cursor cc = ccDbHelper.read(Schemas.CCDatabaseEntry.EMAIL, user.getEmail());
        String name = "UNKNOWN";
        if (cc.getCount() > 0) {
            cc.moveToFirst();
            name = cc.getString(cc.getColumnIndexOrThrow(Schemas.CCDatabaseEntry.NAME));
            intent = new Intent(InitializingActivity.this, CCProfileActivity.class);
        } else {
            cc = schoolDbHelper.read(Schemas.SchoolDatabaseEntry.EMAIL, user.getEmail());
            if (cc.getCount() > 0) {
                cc.moveToFirst();
                name = cc.getString(cc.getColumnIndexOrThrow(Schemas.SchoolDatabaseEntry.NAME));
                intent = new Intent(InitializingActivity.this, SchoolDbHelper.class);
            }
        }
        if (user.getDisplayName() == null) {
            Log.d(TAG, "setName: " + name);
            UserProfileChangeRequest userProfileChangeRequest
                    = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(userProfileChangeRequest)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            startActivity(intent);
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(
                                    InitializingActivity.this, e.getMessage(), Toast.LENGTH_SHORT
                            ).show();
                            dialog.dismiss();
                            finish();
                        }
                    });
        } else {
            startActivity(intent);
            dialog.dismiss();
            finish();
        }
    }
}
