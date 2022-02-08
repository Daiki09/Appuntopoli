package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class PersonalPdfs_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private Toolbar mToolbar;

    private RecyclerView personalPdfResults;

    private DatabaseReference allPdfRef, LikesRef, PersonalPdfRef, KeyRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_pdfs);

        CheckConnection();

        allPdfRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");
        PersonalPdfRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        KeyRef = FirebaseDatabase.getInstance().getReference().child("PersonalPdfs");

        mToolbar = findViewById(R.id.personalpdfs_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Lista Pdf Caricati");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        personalPdfResults = findViewById(R.id.personalpdfs_results);
        personalPdfResults.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(PersonalPdfs_Activity.this);
        personalPdfResults.setLayoutManager(linearLayoutManager);
        ListPdf();

    }


    public static class PersonalPdfsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        LinearLayout PdfGroup;
        TextView Fullname, PdfTitle, PdfLike;

        public PersonalPdfsViewHolder(View itemView) {
            super(itemView);
            this.mView = itemView;

            this.PdfGroup = mView.findViewById(R.id.pdf_group);
            this.PdfTitle = mView.findViewById(R.id.personal_pdf);
            this.Fullname = mView.findViewById(R.id.personal_name);
            this.PdfLike = mView.findViewById(R.id.personal_like);

        }

    }


    private void ListPdf() {

        Query query = PersonalPdfRef.orderByChild("uid")
                .equalTo(currentUserID);

        FirebaseRecyclerOptions<Pdfs> options =
                new FirebaseRecyclerOptions.Builder<Pdfs>()
                        .setQuery(query, Pdfs.class)
                        .build();

        FirebaseRecyclerAdapter<Pdfs, PersonalPdfs_Activity.PersonalPdfsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Pdfs, PersonalPdfs_Activity.PersonalPdfsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull PersonalPdfs_Activity.PersonalPdfsViewHolder holder, int position, Pdfs model) {

                final String PdfKey = getRef(position).getKey();

                holder.Fullname.setText(model.getFullname());
                holder.PdfTitle.setText(model.getPdfTitle());

                holder.PdfGroup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckConnection();

                        Intent dialogClickIntent = new Intent(PersonalPdfs_Activity.this, DialogClickPersonal_Activity.class);
                        dialogClickIntent.putExtra("PdfKey", PdfKey);
                        startActivity(dialogClickIntent);

                    }
                });

            }

            @NonNull
            @Override
            public PersonalPdfs_Activity.PersonalPdfsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_personal_pdfs, parent, false);
                PersonalPdfs_Activity.PersonalPdfsViewHolder viewHolder = new PersonalPdfs_Activity.PersonalPdfsViewHolder(view);
                return viewHolder;

            }
        };

        personalPdfResults.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

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

            Intent CheckConnectionIntent = new Intent(PersonalPdfs_Activity.this, CheckConnectionActivity.class);
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