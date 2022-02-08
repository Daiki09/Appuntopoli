package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class Setup_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private EditText setupUserName, setupFullName, setupSchool;
    private Button SaveInformationButton;
    private CircleImageView setupProfileImage;
    private ProgressDialog loadingBar;

    private Uri ImageUri;
    private Uri resultUri;
    private String downloadUrl;

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef;
    private StorageReference UserProfileImageRef;
    String currentUserID;
    private final static int Gallery_Pick = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        CheckConnection();

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile images");

        setupUserName = findViewById(R.id.setup_username);
        setupFullName = findViewById(R.id.setup_full_name);
        setupSchool = findViewById(R.id.setup_school);

        loadingBar = new ProgressDialog(Setup_Activity.this);

        SaveInformationButton = findViewById(R.id.setup_information_button);

        setupProfileImage = findViewById(R.id.setup_profile_image);

        SaveInformationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                SaveAccountSetupInformation();

            }
        });

        setupProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                OpenGallery();

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

        }

        StartLoadingBar("Caricando l'immagine..");

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK){

                resultUri = result.getUri();

                downloadUrl = resultUri.toString();

                Picasso.get().load(downloadUrl).placeholder(R.drawable.profile).into(setupProfileImage);

            }else {

                Toast.makeText(Setup_Activity.this, "Error Occurred: Image can't be cropped, try again.", Toast.LENGTH_LONG).show();

            }

        }

        CloseLoadingBar();

    }


    private void SaveAccountSetupInformation() {

        String username = setupUserName.getText().toString();
        String fullname = setupFullName.getText().toString();
        String school = setupSchool.getText().toString();

        if(TextUtils.isEmpty(username)) {

            Toast.makeText(Setup_Activity.this, "Per favore inserisci il tuo username..", Toast.LENGTH_LONG).show();

        }else if(TextUtils.isEmpty(fullname)) {

            Toast.makeText(Setup_Activity.this, "Per favore inserisci il tuo nome completo..", Toast.LENGTH_LONG).show();

        }else if(TextUtils.isEmpty(school)) {

            Toast.makeText(Setup_Activity.this, "Per favore inserisci la tua scuola/università..", Toast.LENGTH_LONG).show();

        }else if(ImageUri == null) {

            Toast.makeText(Setup_Activity.this, "Per favore selezione un'immagine del profilo..", Toast.LENGTH_LONG).show();

        }else {

                StartLoadingBar("Salvando le informazioni..");

                HashMap userMap = new HashMap();
                userMap.put("Username", username);
                userMap.put("Fullname", fullname);
                userMap.put("School", school);
                userMap.put("Competenze", "");
                userMap.put("apPunti", 500);

            StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");

            filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if(task.isSuccessful()){

                        Toast.makeText(Setup_Activity.this, "Immagine del profilo caricata con successo", Toast.LENGTH_LONG).show();

                        UsersRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {

                                if(task.isSuccessful()){

                                    SendUserToMainActivity();
                                    Toast.makeText(Setup_Activity.this, "Il tuo account è stato aggiornato con successo", Toast.LENGTH_LONG).show();
                                    CloseLoadingBar();

                                }else {

                                    Toast.makeText(Setup_Activity.this, "Errore durante l'aggiornamento dell'account", Toast.LENGTH_LONG).show();
                                    CloseLoadingBar();

                                }

                            }

                        });

                    }else {

                        String message = task.getException().getMessage();
                        Toast.makeText(Setup_Activity.this, "Errore nell'aggiornamento dell'immagine del profilo" + message, Toast.LENGTH_LONG).show();
                        CloseLoadingBar();

                    }

                }

            });  //save crop image into firebase storage

        }

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

        Intent mainIntent = new Intent (Setup_Activity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

            Intent CheckConnectionIntent = new Intent(Setup_Activity.this, CheckConnectionActivity.class);
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