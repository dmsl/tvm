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

package cy.ac.ucy.cs.tvm.tvm.simulation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.AsyncTaskHttpExecutor;
import cy.ac.ucy.cs.tvm.MainActivity;
import java.util.ArrayList;

/** The experimental series used for the TVM paper. */
public class TestTVM {

    private static final String TAG = TestTVM.class.getSimpleName();
    public static ArrayList<Integer> positions;
    public static App sApp;
    public static MainActivity sMainActivity;
    public static ArrayList<Test_DAO> times = new ArrayList<Test_DAO>();
    public static Test_DAO currentTime;

    private static WalkSimulator walkSimulator;
    public static final int EXP_ID_SERIES_1 = 1;
    public static final int EXP_ID_WALKING_SIMULATOR = 0;

    public static void FinalizeWalkingSimulator(App app) {
        app.showToast("Walking simulator finished.");
        app.currentSimulationPosition = 0;
        app.buttonWalkingSimulator.setText("" + app.currentSimulationPosition);
        App.menuItemTrackMe.setEnabled(true);
        App.menuItemFindMe.setEnabled(true);
    }

    public static void InitWalkingSimulator(MainActivity mainActivity, App app) {
        app.currentSimulationPosition = 0;
        TestTVM.sMainActivity = mainActivity;
        TestTVM.sApp = app;
        App.menuItemTrackMe.setEnabled(false);
        App.menuItemFindMe.setEnabled(false);

        walkSimulator = new WalkAtUcy_8(app);
        currentTime = new Test_DAO(sApp, EXP_ID_WALKING_SIMULATOR, walkSimulator.routeName);

        app.singleStepSimulation = true;
    }

    /** K Anon 3,5,7,10 in dataset 1 */
    public static void RunExperiment(MainActivity mainActivity, App app) {
        InitWalkingSimulator(mainActivity, app);

        sApp.showToast("Real navigation disabled.\nRunning experiment 1/TVM");
        startExperiment();
    }

    private static void startExperiment() {
        sApp.initExperimentSeries1();

        sApp.singleStepSimulation = false;

        // INFO this is how we change rootes
        // eg 1 route simulateWalking = new SimulateMovement_UCY1(sApp);
        walkSimulator = new WalkAtUcy_300(sApp);

        // create and save current timings
        currentTime = new Test_DAO(sApp, EXP_ID_SERIES_1, walkSimulator.routeName);
        times.add(currentTime);

        sApp.showToast("K anonymity: " + sApp.getkAnonymity() + " Dataset: " + sApp.datasetInUse);

        //Save initial values
        currentTime.position = sApp.currentSimulationPosition;

        // start realAutoSimulation// Fake N location
        walkSimulator.simulatePosition(sApp.currentSimulationPosition);
    }

    /** */
    public static void continueExperiment() {

        // single stepping handles position increases elsewhere
        if (!sApp.singleStepSimulation) {
            sApp.currentSimulationPosition++;
        }
        sApp.showToast(
                "Position : " + sApp.currentSimulationPosition + "/" + walkSimulator.max_positions);
        Log.e(
                TAG,
                "Position : " + sApp.currentSimulationPosition + "/" + walkSimulator.max_positions);

        if (sApp.currentSimulationPosition > walkSimulator.max_positions) {
            // Submit results
            String parameters[] = new String[1];
            String params = currentTime.getHttpGetRequestParameters();
            Log.e(TAG, "Parameters: " + params);
            parameters[0] = params;

            // Save the data to server
            new AsyncTaskHttpExecutor(
                            sApp,
                            App.getExperimentsURL(),
                            App.AsyncTaskType.saveExperimentData,
                            parameters,
                            null)
                    .execute();

            sApp.runningExperiment = false; //experiment finished!

            Intent i;
            PackageManager manager = sMainActivity.getPackageManager();
            i = manager.getLaunchIntentForPackage(App.PACKAGE_POWER_TUTOR);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            sMainActivity.startActivity(i);

            sApp.showToast("Press Stop Profiler, and then\n Save log\nApplication now stops!");
            sMainActivity.finish(); //terminate this activity! (so no battery is wasted!)
            return;
        }

        if (!sApp.singleStepSimulation) { // continue
            walkSimulator.simulatePosition(sApp.currentSimulationPosition);
        }
    }
}
