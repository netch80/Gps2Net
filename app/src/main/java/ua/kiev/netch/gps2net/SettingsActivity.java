package ua.kiev.netch.gps2net;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.TwoStatePreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Log.i("SettingsActivity", String.format("__: making summary: preference class: %s", preference.getClass().getName()));

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof TwoStatePreference) {
                if (stringValue.equals("")) {
                    stringValue = "false";
                }
                if (!stringValue.equals("false")) {
                    stringValue = "true";
                }
                preference.setSummary(stringValue);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        final SharedPreferences defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(preference.getContext());
        final String key = preference.getKey();
        // Netch: HACK: it cannot determine extraction type automatically
        if (key.equals("enabled")) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    defaultSharedPreferences.getBoolean(key, false));

        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    defaultSharedPreferences.getString(key, ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        //-Log.i("SettingsActivity", String.format("DefaultPreferences name: %s", PreferenceManager.getDefaultSharedPreferencesName(this)));
        addPreferencesFromResource(R.xml.pref_settings);
        //-setHasOptionsMenu(false);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("enabled"));
        bindPreferenceSummaryToValue(findPreference("target_host"));
        bindPreferenceSummaryToValue(findPreference("target_port"));
        bindPreferenceSummaryToValue(findPreference("send_interval"));
        bindPreferenceSummaryToValue(findPreference("client_id"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("SettingsActivity", "stopped");
    }
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This means the button "back" is pressed on screen
            if (!super.onMenuItemSelected(featureId, item)) {
                Log.i("SettingsActivity", "navigating up");
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return false;
    }
}
