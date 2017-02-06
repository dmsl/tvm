/*
 * Copyright (c) 2013, Data Management Systems Lab (DMSL), University of Cyprus.
 *
 * Author: P. Mpeis pmpeis01@cs.ucy.ac.cy (University of Cyprus)
 *
 * Project supervisors: A. Konstantinides, D. Zeinalipour-Yazti (University of Cyprus)
 *
 * This file is part of TVM.
 * TVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cy.ac.ucy.cs.tvm;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.MenuItem;

public class Preferences extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = PreferenceActivity.class.getSimpleName();
    int toggleCnt = 0;
    boolean toggleDone = false;
    private App app;

    /** Build preference menu when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        app = (App) getApplication();

        getPreferenceManager().setSharedPreferencesName(getString(R.string.preferences));

        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /** Clear RAM Cache */
    private void showClearRamCacheDialog() {

        Builder builder = new Builder(this);
        builder.setTitle("Clear parsed radiomaps");

        builder.setMessage(
                        "Are you sure you want to delete all parsed radiomaps?\nNOTE: Application will reload!")
                .setCancelable(true)
                .setPositiveButton(
                        "Yes",
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                app.showToast("Restarting to take effect..");
                                Log.i(TAG, "Cleared RAM cache");

                                app.rmapCache.clearRamCache();
                                app.restartApp();
                            }
                        })
                .setNegativeButton(
                        "No",
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showClearSDCacheDialog() {

        Builder builder = new Builder(this);
        builder.setTitle("Clear stored radiomaps");

        builder.setMessage(
                        "Are you sure you want to delete all radiomaps from your device's storage?")
                .setCancelable(true)
                .setPositiveButton(
                        "Yes",
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                app.rmapCache.clearSDCache();
                            }
                        })
                .setNegativeButton(
                        "No",
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, final Preference preference) {

        try {
            // Clear SD Cache
            if (preference.getKey().equals(getString(R.string.pref_key_clear_sd_card_cache))) {

                showClearSDCacheDialog();

            } else if (preference.getKey().equals(getString(R.string.pref_key_k_anonymity))) {

                App.showKAnonymityPickerDialog(this, App.MINIMUM_ANONYMITY, App.MAXIMUM_ANONYMITY);

            } else if (preference.getKey().equals(getString(R.string.pref_key_clear_ram_cache))) {

                showClearRamCacheDialog();

            } else if (preference
                    .getKey()
                    .equals(getString(R.string.pref_key_developer_mode_toggle))) {
                ++toggleCnt;

                if (toggleCnt == 1) {
                    app.showToast("Are you a developer?");
                }
                if (toggleCnt > 4 && !toggleDone) {

                    app.showToast("You are now a developer!");
                    // Enable developer preference
                    Preference devPref =
                            findPreference(getString(R.string.pref_key_developer_mode));
                    devPref.setEnabled(true);
                    Preference realSim =
                            findPreference(getString(R.string.pref_key_real_simulation));
                    realSim.setEnabled(true);
                    toggleDone = true;
                }
            }
        } catch (NullPointerException e) {
            // do noth:
            // some preferences dont have key (they are categories)
        }
        return false;
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaColumns.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    /** Sets up a listener when the preference activity is resumed. */
    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    /** Unregisters the listener before the preference activity is destroyed. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the listener whenever a key changes
        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /** Unregisters the listener when a preference activity is paused. */
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /*
     * (non-Javadoc)
     *
     * Do extra modifications when preferences change
     *
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#
     * onSharedPreferenceChanged(android.content.SharedPreferences,
     * java.lang.String)
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preference, String preferenceKey) {

        // Change SD Card caching status
        if (preferenceKey.equals(getString(R.string.pref_key_sd_card_cache_status))) {

            app.cacheSDcard = preference.getBoolean(preferenceKey, false);
            Log.i(TAG, "SC card cache: " + app.cacheSDcard);

        }
        // Change RAM cache status
        else if (preferenceKey.equals(getString(R.string.pref_key_ram_cache_status))) {

            app.cacheRam = preference.getBoolean(preferenceKey, false);
            Log.i(TAG, "RAM cache: " + app.cacheRam);

        }
        // Change localization algorithm
        else if (preferenceKey.endsWith(getString(R.string.pref_key_localization_algorithm))) {
            String algo =
                    preference.getString(getString(R.string.pref_key_localization_algorithm), "");
            app.setLocalizationAlgorithm(algo);
            Log.i(TAG, "Loc algo: " + app.localizationAlgorithm.toString());

        }

        // Change anonymity algorithm
        else if (preferenceKey.endsWith(getString(R.string.pref_key_anonymity_algorithm))) {
            String algo =
                    preference.getString(getString(R.string.pref_key_anonymity_algorithm), "");
            app.setAnonymityAlgorithm(algo);
            app.firstBloomDone = false;
            Log.i(TAG, "Anon algo: " + app.anonymityAlgorithm.toString());

        }
        // Developer mode
        else if (preferenceKey.equals(getString(R.string.pref_key_developer_mode))) {

            app.developerMode = preference.getBoolean(preferenceKey, false);
            Log.i(TAG, "Dev mode: " + app.developerMode);

        }

        // Real simulation mode
        else if (preferenceKey.equals(getString(R.string.pref_key_real_simulation))) {

            app.realSimulation = preference.getBoolean(preferenceKey, false);
            Log.i(TAG, "Real simulation: " + app.realSimulation);

            if (app.realSimulation) {
                app.menuItemTrackMe.setEnabled(false);
                app.menuItemFindMe.setEnabled(false);
            } else {
                app.menuItemTrackMe.setEnabled(true);
                app.menuItemFindMe.setEnabled(true);
            }

        }
        // Draw routes
        else if (preferenceKey.equals(getString(R.string.pref_key_draw_routes))) {

            app.drawRoutes = preference.getBoolean(preferenceKey, true);
            Log.i(TAG, "Draw routes: " + app.drawRoutes);

        }
        // Always scan mode
        else if (preferenceKey.equals(getString(R.string.pref_key_always_scan))) {

            app.alwaysScan = preference.getBoolean(preferenceKey, false);

            if (!app.isTrackmeEnabled()) {
                app.stopLocalizationService();
            }
            Log.i(TAG, "Always scan: " + app.alwaysScan);

        }
        // K parameter
        else if (preferenceKey.equals(getString(R.string.pref_key_k_parameter))) {

            app.kParameter = Integer.parseInt(preference.getString(preferenceKey, "0"));
            Log.i(TAG, "K parameter: " + app.kParameter);

        }

        // Samples interval
        else if (preferenceKey.equals(getString(R.string.pref_key_samples_interval))) {

            app.wifiScanInterval = Integer.parseInt(preference.getString(preferenceKey, "1000"));
            Log.i(TAG, "Samples Interval: " + app.wifiScanInterval);

        }
        // Ram cache slots
        else if (preferenceKey.equals(getString(R.string.pref_key_ram_cache_slots))) {

            app.cacheRamSlots = Integer.parseInt(preference.getString(preferenceKey, "0"));
            Log.i(TAG, "Ram slots: " + app.cacheRamSlots);

            // Ram cleared
            app.rmapCache.clearRamCache();
            app.showToast("Ram cache cleared");
            Log.i(TAG, "Cleared RAM cache");

        }
        // Datasets
        else if (preferenceKey.equals(getString(R.string.pref_key_datasets))) {

            app.datasetInUse = Integer.parseInt(preference.getString(preferenceKey, "0"));

            Log.i(TAG, "Dataset in use: " + app.cacheRamSlots);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
