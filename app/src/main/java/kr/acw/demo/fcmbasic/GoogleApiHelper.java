package kr.acw.demo.fcmbasic;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by hmj on 2019-02-25.
 *
 * @since 0.1
 */
public class GoogleApiHelper implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener/*,
        LocationListener*/ {

    private static String TAG = GoogleApiClient.class.getSimpleName();
    private Context context;
    private GoogleApiClient googleApiClient;
    private ConnectionListener connectionListener;
    private Bundle connectionBundle;
//    private LocationRequest locationRequest;
//    private Location _location;


    public GoogleApiHelper(Context context) {
        this.context = context;
        buildGoogleApiClient();
        connect();
    }
/*
    public Location getCurrentLocation() {
        return _location;
    }
*/
    public GoogleApiClient getGoogleApiClient() {
        return this.googleApiClient;
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
        if (this.connectionListener != null && isConnected()) {
            connectionListener.onConnected(connectionBundle);
        }
    }

    public void connect() {
        Log.d(TAG, "connect()");
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    public void disconnect() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    public boolean isConnected() {
        Log.i(TAG, "isConnected() ? null=" + googleApiClient + " connected=" + googleApiClient.isConnected());
        return googleApiClient != null && googleApiClient.isConnected();
    }

    private void buildGoogleApiClient() {
//        locationRequest = LocationRequest.create();
//        locationRequest.setInterval(10 * 60 * 1000);
//        locationRequest.setFastestInterval(5000);
//        locationRequest.setMaxWaitTime(60 * 60 * 1000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connectionBundle = bundle;
        if (connectionListener != null) {
            connectionListener.onConnected(bundle);
        }

        /*
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        */
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() : googleApiClient.connect()");
        googleApiClient.connect();
        if (connectionListener != null) {
            connectionListener.onConnectionSuspended(i);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() : connectionResult = " + connectionResult);
        if (connectionListener != null) {
            connectionListener.onConnectionFailed(connectionResult);
        }
    }
    /*
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;

        this._location = location;
    }
    */
    public interface ConnectionListener {
        void onConnectionFailed(@NonNull ConnectionResult connectionResult);
        void onConnectionSuspended(int i);
        void onConnected(Bundle bundle);
    }
}
