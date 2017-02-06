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

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

import cy.ac.ucy.cs.tvm.App;

/**
 * An interface to the Wifi hardware of the device:
 *
 * <ul>
 *   <li>starts wifi
 *   <li>stops wifi
 *   <li>returns wifi scan results
 * </ul>
 */
public class LightWifiManager {

    public static final String TAG = LightWifiManager.class.getSimpleName();
    App app;
    WifiManager mainWifi;

    public LightWifiManager(App app) {
        this.app = app;

        mainWifi = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        app.wasWifiEnabled = mainWifi.isWifiEnabled();
    }

    public void start() {
        enableWifi();
    }

    public void stop() {
        disableWifi();
    }

    public void scanArea() {
        mainWifi.startScan();
    }

    private void disableWifi() {
        if (!app.wasWifiEnabled) {
            Log.i(TAG, "Disabling wifi");
            if (mainWifi.isWifiEnabled())
                if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_DISABLING)
                    mainWifi.setWifiEnabled(false);
        }
    }

    private void enableWifi() {
        if (!mainWifi.isWifiEnabled())
            if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
                mainWifi.setWifiEnabled(true);
    }

    public List<ScanResult> getScanResults() {
        return mainWifi.getScanResults();
    }
}
