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
import cy.ac.ucy.cs.tvm.tvm.Heading;
import cy.ac.ucy.cs.tvm.tvm.LocalizationAlgorithms;
import cy.ac.ucy.cs.tvm.tvm.RadioMap;
import cy.ac.ucy.cs.tvm.tvm.simulation.TestTVM;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An asynchronous task to parse a raw radiomap. Radiomaps are received from our server in text raw
 * format, and need to be processed before using them.
 */
public class AsyncTaskParseRmap extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = AsyncTaskParseRmap.class.getSimpleName();
    Long startTime;
    App app;
    boolean anotherProgress = false;
    BufferedReader reader = null;
    String key = null;
    boolean foundData;
    boolean ramHit = false;

    public AsyncTaskParseRmap(App app) {
        this.app = app;
        foundData = false;
        // Find current Rmap ram slot
        // If RAM cache enabled or not
        prepareRmapRam();
    }

    /** Find current ram slot for Radiomap according to user settings CHANGE */
    private void prepareRmapRam() {
        switch (app.cacheData.type) {
            case RAM:
                // Ram hit - Radiomap already parsed
                ramHit = true;
                break;
            case SD: // Ram Miss - Radiomap needs to be parsed
            case MISS:
                ramHit = false;
                // Ram caching is enabled
                if (app.cacheRam) {
                    // Save radiomap in a cache slot
                    app.cacheData.currentRmap =
                            app.rmapCache.ramCacheParsedRadiomap(app.cacheData.currentMac);
                }
                // Ram caching is disabled
                else {
                    // Clear and return first RAM slot
                    app.cacheData.currentRmap = new RadioMap(app);
                }

                // Clear fake rmaps
                app.cacheData.recycleFakeRmaps();
                break;

            default:
                break;
        }
    }

    @Override
    protected void onPreExecute() {

        if (app.isInBgProgress()) {
            anotherProgress = true;
            return;
        }
        app.setBgProgressOn(); // this process is running

        app.progressBarBgTasks.setVisibility(View.VISIBLE);
        app.textViewMessage3.setText("Parsing rmap...");
        Log.i(TAG, "Parsing rmap...");
        startTime = System.currentTimeMillis();

        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        if (anotherProgress) {
            Log.i(TAG, "Process(parse rm)  not runned: another process was already running");
            return false;
        }

        if (ramHit) {
            return true;
        }

        // Just fetched rmap from web, so there is no
        // need to go down to sd. parse it from RAM
        if (app.downloadRamCache != null) {

            Log.i(TAG, "Download RAM cache used!");

            // Read data from real rmap table
            for (int i = 0; i < app.downloadRamCache.length; i++) {
                if (!processRmapLine(app.cacheData.currentRmap, app.downloadRamCache[i])) {
                    return false;
                }
            }

            // Empty real rmap download cache
            app.downloadRamCache = null;

            // Parse fake rmaps
            if (app.isTVMenabled()) {

                Log.e(TAG, "app.downloadRamFakesCache.size(): " + app.downloadRamFakesCache.size());

                // for all fake rmaps
                for (int i = 0; i < app.downloadRamFakesCache.size(); i++) {

                    // Get all fake data from downloadRAM
                    String[] fakeData = app.downloadRamFakesCache.get(i);
                    // Save fake data download ram to appropriate fake radiomap
                    for (int j = 0; j < fakeData.length; j++) {
                        try {
                            if (!processRmapLine(
                                    app.cacheData.currentFakeRmaps.get(i), fakeData[j])) {
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    }
                }
            }
        }

        // Parse radiomap from SD card
        // Server wont know anything about our location
        else {
            try {
                reader = new BufferedReader(new FileReader(app.cacheData.currentRmapFile));
                String line;
                // Read data from file
                while ((line = reader.readLine()) != null) {
                    if (!processRmapLine(app.cacheData.currentRmap, line)) return false;
                }
                reader.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return foundData;
    }

    /**
     * @param rmap Radiomap to parse line for
     * @param line to parse and store in a radiomap
     * @return false if there is an error in a line
     */
    private boolean processRmapLine(RadioMap rmap, String line) {

        ArrayList<String> lineData = null;
        if (line.trim().equals("")) return true;
        line = line.replace(" ", "");
        lineData = split(line, ',', App.NanValueUsed);

        // Detect if is a special line
        if (line.startsWith("#")) {
            // Detect if its mac address line with heading
            if (line.contains("X,Y,HEADING,")) {
                // Must have more than 4 fields
                if (lineData.size() < 4) return false;

                // Store all Mac Addresses
                for (int i = 3; i < lineData.size(); ++i) {
                    rmap.getmacAddresses().add(lineData.get(i));
                }
            }
            // If its an old format line, show log
            else if (line.contains("X,Y")) {
                Log.e(TAG, "Radiomap is in an old format, without heading");
                return false;
            } else if (line.contains("NaN")) {
                App.NanValueUsed = line.replace("#NaN", "");
            }
            // start from beginning
            return true;
        }

        if (lineData.size() < 3) return false;
        key = lineData.get(0) + " " + lineData.get(1);
        ArrayList<String> rssValues = new ArrayList<String>();

        // Save all RSS Values (heading is ignored)
        for (int i = 3; i < lineData.size(); ++i) {
            String rss = lineData.get(i);

            if (rss == null) {
                Log.e(TAG, "rss IS null");
            }

            if (rss.equals("")) rss = App.NanValueUsed;

            // Save RSS value
            rssValues.add(lineData.get(i));
        }

        // Equal number of MAC address and RSS Values
        if (rmap.getmacAddresses().size() != rssValues.size()) {
            Log.e(
                    TAG,
                    "Cant match RSS values with mac addresses: "
                            + rmap.getmacAddresses().size()
                            + "/"
                            + rssValues.size());
            return false;
        }

        // Save line in appropriate table
        // position 2 in line data contains the heading!
        rmap.getlocationRssHashMap(lineData.get(2)).put(key, rssValues);
        addUnique(rmap.getorderList(), key);
        foundData = true; // Found something in rmap
        return true;
    }

    /** Add unique key to order list */
    private void addUnique(ArrayList<String> olist, String key) {

        for (int i = 0; i < olist.size(); i++) {
            if (olist.get(i).equals(key)) {
                return;
            }
        }
        olist.add(key);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        app.setBgProgressOff(); // this process finished

        // If another progress was running, dont continue
        if (anotherProgress) return;

        Long endTime = System.currentTimeMillis();

        float totalTime = (float) ((endTime - startTime) / 1000.0);
        String totalTimeS = String.format("%.2f", totalTime);

        //save parse time
        if (app.runningExperiment) {
            TestTVM.currentTime.addParseTime(totalTime);
        }

        app.progressBarBgTasks.setVisibility(View.INVISIBLE);

        // Radiomap was in RAM - didnt parsed again
        if (ramHit) {
            app.textViewMessage3.setText("RAM HIT: " + totalTimeS);
            Log.i(TAG, "RAM HIT. Time: " + totalTimeS);

            // Find users location
            findUsersLocation();
        }
        // Successfully parsed radiomap
        else if (result == true) {

            app.textViewMessage3.setText("Parsed rmap: " + totalTimeS);
            Log.i(TAG, "Success: parsed rmap. Time: " + totalTimeS);

            findUsersLocation();
        }
        // Failed to parse rmap
        else {

            app.textViewMessage3.setText("Fail: parse rmap");
            Log.i(TAG, "Fail: parse rmap");
            // Delete rmap
            app.cacheData.currentRmapFile.delete();
            Log.i(TAG, "Deleting cached mac: " + app.cacheData.currentRmapFile.getAbsolutePath());

            // Download new rmap
            // Send broadcast to get partial radiomap
            Intent intent = new Intent();
            intent.setAction(App.BROADCAST_RECEIVER_GET_PARTIAL_RMAP);
            app.sendBroadcast(intent);
        }
    }

    /**
     * @param line to split
     * @param c char use to split line
     * @return return splitted table
     */
    private ArrayList<String> split(String line, char c, String nanValue) {

        ArrayList<String> result = new ArrayList<String>();

        // the mac addresses line, just split it with character
        if (line.startsWith("#") && line.contains("X,Y,HEADING,")) {
            result.addAll(Arrays.asList(line.split(new String(new char[] {c}), -1)));
        }
        // if its a regular line
        else {

            String rss = null;
            for (int i = 0; i < line.length(); i++) {

                // Found delimit character
                if (line.charAt(i) == c) {
                    // Flash constructed string to result, and reinit new
                    // rss string. If rss was null, put the Nan value
                    if (rss == null) rss = nanValue;

                    result.add(rss);
                    rss = null;

                    //if its the last character, and it was comma, add another nan value, for the last value
                    if (i == line.length() - 1) {
                        result.add(nanValue);
                    }
                }
                // Construct rss value
                else {
                    if (rss == null) {
                        rss = new String();
                    }

                    rss += line.charAt(i);

                    //if its the last character, save it (the very last number)
                    if (i == line.length() - 1) {
                        result.add(rss);
                    }
                }
            }
        }

        return result;
    }

    /** Executes task to find users location */
    private void findUsersLocation() {
        LocalizationAlgorithms.AsyncTaskFindLocation asyncTaskFindLocation =
                new LocalizationAlgorithms.AsyncTaskFindLocation(app, (int) Heading.azimuth);
        asyncTaskFindLocation.execute();
    }
}
