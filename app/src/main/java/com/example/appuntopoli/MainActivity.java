package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements ConnectionReceiver.ReceiverListener {

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RecyclerView pdfList;
    private Toolbar mToolbar;

    private CircleImageView NavProfileImage;
    private TextView NavProfileUserName, mainPunti;
    private ImageButton AddNewPdfButton;

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef, PostsRef, PdfsRef, LikesRef;
    private StorageReference UserProfileImageRef;

    private String currentUserID;
    private String apPunti;
    private Boolean likecheck = false;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CheckConnection();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        PdfsRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile images");

        navigationView = findViewById(R.id.navigation_view);


        mToolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Home");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        drawerLayout = findViewById(R.id.drawable_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();


        mainPunti = findViewById(R.id.main_punti);

        AddNewPdfButton = findViewById(R.id.add_new_pdf_button);

        View navView = navigationView.inflateHeaderView(R.layout.navigation_header);
        NavProfileImage = navView.findViewById(R.id.nav_profile_image);
        NavProfileUserName = navView.findViewById(R.id.nav_user_full_name);


        pdfList = findViewById(R.id.all_users_pdfs_list);
        pdfList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManagerPdf = new LinearLayoutManager(MainActivity.this);
        linearLayoutManagerPdf.setReverseLayout(true);
        linearLayoutManagerPdf.setStackFromEnd(true);
        pdfList.setLayoutManager(linearLayoutManagerPdf);


        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if(snapshot.exists()){

                    if(snapshot.hasChild("Username")) {

                        String username = snapshot.child("Username").getValue().toString();
                        NavProfileUserName.setText(username);
                        apPunti = snapshot.child("apPunti").getValue().toString();
                        mainPunti.setText(apPunti + "apPunti");

                    }else {

                        Toast.makeText(MainActivity.this, "Profile name doesn't exists", Toast.LENGTH_LONG).show();

                    }

                }else {

                    SendUserToSetupActivity();

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {

                Toast.makeText(MainActivity.this, "Error Occurred: "+error, Toast.LENGTH_LONG).show();
                SendUserToLoginActivity();

            }
        });

        UserProfileImageRef.child(currentUserID + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).placeholder(R.drawable.profile).into(NavProfileImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                UserMenuSelector(item);
                return false;
            }
        });


        AddNewPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnection();

                SendUserToPdfActivity();

            }
        });

        DisplayAllUsersPdfs();

    }


    @Override
    protected void onStart() {

        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){

            SendUserToLoginActivity();

        }else {

            checkifEmailVerified();

        }

    }


    public static class PdfsViewHolder extends RecyclerView.ViewHolder{


        View mView;

        TextView time, date, title, fullname;
        TextView numLike;
        CircleImageView profileImage;
        ImageButton likepdfButton;
        int counterlikes;
        private FirebaseAuth mAuth;
        String currentUserID;
        DatabaseReference LikesRef, PdfsRef;


        public PdfsViewHolder(View itemView) {

            super(itemView);
            mView = itemView;

            mAuth = FirebaseAuth.getInstance();
            currentUserID = mAuth.getCurrentUser().getUid();

            fullname = mView.findViewById(R.id.pdf_username);
            time = mView.findViewById(R.id.pdf_time);
            date = mView.findViewById(R.id.pdf_date);
            title = mView.findViewById(R.id.pdf_title);
            profileImage = mView.findViewById(R.id.pdf_profile_image);

            numLike = mView.findViewById(R.id.numlike_display);
            likepdfButton = mView.findViewById(R.id.like_button);


            LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
            PdfsRef = FirebaseDatabase.getInstance().getReference().child("Pdfs");


        }

        public void setLikeButtonStatus(String PdfKey){

            LikesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if(snapshot.child(PdfKey).hasChild(currentUserID)){

                        counterlikes = (int) snapshot.child(PdfKey).getChildrenCount();
                        likepdfButton.setImageResource(R.drawable.like);
                        numLike.setText(counterlikes + " Mi Piace");

                    }else {

                        counterlikes = (int) snapshot.child(PdfKey).getChildrenCount();
                        likepdfButton.setImageResource(R.drawable.dislike);
                        numLike.setText(counterlikes + " Mi Piace");

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

    }


    private void DisplayAllUsersPdfs() {

        FirebaseRecyclerOptions<Pdfs> options1 =
                new FirebaseRecyclerOptions.Builder<Pdfs>()
                        .setQuery(PdfsRef, Pdfs.class)
                        .build();

        FirebaseRecyclerAdapter<Pdfs, PdfsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Pdfs, PdfsViewHolder>(options1) {
            @Override
            protected void onBindViewHolder(@NonNull MainActivity.PdfsViewHolder holder, int position, Pdfs model) {

                final String PdfKey = getRef(position).getKey();

                holder.fullname.setText(model.getFullname());
                holder.date.setText("  " + model.getDate());
                holder.time.setText("  " + model.getTime());
                holder.title.setText(model.getPdfTitle());

                UserProfileImageRef.child(model.getUid() + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).placeholder(R.drawable.profile).into(holder.profileImage);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {

                    }
                });

                holder.setLikeButtonStatus(PdfKey);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckConnection();

                        Intent dialogClickIntent = new Intent(MainActivity.this, DialogClickTitle_Activity.class);
                        dialogClickIntent.putExtra("PdfKey", PdfKey);
                        startActivity(dialogClickIntent);

                    }
                });

                holder.likepdfButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        likecheck = true;

                        LikesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                if(likecheck.equals(true)){

                                    if(snapshot.child(PdfKey).hasChild(currentUserID)){

                                        likecheck = false;
                                        LikesRef.child(PdfKey).child(currentUserID).removeValue();

                                    }else {

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


            @Override
            public PdfsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_pdf_layout, parent, false);
                PdfsViewHolder viewHolder = new PdfsViewHolder(view);
                return viewHolder;

            }
        };

        pdfList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

    }

    private void checkifEmailVerified() {

        FirebaseUser user = mAuth.getCurrentUser();

        if(!user.isEmailVerified()){

            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.i("Success", "Yes");
                    }
                    else{
                        Log.i("Success", "No");}
                }
            });

            Toast.makeText(MainActivity.this, "Indirizzo e-mail non verificato, hai ricevuto una richiesta di conferma nella tua casella di posta elettronica.", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            SendUserToLoginActivity();

        }

    }

    private void SendUserToLoginActivity() {
        CheckConnection();

        Intent loginIntent = new Intent(MainActivity.this, Login_Activity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();

    }

    private void SendUserToMainActivity() {
        CheckConnection();

        Intent mainIntent = new Intent(MainActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();

    }


    private void SendUserToPdfActivity() {
        CheckConnection();

        Intent AddNewPdfIntent = new Intent(MainActivity.this, Pdf_Activity.class);
        startActivity(AddNewPdfIntent);

    }


    private void SendUserToSetupActivity() {
        CheckConnection();

        Intent setupIntent = new Intent(MainActivity.this, Setup_Activity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();

    }


    private void SendUserToProfileActivity() {
        CheckConnection();

        Intent profileIntent = new Intent(MainActivity.this, Profile_Activity.class);
        startActivity(profileIntent);

    }


    private void SendUserToPersonalPdfsActivity() {
        CheckConnection();

        Intent PersonalPdfsIntent = new Intent(MainActivity.this, PersonalPdfs_Activity.class);
        startActivity(PersonalPdfsIntent);

    }


    private void SendUserToFindPdfsActivity() {
        CheckConnection();

        Intent findPdfsIntent = new Intent(MainActivity.this, FindPdfs_Activity.class);
        startActivity(findPdfsIntent);

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

            Intent CheckConnectionIntent = new Intent(MainActivity.this, CheckConnectionActivity.class);
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


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(actionBarDrawerToggle.onOptionsItemSelected(item)) {
            CheckConnection();


            return true;

        }

        return super.onOptionsItemSelected(item);

    }


    private void UserMenuSelector(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.nav_pdf:
                SendUserToPdfActivity();
                break;

            case R.id.nav_profile:
                SendUserToProfileActivity();
                break;

            case R.id.nav_home:
                SendUserToMainActivity();
                break;

            case R.id.nav_pdfs:
                SendUserToPersonalPdfsActivity();
                break;

            case R.id.nav_find_pdfs:
                SendUserToFindPdfsActivity();
                break;

            case R.id.nav_logout:

                mAuth.signOut();
                SendUserToLoginActivity();
                break;

        }

    }
}