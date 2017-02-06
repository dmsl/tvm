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

package cy.ac.ucy.cs.tvm.Bloomfilter;

import com.google.android.gms.maps.model.LatLng;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.AsyncTaskHttpExecutor;
import cy.ac.ucy.cs.tvm.tvm.Heading;
import cy.ac.ucy.cs.tvm.tvm.LocalizationAlgorithms;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;
import cy.ac.ucy.cs.tvm.tvm.RadioMap;

/**
 * The TVM family of algorithms:
 *
 * <ul>
 *   <li> TVM0:
 *       <ul>
 *         <li>ask each time using a bloomfilter. When doing consecutive localization (tracking) the
 *             server can deduce user's real location.
 *       </ul>
 *
 *   <li> TVM1:
 *       <ul>
 *         <li>ask the server the first time using bloomfilters, and on subsequent requests use fake
 *             positions in the place of the false positives. Server can deduce user's position
 *             after some processing of the covered distances by the user.
 *       </ul>
 *
 *   <li> TVM2:
 *       <ul>
 *         <li>like TVM1, but when calculating fake positions to cover the false positives it uses
 *             positions that are actually feasible for the user to do. e.g. when the real route has
 *             covered 10meters, then any of the fake routes should cover roughly the same.
 *       </ul>
 *
 * </ul>
 */
public class TVMalgorithms {

    private static final String TAG = TVMalgorithms.class.getSimpleName();

    /** @return a new fake random range */
    public static String chooseARandomNeighbor(RadioMap fakeRadiomap, String fakeMac) {

        // Random heading
        int randomHeading = App.random.nextInt(App.MAX_DEGREES);

        // Hashmap for the random heading
        HashMap<String, ArrayList<String>> randomHashmap;
        List<String> keys;

        randomHashmap = fakeRadiomap.getlocationRssHashMap(randomHeading);

        ArrayList<String> fakeMacs = fakeRadiomap.getmacAddresses();

        int fakeMacIndex = findMacIndex(fakeMac, fakeMacs); // must avoid it

        // Get random hashmap entry
        keys = new ArrayList<String>(randomHashmap.keySet());
        int randomKey = App.random.nextInt(keys.size());
        String randomKeyS = keys.get(randomKey);
        ArrayList<String> rssValues = randomHashmap.get(randomKeyS);

        // Find a not nan rss value
        for (int i = 0; i < rssValues.size(); i++) {
            if (fakeMacIndex == i) continue; // ignore previous fake mac

            // If its not a NaN value - FUTURE CHECK THIS:
            // parse it and compare it as integer
            if (!rssValues.get(i).equals(App.NanValueUsed)) {
                return fakeMacs.get(i); // Found new fake mac
            }
        }

        return null; // failed to found new fake mac
    }

    /** @return next real and N fake MAC Addresses based on Euclidean distance */
    public static ArrayList<String> chooseNeighbors_TVM2(App app, int heading) {

        ArrayList<String> result = new ArrayList<String>();
        LatLng enteredPosition = app.previousCoordinates;
        LatLng exitedPosition = app.currentCoordinates;
        ArrayList<RadioMap> fakeRmaps = app.cacheData.currentFakeRmaps;
        ArrayList<String> fakeMacs = app.currentFakeMatchedMacs;

        // Index to user for heading
        int headingIndex = RadioMap.findTableIndex(heading);
        ArrayList<LogRecord> enteredLogRecord = app.enteredScanListGet();
        ArrayList<LogRecord> exitedLogRecord = app.exitedScanListGet();

        double realDistanceCovered =
                LocalizationAlgorithms.distance(enteredPosition, exitedPosition);

        /*
         *
         * Get RSS Values for entered and exited positions from an AP Range
         */
        // Find RSS Values of previous mac when exited and entered an AP
        float prevMAC_enteredRSS = getRss(enteredLogRecord, app.previousMac);
        float prevMAC_exitedRSS = getRss(exitedLogRecord, app.previousMac);

        // Find RSS values of current mac when exited and entered an AP
        float curMAC_enteredRSS = getRss(enteredLogRecord, app.cacheData.currentMac);
        float curMAC_exitedRSS = getRss(exitedLogRecord, app.cacheData.currentMac);

        // New fake macs from new neighbors
        ArrayList<String> newFakeMacs = new ArrayList<String>();
        ArrayList<Integer> newFakeMacColumns = new ArrayList<Integer>();

        int currentFakeColumn;

        // For all fake macs
        for (int m = 0; m < fakeMacs.size(); m++) {

            float threshold = App.EXIT_RSS_THRESHOLD;
            String fakeMac = fakeMacs.get(m);
            RadioMap fakeRmap = fakeRmaps.get(m);

            // Fake HashMap of RMap
            HashMap<String, ArrayList<String>> fakeHmap =
                    fakeRmap.getlocationRssHashMap(headingIndex);
            ArrayList<String> fakeRmapMacs = fakeRmap.getmacAddresses();

            ArrayList<ArrayList<String>> enteredRows = new ArrayList<ArrayList<String>>();
            ArrayList<LatLng> enteredRowsPositions = new ArrayList<LatLng>();
            ArrayList<PossibleNeighbor> exitedRows = new ArrayList<PossibleNeighbor>();

            // Get MAC Address column, of fake MAC Address
            currentFakeColumn = getMacColumnWithMacs(fakeMac, fakeRmapMacs);

            // Find possible entered and exited positions of current fake AP
            float closestEntered = Float.MAX_VALUE;
            float closestExited = Float.MAX_VALUE;
            float tmpRes;
            boolean foundClosedEntered = false, foundClosedExited = false;

            String firstRowKeyStr = null;
            LatLng firstRowKey = null;

            if (fakeHmap.keySet().iterator().hasNext()) {
                firstRowKeyStr = fakeHmap.keySet().iterator().next();
            }

            try {
                String[] firstRowKeyStrTbl = firstRowKeyStr.split(" ");
                // Save parsed key value
                firstRowKey =
                        new LatLng(
                                Double.parseDouble(firstRowKeyStrTbl[0]),
                                Double.parseDouble(firstRowKeyStrTbl[1]));
            } catch (Exception e) {
                firstRowKey = new LatLng(0, 0);
            }

            PossibleNeighbor found;
            String macFound;
            try {
                for (Iterator i = fakeHmap.entrySet().iterator();
                        i.hasNext() && (!foundClosedEntered || !foundClosedExited);
                        i.remove()) {

                    Map.Entry pairs = (Map.Entry) i.next();

                    // Get rows of radiomap (Key + values)
                    String rowKeyString = (String) pairs.getKey();
                    // Split key: lat" "lon
                    String[] rowKeyTbl = rowKeyString.split(" ");

                    // Save parsed key value
                    LatLng rowKey =
                            new LatLng(
                                    Double.parseDouble(rowKeyTbl[0]),
                                    Double.parseDouble(rowKeyTbl[1]));

                    ArrayList<String> rowRss = (ArrayList<String>) pairs.getValue();

                    // If is a NaN value, continue
                    if (rowRss.get(currentFakeColumn).equals(App.NanValueUsed)) continue;

                    float curFakeColumnRSS = Float.parseFloat(rowRss.get(currentFakeColumn));

                    // If still searching for closest Entered Point
                    if (!foundClosedEntered) {

                        // Save all rows of possible entered(to AP) positions
                        tmpRes = curFakeColumnRSS - prevMAC_enteredRSS;
                        if (tmpRes < 0) tmpRes *= -1;

                        // Find closest RSS value of fake AP to real starting
                        // position
                        // Do this because we cant find
                        if (tmpRes < closestEntered) {
                            enteredRows.clear();
                            enteredRowsPositions.clear();
                            enteredRows.add(rowRss);
                            enteredRowsPositions.add(rowKey);

                            closestEntered = tmpRes;

                            if (closestEntered == 0) {
                                foundClosedEntered = true;
                            }
                            continue;
                        } else if (tmpRes == closestEntered) {

                            enteredRows.add(rowRss);
                            enteredRowsPositions.add(rowKey);
                            continue;
                        }
                    }

                    // If still waiting for Closest Exit Point
                    if (!foundClosedExited) {
                        // If row isnt on enteredRows, it may be on exitedRows

                        // Save all rows of possible exited positions

                        tmpRes = curFakeColumnRSS - prevMAC_exitedRSS;
                        if (tmpRes < 0) tmpRes *= -1;

                        // If difference closer than previous result -
                        // threshold
                        if (tmpRes < (closestExited - threshold)) {

                            exitedRows.clear();
                            closestExited = tmpRes;

                            // Get columns of possible new fake neighbors
                            // (of that row)
                            PossibleNeighbor p =
                                    findMatchingRssNeighbors(
                                            rowRss,
                                            rowKey,
                                            curMAC_exitedRSS,
                                            currentFakeColumn,
                                            newFakeMacColumns);

                            // Save possible row, and possible
                            // neighbors on that row
                            exitedRows.add(p);

                            if (closestExited == 0) {
                                foundClosedExited = true;
                            }

                        } else if (tmpRes >= closestExited && tmpRes <= closestExited + threshold) {
                            // Get columns of possible new fake neighbors
                            // (of that row)
                            PossibleNeighbor p =
                                    findMatchingRssNeighbors(
                                            rowRss,
                                            rowKey,
                                            curMAC_exitedRSS,
                                            currentFakeColumn,
                                            newFakeMacColumns);

                            // Save possible row, and possible
                            // neighbors on that row
                            exitedRows.add(p);

                            // ADVICE cut threshold in half here!
                            if (exitedRows.size() > 5) {
                                threshold /= 5;
                            }
                        }
                    }
                } // End of Partial Radiomap loop

                // Workaround for isolated AP
                if (exitedRows.size() == 0) {
                    PossibleNeighbor p =
                            findMatchingRssNeighbors(
                                    enteredRows.get(0),
                                    firstRowKey,
                                    curMAC_exitedRSS,
                                    currentFakeColumn,
                                    newFakeMacColumns);
                    exitedRows.add(p);
                }

                int foundEntered = -1;
                int foundExited = -1;
                double closestResult = Double.MAX_VALUE;

                boolean exactMatch = false;

                // Find the best-fit route
                for (int i = 0; i < enteredRows.size() && !exactMatch; i++) {
                    for (int j = 0; j < exitedRows.size(); j++) {

                        enteredPosition = enteredRowsPositions.get(i);
                        exitedPosition = exitedRows.get(j).coordinates;

                        double fakeDistance =
                                LocalizationAlgorithms.distance(enteredPosition, exitedPosition);
                        // Calculate result
                        double distanceResult = realDistanceCovered - fakeDistance;

                        if (distanceResult < 0) distanceResult *= -1;

                        // Save closest result (Best solution)
                        if (distanceResult < closestResult) {
                            closestResult = distanceResult;
                            foundEntered = i;
                            foundExited = j;

                            if (closestResult == 0) {
                                exactMatch = true;
                                break;
                            }
                        }
                    }
                }

                if (foundEntered == -1 || foundExited == -1) {
                    Log.e(TAG, "Failed to found entered or exited position");
                }

                found = exitedRows.get(foundExited);

                macFound = fakeRmapMacs.get(found.macIndex);

            } catch (Exception e) {

                // check there is a bug i some cases!
                Log.e(TAG, "Failed to choose neighbor based on RSS values");
                // check pick up random here!  (the first)

                try {
                    macFound = fakeRmapMacs.get(0);
                    found = new PossibleNeighbor();

                    found.macIndex = 0;
                } catch (Exception ee) {
                    macFound = fakeMac; //failed to find a random fake mac. use previous one
                    found = new PossibleNeighbor();
                    found.macIndex = 0;
                }
            }

            // Save new fake MAC Address found
            result.add(macFound);

            newFakeMacs.add(macFound);
            newFakeMacColumns.add(found.macIndex);
        }
        return result;
    }

    /** Fidn the RSS value of the given mac in an ArrayList LocRecord */
    private static int getRss(ArrayList<LogRecord> exitedLogRecord, String mac) {

        int rssValue = Integer.parseInt(App.NanValueUsed);

        for (int i = 0; i < exitedLogRecord.size(); i++) {
            if (exitedLogRecord.get(i).getBssid().equals(mac)) {
                rssValue = exitedLogRecord.get(i).getRss();
                break;
            }
        }
        return rssValue;
    }

    /**
     * Finds next possible fake neighbor Access Point. Search all the neighbors to find the closest
     * RSS value to the real new AP neighbor
     *
     * @param curMAC_exitedRSS real RSS value from real neighbor
     * @param currentFakeColumn Column of MAC of current fake AP
     */
    private static PossibleNeighbor findMatchingRssNeighbors(
            ArrayList<String> rowRss,
            LatLng coordinates,
            float curMAC_exitedRSS,
            int currentFakeColumn,
            ArrayList<Integer> newFakeMacColumns) {

        PossibleNeighbor result = new PossibleNeighbor();

        float closest = Float.MAX_VALUE;
        float tmp;
        int index = -1;
        boolean toContinue;

        for (int i = 0; i < rowRss.size(); i++) {
            // Continue on current fake AP column, or a NaN column
            if (rowRss.get(i).equals(App.NanValueUsed)) continue;
            if (i == currentFakeColumn) continue;

            toContinue = false;
            // Continue on columns of already found fake neighbors
            for (int j = 0; j < newFakeMacColumns.size(); j++) {
                if (i == newFakeMacColumns.get(j)) {
                    toContinue = true;
                    break;
                }
            }

            if (toContinue) continue;

            tmp = Float.parseFloat(rowRss.get(i)) - curMAC_exitedRSS;
            if (tmp < 0) tmp *= -1;

            if (tmp < closest) {
                index = i;
                closest = tmp;

                if (closest == 0) {
                    // ADVICE
                    break;
                }
            }
        }

        result.coordinates = coordinates;
        result.close = closest;
        result.macIndex = index;
        result.rowRss = rowRss;

        return result;
    }

    /**
     * @param mac
     * @param macArrayList
     * @return
     */
    private static int getMacColumnWithMacs(String mac, ArrayList<String> macArrayList) {

        for (int i = 0; i < macArrayList.size(); i++) {
            if (macArrayList.get(i).equals(mac)) return i;
        }

        // not found
        return -1;
    }

    /**
     * @param mac to find its index
     * @return logRecord of recorded MAC+RSS value pairs
     */
    private static int getMacColumn(String mac, ArrayList<LogRecord> logRecord) {

        for (int i = 0; i < logRecord.size(); i++) {
            if (logRecord.get(i).getBssid().equals(mac)) return i;
        }
        // not found
        return -1;
    }

    /** @return the index of the fake mac given as input, from all the fake macs */
    private static int findMacIndex(String fakeMac, ArrayList<String> fakeMacs) {

        for (int i = 0; i < fakeMacs.size(); i++) {
            if (fakeMac.equals(fakeMacs.get(i))) return i;
        }

        // not found
        return -1;
    }

    /** @author paschalis */
    public static class AsyncTaskTVMalgorithms extends AsyncTask<Void, Void, String> {

        private static int realRandomIndex;
        private App app;
        private String mac;

        public AsyncTaskTVMalgorithms(App app, String mac) {
            this.app = app;
            this.mac = mac;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {

            if (app.tvm1pRequests != null) {
                replaceFaultyMac();
            } else {

                ArrayList<String> newFakes;
                switch (app.anonymityAlgorithm) {
                    case TVM1:
                        newFakes = new ArrayList<String>();

                        // Calculate a new random fake range for all fake
                        // mac addresses
                        for (int i = 0; i < app.cacheData.currentFakeRmaps.size(); i++) {
                            newFakes.add(
                                    TVMalgorithms.chooseARandomNeighbor(
                                            app.cacheData.currentFakeRmaps.get(i),
                                            app.currentFakeMatchedMacs.get(i)));
                        }

                        buildRandomFakesAndRealsOrder(newFakes);

                        break;
                    case TVM2:
                        newFakes = TVMalgorithms.chooseNeighbors_TVM2(app, (int) Heading.azimuth);
                        if (newFakes == null) {
                            Log.i(TAG, "Fake new nulls");
                        } else {
                            for (int i = 0; i < newFakes.size(); i++) {
                                Log.i(TAG, "new fake: " + newFakes.get(i));
                            }
                        }

                        buildRandomFakesAndRealsOrder(newFakes);
                        break;

                    default:
                        break;
                }
            }
            return app.tvm1pRequests;
        }

        /** */
        private void replaceFaultyMac() {

            String[] previousRequest = app.tvm1pRequests.split(",");
            // Replace faulty mac
            previousRequest[realRandomIndex] = mac;
            String result = "";
            int size = previousRequest.length;
            for (int i = 0; i < size - 1; i++) {
                result += previousRequest[i] + ",";
            }

            result += previousRequest[size - 1];
            app.tvm1pRequests = result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            app.setBgProgressOff();
            String[] getParams = {
                App.URL_TYPE
                        + "mac"
                        + App.URL_FILTER
                        + result
                        + App.URL_EXTRAS
                        + App.URL_EXTRAS_V3
                        + App.URL_DATASET
                        + app.datasetInUse
            };

            // Get partial radiomap
            new AsyncTaskHttpExecutor(
                            app,
                            App.getVMServerURL(),
                            App.AsyncTaskType.getMultiMacs,
                            getParams,
                            mac)
                    .execute();
        }

        /** @return a string with new fake macs + real mac in a random order */
        private void buildRandomFakesAndRealsOrder(ArrayList<String> newFakes) {

            realRandomIndex = App.random.nextInt(newFakes.size());

            // Add real mac in a random index
            newFakes.add(realRandomIndex, mac);

            String result = "";
            int size = newFakes.size();

            for (int i = 0; i < size - 1; i++) {
                result += newFakes.get(i) + ",";
            }

            result += newFakes.get(size - 1);
            Log.e(TAG, "new fake+real macs multi-mac request: " + result);
            app.tvm1pRequests = result;
        }

        /**
         * INFO this method tries to randomise the real and the fake mac addresses on new requests
         * to server eg: first time: realMac, fakeMac1, fakeMac2, fakeMac3 next time: newFakeMac3,
         * newRealMac, newFakeMac2, newFakeMac1 I dont know if this is usable or not!
         *
         * @return a string with new fake macs + real mac in a random order
         */
        private void buildRandomFakesAndRealsOrderTvmPlus(ArrayList<String> newFakes) {

            String[] previousRequest = app.tvm1pRequests.split(",");

            int newFakesCnt = 0;
            // Replace previous mac with new one and fakes with new fakes
            for (int i = 0; i < previousRequest.length; i++) {
                if (previousRequest[i].equals(app.previousMac)) {
                    previousRequest[i] = mac;
                } else {
                    previousRequest[i] = newFakes.get(newFakesCnt++);
                }
            }

            String result = "";
            int size = previousRequest.length;
            for (int i = 0; i < size - 1; i++) {
                result += previousRequest[i] + ",";
            }
            result += previousRequest[size - 1];
            app.tvm1pRequests = result;
        }
    }

    public static class PossibleNeighbor {
        LatLng coordinates;
        ArrayList<String> rowRss;
        int macIndex;
        float close;
    }
}
