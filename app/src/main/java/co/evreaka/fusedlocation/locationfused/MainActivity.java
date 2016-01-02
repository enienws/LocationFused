package co.evreaka.fusedlocation.locationfused;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {
    private static final String LOG_TAG = "LocationFused.MainActivity";
    private LocationProvider locationProvider;
    private Handler uiThreadMessageHandler;

    private final int APPLICATION_FINE_LOCATION_PERMISSION = 1;
    private final int APPLICATION_WRITE_EXTERNAL_PERMISSON = 2;

    //GUI related elements
    private Button writeDataToFilePB;
    private TextView locationViewTV;
    private TextView access2LocationLE;

    private boolean GPSUsageGranted = false;
    private boolean WriteExternalGranted = false;

    public class UIMessageHandler implements Handler.Callback
    {
        public static final int TEXT_VIEW = 0;
        @Override
        public boolean handleMessage(Message msg)
        {
            if(msg.what == TEXT_VIEW)
            {
                Location location = (Location)msg.obj;
                locationViewTV.append(location.toString() + "\n");
                //Auto scroll related code block.
                final Layout layout = locationViewTV.getLayout();
                if(layout != null)
                {
                    int scrollDelta = layout.getLineBottom(locationViewTV.getLineCount() - 1)
                            - locationViewTV.getScrollY() - locationViewTV.getHeight();
                    if(scrollDelta > 0)
                        locationViewTV.scrollBy(0, scrollDelta);
                }
                //End of auto scroll

            }
            return  true;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "On create method is called.");

        //Create the uiMessageHandler
        uiThreadMessageHandler = new Handler(new UIMessageHandler());

        //Get the GUI elements
        GetGUIElements();

        //Handle Location App Permissions
        HandleAppPermission();

        //Instantiate LocationProvider class
        locationProvider = new LocationProvider(getApplicationContext());
        locationProvider.SetUIessageHandler(uiThreadMessageHandler);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        Log.d(LOG_TAG, "On start method is called.");

        //Let location provider to connect location services
        locationProvider.ConnectToLocationService();

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.d(LOG_TAG, "On resume method is called.");

//        //Enable getting location updates from location provider
//        locationProvider.EnableLocationUpdates();

        locationProvider.ResumetWriteLocationsToDropBox();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        Log.d(LOG_TAG, "On pause method is called.");

//        //Disable getting location updates from location provider
//        locationProvider.DisableLocationUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case APPLICATION_FINE_LOCATION_PERMISSION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    access2LocationLE.setText("Granted.");
                    GPSUsageGranted = true;
                }
                else
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    access2LocationLE.setText("Denied.");
                    GPSUsageGranted = false;
                }
            }
            break;
            case APPLICATION_WRITE_EXTERNAL_PERMISSON:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    WriteExternalGranted = true;
                }
                else
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    WriteExternalGranted = false;
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void HandleAppPermission()
    {
        //Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    APPLICATION_FINE_LOCATION_PERMISSION);
        }
        else
        {
            //Permission is granted.
            //We do not need any extra action to take permission
            access2LocationLE.setText("Granted.");
        }

        //Check for write permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    APPLICATION_WRITE_EXTERNAL_PERMISSON);
        }
    }

    private void GetGUIElements()
    {
        writeDataToFilePB = (Button)findViewById(R.id.writeDataToFilePB);

        locationViewTV = (TextView)findViewById(R.id.locationViewTV);
        locationViewTV.setMovementMethod(new ScrollingMovementMethod());
        locationViewTV.setText("");

        access2LocationLE = (TextView)findViewById(R.id.access2LocationLE);
        access2LocationLE.setText("");
    }


    //Event handlers
    public void WriteDataToFilePBClicked(View view)
    {
        locationProvider.WriteLocationsToFile();
    }

    public void WriteData2Dropbox(View view)
    {
        locationProvider.WriteLocationsToDropBox(this);
    }
}
