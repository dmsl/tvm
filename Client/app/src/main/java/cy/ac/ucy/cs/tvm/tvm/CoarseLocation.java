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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import java.util.Timer;

/** Getting Coarse Location readings of the user */
public class CoarseLocation {

    public static final int REQUEST_PERMISSION_COARSE_LOCATION = 1;
    public static final int REQUEST_PERMISSION_FINE_LOCATION = 2;

    Timer timer1;
    LocationManager lm;
    LocationResult locationResult;
    boolean network_enabled = false;

    public boolean getLocation(Context context, LocationResult result) {
        //I use LocationResult callback class to pass location value from MyLocation to user code.
        locationResult = result;

        // permissions not acquired yet
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        if (lm == null) lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        //don't start listeners if no provider is enabled
        if (!network_enabled) return false;

        if (network_enabled) {
            lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        }

        timer1 = new Timer();

        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable myRunnable =
                new Runnable() {

                    @Override
                    public void run() {
                        lm.removeUpdates(locationListenerGps);
                        lm.removeUpdates(locationListenerNetwork);

                        Location net_loc = null, gps_loc = null;

                        if (network_enabled)
                            net_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        //if there are both values use the latest one
                        if (gps_loc != null && net_loc != null) {
                            if (gps_loc.getTime() > net_loc.getTime())
                                locationResult.gotLocation(gps_loc);
                            else locationResult.gotLocation(net_loc);
                            return;
                        }

                        if (gps_loc != null) {
                            locationResult.gotLocation(gps_loc);
                            return;
                        }
                        if (net_loc != null) {
                            locationResult.gotLocation(net_loc);
                            return;
                        }
                        locationResult.gotLocation(null);
                    }
                };

        mainHandler.post(myRunnable);

        return true;
    }

    LocationListener locationListenerGps =
            new LocationListener() {

                public void onLocationChanged(Location location) {
                    timer1.cancel();
                    locationResult.gotLocation(location);
                    lm.removeUpdates(this);
                    lm.removeUpdates(locationListenerNetwork);
                }

                public void onProviderDisabled(String provider) {}

                public void onProviderEnabled(String provider) {}

                public void onStatusChanged(String provider, int status, Bundle extras) {}
            };

    LocationListener locationListenerNetwork =
            new LocationListener() {
                public void onLocationChanged(Location location) {
                    timer1.cancel();
                    locationResult.gotLocation(location);
                    lm.removeUpdates(this);
                    lm.removeUpdates(locationListenerGps);
                }

                public void onProviderDisabled(String provider) {}

                public void onProviderEnabled(String provider) {}

                public void onStatusChanged(String provider, int status, Bundle extras) {}
            };

    public abstract static class LocationResult {
        public abstract void gotLocation(Location location);
    }
}
