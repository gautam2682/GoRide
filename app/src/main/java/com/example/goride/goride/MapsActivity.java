package com.example.goride.goride;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.goride.goride.Utils.Calculate_distance.calculateDistance;
import static com.example.goride.goride.Utils.DisplayPromptGps.displayPromptForEnablingGPS;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    LatLng latLng1,latLng;
    private GoogleMap mMap;
    UiSettings mUiSettings;
    Marker mCurrLocation; Location mLastLocation;
    FloatingSearchView searchdestination,searchsource;
    public final int  REQUEST_CODE_AUTOCOMPLETE =1;List<Address> addresses = null;

    private LatLng mCenterLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        initView();

    }

    private void initView() {
        searchdestination=(FloatingSearchView) findViewById(R.id.floating_search_destination);
        searchsource=(FloatingSearchView)findViewById(R.id.floating_search_source);
        searchdestination.setDimBackground(false);
        searchsource.setDimBackground(false);
        searchdestination.setOnFocusChangeListener(new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {
                openAutocompleteActivity();
            }

            @Override
            public void onFocusCleared() {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mUiSettings = mMap.getUiSettings();

        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                            1);
                    return;
        }
        buildGoogleApiClient();
        mGoogleApiClient.connect();
;
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mMap.clear();
                mMap.addMarker( new MarkerOptions()
                        .position( cameraPosition.target )
                        .title( cameraPosition.toString() )
                );
            }
        });




    }


    @Override
    public void onLocationChanged(Location location) {

        //remove previous current location marker and add new one at current position
        if (mCurrLocation != null) {
            mCurrLocation.remove();

        }
        PlaceMarkerSource(location);
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Toast.makeText(getApplicationContext(),"Inside check self permission",Toast.LENGTH_SHORT).show();
            return;
        }
       mLastLocation  = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //place marker at current position
         PlaceMarkerSource(mLastLocation);
        }else{
            displayPromptForEnablingGPS(MapsActivity.this);

        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); //5 seconds
        mLocationRequest.setFastestInterval(3000); //3 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    private void PlaceMarkerSource(Location mLastLocation) {
        mMap.clear();
        latLng1= new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng1);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocation = mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()),16));
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses= geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        searchsource.setSearchText((addresses.get(0).getSubLocality()));
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"onConnectionSuspended",Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,"onConnectionFailed",Toast.LENGTH_SHORT).show();


    }

    protected synchronized void buildGoogleApiClient() {
        Toast.makeText(this,"buildGoogleApiClient",Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }



    public void onMapSearch(View view) {
       EditText locationSearch = (EditText) findViewById(R.id.floating_search_destination);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(String.valueOf(locationSearch));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            mMap.addMarker(markerOptions);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(address.getLatitude(),address.getLongitude()),10));
            addLines(latLng,latLng1);

        }
    }

    private void addLines(LatLng latLng, LatLng latLng1) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latLng.latitude,latLng.longitude),12));
        GoogleDirection.withServerKey("AIzaSyCM2WYR-LElKfDKK7xnx9GcEZ32-RkfeSQ")
                .from(latLng)
                .to(latLng1)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        //Snackbar.make(btnRequestDirection, "Success with status : " + direction.getStatus(), Snackbar.LENGTH_SHORT).show();
                        if (direction.isOK()) {
                            ArrayList<LatLng> sectionPositionList = direction.getRouteList().get(0).getLegList().get(0).getSectionPoint();
                            for (LatLng position : sectionPositionList) {
                           //     mMap.addMarker(new MarkerOptions().position(position));
                            }

                            List<Step> stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
                            ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(getApplicationContext(), stepList, 5, Color.BLACK, 3, Color.BLUE);
                            for (PolylineOptions polylineOption : polylineOptionList) {
                                mMap.addPolyline(polylineOption);
                            }
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                    }
                });
    }

    private void openAutocompleteActivity() {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .build(this);
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
        } catch (GooglePlayServicesRepairableException e) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(),
                    0 /* requestCode */).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            String message = "Google Play Services is not available: " +
                    GoogleApiAvailability.getInstance().getErrorString(e.errorCode);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that the result was from the autocomplete widget.
        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK) {
                // Get the user's selected place from the Intent.
                Place place = PlaceAutocomplete.getPlace(this, data);
                searchdestination.setSearchText(place.getName());
                 addLines(place.getLatLng(),latLng1);
                String dis = String.valueOf(calculateDistance(place.getLatLng().latitude,place.getLatLng().longitude,latLng1.latitude,latLng1.longitude));
                Snackbar.make(getCurrentFocus(),String.valueOf(dis),Snackbar.LENGTH_SHORT).show();
                Log.d("Tag",dis);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    onMapReady(mMap);
                } else {

                    Toast.makeText(MapsActivity.this, "Permission denied ", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
