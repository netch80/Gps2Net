package ua.kiev.netch.gps2net;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    SharedPreferences shared_preferences = null;
    SharedPreferences.OnSharedPreferenceChangeListener sp_change_listener = null;
    Timer timer_request = null;
    float longitude = Float.NaN;
    float latitude = Float.NaN;
    LocationManager locationManager = null;
    MyLocationListener llistener = null;
    MyReceiver receiver = null;
    long last_updated = 0;
    long wait_for_update = 0;
    String action_service_notify;

    // FIXME: seems the same as in SendingService; think on merging
    static class MyLocationListener implements LocationListener {
        MainActivity owner = null;

        MyLocationListener(MainActivity n_owner) {
            super();
            owner = n_owner;
        }

        public void onLocationChanged(Location new_location) {
            owner.onLocationChanged(new_location);
        }

        public void onProviderEnabled(String _x) {
            // Re-request location
            owner.requestLocation();
        }

        public void onProviderDisabled(String _x) {
            // Satisfy abstract class, no implementation
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Satisfy abstract class, no implementation
        }
    }

    // Listener for messages from service, etc.
    static class MyReceiver extends BroadcastReceiver {
        MainActivity owner = null;
        String sn_action;

        MyReceiver(MainActivity n_owner) {
            super();
            owner = n_owner;
            sn_action = getClass().getPackage().getName() + ".ServiceNotify";
        }

        @Override
        public void onReceive (Context context, Intent intent) {
            Log.i("MainActivity", String.format("Receiver got something: action=<%s>", intent.getAction()));
            if (intent.getAction().equals(sn_action)) {
                String key = intent.getStringExtra("key");
                if (key != null) {
                    owner.processServiceNotify(intent, key);
                }
            }
        }
    }

    MainActivity() {
        super();
        llistener = new MyLocationListener(this);
        receiver = new MyReceiver(this);
        // For messages from sending service
        action_service_notify = getClass().getPackage().getName() + ".ServiceNotify";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        // fab.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View view) {
        //     Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //             .setAction("Action", null).show();
        // }
        // });

        // See https://developer.android.com/reference/android/content/SharedPreferences.html
        shared_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final MainActivity self = this;
        sp_change_listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            MainActivity main_activity = self;

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                // your stuff here
                Log.i("MainActivity", String.format("preference changed, key=%s", key));
                if (key.equals("enabled")) {
                    self.onChangeEnabledSetting();
                }
            }
        };
        shared_preferences.registerOnSharedPreferenceChangeListener(sp_change_listener);
        // Main activity always subscribes for location, in spite of enabling for service
        // TODO subscribe and show
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        TextView v_SendingEnabled = (TextView) findViewById(R.id.v_SendingEnabled);
        v_SendingEnabled.setText(Boolean.toString(shared_preferences.getBoolean("enabled", false)));
    }

    @Override
    protected void onDestroy() {
        shared_preferences.unregisterOnSharedPreferenceChangeListener(sp_change_listener);
        sp_change_listener = null;
        shared_preferences = null;
        super.onDestroy();
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
            Log.i("info", "Settings button pressed");
            callSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        Log.i("MainActivity", "onStart");
        super.onStart();
        timer_request = new Timer();
        final MainActivity self = this;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO
                        self.requestLocation();
                    }
                });
            }

        };
        timer_request.schedule(task, 0, 8000);
        IntentFilter filter_from_service = new IntentFilter();
        filter_from_service.addAction(action_service_notify);
        registerReceiver(receiver, filter_from_service);
        onChangeEnabledSetting();
    }

    @Override
    public void onStop() {
        Log.i("MainActivity", "onStop");
        timer_request.cancel();
        unregisterReceiver(receiver);
        super.onStop();
    }
    // @Override
    // public void onClick() {
    // to onClick():
    // case R.id.btnActTwo:
    // Intent intent = new Intent(this, SettingsActivity.class);
    // startActivity(intent);
    // break;
    // }

    public void callSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    void onChangeEnabledSetting() {
        final boolean enabled = shared_preferences.getBoolean("enabled", false);
        if (enabled) {
            Log.i("MainActivity", "starting my service");
            startService(new Intent(this, SendingService.class));
        } else {
            Log.i("MainActivity", "stopping my service");
            stopService(new Intent(this, SendingService.class));
        }
        final TextView v_SendingEnabled = (TextView) findViewById(R.id.v_SendingEnabled);
        v_SendingEnabled.setText(Boolean.toString(enabled));
        v_SendingEnabled.invalidate();
    }

    void requestLocation() {
        Log.i("MainActivity", "requestLocation()");
        long now = System.currentTimeMillis();
        if (now > wait_for_update) {
            // For API >= 23, we shall check real permission
            // Shall call checkPermission() or catch SecurityException
            boolean havePermission = Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
            if (!havePermission) {
                Log.i("MainActivity", "Requesting location permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
            if (havePermission) {
                locationManager.requestSingleUpdate(LocationUtils.genLocationCriteria(), llistener, null);
                wait_for_update = now + 30000; // 30 seconds
                drawTimestampAtElement(wait_for_update, R.id.v_PendingUpdate);
            } else {
                Log.i("MainActivity", "requestLocation: no permission");
                // FIXME: generate permission request
                Toast.makeText(this, "No location permission", Toast.LENGTH_LONG);
            }
        }
    }

    void onLocationChanged(Location new_location) {
        Log.i("MainActivity", "onLocationChanged");
        longitude = (float) new_location.getLongitude();
        final TextView view_longitude = (TextView) findViewById(R.id.v_Longitude);
        view_longitude.setText(Float.toString(longitude));
        view_longitude.invalidate();
        latitude = (float) new_location.getLatitude();
        final TextView view_latitude = (TextView) findViewById(R.id.v_Latitude);
        view_latitude.setText(Float.toString(latitude));
        view_latitude.invalidate();
        // Show last update timestamp
        last_updated = System.currentTimeMillis();
        drawTimestampAtElement(last_updated, R.id.v_LastUpdated);
        // Show we don't have pending update
        wait_for_update = 0;
        final TextView view_pendingUpdate = (TextView) findViewById(R.id.v_PendingUpdate);
        view_pendingUpdate.setText("-");
        view_pendingUpdate.invalidate();
    }

    void processServiceNotify(Intent intent, String key) {
        Log.i("MainActivity", String.format("processServiceNotify: key=%s", key));
        if (key.equals("wait_for_alarm")) {
            long value = intent.getLongExtra("value", 0);
            drawTimestampAtElement(value, R.id.v_ServiceWaitForAlarm);
        }
        if (key.equals("wait_for_update")) {
            long value = intent.getLongExtra("value", 0);
            drawTimestampAtElement(value, R.id.v_ServiceWaitForUpdate);
        }
    }

    void drawTimestampAtElement(long timestamp, int element_id) {
        final TextView element = (TextView) findViewById(element_id);
        if (timestamp > 0) {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            element.setText(df.format(new Date(timestamp)));
        } else {
            element.setText("-");
        }
    }
}