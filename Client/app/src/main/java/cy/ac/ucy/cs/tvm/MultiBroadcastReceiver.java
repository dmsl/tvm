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

import com.google.android.gms.maps.model.LatLng;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cy.ac.ucy.cs.tvm.tvm.CoarseLocation;
import cy.ac.ucy.cs.tvm.tvm.LocalizationService;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;
import cy.ac.ucy.cs.tvm.tvm.simulation.TestTVM;
import cy.ac.ucy.cs.tvm.tvm.simulation.WalkSimulatorClassGenerator;

import static cy.ac.ucy.cs.tvm.map.MapUtilities.centerMapInCoarseLocation;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.updateMap;

/**
 * This BroadcastReceiver defines the asynchronous flow within the application. All background tasks
 * are passing through this receiver:
 *
 * <ul>
 *   <li>when the os has ready the "Wifi Scan" results
 *   <li>when the app requests to get a partial radiomap
 *   <li>when the radiomap was received and needs to be parsed
 *   <li>when experiments or walking simulation is running
 * </ul>
 */
public class MultiBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = MultiBroadcastReceiver.class.getSimpleName();
    App app;

    @Override
    public void onReceive(Context context, Intent intent) {
        app = (App) context.getApplicationContext();

        if (app.isInBgProgress()) return;

        String action = intent.getAction();
        if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            processWifiResults();
        } else if (action.equals(App.BROADCAST_RECEIVER_GET_PARTIAL_RMAP)) {
            getPartialRadiomap(context);
        } else if (action.equals(App.BROADCAST_PARSE_RMAP_AND_FIND_LOCATION)) {
            parseRmapAndlocalizeUser(context);
        } else if (action.equals(App.BROADCAST_EXPERIMENT_LOCALISE_INSTANCE)) {
            Log.e(TAG, "onReceive: continue experiment");
            if (app.anonymityAlgorithm.equals(App.AnonymityAlgorithm.GMAPS)) {
                centerMapInCoarseLocation(app);
            } else {
                TestTVM.continueExperiment();
            }
        }
    }

    /** Parse radiomap (if not already parsed) and localize user */
    private void parseRmapAndlocalizeUser(Context context) {
        app = (App) context.getApplicationContext();

        // Parse radiomap, and then localize
        AsyncTaskParseRmap asyncTaskParseRmap = new AsyncTaskParseRmap(app);
        asyncTaskParseRmap.execute();
    }

    /** Gets partial readiomap 1 try: RAM cache 2 try: SD Card cache try: download from server */
    private void getPartialRadiomap(Context context) {
        if (app.currentScanListIsEmpty()) {
            app.textViewMessage1.setText("No Access Point Found");
            Log.i(TAG, "No Access Point Found");
            handleRadiomapGetError();
            return;
        } else {

            // Find strongest listening MACS to 3 to enable smart buffer
            ArrayList<String> strongestMacs = getStrongestMacs(app.currentScanListGet());

            app.rmapCache.fillCacheData(strongestMacs);

            // Save previous MAC address
            app.previousMac = app.cacheData.currentMac;

            // If full radiomap will be downloaded
            if (app.anonymityAlgorithm.equals(App.AnonymityAlgorithm.FRMAP)) {
                //strongest mac is the all!
                app.cacheData.currentMac = App.ALL_RADIOMAP;
            } else if (app.anonymityAlgorithm.equals(App.AnonymityAlgorithm.GMAPS)) {
                //Continue execution form this point
                app.textViewMessage1.setText("Google Maps (no privacy)");

                getGooglesCoarseLocation(app);
                return;
            }

            // Update strongest used mac
            app.textViewMessage2.setText("SMac: " + app.cacheData.currentMac);

            Log.i(TAG, "SMAC chosen: " + app.cacheData.currentMac);
            Log.i(TAG, "Cache? " + app.cacheData.type.toString());

            app.textViewMessage2.setText("Cache: " + app.cacheData.type.toString());

            switch (app.cacheData.type) {
                case RAM:
                case SD:
                    //Data fetched are zero (cache is used!)
                    if (app.runningExperiment) {
                        //save messages and download time
                        TestTVM.currentTime.addMessagesNumber(0);
                    }

                    // Send broadcast to localize user
                    Intent intent = new Intent();
                    intent.setAction(App.BROADCAST_PARSE_RMAP_AND_FIND_LOCATION);
                    context.sendBroadcast(intent);
                    break;

                case MISS:
                    // Missed or caches disabled
                    // Get radiomap form server

                    // Save exiting RSS values (which are the previous of
                    // current)
                    app.exitedScanListAddAll(app.previousScanListGet());
                    app.enteredScanListAddAll(app.currentScanListGet());

                    // Save previous location
                    app.previousCoordinates = app.currentCoordinates;
                    app.locationHistory.add(app.previousCoordinates);

                    // Download and cache file to Download RAM and SD Card
                    app.rmapCache.cacheRadiomapFile(app.cacheData);
                    break;

                default:
                    break;
            }
        }
    }

    private void getGooglesCoarseLocation(final App app) {
        //Try to get coarse location from Google's Location Services using WiFi
        CoarseLocation.LocationResult locationResult =
                new CoarseLocation.LocationResult() {

                    @Override
                    public void gotLocation(Location location) {

                        try {
                            // Zoom map to the location
                            double gotLat = location.getLatitude();
                            double gotLon = location.getLongitude();

                            app.currentCoordinates = new LatLng(gotLat, gotLon);

                            MainActivity.updateDeveloperInfoLabels();
                            updateMap(app);

                            Intent intent = new Intent();
                            intent.setAction(App.BROADCAST_EXPERIMENT_LOCALISE_INSTANCE);
                            app.sendBroadcast(intent);

                        } catch (NullPointerException e) {
                            //failed to get coarse location
                        }
                    }
                };

        CoarseLocation myLocation = new CoarseLocation();
        myLocation.getLocation(app.getApplicationContext(), locationResult);
    }

    private void handleRadiomapGetError() {
        App.menuItemTrackMe.setChecked(false);
        App.menuItemTrackMe.setIcon(MainActivity.MENU_TRACKME_OFF);
    }

    /** Returns the strongest macs MAX value used is: 5 */
    private ArrayList<String> getStrongestMacs(ArrayList<LogRecord> latestScanList) {

        // Copy the latest scan in a new arraylist
        ArrayList<LogRecord> ls2 = new ArrayList<LogRecord>();
        ls2.addAll(latestScanList);
        ArrayList<String> result = new ArrayList<String>();

        final int MAX_MACS = 5;

        // Get 5 strongest macs
        for (int i = 0; i < MAX_MACS && ls2.size() > 0; i++) {
            result.add(popStrongestListeningMac(ls2));
        }
        return result;
    }

    private String getStrongestListeningMac(ArrayList<LogRecord> logList) {

        // Minimum possible RSS value
        // int strongestRss = Integer.parseInt(App.NanValueUsed);
        // String bssid = null;
        LogRecord strongestLr = new LogRecord("MAC_ERROR", Integer.parseInt(App.NanValueUsed));

        // Find strongest log record
        for (LogRecord logRecord : logList) {

            // If found new strongest RSS
            if (logRecord.getRss() > strongestLr.getRss()) {
                strongestLr = logRecord;
            }
        }
        return strongestLr.getBssid();
    }

    private String popStrongestListeningMac(ArrayList<LogRecord> logList) {
        // Minimum possible RSS value
        LogRecord strongestLr = new LogRecord("MAC_ERROR", Integer.parseInt(App.NanValueUsed));

        // Find strongest log record
        for (LogRecord logRecord : logList) {

            if (app.rmapCache.isProblematicMac(logRecord.getBssid())) continue;

            // If found new strongest RSS
            if (logRecord.getRss() > strongestLr.getRss()) {
                strongestLr = logRecord;
            }
        }

        // pop strongest log record
        logList.remove(strongestLr);
        return strongestLr.getBssid();
    }

    private synchronized void processWifiResults() {
        app.setBgProgressOn();
        List<ScanResult> wifiList = app.lightWifiManager.getScanResults();

        try {
            app.scanAPs.setText("AP:  " + wifiList.size());
        } catch (NullPointerException e) {
            Log.e(TAG, "wifiList was null");
        }
        // Show strongest mac
        app.textViewMessage1.setText(
                "Strongest AP:" + getStrongestListeningMac(app.currentScanListGet()));

        app.currentScanListClear();
        LogRecord lr = null;

        // If we receive results, add them to latest scan list
        if (wifiList != null && !wifiList.isEmpty()) {
            for (int i = 0; i < wifiList.size(); i++) {

                if (isntProblematic(wifiList.get(i).BSSID)) {
                    lr = new LogRecord(wifiList.get(i).BSSID, wifiList.get(i).level);
                    app.currentScanListAdd(lr);
                }
            }
        }

        //save positions
        if (WalkSimulatorClassGenerator.recordingRoute) {
            WalkSimulatorClassGenerator.addNewPosition(app.currentScanListGet());
        }

        app.setBgProgressOff();

        // If user is in trackme mode
        // get partial rmap and then localize
        if (app.isTrackmeEnabled() || app.isFindmeEnabled()) {
            Intent intent = new Intent();
            intent.setAction(App.BROADCAST_RECEIVER_GET_PARTIAL_RMAP);
            app.sendBroadcast(intent);

            // If findme was running, disable it and stop service
            if (app.isFindmeEnabled() && !app.alwaysScan) {
                app.stopService(new Intent(app, LocalizationService.class));
            }
        }
    }

    /**
     * Returns true if Access Point isnt problematic Problematic: there is not data for it in our
     * Server's database
     *
     * @return true if mac isnt problematic
     */
    private boolean isntProblematic(String mac) {
        for (int i = 0; i < app.problematicAPs.size(); i++) {
            if (mac.equals(app.problematicAPs.get(i))) {
                return false;
            }
        }
        return true;
    }
}
