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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Register_Activity extends AppCompatActivity {

    private EditText UserEmail, UserPassword, UserConfirmPassword;
    private Button CreateAccountButton;
    private ProgressDialog loadingBar;
    private TextView LoginLink;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        UserEmail = findViewById(R.id.register_email);
        UserPassword = findViewById(R.id.register_password);
        UserConfirmPassword = findViewById(R.id.register_confirm_password);
        LoginLink = findViewById(R.id.login_link);

        loadingBar = new ProgressDialog(Register_Activity.this);

        CreateAccountButton = findViewById(R.id.register_create_account);

        CreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CreateNewAccount();

            }
        });

        LoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SendUserToLoginActivity();

            }
        });


    }



    private void CreateNewAccount() {

        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();
        String confirmPassword = UserConfirmPassword.getText().toString();

        if(TextUtils.isEmpty(email)) {

            Toast.makeText(Register_Activity.this, "Per favore inserisci la tua e-mail", Toast.LENGTH_LONG).show();

        }else if(TextUtils.isEmpty(password)) {

            Toast.makeText(Register_Activity.this, "Per favore inserire una password", Toast.LENGTH_LONG).show();

        }else if(TextUtils.isEmpty(confirmPassword)) {

            Toast.makeText(Register_Activity.this, "Per favore conferma password", Toast.LENGTH_LONG).show();

        }else if(!password.equals(confirmPassword)){

            Toast.makeText(Register_Activity.this, "Le password inserite non corrispondono", Toast.LENGTH_LONG).show();

        }else {

            StartLoadingBar("Creando un nuovo account...");

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){

                                FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
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

                                SendUserToSetupActivity();
                                Toast.makeText(Register_Activity.this, "Registrazione avvenuta con successo", Toast.LENGTH_LONG).show();
                                CloseLoadingBar();

                            }else {

                                String message = task.getException().getMessage();
                                Toast.makeText(Register_Activity.this, "Errore durante la registrazione", Toast.LENGTH_LONG).show();
                                CloseLoadingBar();

                            }

                        }
                    });

        }

    }




    private void StartLoadingBar(String title) {

        loadingBar.setTitle(title);
        loadingBar.setMessage("Per favore attendi mentre creiamo il tuo nuovo account...");
        loadingBar.show();
        loadingBar.setCanceledOnTouchOutside(true);

    }


    private void CloseLoadingBar() {

        loadingBar.dismiss();

    }


    private void SendUserToSetupActivity() {

        Intent setupIntent = new Intent(Register_Activity.this, Setup_Activity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();

    }


    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(Register_Activity.this, Login_Activity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();

    }


}