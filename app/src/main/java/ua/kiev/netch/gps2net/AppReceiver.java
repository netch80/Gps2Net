package ua.kiev.netch.gps2net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //-throw new UnsupportedOperationException("Not yet implemented");
        String action = intent.getAction();
        // FIXME: some docs say: <action android:name="android.intent.action.QUICKBOOT_POWERON" />
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO here: get preferences, check them; if enabled, start service
            Log.i("boot_received", "got BOOT_COMPLETED");
            SharedPreferences shared_preferences;
            shared_preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (shared_preferences.getBoolean("enabled", false)) {
                Log.i("AppReceiver", "shall start my service");
                context.startService(new Intent(context, SendingService.class));
            }
        }
    }
}
