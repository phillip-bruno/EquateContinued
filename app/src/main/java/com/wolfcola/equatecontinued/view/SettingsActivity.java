package com.wolfcola.equatecontinued.view;


import static com.wolfcola.equatecontinued.ResourceArrayParser.getUnitTypeKeyArray;
import static com.wolfcola.equatecontinued.ResourceArrayParser.getUnitTypeNameArray;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.wolfcola.equatecontinued.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings screen using AndroidX Preference.
 */
public class SettingsActivity extends AppCompatActivity {
    public final static String UNIT_TYPE_PREF_KEY = "unit_type_prefs";

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            (preference, value) -> {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);
                } else {
                    preference.setSummary(stringValue);
                }
                return true;
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
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PrefsFragment()).commit();
        setupActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish(); //closes settings activity
            return true;
        }
        return super.onOptionsItemSelected(item);
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


    public static class PrefsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            setUpUnitTypePrefs();
        }

        /**
         * Helper Class to setup the default Unit Type preference list in code
         */
        private void setUpUnitTypePrefs() {
            PreferenceScreen screen = getPreferenceScreen();
            MultiSelectListPreference listPref = new MultiSelectListPreference(requireContext());
            listPref.setOrder(0);
            listPref.setDialogTitle(R.string.unit_select_title);
            listPref.setKey(UNIT_TYPE_PREF_KEY);
            listPref.setSummary(R.string.unit_select_summary);
            listPref.setTitle(R.string.unit_select_title);
            listPref.setEntries(getUnitTypeNameArray(getResources()));

            String[] keyArray = getUnitTypeKeyArray(getResources());
            listPref.setEntryValues(keyArray);

            final Set<String> result = new HashSet<>();
            Collections.addAll(result, keyArray);

            listPref.setDefaultValue(result);

            screen.addPreference(listPref);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();

            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
