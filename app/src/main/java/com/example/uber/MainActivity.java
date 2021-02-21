package com.example.uber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private Button mDriver,mCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //@Override
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDriver = (Button) findViewById(R.id.driver);
        mCustomer = (Button) findViewById(R.id.customer);

        mDriver.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DriverLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        mCustomer.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this,CustomerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
         });

    }

    private int counter = 0;

    @Override
    public void onBackPressed() {
        counter++;

        if (counter == 1){
            Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }

        if (counter == 2){
            super.onBackPressed();
            counter = 0;
            finish();
            return;
        }
    }


}
