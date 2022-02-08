package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DialogClickPersonal_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private int counterlikes;
    private TextView Fullname, PdfTitle, PdfLike;
    private Button apri, elimina;
    String title, fullname, PdfKey2;

    private DatabaseReference DialogPdfRef, LikesRef, PersonalPdfRef, KeyRef, PossessorCheckRef;
    private StorageReference PdfStorageRef;
    private FirebaseAuth mAuth;
    private String currentUserID, PdfKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_click_personal);

        CheckConnection();

        PdfKey = getIntent().getExtras().get("PdfKey").toString();

        DialogPdfRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        PdfStorageRef = FirebaseStorage.getInstance().getReference().child("Post Pdfs");
        PossessorCheckRef = FirebaseDatabase.getInstance().getReference().child("PossessorCheck");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        PersonalPdfRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs");

        KeyRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs").child(PdfKey);

        PdfTitle = findViewById(R.id.personaltitle_pdf);
        Fullname = findViewById(R.id.personalpossessor_name);
        PdfLike = findViewById(R.id.personalpdf_like);

        apri = findViewById(R.id.apri);
        elimina = findViewById(R.id.elimina);

        apri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                PersonalPdfRef.child(PdfKey).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String namePdf = snapshot.child("postPdf").getValue().toString();

                        PdfStorageRef.child(namePdf).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                                browserIntent.setDataAndType(uri, "application/pdf");
                                Intent chooser = Intent.createChooser(browserIntent, "Open file..");
                                chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(chooser);

                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });

        elimina.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                DeleteCurrentPdf();

            }

        });


        PersonalPdfRef.child(PdfKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange (@NonNull DataSnapshot snapshot){

                if (snapshot.exists()) {

                    title = snapshot.child("title").getValue().toString();
                    fullname = snapshot.child("fullname").getValue().toString();

                    PdfTitle.setText(title);
                    Fullname.setText(fullname);

                    KeyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            PdfKey2 = snapshot.child("key").getValue().toString();

                            LikesRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    counterlikes = (int) snapshot.child(PdfKey2).getChildrenCount();
                                    PdfLike.setText(counterlikes + " Mi Piace");

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }

            }

            @Override
            public void onCancelled (@NonNull DatabaseError error){

            }
        });

    }

    private void DeleteCurrentPdf() {

      Intent deleteClickIntent = new Intent(DialogClickPersonal_Activity.this, DeletePdf_Activity.class);
      deleteClickIntent.putExtra("PdfKey", PdfKey);
      deleteClickIntent.putExtra("PdfKey2", PdfKey2);
      deleteClickIntent.putExtra("currentUserID", currentUserID);
      startActivity(deleteClickIntent);

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

            Intent CheckConnectionIntent = new Intent(DialogClickPersonal_Activity.this, CheckConnectionActivity.class);
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