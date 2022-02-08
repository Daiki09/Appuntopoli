package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private EditText profileName, profileSchool, profileCompetenze;
    private TextView profileFullname, profileapPunti;
    private CircleImageView profileImage;
    private Button salva_modifiche;
    private Toolbar mToolbar;

    private ProgressDialog loadingBar;

    private Uri ImageUri;
    private Uri resultUri;
    private String downloadUrl;

    private StorageReference UserProfileImageRef;
    private DatabaseReference myprofileUsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;
    private final static int Gallery_Pick = 10;


    private String myprofileUsername, myprofileFullname, myprofileSchool, myprofileCompetenze, myprofileapPunti;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        CheckConnection();

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        myprofileUsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);

        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile images");

        loadingBar = new ProgressDialog(Profile_Activity.this);

        mToolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Il tuo Profilo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        profileName = findViewById(R.id.myprofile_username);
        profileFullname = findViewById(R.id.myprofile_full_name);
        profileSchool = findViewById(R.id.myprofile_school);
        profileCompetenze = findViewById(R.id.myprofile_competenze);
        profileapPunti = findViewById(R.id.myprofile_apPunti);
        salva_modifiche = findViewById(R.id.myprofile_salva_modifiche);

        profileImage = findViewById(R.id.myprofile_image);


        myprofileUsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    UserProfileImageRef.child(currentUserID + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).placeholder(R.drawable.profile).into(profileImage);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                        }
                    });

                    myprofileUsername = snapshot.child("Username").getValue().toString();
                    profileName.setText(myprofileUsername);
                    myprofileFullname = snapshot.child("Fullname").getValue().toString();
                    profileFullname.setText(myprofileFullname);
                    myprofileSchool = snapshot.child("School").getValue().toString();
                    profileSchool.setText(myprofileSchool);
                    myprofileCompetenze = snapshot.child("Competenze").getValue().toString();
                    profileCompetenze.setText(myprofileCompetenze);
                    myprofileapPunti = snapshot.child("apPunti").getValue().toString();
                    profileapPunti.setText("Punti disponibili: \n" + myprofileapPunti + " apPunti");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                OpenGallery();

            }
        });

        salva_modifiche.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CheckConnection();

                String username = profileName.getText().toString();
                String school = profileSchool.getText().toString();
                String competenze = profileCompetenze.getText().toString();

                HashMap userMap = new HashMap();
                userMap.put("Username", username);
                userMap.put("School", school);
                userMap.put("Competenze", competenze);

                myprofileUsersRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {

                        if(task.isSuccessful()){

                            SendUserToMainActivity();
                            Toast.makeText(Profile_Activity.this, "Il tuo account è stato aggiornato con successo", Toast.LENGTH_LONG).show();
                            CloseLoadingBar();

                        }else {

                            String message = task.getException().getMessage();
                            Toast.makeText(Profile_Activity.this, "Error Occurred: "+message, Toast.LENGTH_LONG).show();
                            CloseLoadingBar();

                        }

                    }

                });

            }

        });


    }

    private void OpenGallery() {
        CheckConnection();

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Gallery_Pick);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode == Gallery_Pick && resultCode == RESULT_OK && data != null){

            ImageUri = data.getData();
            CropImage.activity(ImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);

        }else {

            SendUserToMainActivity();

        }

        StartLoadingBar("Caricando l'immagine..");

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK){

                resultUri = result.getUri();

                downloadUrl = resultUri.toString();

                Picasso.get().load(downloadUrl).placeholder(R.drawable.profile).into(profileImage);

            }else {

                Toast.makeText(Profile_Activity.this, "Error Occurred: Image can't be cropped, try again.", Toast.LENGTH_LONG).show();

            }

        }

        StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");

        filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                if(task.isSuccessful()){

                Toast.makeText(Profile_Activity.this, "Immagine del profilo caricata con successo", Toast.LENGTH_LONG).show();

                }else {

                    Toast.makeText(Profile_Activity.this, "Error nel caricamento dell'immagine", Toast.LENGTH_LONG).show();

                }
            }

        });

        CloseLoadingBar();
        SendUserToMainActivity();

    }

    private void StartLoadingBar(String title) {

        loadingBar.setTitle(title);
        loadingBar.setMessage("Per favore attendere...");
        loadingBar.show();
        loadingBar.setCanceledOnTouchOutside(true);

    }


    private void CloseLoadingBar() {

        loadingBar.dismiss();

    }

    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(Profile_Activity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();

    }

    private void CheckConnection() {

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction("android.new.conn.CONNECTIVITY_CHANGE");
        registerReceiver(new ConnectionReceiver(), intentFilter);
        ConnectionReceiver.Listener = this;

        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();

        showConnectionMessage(isConnected);
    }

    private void showConnectionMessage(boolean isConnected) {

        if (!isConnected) {

            Intent CheckConnectionIntent = new Intent(Profile_Activity.this, CheckConnectionActivity.class);
            startActivity(CheckConnectionIntent);

        }

    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        showConnectionMessage(isConnected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CheckConnection();
    }

}