package com.example.uber;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterCustomer extends AppCompatActivity implements View.OnClickListener {

    private EditText mName, mEmail, mPassword, mAge;
    private ProgressBar mprogressRegister;
    private TextView mBanner;

    private Button mRegister;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_customer);
        mRegister = (Button) findViewById(R.id.register);
        mRegister.setOnClickListener(this);

        mBanner = (TextView) findViewById(R.id.banner);
        mBanner.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.editTextName);
        mEmail = (EditText) findViewById(R.id.editTextEmail);
        mPassword = (EditText) findViewById(R.id.editTextPassword);
        mAge = (EditText) findViewById(R.id.editTextAge);

        mprogressRegister = (ProgressBar) findViewById(R.id.progressBarRegister);
        mprogressRegister.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();

    }


    @Override
    public void onBackPressed() {
            Intent intent = new Intent(RegisterCustomer.this, CustomerLoginActivity.class);
            startActivity(intent);
            finish();
            return;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.banner:
                startActivity(new Intent(this,  MainActivity.class));
                finish();
                break;

            case R.id.register:
                registerUser();
                break;
        }
    }

    private void registerUser() {
        final String email = mEmail.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        final String name = mName.getText().toString().trim();
        final String age = String.valueOf(mAge.getText());

        if(name.isEmpty()){
            mName.setError("Full Name is required!");
            mName.requestFocus();
            return;
        }

        if(age.isEmpty()){
            mAge.setError("Age is required!");
            mAge.requestFocus();
            return;
        }

        if(email.isEmpty()){
            mEmail.setError("Email is required!");
            mEmail.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            mEmail.setError("Email should be legitimate");
            mEmail.requestFocus();
            return;
        }

        if(password.isEmpty()){
            mPassword.setError("Password is required");
            mPassword.requestFocus();
            return;
        }

        if(password.length() < 6){
            mPassword.setError("Min password Length is 6 characters");
            mPassword.requestFocus();
            return;
        }

        mprogressRegister.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //Customer customer = new Customer(name, age, email);
                            Toast.makeText(RegisterCustomer.this, "Customer has been registered successfully", Toast.LENGTH_LONG).show();
                            mprogressRegister.setVisibility(View.GONE);
                            Intent intent = new Intent(RegisterCustomer.this, CustomerMapActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            Toast.makeText(RegisterCustomer.this, "Failed to register, try again!", Toast.LENGTH_SHORT).show();
                            mprogressRegister.setVisibility(View.GONE);
                        }
                    }
                });

                          /* FirebaseDatabase.getInstance().getReference("Users")
                                    .child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {

                        }
                        else {
                            Toast.makeText(RegisterCustomer.this, "Failed to register.", Toast.LENGTH_SHORT).show();
                            mprogressRegister.setVisibility(View.GONE);
                        }
                    }
                });*/
    }
}