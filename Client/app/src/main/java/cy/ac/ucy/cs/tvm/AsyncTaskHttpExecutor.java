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

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import com.google.android.gms.maps.model.LatLng;
import cy.ac.ucy.cs.tvm.Bloomfilter.Bloomfilter;
import cy.ac.ucy.cs.tvm.Bloomfilter.TVMalgorithms;
import cy.ac.ucy.cs.tvm.tvm.simulation.TestTVM;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Async task for executing Post/Get Requests to our Server. */
public class AsyncTaskHttpExecutor extends AsyncTask<Void, Integer, String> {

    private static final String TAG = AsyncTaskHttpExecutor.class.getSimpleName();
    /**
     * Stores all macs found using TVM1+ algorithms. Used in case that first try with server has no
     * matches with real radiomaps new mac range
     */
    static String allMacs = null;

    String url;
    String params = "";
    App.AsyncTaskType type = App.AsyncTaskType.notset;
    String mac; // strongest listening MAC address
    long startTime;
    boolean anotherProcess = false;
    App app;

    public AsyncTaskHttpExecutor(
            App app, String url, App.AsyncTaskType type, String[] parameters, String mac) {
        this.app = app;
        this.url = url;
        this.type = type;

        if (parameters != null) {
            this.params = parameters[0];

            // Save strongest MAC Address
            if (parameters.length == 2) {
                this.mac = parameters[1];
            } else {
                this.mac = mac;
            }
        }
    }

    @Override
    protected void onPreExecute() {

        app.layoutTopMessages.setBackgroundColor(app.getColor(R.color.teal600));
        if (app.isInBgProgress()) {
            anotherProcess = true;
            return;
        }

        app.setBgProgressOn(); // this process is running
        startTime = System.currentTimeMillis();
        app.progressBarBgTasks.setVisibility(View.VISIBLE);

        super.onPreExecute();

        // If checking VPN
        switch (type) {
            case checkVpn:
                // If user is not in vpn
                if (!app.isInVpn()) {
                    app.textViewMessage2.setText("Checking VPN...");
                    Log.i(TAG, "Checking VPN...");
                }
                break;
            case getMac:
                app.textViewMessage2.setText("Dling rmap..");
                Log.i(TAG, "Downloading rmap..");
                break;
            case getBloomSize:
                app.textViewMessage2.setText("Getting bloom size..");
                Log.i(TAG, "Getting bloom size..");
                break;
            case getBloom:
                app.textViewMessage2.setText("TVM0 dling rmap..");
                Log.i(TAG, "TVM0 dling rmap..");
                break;
            case getMultiMacs:
                app.textViewMessage2.setText("TVM1+ dling rmap..");
                Log.i(TAG, "TVM1+ dling rmap..");
                break;
            case saveExperimentData:
                app.showToast(
                        "Sending experiment data to server...\nPull PowerTutor data manually using bash script");
                break;

            default:
                break;
        }
    }

    @Override
    protected String doInBackground(Void... v) {

        if (anotherProcess) return null;
        String result = null;

        switch (type) {
            case getMac:
                result = App.executeGet(url, params, type);
                break;
            case getMultiMacs:
                result = App.executeGet(url, params, type);
                break;
            case getBloomSize:
                result = App.executeGet(url, params, type);
                break;
            case getBloom:
                result = App.executeGet(url, params, type);
                break;
            case checkVpn:
                if (!app.isInVpn()) result = App.executeGet(url, params, type);
                break;
            case saveExperimentData:
                result = App.executeGet(url, params, type);
                break;

            default:
                break;
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {

        super.onPostExecute(result);
        app.setBgProgressOff(); // this process finished

        if (anotherProcess) return;

        Long endTime = System.currentTimeMillis();
        float totalTime = (float) ((endTime - startTime) / 1000.0);

        String totalTimeS = String.format("%.2f", totalTime);

        // Hide progress bar
        app.progressBarBgTasks.setVisibility(View.INVISIBLE);

        switch (type) {
            case checkVpn:
                if (!app.isInVpn()) {
                    if (result == null) {
                        app.textViewMessage1.setText(app.getString(R.string.vpn_problem));
                        app.textViewMessage2.setText("");
                        app.layoutTopMessages.setBackgroundColor(app.getColor(R.color.red900));
                        Log.i(TAG, app.getString(R.string.vpn_problem));
                    } else {
                        Log.i(TAG, "VPN ok. Time: " + totalTimeS);
                        app.layoutTopMessages.setBackgroundColor(app.getColor(R.color.teal600));
                        app.setVpnOkay();
                        getRadiomap();
                    }
                } else {
                    // User was in VPN
                    getRadiomap();
                }
                break;

            case getMac:
                if (result == null) {
                    app.textViewMessage2.setText("Failed. Is cluster down?");
                    Log.i(TAG, "Failed. Is cluster down?");
                    // Next time re-checK VPN
                    app.setVpnNotAvailable();
                } else {
                    app.textViewMessage2.setText("Server responsed. Time: " + totalTimeS);
                    Log.i(TAG, "Server responsed. Time: " + totalTimeS);
                    if (app.runningExperiment) {
                        //save messages and download time
                        TestTVM.currentTime.addMessagesNumber(result.length());
                        TestTVM.currentTime.addDownloadTime(totalTime);
                    }
                    // Parse result
                    parseAndProcessSingleRmap(result);
                }
                break;

            case getBloomSize:

                // From here TVM0+ algorithms continue
                if (result == null) {
                    app.textViewMessage2.setText("Failed. Is cluster down?");
                    Log.i(TAG, "Failed. Is cluster down?");
                    // Next time re-checK VPN
                    app.setVpnNotAvailable();
                } else {
                    app.textViewMessage2.setText("Server responsed. Time: " + totalTimeS);
                    Log.i(TAG, "Server responsed. Time: " + totalTimeS);
                    // Parse result
                    parseBloomSize(result);
                }
                break;

            case getBloom:
                if (result == null) {
                    app.textViewMessage2.setText("Failed. Is cluster down?");
                    Log.i(TAG, "Failed. Is cluster down?");
                    // Next time re-checK VPN
                    app.setVpnNotAvailable();
                    app.bloomfilter = null;
                    app.bloomSize = -1;
                } else {
                    app.textViewMessage2.setText("Server responsed. Time: " + totalTimeS);
                    Log.i(TAG, "Server responsed. Time: " + totalTimeS);

                    if (app.runningExperiment) {
                        TestTVM.currentTime.addMessagesNumber(result.length());
                        TestTVM.currentTime.addDownloadTime(totalTime);
                    }
                    parseAndProcessManyRadiomaps(result, app.isTVM0());
                }

                break;

            case getMultiMacs:
                if (result == null) {
                    app.textViewMessage2.setText("Failed. Is cluster down?");
                    Log.i(TAG, "Failed. Is cluster down?");
                    // Next time re-Check VPN
                    app.setVpnNotAvailable();
                    app.bloomfilter = null;
                    app.bloomSize = -1;
                } else {
                    app.textViewMessage2.setText("Server responsed. Time: " + totalTimeS);
                    Log.i(TAG, "Server responsed. Time: " + totalTimeS);

                    if (app.runningExperiment) {
                        TestTVM.currentTime.addMessagesNumber(result.length());
                        TestTVM.currentTime.addDownloadTime(totalTime);
                    }
                    // Parse result
                    parseAndProcessManyRadiomaps(result, app.isTVM0());
                }

                break;
            case saveExperimentData:
                if (result == null) {
                    app.showToast("Failed to save data in server.");
                    // Next time re-Check VPN
                } else {
                    JSONObject code = null;
                    try {
                        code = new JSONObject(result);

                        if (code.getInt("code") == -1) {
                            app.showToast("Failed to save data in server.");
                        } else {
                            app.showToast("Experiment data saved in server!");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    /** Parses and Process JSon results from server */
    private void parseAndProcessSingleRmap(String resultStr) {

        Intent intent;
        JSONObject result = null;
        int returnCode = App.CODE_UNKNOWN_ERR0R;
        String returnMessage = null;

        try {
            result = new JSONObject(resultStr);
            returnCode = result.getInt("code");
            returnMessage = result.getJSONArray("rmaps").getJSONObject(0).getString("rmap");
        } catch (JSONException e) {
            returnCode = App.CODE_FAILED_TO_PARSE_JSON;
            Log.e(TAG, resultStr);

            if (app.runningExperiment) {
                TestTVM.continueExperiment();
                return;
            }
        }

        switch (returnCode) {
            case App.CODE_UNKNOWN_ERR0R:
                app.textViewMessage2.setText("Unknown error from server");
                Log.i(TAG, "Unknown error from server");
                break;

            case App.CODE_FAILED_TO_PARSE_JSON:
                app.textViewMessage2.setText("Failed to parse result from server");
                Log.i(TAG, "Failed to parse result from server");
                break;

            case App.CODE_WRONG_ARGS:
                app.textViewMessage2.setText("Error from server:\n");
                Log.i(TAG, "Error from server:\n");
                break;

            case App.CODE_NO_RESULTS:
                app.textViewMessage2.setText(returnMessage);
                Log.e(TAG, "Error: " + returnMessage);

                // Delete RMap from sdcard cache, mark it as not found,
                // and fetch a new partial rmap
                File macFile = new File(mac);

                String[] macFileStr = macFile.getAbsolutePath().split("/");
                String mac2 = macFileStr[macFileStr.length - 1];

                saveProblematicAPandRefetchMap(mac2);

                break;

            case App.CODE_SUCCESS:
                app.textViewMessage2.setText("Success: got rmap");
                Log.i(TAG, "Sucess: got rmap");

                saveToFileAndDownloadRam(returnMessage);

                // Send broadcast to localize user
                intent = new Intent();
                intent.setAction(App.BROADCAST_PARSE_RMAP_AND_FIND_LOCATION);
                app.sendBroadcast(intent);

                break;

            default:
                break;
        }
    }

    /** Save data got from server in a SD card cache and in download cache */
    private void saveToFileAndDownloadRam(String rmap) {

        try {
            // Save real rmap file in table (in RAM)
            app.downloadRamCache = rmap.split("\n");
            if (app.cacheSDcard)
                // Save file to SDcard cache
                saveToSD();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Save data got from server in a SD card cache and in download cache */
    private void saveToFileAndDownloadRam(ArrayList<String> fakeRmaps, String realRmap) {
        try {

            app.downloadRamCache = realRmap.split("\n");
            app.downloadRamFakesCache.clear();

            // Save fake rmap files in table (in RAM)
            for (int i = 0; i < fakeRmaps.size(); i++) {
                app.downloadRamFakesCache.add(fakeRmaps.get(i).split("\n"));
            }

            if (app.cacheSDcard) saveToSD();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Creates a file and saves a string table in it */
    private void saveToSD() throws IOException {
        app.cacheData.currentRmapFile.createNewFile();
        saveRmapToFile(app.downloadRamCache, app.cacheData.currentRmapFile);
    }

    private void saveRmapToFile(String data[], File file) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        for (int i = 0; i < data.length; i++) {
            fileWriter.write(data[i] + "\n");
        }
        fileWriter.close();
    }

    /** Get the rmap using the algorithm from preferences */
    private void getRadiomap() {

        Log.i(TAG, "Using algorithm: " + app.anonymityAlgorithm.toString());

        // Choose anonymity algorithm
        switch (app.anonymityAlgorithm) {
                // Get partial radiomap with MAC
            case PRMAP:
                getPartialRadiomapWithMac();
                break;
            case TVM0:
                // Get partial readiomap with TVM0
                getPartialRadiomapWithTVM0();
                break;
            case TVM1:
            case TVM2:
                // Get partial readiomap with TVM+ algorithms
                getPartialRadiomapWithTVM1plus();
                break;
            case FRMAP:
                //change the mac to the keyword all
                mac = App.ALL_RADIOMAP;
                getPartialRadiomapWithMac(); //but the mac input is
                return;

            default:
                break;
        }
    }

    /**
     * Parces and process bloom results
     *
     * @param result string of json object content
     */
    private void parseAndProcessManyRadiomaps(String result, boolean isBloom) {
        JSONObject jsonObject = null;
        int returnCode = App.CODE_UNKNOWN_ERR0R;
        JSONArray matches = null;

        try {
            jsonObject = new JSONObject(result);
            returnCode = jsonObject.getInt("code");
            ArrayList<LatLng> newFakeLocs = new ArrayList<LatLng>();
            app.currentFakeMatchedMacs.clear();

            if (returnCode == App.CODE_SUCCESS) {

                matches = jsonObject.getJSONArray("matches");

                //cant preserve users anonymity
                if (matches.length() < app.getkAnonymity()) {
                    app.showToast(
                            "Cant preserve "
                                    + app.getkAnonymity()
                                    + ".\n"
                                    + "Instead "
                                    + matches.length()
                                    + " anonymity will preserved");
                    app.setkAnonymity(matches.length());
                }

                int realRmapIndex = -1;

                // List with the indices of the fake rmaps that will be used
                ArrayList<Integer> fakeRmapIndices = new ArrayList<Integer>();

                // Save fake locations
                // and process real mac address
                for (int i = 0; i < matches.length(); i++) {

                    // MAC address matched
                    String matchedMac = matches.getJSONObject(i).getString("mac");
                    String[] matchedLocation = matches.getJSONObject(i).getString("loc").split(",");

                    // save index of real mac's rmap
                    if (mac.equals(matchedMac)) {
                        if (matchedLocation.length == 2) {
                            realRmapIndex = i;

                        } else {
                            Log.i(TAG, "Matched with a not yet inserted mac from server: " + mac);
                        }
                    }
                    // Save fake mac locations
                    else {
                        try {

                            newFakeLocs.add(
                                    new LatLng(
                                            Double.parseDouble(matchedLocation[0]),
                                            Double.parseDouble(matchedLocation[1])));

                            // Save fake mac address
                            app.currentFakeMatchedMacs.add(matchedMac);

                            // Save fake rmap index in json object
                            fakeRmapIndices.add(i);

                        } catch (Exception e) {
                            // Something is terribly wrong!
                            Log.wtf(
                                    TAG,
                                    "Something went wrong. A fake MAC, dont exists in server?\n"
                                            + "Dataset chosen is wrong.\nMac failed: "
                                            + matchedMac);
                            app.showToast(
                                    "Something went wrong. A fake MAC, dont exists in server?\nDataset chosen is wrong");
                        }
                    }
                }

                // Something is terribly wrong
                if (realRmapIndex < 0) {
                    Log.e(TAG, "Error: read MAC address dont exists in server: " + mac);

                    // Remove problematic Access point
                    saveProblematicAPandRefetchMap(mac);

                    return;
                } else {

                    // delete application previous request
                    app.tvm1pRequests = null;

                    // Purge extra fake rmap matches
                    purgeExtraMatches(fakeRmapIndices, newFakeLocs);

                    Log.v(TAG, "New fakes locs size after purge: " + newFakeLocs.size());

                    // Save found coordinates
                    app.fakeMatchedLocations.add(newFakeLocs);

                    String s = "";
                    for (int j = 0; j < fakeRmapIndices.size(); j++) {
                        s += fakeRmapIndices.get(j) + " ";
                    }

                    // In 0 index is the real rmap, and on other
                    // indices the fake ones
                    ArrayList<String> fakeMacs = getFakeRmaps(jsonObject, fakeRmapIndices);

                    // Get real rmap
                    String realRmap = getRealRmap(jsonObject, realRmapIndex);

                    saveToFileAndDownloadRam(fakeMacs, realRmap);
                    // Localize user
                    Intent intent;
                    // Send broadcast to parse radiomap and localize user
                    intent = new Intent();
                    intent.setAction(App.BROADCAST_PARSE_RMAP_AND_FIND_LOCATION);
                    app.sendBroadcast(intent);
                }
            } else {
                String message = jsonObject.getString("message");
                Log.e(TAG, "Error: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();

            Log.e(TAG, "Error: " + e.getMessage());

            // If an experiment running, continue it
            if (app.runningExperiment) {
                app.showToast("Skipped a positions value.");
                TestTVM.continueExperiment();
            }
        }
    }

    /** @return String data of the real radiomap */
    private String getRealRmap(JSONObject jsonObject, int realIndex) throws JSONException {

        // Get real rmap
        JSONArray rmapJsonArray = jsonObject.getJSONArray("rmaps");

        return rmapJsonArray.getJSONObject(realIndex).getString("rmap");
    }

    /** @return Arraylist with all fake rmaps */
    private ArrayList<String> getFakeRmaps(
            JSONObject jsonObject, ArrayList<Integer> fakeRmapIndices) throws JSONException {

        ArrayList<String> fakeRmaps = new ArrayList<String>();

        // Get real rmap
        JSONArray rmapJsonArray = jsonObject.getJSONArray("rmaps");

        // Save fake radiomaps
        for (int i = 0; i < fakeRmapIndices.size(); i++) {

            String fakeRmapJsonData =
                    rmapJsonArray.getJSONObject(fakeRmapIndices.get(i)).getString("rmap");
            fakeRmaps.add(fakeRmapJsonData);
        }

        return fakeRmaps;
    }

    /** Save problematic mac in a list, so its avoided to in future wifi scans */
    private void saveProblematicAPandRefetchMap(String problematicMac) {

        app.problematicAPs.add(problematicMac);

        // Send broadcast to get partial radiomap again, with excluded
        // the mac address w/o results form server
        Intent intent = new Intent();
        intent.setAction(App.BROADCAST_RECEIVER_GET_PARTIAL_RMAP);
        app.sendBroadcast(intent);
    }

    /** Purge extra matches from bloom filter */
    private void purgeExtraMatches(
            ArrayList<Integer> fakeRmapIndices, ArrayList<LatLng> newFakeLocs) {

        while (app.currentFakeMatchedMacs.size() > app.getkAnonymity_m1()) {
            removeFakeMatch(fakeRmapIndices, newFakeLocs);
        }
    }

    /** Removes a fake match from from macs and fake locations */
    private void removeFakeMatch(
            ArrayList<Integer> fakeRmapIndices, ArrayList<LatLng> newFakeLocs) {

        for (int i = 0; i < app.currentFakeMatchedMacs.size(); i++) {
            // If its not the real mac, remove it
            newFakeLocs.remove(i);
            app.currentFakeMatchedMacs.remove(i);
            fakeRmapIndices.remove(i);
            return;
        }
    }

    private void parseBloomSize(String result) {

        JSONObject jsonObject = null;
        int returnMessage = -1;

        try {
            // Create JSON Obj based on the result!
            jsonObject = new JSONObject(result);
            returnMessage = jsonObject.getInt("message");

            app.bloomSize = returnMessage;
            app.bloomfilter = new Bloomfilter(app.bloomSize);

            String bloomfilter = app.bloomfilter.generateBloomFilter(mac);
            Log.i(TAG, " Bloomfilter: " + bloomfilter);

            String[] getParams = {
                App.URL_TYPE
                        + "bloom"
                        + App.URL_FILTER
                        + bloomfilter
                        + App.URL_EXTRAS
                        + App.URL_EXTRAS_V3
                        + App.URL_DATASET
                        + app.datasetInUse
            };

            // Get partial radiomaps
            new AsyncTaskHttpExecutor(
                            app, App.getVMServerURL(), App.AsyncTaskType.getBloom, getParams, mac)
                    .execute();

        } catch (JSONException e) {
        }
    }

    /** Runs a new Task to get a Parial Radiomap from server for a MAC address range */
    private void getPartialRadiomapWithMac() {
        String[] getParams = {
            App.URL_TYPE
                    + "mac"
                    + App.URL_FILTER
                    + mac
                    + App.URL_EXTRAS
                    + App.URL_EXTRAS_V3
                    + App.URL_DATASET
                    + app.datasetInUse
        };

        new AsyncTaskHttpExecutor(
                        app, App.getVMServerURL(), App.AsyncTaskType.getMac, getParams, mac)
                .execute();
    }

    /**
     * Runs a new Task to get a Parial Radiomap from server with a mac address First finds out the
     * bloom size, and then sends the bloom filter and parse results
     */
    private void getPartialRadiomapWithTVM0() {
        String[] getParams = {
            App.URL_TYPE + App.TYPE_BLOOMSIZE + App.URL_DATASET + app.datasetInUse
        };

        new AsyncTaskHttpExecutor(
                        app, App.getVMServerURL(), App.AsyncTaskType.getBloomSize, getParams, mac)
                .execute();
    }

    /**
     * Runs a new Task to get a Parial Radiomap from server with a mac address First finds out the
     * bloom size, and then sends the bloom filter and parse results
     */
    private void getPartialRadiomapWithTVM1plus() {

        // Use bloomfilter method for first time
        if (!app.firstBloomDone) {

            String[] getParams = {App.URL_TYPE + "bloomsize" + App.URL_DATASET + app.datasetInUse};
            Log.e(TAG, "MAC used in TVM1+: " + mac);

            // Get partial radiomaps
            new AsyncTaskHttpExecutor(
                            app,
                            App.getVMServerURL(),
                            App.AsyncTaskType.getBloomSize,
                            getParams,
                            mac)
                    .execute();
        } else {

            app.setBgProgressOn();
            // Calculate new fake positions, and continue localization from TVM Atask
            TVMalgorithms.AsyncTaskTVMalgorithms tvmAlgorithms =
                    new TVMalgorithms.AsyncTaskTVMalgorithms(app, mac);

            tvmAlgorithms.execute();
        }
    }
} // ASyncTask Class
