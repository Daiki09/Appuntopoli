package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ClickPdfActivity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private TextView PdfTitle, PdfDate, PdfTime, PdfFullname, numLike;
    private CircleImageView NavProfileImage;
    private Button DeleteButton;
    private DatabaseReference ClickPdfRef, LikesRef, PossessorCheckRef;
    private StorageReference UserProfileImageRef;
    private FirebaseAuth mAuth;
    private ImageButton likepdfButton;
    private int counterlikes;
    private Toolbar mToolbar;

    String title, date, time, fullname;

    private String PdfKey, currentUserID, databaseUserID;
    private Boolean likecheck = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_click_pdf);

        CheckConnection();

        PdfTitle = findViewById(R.id.click_pdf_title);
        PdfDate = findViewById(R.id.click_pdf_date);
        PdfTime = findViewById(R.id.click_pdf_time);
        NavProfileImage = findViewById(R.id.click_pdf_profile_image);
        PdfFullname = findViewById(R.id.click_pdf_username);

        mToolbar = findViewById(R.id.click_pdf_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Il tuo Profilo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        DeleteButton = findViewById(R.id.click_pdf_delete);


        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();


        PdfKey = getIntent().getExtras().get("PdfKey").toString();
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile images");
        ClickPdfRef = FirebaseDatabase.getInstance().getReference().child("Pdfs").child(PdfKey);
        PossessorCheckRef = FirebaseDatabase.getInstance().getReference().child("PossessorCheck");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");


        DeleteButton.setVisibility(View.INVISIBLE);


        ClickPdfRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    databaseUserID = snapshot.child("uid").getValue().toString();

                    title = snapshot.child("title").getValue().toString();
                    date = snapshot.child("date").getValue().toString();
                    time = snapshot.child("time").getValue().toString();
                    fullname = snapshot.child("fullname").getValue().toString();

                    PdfTitle.setText(title);
                    PdfDate.setText(date);
                    PdfTime.setText(time);
                    PdfFullname.setText(fullname);

                    numLike = findViewById(R.id.click_numlike_display);
                    likepdfButton = findViewById(R.id.click_like_button);


                    UserProfileImageRef.child(databaseUserID + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).placeholder(R.drawable.profile).into(NavProfileImage);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                        }
                    });

                    if (currentUserID.equals(databaseUserID)) {

                        DeleteButton.setVisibility(View.VISIBLE);

                    }


                    LikesRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot.child(PdfKey).hasChild(currentUserID)) {

                                counterlikes = (int) snapshot.child(PdfKey).getChildrenCount();
                                likepdfButton.setImageResource(R.drawable.like);
                                numLike.setText(counterlikes + " Mi Piace");


                            } else {

                                counterlikes = (int) snapshot.child(PdfKey).getChildrenCount();
                                likepdfButton.setImageResource(R.drawable.dislike);
                                numLike.setText(counterlikes + " Mi Piace");

                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    likepdfButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            likecheck = true;

                            LikesRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    if (likecheck.equals(true)) {

                                        if (snapshot.child(PdfKey).hasChild(currentUserID)) {

                                            likecheck = false;
                                            LikesRef.child(PdfKey).child(currentUserID).removeValue();

                                        } else {

                                            LikesRef.child(PdfKey).child(currentUserID).setValue(true);
                                            likecheck = false;

                                        }

                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }
                    });

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        DeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                DeleteCurrentPdf();

            }
        });

    }


    private void DeleteCurrentPdf() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ClickPdfActivity.this, R.style.AlertDialogTheme);
        builder.setTitle("Il pdf verrà elimininato definitivamente");

        builder.setPositiveButton("Conferma", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CheckConnection();

                ClickPdfRef.removeValue();
                LikesRef.child(PdfKey).removeValue();
                PossessorCheckRef.child(PdfKey).removeValue();
                Toast.makeText(ClickPdfActivity.this, "Il pdf è stato eliminato", Toast.LENGTH_LONG).show();
                SendUserToMainActivity();

            }
        });

        builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CheckConnection();

                dialog.cancel();

            }
        });

        Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.background_light);

    }


    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(ClickPdfActivity.this, MainActivity.class);
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

            Intent CheckConnectionIntent = new Intent(ClickPdfActivity.this, CheckConnectionActivity.class);
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