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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class FindPdfs_Activity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private Toolbar mToolbar;

    private EditText searchText;
    private ImageButton searchButton;

    private String searchInput;
    private int counterlikes;

    private RecyclerView searchResults;

    private DatabaseReference allPdfRef, LikesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pdfs);

        CheckConnection();

        allPdfRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");


        mToolbar = findViewById(R.id.personalpdfs_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Cerca pdf..");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        searchResults = findViewById(R.id.findpdfs_results);
        searchResults.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(FindPdfs_Activity.this);
        searchResults.setLayoutManager(linearLayoutManager);

        searchText = findViewById(R.id.findpdfs_search);
        searchButton = findViewById(R.id.findpdfs_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                searchInput = searchText.getText().toString().toUpperCase();
                SearchPdf(searchInput);

            }
        });

    }


    public static class FindPdfsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        LinearLayout PdfGroup;
        TextView Fullname, PdfTitle, PdfLike;

        public FindPdfsViewHolder(View itemView) {
            super(itemView);
            this.mView = itemView;

            this.PdfGroup = mView.findViewById(R.id.all_pdf_group);
            this.PdfTitle = mView.findViewById(R.id.personal_pdf);
            this.Fullname = mView.findViewById(R.id.personale_name);
            this.PdfLike = mView.findViewById(R.id.personal_like);

        }

    }


    private void SearchPdf(String searchInput) {

        Query query = allPdfRef.orderByChild("title")
                .startAt(searchInput).endAt(searchInput + "\uf88f");

        FirebaseRecyclerOptions<Pdfs> options =
                new FirebaseRecyclerOptions.Builder<Pdfs>()
                        .setQuery(query, Pdfs.class)
                        .build();

        FirebaseRecyclerAdapter<Pdfs, FindPdfsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Pdfs, FindPdfsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindPdfs_Activity.FindPdfsViewHolder holder, int position, Pdfs model) {

                final String PdfKey = getRef(position).getKey();

                holder.Fullname.setText(model.getFullname());
                holder.PdfTitle.setText(model.getPdfTitle());

                holder.PdfGroup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckConnection();

                        Intent dialogClickIntent = new Intent(FindPdfs_Activity.this, DialogClickTitle_Activity.class);
                        dialogClickIntent.putExtra("PdfKey", PdfKey);
                        startActivity(dialogClickIntent);

                    }
                });

                LikesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        counterlikes = (int) snapshot.child(PdfKey).getChildrenCount();
                        holder.PdfLike.setText(counterlikes + " Mi Piace");

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }

                });


            }

            @NonNull
            @Override
            public FindPdfsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_pdfs_display_layout, parent, false);
                FindPdfsViewHolder viewHolder = new FindPdfsViewHolder(view);
                return viewHolder;

            }
        };

        searchResults.setAdapter(firebaseRecyclerAdapter);
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

            Intent CheckConnectionIntent = new Intent(FindPdfs_Activity.this, CheckConnectionActivity.class);
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