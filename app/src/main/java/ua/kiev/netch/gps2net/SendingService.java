package ua.kiev.netch.gps2net;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class SendingService extends Service {

    long last_updated = 0;
    long wait_for_alarm = 0;
    long wait_for_update = 0;
    AlarmManager alarmManager = null;
    String action_service_notify;
    String action_alarm;

    SendingService() {
        super();
        action_service_notify = getClass().getPackage().getName() + ".ServiceNotify";
        action_alarm = getClass().getPackage().getName() + ".Alarm";
    }

    static class MyLocationListener implements LocationListener {
        SendingService owner = null;

        MyLocationListener(SendingService n_owner) {
            super();
            owner = n_owner;
        }

        public void onLocationChanged(Location new_location) {
            owner.onLocationChanged(new_location);
        }

        public void onProviderEnabled(String _x) {
            // Re-request location, despite can be already requested.
            owner.requestLocation();
        }

        public void onProviderDisabled(String _x) {
            // Satisfy abstract class, no implementation
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Satisfy abstract class, no implementation
        }
    }

    static class MyReceiver extends BroadcastReceiver {
        SendingService owner = null;
        String action_alarm;

        MyReceiver(SendingService n_owner) {
            super();
            owner = n_owner;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("SendingService", String.format("got something: action=<%s>", intent.getAction()));
            if (intent.getAction().equals(action_alarm)) {
                owner.setWaitForAlarm(0);
                owner.requestLocation();
                owner.scheduleNext();
            } else {
                Log.w("SendingService", String.format("Unknown received intent action: %s", intent.getAction()));
            }
        }
    }

    static class MySender extends AsyncTask<Void, Void, Void> {
        String target_host;
        int target_port;
        String message;

        @Override
        protected Void doInBackground(Void... params_) {
            NetworkUtils.send(target_host, target_port, message);
            return null;
        }
    }

    SharedPreferences preferences = null;
    LocationManager locationManager = null;
    MyLocationListener llistener = null;
    MyReceiver receiver = null;

    @Override
    public void onCreate() {
        Log.i("SendingService", "onCreate");
        llistener = new MyLocationListener(this);
        receiver = new MyReceiver(this);
        receiver.action_alarm = action_alarm;
        IntentFilter filter_from_service = new IntentFilter();
        filter_from_service.addAction(action_alarm);
        registerReceiver(receiver, filter_from_service);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("enabled", false)) {
            stopSelf();
            return;
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocation();
        scheduleNext();
    }

    @Override
    public void onDestroy() {
        Log.i("SendingService", "onDestroy");
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
        // Return the communication channel to the service.
        //- throw new UnsupportedOperationException("Not yet implemented");
    }

    void requestLocation() {
        Log.i("SendingService", "requestLocation");
        // Shall call checkPermission() or catch SecurityException
        boolean havePermission = Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
        if (havePermission) {
                locationManager.requestSingleUpdate(LocationUtils.genLocationCriteria(), llistener, null);
                setWaitForUpdate(System.currentTimeMillis());
        }
    }

    public void onLocationChanged(Location location) {
        Log.i("SendingService", "onLocationChanged");
        // TODO send this location
        // TODO? send the same data to main activity, if present
        //
        long secs = location.getTime() / 1000;
        float accuracy = -1;
        if(location.hasAccuracy()) {
            accuracy = location.getAccuracy();
        }
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        int client_id = Integer.valueOf(preferences.getString("client_id", "0"));
        String msg1 = String.format(Locale.US, "v=1 id=%d time=%d lat=%1.5f lgt=%1.5f acr=%1.5f",
                client_id, secs, latitude, longitude, accuracy);
        MySender async_sender = new MySender();
        async_sender.target_host = preferences.getString("target_host", "");
        async_sender.target_port = Integer.valueOf(preferences.getString("target_port", "0"));
        async_sender.message = msg1;
        async_sender.execute();
        //NetworkUtils.send(
        //        preferences.getString("target_host", ""),
        //        Integer.valueOf(preferences.getString("target_port", "0")),
        //        msg1
        //);
        scheduleNext();
        last_updated = System.currentTimeMillis();
        setWaitForUpdate(0);
    }

    public void scheduleNext() {
        // Request re-update.
        Log.i("SendingService", "scheduleNext");
        long current_time = System.currentTimeMillis();
        if (wait_for_alarm <= current_time) {
            // FIXME: documentation for AlarmManager recommends using Handler
            //Intent intent = new Intent(this, receiver.getClass());
            //intent.setAction(action_alarm);
            Intent intent = new Intent(action_alarm);
            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(this, 0, intent, 0);
            long current_uptime = SystemClock.elapsedRealtime();
            int send_interval;
            String send_interval_str = preferences.getString("send_interval", "");
            try {
                send_interval = Integer.valueOf(send_interval_str, 10);
            } catch (NumberFormatException e) {
                send_interval = 300;
            }
            long period_millis = 1000 * send_interval;
            Log.i("SendingService", String.format("scheduleNext: current_uptime=%d send_interval=%d", current_uptime, send_interval));
            // TODO migrate to setInexactRepeating()
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    current_uptime + period_millis,
                    pendingIntent);
            setWaitForAlarm(current_time + period_millis);
        }
    }

    void setWaitForAlarm(long new_value) {
        Log.i("SendingService", String.format("setWaitForAlarm (%s)", (new_value > 0) ? "real" : "0"));
        wait_for_alarm = new_value;
        notifyActivity("wait_for_alarm", new_value);
    }

    void setWaitForUpdate(long new_value) {
        Log.i("SendingService", String.format("setWaitForUpdate(%s)", (new_value > 0) ? "real" : "0"));
        wait_for_update = new_value;
        notifyActivity("wait_for_update", new_value);
    }

    void notifyActivity(String key, long value) {
        //Intent intent = new Intent(this, MainActivity.class);
        //intent.setAction(action_service_notify);
        Intent intent = new Intent(action_service_notify);
        //-intent.setClass(this, MainActivity.MyReceiver.class);
        intent.putExtra("key", key);
        intent.putExtra("value", value);
        sendBroadcast(intent);
    }

}
