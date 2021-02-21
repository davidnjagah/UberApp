package com.example.uber;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mNameField, mPhoneField;

    private Button mBack, mConfirm;

    private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;

    private String userID;
    private String mName;
    private String mPhone;
    private String mProfileImageUrl;

    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings_acitivity);

        mNameField = (EditText) findViewById(R.id.name);
        mPhoneField = (EditText) findViewById(R.id.phone);

        mProfileImage = (ImageView) findViewById(R.id.profileImage);
        mProfileImage.setOnClickListener(this);

        mConfirm = (Button) findViewById(R.id.confirm);
        mConfirm.setOnClickListener(this);

        mBack = (Button) findViewById(R.id.back);
        mBack.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(userID);

        getUserInfo();  //if i place this above the database initialization then the app will crash.

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;

            case R.id.confirm:
                saveUserInformation();
                break;

            case R.id.profileImage:
                Intent intentPic = new Intent((Intent.ACTION_PICK));
                intentPic.setType("image/*");
                startActivityForResult(intentPic, 1);
                break;

        }
    }


    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount()>0){ //have a check on the children count so as to confirm that the target child has registered user info
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue(); //a map is used when your picking many children from an instance in the database
                    if(map.get("name") != null){                          //If you dont check if its not equal to null then the app will crash
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if(map.get("profileImageUrl") != null){
                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplicationContext()).load(mProfileImageUrl).into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));
    }

    private void saveUserInformation(){
        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();

        Map userInfo = new HashMap(); //a Hashmap is used when you want to put multiple children data in an instance or those that contain numbers
        userInfo.put("name", mName); // adversely i can also use a customised class object in android studio to rep the user like in the @RegisterCustomer.class
        userInfo.put("phone", mPhone);


        if(mName.isEmpty()){
            mNameField.setError("Full Name is required!");
            mNameField.requestFocus();
            return;
        }

        if(mPhone.isEmpty()){
            mPhoneField.setError("Phone number is required!");
            mPhoneField.requestFocus();
            return;
        }

        if(resultUri != null) {
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUrl) {
                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl", downloadUrl.toString());
                            mCustomerDatabase.updateChildren(newImage);
                            finish();
                            return;
                            //do something with downloadurl
                        }
                    });
                }
            });
        }else{
            Toast.makeText(DriverSettingsActivity.this, "Please select a profile image.", Toast.LENGTH_SHORT).show();
            return;
        }
        mCustomerDatabase.updateChildren(userInfo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;

            mProfileImage.setImageURI(resultUri);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}