package com.example.uber;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.location.LocationServices.API;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;


public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {



    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, mRequest,mSettings;
    private LatLng pickupLocation;
    private Marker mDriverMaker;

    private Boolean requestBol = false;

    private Marker pickupMarker;

    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private TextView mDriverName, mDriverPhone, mDriverCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogout = (Button) findViewById(R.id.logoutCustomer);

        mSettings = (Button) findViewById(R.id.settingsCustomer);
        //mSettings.setOnClickListener(this);

        mRequest = (Button) findViewById(R.id.request);
       //mRequest.setOnClickListener(this);

        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);

        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);

        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseDatabase.getInstance().getReference("Users")
                        .child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();                              //The issue i had is that i hadn't made a child for Customers in the db so it was looking
                Toast.makeText(CustomerMapActivity.this, "You have been signed out successfully", Toast.LENGTH_SHORT).show();           // for a child to delete but instead it couldn't delete it. Instead i was just setting
                FirebaseAuth.getInstance().signOut();                                                                                                // for Customers to the current UID and it was changing every i log in with a different user.
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);                                             // So reference is a way to create paths for the database always be aware of that. And they are set once i set value or intialize them.
                startActivity(intent);                                                                                                              // Every child has to have a value. If it doesn't it will cause the value to be erased from the database.
                finish();                                                                                                                           // By setting .setValue() to null, it will cause the value to be erased from the realtime database.

            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentSettings = new Intent(CustomerMapActivity.this, CustomerSettingsAcitivity.class);
                startActivity(intentSettings);
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(requestBol){
                    requestBol = false;
                    geoQuery.removeAllListeners();

                    if(driverLocationRefListener!=null) { //worry about processes connected to the app that are null cause they can make the app crash.
                        driverLocationRef.removeEventListener(driverLocationRefListener);
                    }

                    if (mDriverMaker != null){
                        mDriverMaker.remove();
                    }

                    if(driverFoundID!= null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                        driverRef.setValue(true);
                        driverFoundID = null;
                    }

                    driverFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if (pickupMarker != null){
                        pickupMarker.remove();
                    }
                    mRequest.setText("Call an Uber");

                }
                else {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

                    if(mLastLocation != null) {
                        requestBol = true;
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));


                        pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        LatLng markerLocation = new LatLng(pickupLocation.latitude + 0.005, pickupLocation.longitude);
                        pickupMarker = mMap.addMarker(new MarkerOptions().position(markerLocation).title("Pickup Here").icon(bitmapDescriptorFromVector(getBaseContext(), R.drawable.ic_marker_svg)));

                        mRequest.setText("Getting you a Driver...");

                        getClosestDriver();

                    }else{
                        Toast.makeText(CustomerMapActivity.this, "We are retrieving your Location", Toast.LENGTH_SHORT).show();
                       }
                }
            }
        });
    }


    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0   , vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    DatabaseReference driverLocation;
    GeoQuery geoQuery;
    private void getClosestDriver() {
        driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

            GeoFire geoFire = new GeoFire(driverLocation);

            geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);

            geoQuery.removeAllListeners();

            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (!driverFound && requestBol) {
                        driverFound = true;
                        driverFoundID = key;

                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        HashMap map = new HashMap();
                        map.put("customerRideId", customerId);
                        driverRef.updateChildren(map);

                        getDriverLocation();
                        getDriverInfo();
                        mRequest.setText("Looking for driver Location.");
                    }
                    else{

                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    if (!driverFound) {
                        radius++;
                        getClosestDriver();
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });
        }


    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");
        //driverLocation.child(driverFoundID).removeValue();
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLng);
                    if(mDriverMaker != null){
                        mDriverMaker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);
                    
                    float distance = loc1.distanceTo(loc2);
                    float direction = loc2.bearingTo(loc1);
                    mRequest.setText("Driver Found: "+ distance + " The Bearing is: " + direction );

                    //while (distance>=500){

                    if(direction > 0 ){ //Moving away to the left
                        mDriverMaker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver").icon(bitmapDescriptorFromVector(getBaseContext(), R.drawable.ic_car_right)));

                    }
                    else {
                        mDriverMaker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver").icon(bitmapDescriptorFromVector(getBaseContext(), R.drawable.ic_car_svg)));
                    }
                     if(distance<100) {
                        mRequest.setText("Driver's Here.");
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!=null){
                        mDriverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected  synchronized  void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


   // @Override
   // protected void onStop() {
   //     super.onStop();
   // }


   /* public ValueEventListener getDriverLocationRefListener() {
        return driverLocationRefListener;
    }

    public void setDriverLocationRefListener(ValueEventListener driverLocationRefListener) {
        this.driverLocationRefListener = driverLocationRefListener;
    }*/
}