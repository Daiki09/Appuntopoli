package com.example.appuntopoli;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class Login_Activity extends AppCompatActivity {

    private Button LoginButton;
    private ImageView GoogleSignInButton;
    private EditText UserEmail, UserPassword;
    private TextView NeedNewAccountLink;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    private static final int RC_SIGN_IN = 15;
    private static final String TAG = "Login_Activity";
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        NeedNewAccountLink = findViewById(R.id.login_link);
        UserEmail = findViewById(R.id.login_email);
        UserPassword = findViewById(R.id.login_password);
        LoginButton = findViewById(R.id.login_button);

        GoogleSignInButton = findViewById(R.id.google_signin_button);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);



        loadingBar = new ProgressDialog(Login_Activity.this);


        NeedNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SendUserToRegisterActivity();

            }
        });

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AllowingUserToLogin();

            }
        });

        GoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StartLoadingBar("Ricerca Account Google...");
                signIn();

            }
        });


    }




    private void AllowingUserToLogin() {

        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();

        if(TextUtils.isEmpty(email)){

            Toast.makeText(Login_Activity.this, "Per favore inserisci la tua e-mail", Toast.LENGTH_LONG).show();

        }else if(TextUtils.isEmpty(password)){

            Toast.makeText(Login_Activity.this, "Per favore inserisci la tua password", Toast.LENGTH_LONG).show();

        }else {

            StartLoadingBar("Accesso in corso..");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){

                                SendUserToMainActivity();

                                Toast.makeText(Login_Activity.this, "Accesso Effettuato", Toast.LENGTH_LONG).show();
                                CloseLoadingBar();

                            }else {

                                Toast.makeText(Login_Activity.this, "Errore nel login.", Toast.LENGTH_LONG).show();
                                CloseLoadingBar();

                            }

                        }
                    });

        }

    }


    private void signIn() {

        CloseLoadingBar();

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            StartLoadingBar("Accesso a Google..");

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);

            try {

                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
                Toast.makeText(Login_Activity.this, "Per favore attendere, autenticazione in corso...", Toast.LENGTH_LONG).show();

            }catch (ApiException e) {

                Toast.makeText(Login_Activity.this, "Autenticazione non riuscita" + e, Toast.LENGTH_LONG).show();
                CloseLoadingBar();

            }

        }
    }


    private void firebaseAuthWithGoogle(String account) {

        AuthCredential credential = GoogleAuthProvider.getCredential(account, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Log.d(TAG, "signInWithCredential:success");
                            SendUserToMainActivity();
                            CloseLoadingBar();

                        } else {

                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String message = task.getException().toString();
                            SendUserToLoginActivity();
                            Toast.makeText(Login_Activity.this, "Autenticazione non riuscita: " + message, Toast.LENGTH_LONG).show();
                            CloseLoadingBar();

                        }
                    }
                });

    }


    @Override
    protected void onStart() {

        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){

            SendUserToMainActivity();

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

        Intent mainIntent = new Intent (Login_Activity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();

    }


    private void SendUserToRegisterActivity() {

        Intent registerIntent = new Intent (Login_Activity.this, Register_Activity.class);
        startActivity(registerIntent);

    }


    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(Login_Activity.this, Login_Activity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();

    }

}