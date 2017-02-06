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

package cy.ac.ucy.cs.tvm.tvm;

import com.google.android.gms.maps.model.LatLng;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.MainActivity;
import cy.ac.ucy.cs.tvm.cache.RMapCache;
import cy.ac.ucy.cs.tvm.tvm.simulation.TestTVM;

import static cy.ac.ucy.cs.tvm.map.MapUtilities.updateMap;

/**
 * Localization algorithms:
 *
 * <ul>
 *   <li>K Nearest Neighbor (KNN)
 *   <li>Weighted K Nearest Neighbor (WKNN)
 *   <li>Minimum Measure Square Error (MMSE)
 *   <li>Weighted Minimum Measure Square Error (WMMSE)
 * </ul>
 */
public class LocalizationAlgorithms {

    private static final String TAG = LocalizationAlgorithms.class.getSimpleName();
    private App app;

    public LocalizationAlgorithms(App app) {
        this.app = app;
    }

    /**
     * Calculates the Euclidean distance between the currently observed RSS values and the RSS
     * values for a specific location.
     *
     * @param l1 RSS values of a location in radiomap
     * @param l2 RSS values currently observed
     * @return The Euclidean distance, or MIN_VALUE for error
     */
    private static double calculateEuclideanDistance(ArrayList<String> l1, ArrayList<String> l2) {

        double finalResult = 0, v1, v2, temp;
        String str;

        for (int i = 0; i < l1.size(); ++i) {
            try {
                str = l1.get(i);
                v1 = Double.valueOf(str.trim()).doubleValue();
                str = l2.get(i);
                v2 = Double.valueOf(str.trim()).doubleValue();
            } catch (Exception e) {
                return Double.NEGATIVE_INFINITY;
            }

            // do the procedure
            temp = v1 - v2;
            temp *= temp;

            // do the procedure
            finalResult += temp;
        }

        return ((double) Math.sqrt(finalResult));
    }

    /**
     * Calculates the Euclidean distance between the currently observed RSS values and the RSS
     * values for a specific location.
     *
     * @param l1 RSS values of a location in radiomap
     * @param l2 RSS values currently observed
     * @return The Euclidean distance, or MIN_VALUE for error
     */
    public static double calculateEuclideanDistanceDouble1(LatLng l1, LatLng l2) {

        double finalResult = 0, temp;

        temp = l1.latitude - l2.latitude;
        temp *= temp;
        finalResult += temp;

        temp = l1.longitude - l2.longitude;
        temp *= temp;
        finalResult += temp;

        return (Math.sqrt(finalResult));
    }

    /**
     * Calculates the Probability of the user being in the currently observed RSS values and the RSS
     * values for a specific location.
     *
     * @param l1 RSS values of a location in radiomap
     * @param l2 RSS values currently observed
     * @return The Probability for this location, or MIN_VALUE for error
     */
    public static double calculateProbability(
            ArrayList<String> l1, ArrayList<String> l2, double sGreek) {

        double finalResult = 1, v1, v2, temp;
        String str;

        for (int i = 0; i < l1.size(); ++i) {

            try {
                str = l1.get(i);
                v1 = Double.valueOf(str.trim()).doubleValue();
                str = l2.get(i);
                v2 = Double.valueOf(str.trim()).doubleValue();
            } catch (Exception e) {
                return Double.NEGATIVE_INFINITY;
            }

            temp = v1 - v2;
            temp *= temp;
            temp = -temp;
            temp /= (double) (sGreek * sGreek);
            temp = (double) Math.exp(temp);
            finalResult *= temp;
        }
        return finalResult;
    }

    /**
     * Reads the parameters from the file FUTURE check this
     *
     * @param file the file of radiomap, to read parameters
     * @param algorithm_choice choice of several algorithms
     * @return The parameter for the algorithm
     */
    private static String readParameter(File file, int algorithm_choice) {

        String line;
        BufferedReader reader = null;
        FileReader fr = null;

        String parameter = null;

        try {
            fr = new FileReader(file.getAbsolutePath() + "-parameters");
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                /* Ignore the labels */
                if (line.startsWith("#") || line.trim().equals("")) {
                    continue;
                }

                /* Split fields */
                String[] temp = line.split(":");

                /* The file may be corrupted so ignore reading it */
                if (temp.length != 2) {
                    return null;
                }

                if (algorithm_choice == 0 && temp[0].equals("NaN")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 1 && temp[0].equals("KNN")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 2 && temp[0].equals("WKNN")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 3 && temp[0].equals("MAP")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 4 && temp[0].equals("MMSE")) {
                    parameter = temp[1];
                    break;
                }
            }
            fr.close();
            reader.close();
        } catch (Exception e) {
            return null;
        }

        return parameter;
    }

    /** Dinstance between two locs in */
    public static double distance(LatLng StartP, LatLng EndP) {
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return 6366000 * c;
    }

    /**
     * Calculate users location
     *
     * @param latestScanList the current scan list of APs
     * @param radioMap the constructed Radio Map
     * @param heading choice of several algorithms
     * @return the location of user
     */
    public boolean calculateRealLocation(
            ArrayList<LogRecord> latestScanList,
            RadioMap radioMap,
            int heading,
            App.LocalizationAlgorithm algorithm) {

        int i, j, notFoundCounter = 0;
        ArrayList<String> MacAdressList = radioMap.getmacAddresses();
        ArrayList<String> Observed_RSS_Values = new ArrayList<String>();
        LogRecord temp_LR;
        boolean found;

        // Check which mac addresses of radio map, we are currently listening.
        for (i = 0; i < MacAdressList.size(); ++i) {

            found = false;
            for (j = 0; j < latestScanList.size(); ++j) {

                temp_LR = latestScanList.get(j);

                // MAC Address Matched
                if (MacAdressList.get(i).compareTo(temp_LR.getBssid()) == 0) {

                    Observed_RSS_Values.add(String.valueOf(temp_LR.getRss()));
                    found = true;
                    break;
                }
            }
            // A MAC Address is missing so we place a small value, NaN value
            // if (j == latestScanList.size()){
            if (!found) {
                Observed_RSS_Values.add(String.valueOf(App.NanValueUsed));
                ++notFoundCounter;
            }
        }

        if (notFoundCounter == MacAdressList.size()) {
            return false;
        }

        Log.i(TAG, "Algorithm used: " + algorithm.toString());
        int param = 4;
        boolean result = false;

        switch (algorithm) {
            case KNN:
                result = KNN_WKNN_Algorithm(radioMap, Observed_RSS_Values, param, heading, false);
                break;
            case WKNN:
                result = KNN_WKNN_Algorithm(radioMap, Observed_RSS_Values, param, heading, true);
                break;
            case MMSE:
                result = MAP_MMSE_Algorithm(radioMap, Observed_RSS_Values, param, heading, false);
                break;
            case WMMSE:
                result = MAP_MMSE_Algorithm(radioMap, Observed_RSS_Values, param, heading, true);
                break;
        }

        return result;
    }

    /**
     * Calculates user location based on Weighted/Not Weighted K Nearest Neighbor (KNN) Algorithm
     *
     * @param RMap The radio map structure
     * @param Observed_RSS_Values RSS values currently observed
     * @param isWeighted To be weighted or not
     * @return The estimated user location
     */
    private boolean KNN_WKNN_Algorithm(
            RadioMap RMap,
            ArrayList<String> Observed_RSS_Values,
            int parameter,
            int heading,
            boolean isWeighted) {

        ArrayList<String> RSS_Values;
        double curResult = 0;
        ArrayList<LocDistance> LocDistance_Results_List = new ArrayList<LocDistance>();
        boolean foundLocation = false;
        int K = parameter;

        // Find appropriate hashmap
        HashMap<String, ArrayList<String>> hasmap = RMap.getlocationRssHashMap(heading);

        // Construct a list with locations-distances pairs for currently
        // observed RSS values
        for (String location : hasmap.keySet()) {
            RSS_Values = hasmap.get(location);
            curResult = calculateEuclideanDistance(RSS_Values, Observed_RSS_Values);

            if (curResult == Double.NEGATIVE_INFINITY) {
                Log.e(TAG, "Negative infinity curRes: " + curResult);
                return false;
            }

            LocDistance_Results_List.add(0, new LocDistance(curResult, location));
        }

        // Sort locations-distances pairs based on minimum distances
        Collections.sort(
                LocDistance_Results_List,
                new Comparator<LocDistance>() {

                    public int compare(LocDistance gd1, LocDistance gd2) {
                        return (gd1.getDistance() > gd2.getDistance()
                                ? 1
                                : (gd1.getDistance() == gd2.getDistance() ? 0 : -1));
                    }
                });

        if (!isWeighted) {
            foundLocation = calculateAverageKDistanceLocations(LocDistance_Results_List, K);
        } else {
            foundLocation = calculateWeightedAverageKDistanceLocations(LocDistance_Results_List, K);
        }

        return foundLocation;
    }

    /**
     * Calculates user location based on Probabilistic Maximum A Posteriori (MAP) Algorithm or
     * Probabilistic Minimum Mean Square Error (MMSE) Algorithm
     *
     * @param RMap The radio map structure
     * @param Observed_RSS_Values RSS values currently observed
     * @param isWeighted To be weighted or not
     * @return The estimated user location
     */
    private boolean MAP_MMSE_Algorithm(
            RadioMap RMap,
            ArrayList<String> Observed_RSS_Values,
            int parameter,
            int heading,
            boolean isWeighted) {

        ArrayList<String> RSS_Values;
        double curResult = 0.0d;
        boolean foundLocation = false;
        double highestProbability = Double.NEGATIVE_INFINITY;
        ArrayList<LocDistance> LocDistance_Results_List = new ArrayList<LocDistance>();
        double sGreek = parameter;

        // Find appropriate hashmap
        HashMap<String, ArrayList<String>> hasmap = RMap.getlocationRssHashMap(heading);

        // Find the location of user with the highest probability
        for (String location : hasmap.keySet()) {

            RSS_Values = hasmap.get(location);
            curResult = calculateProbability(RSS_Values, Observed_RSS_Values, sGreek);

            if (curResult == Double.NEGATIVE_INFINITY) return false;
            else if (curResult > highestProbability) {
                highestProbability = curResult;

                String[] loc = location.split(" ");
                app.currentCoordinates =
                        new LatLng(Double.parseDouble(loc[0]), Double.parseDouble(loc[1]));

                return true;
            }

            if (isWeighted) LocDistance_Results_List.add(0, new LocDistance(curResult, location));
        }

        if (isWeighted)
            foundLocation = calculateWeightedAverageProbabilityLocations(LocDistance_Results_List);

        return foundLocation;
    }

    /**
     * Calculates the Average of the K locations that have the shortest distances D
     *
     * @param LocDistance_Results_List Locations-Distances pairs sorted by distance
     * @param K The number of locations used
     * @return The estimated user location, or null for error
     */
    private boolean calculateAverageKDistanceLocations(
            ArrayList<LocDistance> LocDistance_Results_List, int K) {

        double sumX = 0.0f;
        double sumY = 0.0f;

        String[] LocationArray = new String[2];
        double x, y;

        int K_Min = K < LocDistance_Results_List.size() ? K : LocDistance_Results_List.size();

        // Calculate the sum of X and Y
        for (int i = 0; i < K_Min; ++i) {
            LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

            try {
                x = Double.valueOf(LocationArray[0].trim()).doubleValue();
                y = Double.valueOf(LocationArray[1].trim()).doubleValue();
            } catch (Exception e) {
                return false;
            }

            sumX += x;
            sumY += y;
        }

        // Calculate the average
        sumX /= K_Min;
        sumY /= K_Min;

        // Save results
        app.currentCoordinates = new LatLng(sumX, sumY);

        return true;
    }

    /**
     * Calculates the Weighted Average of the K locations that have the shortest distances D
     *
     * @param LocDistance_Results_List Locations-Distances pairs sorted by distance
     * @param K The number of locations used
     * @return The estimated user location, or null for error
     */
    public boolean calculateWeightedAverageKDistanceLocations(
            ArrayList<LocDistance> LocDistance_Results_List, int K) {

        double LocationWeight = 0.0f;
        double sumWeights = 0.0f;
        double WeightedSumX = 0.0f;
        double WeightedSumY = 0.0f;

        String[] LocationArray = new String[2];
        double x, y;

        int K_Min = K < LocDistance_Results_List.size() ? K : LocDistance_Results_List.size();

        // Calculate the weighted sum of X and Y
        for (int i = 0; i < K_Min; ++i) {

            LocationWeight = 1 / LocDistance_Results_List.get(i).getDistance();
            LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

            try {
                x = Double.valueOf(LocationArray[0].trim()).doubleValue();
                y = Double.valueOf(LocationArray[1].trim()).doubleValue();
            } catch (Exception e) {
                Log.e(TAG, "Localization exception: " + e.getMessage());
                return false;
            }

            sumWeights += LocationWeight;
            WeightedSumX += LocationWeight * x;
            WeightedSumY += LocationWeight * y;
        }

        WeightedSumX /= sumWeights;
        WeightedSumY /= sumWeights;

        app.currentCoordinates = new LatLng(WeightedSumX, WeightedSumY);

        return true;
    }

    /**
     * Calculates the Weighted Average over ALL locations where the weights are the Normalized
     * Probabilities
     *
     * @param LocDistance_Results_List Locations-Probability pairs
     * @return The estimated user location, or null for error
     */
    public boolean calculateWeightedAverageProbabilityLocations(
            ArrayList<LocDistance> LocDistance_Results_List) {

        double sumProbabilities = 0.0f;
        double WeightedSumX = 0.0f;
        double WeightedSumY = 0.0f;
        double NP;
        double x, y;
        String[] LocationArray = new String[2];

        // Calculate the sum of all probabilities
        for (int i = 0; i < LocDistance_Results_List.size(); ++i)
            sumProbabilities += LocDistance_Results_List.get(i).getDistance();

        // Calculate the weighted (Normalized Probabilities) sum of X and Y
        for (int i = 0; i < LocDistance_Results_List.size(); ++i) {
            LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

            try {
                x = Double.valueOf(LocationArray[0].trim()).doubleValue();
                y = Double.valueOf(LocationArray[1].trim()).doubleValue();
            } catch (Exception e) {
                return false;
            }

            NP = LocDistance_Results_List.get(i).getDistance() / sumProbabilities;

            WeightedSumX += (x * NP);
            WeightedSumY += (y * NP);
        }

        app.currentCoordinates = new LatLng(WeightedSumX, WeightedSumY);

        return true;
    }

    public static class AsyncTaskFindLocation extends AsyncTask<Void, Void, Boolean> {

        Long startTime;
        App app;
        int heading;
        private boolean anotherProgress = false;

        public AsyncTaskFindLocation(App app, int heading) {
            this.app = app;
            this.heading = heading;
        }

        @Override
        protected void onPreExecute() {
            if (app.isInBgProgress()) {
                anotherProgress = true;

                return;
            }

            app.setBgProgressOn();

            app.progressBarBgTasks.setVisibility(View.VISIBLE);
            app.textViewMessage4.setText("Finding location..");
            Log.i(TAG, "Finding location..");

            startTime = System.currentTimeMillis();

            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (anotherProgress) {
                Log.i(TAG, "Process(localize) not runned: another process was already running");
                return false;
            }

            boolean foundLocation =
                    app.algorithms.calculateRealLocation(
                            app.currentScanListGet(),
                            app.cacheData.currentRmap,
                            heading,
                            app.localizationAlgorithm);

            if (!foundLocation) return false;

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (anotherProgress) return;

            app.setFindmeOff(); // Disable findme
            app.setBgProgressOff(); // this process finished

            Long endTime = System.currentTimeMillis();

            float totalTime = (float) ((endTime - startTime) / 1000.0);
            String totalTimeS = String.format("%.2f", totalTime);

            app.progressBarBgTasks.setVisibility(View.INVISIBLE);

            // Successfully found location
            if (result == true) {
                app.textViewMessage1.setText("Success: located");

                Log.i(TAG, "Success: located. Time: " + totalTimeS);
                app.textViewMessage4.setText("Found location: " + totalTimeS);

                if (app.runningExperiment) {
                    TestTVM.currentTime.addLocaliseTime(totalTime);
                }

                // Clear fault mac addresses
                app.problematicAPs.clear();

                // If TVM1+ algorithms are enabled
                // and user successfully localized,
                // now the fake macs will calculated with TVM1/TVM2 methods
                // If it was cache miss: in ram+sd cases the firstBloom isnt done yet!
                if ((app.isTVMenabled() || app.isTVM0())
                        && app.cacheData.type.equals(RMapCache.CacheType.MISS)) {
                    app.firstBloomDone = true;

                    // If we have >2 matches, show unveil button - CHECK !
                    app.buttonUnveilMatches.setVisibility(View.VISIBLE);
                }

                MainActivity.updateDeveloperInfoLabels();
                updateMap(app);

                if (app.runningExperiment) {
                    // Send broadcast to move to next position
                    // Send broadcast to get partial radiomap
                    Intent intent = new Intent();
                    intent.setAction(App.BROADCAST_EXPERIMENT_LOCALISE_INSTANCE);
                    app.sendBroadcast(intent);
                }

                // Failed to find location
            } else {
                app.textViewMessage1.setText("Out of range.");
                app.textViewMessage4.setText("Failure.");
                Log.e(TAG, "Out of range. ");

                if (app.runningExperiment) {
                    Log.i(TAG, "LocalizationAlgorithms: Continue Experiment");
                    TestTVM.continueExperiment();
                }
            }
        }
    }
}
