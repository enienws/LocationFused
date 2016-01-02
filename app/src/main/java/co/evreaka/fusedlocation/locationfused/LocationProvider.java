package co.evreaka.fusedlocation.locationfused;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

/**
 * Created by engin on 21.11.2015.
 */
public class LocationProvider
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    private static final String LOG_TAG = "LocationFused.LocationProvider";
    private Context applicationContext;

    private AtomicBoolean resuming2DropBox;

    private GoogleApiClient apiClient;
    private LocationRequest locationRequest;
    private Handler uiThreadMessageHandler;
    private LinkedList<EvrekaLocation> historicalLocations;

    private final String APP_KEY = "jcwnjiizztgo46b";
    private final String APP_SECRET = "tvewbez79yaug4o";

    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;

    LocationProvider(Context applicationContext)
    {
        this.applicationContext = applicationContext;

        resuming2DropBox = new AtomicBoolean(false);
        //Initialize List for historical locations
        historicalLocations = new LinkedList<>();

        //Create the api client
        apiClient = new GoogleApiClient.Builder(applicationContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //Create the location request to get periodic location updates from the device
        CreateLocationRequest();
    }

    public void ConnectToLocationService()
    {
        //Connect to the api client
        apiClient.connect();
    }

    public void EnableLocationUpdates()
    {
        //Request location updates from Location Services API
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
    }

    public void DisableLocationUpdates()
    {
        //Remove location updates from Location Services API
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
    }

    public void SetUIessageHandler(Handler uiHandler)
    {
        this.uiThreadMessageHandler = uiHandler;
    }

    public void WriteLocationsToFile() {
        Gson gson = new GsonBuilder().registerTypeAdapter(EvrekaLocation.class, new EvrekaLocationSerializer()).create();

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String fileName = Long.toString(System.currentTimeMillis()) + ".json";
//        String fileName = "deneme.json";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        OutputStreamWriter outputStreamWriter;
        FileOutputStream fileStream;

        try {
            fileStream = new FileOutputStream(file);
            outputStreamWriter = new OutputStreamWriter(fileStream);
            String jsonStr = gson.toJson(historicalLocations);
            //gson.toJson(outputStreamWriter);
            outputStreamWriter.write(jsonStr, 0, jsonStr.length());
            outputStreamWriter.flush();
            outputStreamWriter.close();

        } catch (FileNotFoundException e) {
            Log.d(LOG_TAG, fileName + " cannot be found.");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(LOG_TAG, "IO exception is thrown.");
            e.printStackTrace();
        }
    }

    public void WriteLocationsToDropBox(Activity requestActivity)
    {
        //Create dropbox
        // And later in some initialization function:
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        mDBApi.getSession().startOAuth2Authentication(requestActivity);
        resuming2DropBox.set(true);
    }

    public void ResumetWriteLocationsToDropBox()
    {
        if(!resuming2DropBox.get())
            return;

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }

        new Runnable() {
            @Override
            public void run() {
                //Create the json structure
                Gson gson = new GsonBuilder().registerTypeAdapter(EvrekaLocation.class, new EvrekaLocationSerializer()).create();
                String jsonStr = gson.toJson(historicalLocations);
                byte[] byteArr = jsonStr.getBytes();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArr);

                //Create file name
                String fileName = "tablet" + Long.toString(System.currentTimeMillis()) + ".json";

                //Upload the file
                DropboxAPI.Entry response = null;
                try
                {
                    response = mDBApi.putFile("/" + fileName, inputStream,
                            byteArr.length, null, null);
                }
                catch (DropboxException e)
                {
                    e.printStackTrace();
                }
                Log.i("DbExampleLog", "The uploaded file's rev is: " + response.rev);
                resuming2DropBox.set(false);
            }
        }.run();





//        new AlertDialog.Builder(applicationContext)
//                .setTitle("Dropbox Upload")
//                .setMessage("Dropbox upload is successfull. File name is: " + fileName)
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        // continue with delete
//                    }
//                })
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show();

    }


    private void CreateLocationRequest()
    {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(LOG_TAG, "Connected to Services API.");

        //Enable Location Updates
        EnableLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.d(LOG_TAG, "Connection suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.d(LOG_TAG, "Connection failed.");
    }

    //Extended from Location Listener
    @Override
    public void onLocationChanged(Location location)
    {
        Log.d(LOG_TAG, "On Location changed.");

        //Create Evreka version of Location class
        EvrekaLocation evrekaLocation = new EvrekaLocation(location);

        //Add this location to historical locations list
        historicalLocations.add(evrekaLocation);

        //Update the TextView so that we can see the location message coming from android
        uiThreadMessageHandler.obtainMessage(MainActivity.UIMessageHandler.TEXT_VIEW, evrekaLocation).sendToTarget();
    }
}
