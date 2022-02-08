package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class Pdf_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private Toolbar mToolbar;
    private ImageButton SelectPdfImage;
    private Button UpdatePdfButton;
    private EditText PdfTitle;
    private static final int Pdf_Pick = 10;
    private ProgressDialog loadingBar;

    private Uri PdfUri;
    private String Title;

    private StorageReference PostPdfReference;
    private DatabaseReference UsersRef, PostRef, PersonalPdfRef, PossessorCheckRef;
    private FirebaseAuth mAuth;

    private String currentUserID;

    private String saveCurrentDate, saveCurrentTime, postRandomName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        CheckConnection();

        PostPdfReference = FirebaseStorage.getInstance().getReference();

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PersonalPdfRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs");
        PostRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");
        PossessorCheckRef = FirebaseDatabase.getInstance().getReference().child("PossessorCheck");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        mToolbar = findViewById(R.id.update_pdf_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Carica Pdf");

        loadingBar = new ProgressDialog(Pdf_Activity.this);

        SelectPdfImage = findViewById(R.id.select_pdf_image);
        UpdatePdfButton = findViewById(R.id.update_pdf_button);
        PdfTitle = findViewById(R.id.pdf_title);

        SelectPdfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                OpenFile();

            }
        });

        UpdatePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                ValidationPdfInfo();

            }
        });


    }



    private void OpenFile() {
        CheckConnection();

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("application/pdf");
        startActivityForResult(galleryIntent, Pdf_Pick);

    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Pdf_Pick && resultCode == RESULT_OK && data != null){

            PdfUri = data.getData();

            Picasso.get().load(R.drawable.accepted_pdf).placeholder(R.drawable.select_pdf).into(SelectPdfImage);

        }

    }

    private void ValidationPdfInfo() {

        Title = PdfTitle.getText().toString();

        AssetFileDescriptor fileDescriptor = null;
        try {

            fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(PdfUri , "r");

        } catch (FileNotFoundException e) {

            e.printStackTrace();

        }

        long fileSize = fileDescriptor.getLength()/1024;

        if(PdfUri == null){

            Toast.makeText(Pdf_Activity.this, "Per favore, inserisci un Pdf", Toast.LENGTH_LONG).show();

        }else if(TextUtils.isEmpty(Title)){

            Toast.makeText(Pdf_Activity.this, "Per favore, aggiungi una descrizione al tuo Pdf", Toast.LENGTH_LONG).show();

        }else if(fileSize < 60) {

            Toast.makeText(Pdf_Activity.this, "Per favore, inserisci un pdf valido, non vuoto e di qualità ", Toast.LENGTH_LONG).show();

        }else {

            StartLoadingBar("Caricamento Pdf...");

            StoringPdfToFirebaseStorage();

        }

    }


    private void StoringPdfToFirebaseStorage() {

        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        Calendar calForTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calForTime.getTime());

        postRandomName = saveCurrentDate+saveCurrentTime;

        String lastPath = PdfUri.getLastPathSegment();

        if(lastPath.contains("/")) {

            String path = lastPath.substring(lastPath.lastIndexOf("/") + 1);

            String namePdf = postRandomName + path;

            StorageReference filePath = PostPdfReference.child("Post Pdfs").child(namePdf);

            filePath.putFile(PdfUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()) {

                        Toast.makeText(Pdf_Activity.this, "Pdf caricato con successo nello Storage", Toast.LENGTH_LONG).show();

                        SavingPostInformationToDatabase(namePdf);

                        CloseLoadingBar();

                    } else {

                        String message = task.getException().getMessage();
                        Toast.makeText(Pdf_Activity.this, "Error occurred: " + message, Toast.LENGTH_LONG).show();
                        CloseLoadingBar();

                    }

                }
            });

        }else {

            String namePdf = postRandomName + lastPath + ".pdf";
            StorageReference filePath = PostPdfReference.child("Post Pdfs").child(namePdf);

            filePath.putFile(PdfUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()) {

                        Toast.makeText(Pdf_Activity.this, "Pdf caricato con successo nello storage", Toast.LENGTH_LONG).show();

                        SavingPostInformationToDatabase(namePdf);

                        CloseLoadingBar();

                    } else {

                        String message = task.getException().getMessage();
                        Toast.makeText(Pdf_Activity.this, "Error occurred: " + message, Toast.LENGTH_LONG).show();
                        CloseLoadingBar();

                    }

                }
            });

        }

    }



    private void SavingPostInformationToDatabase(String namePdf) {

        UsersRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if(snapshot.exists()){

                    String stringbalance = snapshot.child("apPunti").getValue().toString();
                    int balance = Integer.parseInt(stringbalance);
                    balance = balance + 500;
                    UsersRef.child(currentUserID).child("apPunti").setValue(balance);


                    String userFullName = snapshot.child("Fullname").getValue().toString();


                    insertInDatabase(currentUserID, postRandomName, userFullName, namePdf);

                    SendUserToMainActivity();


                }

            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

    }


    private void insertInDatabase(String currentUserID, String postRandomName, String userFullName, String namePdf){

        PersonalPdfRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int counterpdf = (int) snapshot.getChildrenCount() + 10000;
                insert(counterpdf, currentUserID, postRandomName, userFullName, namePdf);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void insert(int counterpdf, String currentUserID, String postRandomName, String userFullName, String namePdf){


        String key = counterpdf + currentUserID + postRandomName;

        HashMap postsMap = new HashMap();
        postsMap.put("uid", currentUserID);
        postsMap.put("date", saveCurrentDate);
        postsMap.put("time", saveCurrentTime);
        postsMap.put("title", Title.toUpperCase());
        postsMap.put("postPdf", namePdf);
        postsMap.put("fullname", userFullName);

        HashMap postsMap2 = new HashMap();
        postsMap2.put("uid", currentUserID);
        postsMap2.put("title", Title.toUpperCase());
        postsMap2.put("postPdf", namePdf);
        postsMap2.put("fullname", userFullName);
        postsMap2.put("key", key);

        PossessorCheckRef.child(key).child(currentUserID).setValue(true);

        PersonalPdfRef.child(key).updateChildren(postsMap2).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {

                Toast.makeText(Pdf_Activity.this, "Pdf caricato con successo", Toast.LENGTH_LONG).show();

            }
        });

        PostRef.child(key).updateChildren(postsMap)
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {

                        if(task.isSuccessful()){

                        }else {

                            String message = task.getException().getMessage();
                            Toast.makeText(Pdf_Activity.this, "Error Occurred: "+message, Toast.LENGTH_LONG).show();

                        }

                    }
                });

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


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home){

            SendUserToMainActivity();

        }

        return super.onOptionsItemSelected(item);

    }


    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent (Pdf_Activity.this, MainActivity.class);
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

            Intent CheckConnectionIntent = new Intent(Pdf_Activity.this, CheckConnectionActivity.class);
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