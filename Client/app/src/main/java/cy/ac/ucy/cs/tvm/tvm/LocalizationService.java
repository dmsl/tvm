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

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.MultiBroadcastReceiver;

/**
 * Localization Service:
 *
 * <ul>
 *   <li>Collect listening AP
 *   <li>Received broadcast when listening AP are ready
 *   <li>Get partial radiomap of strongest cached mac according to enabled caches, and anonymous
 *       algorithm
 *   <li>Parse partial radiomap
 *   <li>Localize user according to localization algorithm
 * </ul>
 */
public class LocalizationService extends Service {

    private static final String TAG = LocalizationService.class.getSimpleName();

    LocalizationThread localizationThread;
    public static boolean isRunning = false;
    App app;
    public static MultiBroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        localizationThread = new LocalizationThread();

        app = (App) getApplication();
        receiver = new MultiBroadcastReceiver();
    }

    @Override
    public synchronized void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        // Start user locator
        if (!isRunning) {
            isRunning = true;
            localizationThread.start();
            Log.i(TAG, "Localization Service started");
        }
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();

        // Kill locator service
        if (isRunning) localizationThread.interrupt();

        try {
            unregisterReceiver(receiver);

        } catch (Exception e) {
            Log.e(TAG, "Problem unregister receiver: " + e.getMessage());
        }

        Log.i(TAG, "Localization Service Stopped");
    }

    public class LocalizationThread extends Thread {
        @Override
        public void run() {

            registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            while (isRunning) { // wifi scanner runs
                try {
                    // Get radiomap
                    if (!app.isInBgProgress()) {
                        // Send broadcast to scan for nearest AP's
                        app.lightWifiManager.scanArea();
                    }
                    Thread.sleep(app.wifiScanInterval);
                } catch (InterruptedException e) {
                    // Thread interrupted
                    isRunning = false;
                }
            }
        }
    }

    /** @return true if WifiBgService is running otherwise false */
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
