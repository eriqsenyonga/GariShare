package com.plexosysconsult.garishare;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    View rootView;
    private GoogleMap mMap;
    View bottomSheet, darkener;
    Button bLogout, bUnlockBike;
    private BottomSheetBehavior mBottomSheetBehavior;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.content_map, container, false);

        bLogout = (Button) rootView.findViewById(R.id.b_logout);
        bUnlockBike = (Button) rootView.findViewById(R.id.b_unlock_bike);
        bottomSheet = rootView.findViewById(R.id.bottom_sheet);
        darkener = (View) rootView.findViewById(R.id.view_darkener);


        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        checkLocationServices();

        bLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseAuth.getInstance().signOut();
                Intent i = new Intent(getActivity(), LoginActivity.class);
                startActivity(i);
                // finish();
                return;

            }
        });

        bUnlockBike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Toast.makeText(MainActivity.this, "Coming Soon. Open Scanner", Toast.LENGTH_SHORT).show();
                startQRScanner();

            }
        });

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    mBottomSheetBehavior.setPeekHeight(0);
                    darkener.setVisibility(View.GONE);
                }
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {

                    darkener.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
            }
        });

        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);


    }

    private void checkLocationServices() {

        Boolean gps_enabled = false, network_enabled = false;

        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
        if (!gps_enabled && !network_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();
        }


    }

    private void startQRScanner() {

       // new IntentIntegrator(getActivity()).initiateScan();
        IntentIntegrator.forSupportFragment(MapFragment.this)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
                .initiateScan();


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
       // super.onActivityResult(requestCode, resultCode, data);


        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                // updateText(result.getContents());

                //here we receive the QR of the bike and then send it to the server for opening

                Toast.makeText(getActivity(), "Data" + result.getContents(), Toast.LENGTH_LONG).show();

                openBikeLock();

                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);


            }
        } else {

            Log.d("onActivityResult", "result was null");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void openBikeLock() {

        /*
         * 1. Get bike associated to QR CODE
         * 2. Send information to server to open bike with the current userId and QR CODE
         * 3. As soon as bike opens, start timer and track movements of the bike
         * 4.
         *
         * */


    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
        //  mMap.moveCamera(CameraUpdateFactory.zo);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("OnlineCustomers");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //   LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());


        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

          /*      currentLocation = (Location) locationResult.getLastLocation();

                String result = "Current Location Latitude is " +
                        currentLocation.getLatitude() + "\n" +
                        "Current location Longitude is " + currentLocation.getLongitude();

                resultTextView.setText(result);

                */
                mLastLocation = locationResult.getLastLocation();
                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16));


                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("OnlineCustomers");

                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

            }
        }, Looper.myLooper());

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        //Latitude = 0.3360, Longitude = 32.5731 for maimood

        // Add a marker at Maimood hostel and move the camera
        LatLng maimood = new LatLng(0.3360, 32.5731);
        // Add a marker for Akamwesi Latitude = 0.3355, Longitude = 32.5739
        LatLng akamwesi = new LatLng(0.3355, 32.5739);
        LatLng easternGate = new LatLng(0.3356, 32.5726);
        LatLng rwenzoriTowers = new LatLng(0.3168, 32.5800);
        LatLng rwCourts = new LatLng(0.3163, 32.5802);
        LatLng harunaTowers = new LatLng(0.3399, 32.5716);
        LatLng kakande = new LatLng(0.3403, 32.5726);
        LatLng joint = new LatLng(0.3343, 32.5740);


        mMap.addMarker(new MarkerOptions().position(maimood).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(akamwesi).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(easternGate).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(rwenzoriTowers).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(rwCourts).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(harunaTowers).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(kakande).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));
        mMap.addMarker(new MarkerOptions().position(joint).title("Bike location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike_location)));


        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);

    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }
}
