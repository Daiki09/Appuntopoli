package com.example.appuntopoli;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DeletePdf_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private String PdfKey, PdfKey2, currentUserID;
    private Button Conferma, Annulla;
    private DatabaseReference DialogPdfRef, PersonalPdfRef, PossessorCheckRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_pdf);

        CheckConnection();

        DialogPdfRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");
        PossessorCheckRef = FirebaseDatabase.getInstance().getReference().child("PossessorCheck");
        PersonalPdfRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs");

        PdfKey = getIntent().getExtras().get("PdfKey").toString();
        PdfKey2 = getIntent().getExtras().get("PdfKey2").toString();
        currentUserID = getIntent().getExtras().get("currentUserID").toString();

        Conferma = findViewById(R.id.conferma);
        Annulla = findViewById(R.id.annulla);

        Conferma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                PersonalPdfRef.child(PdfKey).removeValue();
                DialogPdfRef.child(PdfKey).removeValue();
                PossessorCheckRef.child(PdfKey2).child(currentUserID).removeValue();
                Toast.makeText(DeletePdf_Activity.this, "Il pdf è stato eliminato dalla tua lista", Toast.LENGTH_LONG).show();
                SendUserToMainActivity();

            }
        });

        Annulla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                finish();

            }
        });

    }

    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(DeletePdf_Activity.this, MainActivity.class);
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

            Intent CheckConnectionIntent = new Intent(DeletePdf_Activity.this, CheckConnectionActivity.class);
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