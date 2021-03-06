package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class Geofencing implements ResultCallback {

    private static final float GEOFENCE_RADIUS =50 ;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private static final long GEOFENCE_TIMEOUT=86400000;

    public Geofencing(Context context,GoogleApiClient googleApiClient){
        this.mContext=context;
        this.mGoogleApiClient=googleApiClient;
        mGeofenceList=new ArrayList<>();
        mGeofencePendingIntent=null;
    }

    public void registerAllGeofences(){
        if(mGoogleApiClient==null || !mGoogleApiClient.isConnected() || mGeofenceList==null || mGeofenceList.size()==0)
        {
            return;
        }
        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient,getGeofencingRequest(),getmGeofencePendingIntent()).setResultCallback(this);
        }
        catch (SecurityException securityException){
            Log.d("Values","GeofencingClass:Failed registerAllGeofences");
        }
    }

    public void unregisterAllGeoences(){
        if(mGoogleApiClient==null||!mGoogleApiClient.isConnected())
            return;
        try{
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient,getmGeofencePendingIntent()).setResultCallback(this);
        }catch(SecurityException securityexception){
            Log.d("Values","Geofencing:"+securityexception.getMessage());
        }
    }

    public void updateGeofences(PlaceBuffer places){
        mGeofenceList=new ArrayList<>();
        if(places==null || places.getCount()==0)
            return;

        for(Place place:places){

            String placeUID = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLong = place.getLatLng().longitude;

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat,placeLong,GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT).build();

            mGeofenceList.add(geofence);
        }

        }


 //if device is already in the geofences we are about to register
        private GeofencingRequest getGeofencingRequest() {

            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
            builder.addGeofences(mGeofenceList);

            return builder.build();
        }


       private PendingIntent getmGeofencePendingIntent()
        {
            if (mGeofencePendingIntent != null){
                return mGeofencePendingIntent;
               }
            Intent intent = new Intent(mContext,GeofenceBroadcastReciever.class);
            mGeofencePendingIntent=PendingIntent.getBroadcast(mContext,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            return mGeofencePendingIntent;
        }

    @Override
    public void onResult(@NonNull Result result) {
        Log.d("Values","Geofencing class:onResult Callback"+result.getStatus().toString());
    }
}
