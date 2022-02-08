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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class DialogClickTitle_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private int counterlikes;
    private TextView Fullname, PdfTitle, PdfLike, PdfCosto;
    private ImageView valuta;
    private Button sblocca, dettagli;
    String title, fullname;
    private static int counter = 0;
    private String saveCurrentDate, saveCurrentTime, postRandomName;

    private DatabaseReference UsersRef, DialogPdfRef, LikesRef, PersonalPdfRef, KeyRef, PossessorCheckRef;
    private StorageReference PdfStorageRef;
    private FirebaseAuth mAuth;
    private String currentUserID, PdfKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_click_title);

        CheckConnection();

        PdfKey = getIntent().getExtras().get("PdfKey").toString();


        DialogPdfRef = FirebaseDatabase.getInstance().getReference().child("Pdfs").child(PdfKey);
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        PdfStorageRef = FirebaseStorage.getInstance().getReference().child("Post Pdfs");
        PossessorCheckRef = FirebaseDatabase.getInstance().getReference().child("PossessorCheck");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        PersonalPdfRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs");
        KeyRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs").child(PdfKey);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");


        PdfTitle = findViewById(R.id.personaltitle_pdf);
        Fullname = findViewById(R.id.personalpossessor_name);
        PdfLike = findViewById(R.id.personalpdf_like);
        PdfCosto = findViewById(R.id.costo);
        PdfCosto.setVisibility(View.INVISIBLE);

        valuta = findViewById(R.id.valuta);
        valuta.setVisibility(View.INVISIBLE);

        sblocca = findViewById(R.id.sblocca);
        dettagli = findViewById(R.id.dettagli);

        //sblocca.setVisibility(View.INVISIBLE);


        dettagli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                Intent clickPdfIntent = new Intent(DialogClickTitle_Activity.this, ClickPdfActivity.class);
                clickPdfIntent.putExtra("PdfKey", PdfKey);
                startActivity(clickPdfIntent);

            }
        });

        PossessorCheckRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.child(PdfKey).hasChild(currentUserID)){

                    sblocca.setText("Apri");

                    sblocca.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CheckConnection();

                            DialogPdfRef.addValueEventListener(new ValueEventListener() {
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

                }else {

                    sblocca.setText("Sblocca");

                    PdfCosto.setVisibility(View.VISIBLE);
                    valuta.setVisibility(View.VISIBLE);

                    sblocca.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CheckConnection();

                            UsersRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    String stringbalance = snapshot.child("apPunti").getValue().toString();
                                    int balance = Integer.parseInt(stringbalance);

                                    if(balance >= 300){

                                        balance = balance - 300;
                                        UsersRef.child(currentUserID).child("apPunti").setValue(balance);

                                        DialogPdfRef.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                Calendar calForDate = Calendar.getInstance();
                                                SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
                                                saveCurrentDate = currentDate.format(calForDate.getTime());

                                                Calendar calForTime = Calendar.getInstance();
                                                SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
                                                saveCurrentTime = currentTime.format(calForTime.getTime());

                                                postRandomName = saveCurrentDate+saveCurrentTime;

                                                String namePdf = snapshot.child("postPdf").getValue().toString();


                                                insertInDatabase(currentUserID, postRandomName, fullname, namePdf);


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

                                    }else {

                                        Toast.makeText(DialogClickTitle_Activity.this, "Non hai abbastanza apPunti per sbloccare questo pdf", Toast.LENGTH_LONG).show();
                                        return;

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


        DialogPdfRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange (@NonNull DataSnapshot snapshot){

                if (snapshot.exists()) {

                    title = snapshot.child("title").getValue().toString();
                    fullname = snapshot.child("fullname").getValue().toString();

                    PdfTitle.setText(title);
                    Fullname.setText(fullname);


                    LikesRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                counterlikes = (int) snapshot.child(PdfKey).getChildrenCount();
                                PdfLike.setText(counterlikes + " Mi Piace");

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



    private void insertInDatabase(String currentUserID, String postRandomName, String userFullName, String namePdf){


        KeyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String PdfKey2 = snapshot.child("key").getValue().toString();

                PersonalPdfRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        int counterpdf = (int) snapshot.getChildrenCount() + 10000;
                        insert(counterpdf, currentUserID, postRandomName, userFullName, namePdf, PdfKey2);

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

    private void insert(int counterpdf, String currentUserID, String postRandomName, String userFullName, String namePdf, String PdfKey2){

        HashMap postsMap2 = new HashMap();
        postsMap2.put("uid", currentUserID);
        postsMap2.put("title", title.toUpperCase());
        postsMap2.put("postPdf", namePdf);
        postsMap2.put("fullname", userFullName);
        postsMap2.put("key", PdfKey2);

        PossessorCheckRef.child(PdfKey2).child(currentUserID).setValue(true);

        PersonalPdfRef.child(counterpdf + currentUserID + postRandomName).updateChildren(postsMap2).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {

                Toast.makeText(DialogClickTitle_Activity.this, "Pdf salvato nella tua lista", Toast.LENGTH_LONG).show();

            }
        });

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

            Intent CheckConnectionIntent = new Intent(DialogClickTitle_Activity.this, CheckConnectionActivity.class);
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