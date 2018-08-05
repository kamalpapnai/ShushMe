package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION =111 ;
    private static final int PLACE_PICKER_REQUEST=2;
    private static String Tag = "Values";

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private GoogleApiClient mClient;
    private Geofencing mGeofencing;
    private boolean misEnabled;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(Tag,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);




        Switch onOffGeofences = (Switch)findViewById(R.id.enable_switch);
        misEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled),false);
        onOffGeofences.setChecked(misEnabled);

        onOffGeofences.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled),isChecked);

                misEnabled = isChecked;
                editor.commit();

                if(isChecked)
                    mGeofencing.registerAllGeofences();
                else
                    mGeofencing.unregisterAllGeoences();

            }
        });


         mClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                refreshPlacesData();
                Log.d("Values",TAG+"Client Conected");
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d("Values",TAG+"Client Suspended");

            }
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.d("Values",TAG+"Client Connection Failed");
            }
        }).addApi(LocationServices.API).addApi(Places.GEO_DATA_API).enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.d("Values",TAG+"Connection Failed");
            }
        }).build();

        mGeofencing = new Geofencing(this,mClient);



    }

    public void onAddPlaceButtonClicked(View view){
        Log.d(Tag,"onAddPlaceButton");

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            Toast.makeText(MainActivity.this,"Need to Grant Location Permission",Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(MainActivity.this,getString(R.string.location_permission_granted),Toast.LENGTH_SHORT).show();

        try {
            // Start a new Activity for the Place Picker API, this will trigger {@code #onActivityResult}
            // when a place is selected or with the user cancels.
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            Intent i = builder.build(this);
            startActivityForResult(i, PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e("Values", String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e("Values", String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
        } catch (Exception e) {
            Log.e("Values", String.format("PlacePicker Exception: %s", e.getMessage()));
        }

        refreshPlacesData();


    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Values","OnctivityResullt");
        if(requestCode==PLACE_PICKER_REQUEST && resultCode==RESULT_OK){
            Place place=PlacePicker.getPlace(this,data);
            if(place==null){
                Log.d("Values",TAG+"No Place Selected2");

            }

            String placeName=place.getName().toString();
            String placeAddress=place.getAddress().toString();
            String placeId=place.getId();

            ContentValues contentValues = new ContentValues();
            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID,placeId);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI,contentValues);
            refreshPlacesData();

        }
    }

    public void refreshPlacesData(){
        Log.d(Tag,"oRefreshDataPlaces");
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor data = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        if(data==null || data.getCount()==0)
            return;

        List<String> guids = new ArrayList<String>();
        while (data.moveToNext()){
            guids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }

        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mClient,guids.toArray(new String[guids.size()]));
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapChanges(places);
                mGeofencing.updateGeofences(places);

                if(misEnabled)
                    mGeofencing.registerAllGeofences();
            }
        });
    }

    @Override
    public void onResume() {
        Log.d(Tag,"onResume");
        super.onResume();

        // Initialize location permissions checkbox
        CheckBox locationPermissions = (CheckBox) findViewById(R.id.checkbox1);
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.setChecked(false);
        } else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }


        CheckBox ringerPermission = (CheckBox)findViewById(R.id.checkbox2);
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

            if(nm==null || !nm.isNotificationPolicyAccessGranted()){
                ringerPermission.setChecked(false);
                Log.d("Values","Entered Checbox");
            }
            else{
                ringerPermission.setChecked(true);
                ringerPermission.setEnabled(false);
            }
        }

    public void onLocationPermissionClicked(View view) {
        Log.d(Tag,"onLocationPermissionClicked");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);
    }



    public void onRingerPermissionClicked(View view){
        Log.d(Tag,"onRingerPermissionClicked");
        Intent intent = null;
            intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }


}
