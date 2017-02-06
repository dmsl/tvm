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

package cy.ac.ucy.cs.tvm.map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Looper;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Just a fancy rotating marker put on the GoogleMap to show the current position */
public class RotatingMarker extends android.support.v4.app.FragmentActivity {

    private static final String TAG = RotatingMarker.class.getSimpleName();
    private static final int TOTAL_DEGREES = 360;
    private static final int NAV_ICON = R.drawable.direction_pointer;
    App mApp;
    private GoogleMap mMap;
    private Bitmap mOriginalBitmap;
    /** Main dot */
    private Marker mMainMarker;
    /** Show the direction */
    private List<Marker> mPointerMarkers;

    /**
     * A blinking marker, with a custom frequency and fps.
     *
     * @param map - the GoogleMap instance to which the marker is attached
     * @param app - the frequency of the blinking in milliseconds
     */
    public RotatingMarker(GoogleMap map, App app) {
        this.mApp = app;
        this.mMap = map;

        // get the bitmap icon
        mOriginalBitmap = BitmapFactory.decodeResource(app.getResources(), NAV_ICON);
    }

    /**
     * Add markers to map
     *
     * @throws IllegalStateException not in ui thread
     */
    public void addToMap(LatLng position) throws IllegalStateException {
        checkIfUiThread();
        if (mPointerMarkers != null) {
            Log.w(TAG, "Marker was already added.");
            return;
        }

        Geocoder geocoder = new Geocoder(mApp);
        List<Address> address = null;
        try {
            address = geocoder.getFromLocation(position.latitude, position.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String addressString = "";

        try {
            addressString +=
                    ": " + address.get(0).getLocality() + ", " + address.get(0).getCountryName();
        } catch (Exception e) {
            // Noth
        }

        //Add the main marker
        MarkerOptions markerOptions =
                new MarkerOptions()
                        .position(position)
                        .anchor(0.5f, 0.5f)
                        .title("Route Distance: 0")
                        .snippet("Real location" + addressString)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot_no_shadow));
        this.mMainMarker = mMap.addMarker(markerOptions);
        this.mMainMarker.setVisible(true);

        // Add the pointer markers
        mPointerMarkers = new ArrayList<Marker>();
        for (int i = 0; i < TOTAL_DEGREES; i++) {
            mPointerMarkers.add(
                    createNewPointerMarker(generateRotatedBitmap(mOriginalBitmap, i), position));
        }
    }

    /**
     * Removes the marker from the map. It could free up a lot of memory, so use this when you don't
     * need the marker anymore.
     *
     * @throws IllegalStateException - if it isn't called form the UI thread
     */
    public void removeMarker() throws IllegalStateException {
        checkIfUiThread();
        removeMarkers();
    }

    /** Moves the marker to a new position, in sync with the rotating. */
    public void moveMarker(LatLng newPosition) {
        moveMarkers(newPosition);
    }

    private void removeMarkers() {
        if (mPointerMarkers == null) {
            return;
        }

        for (Marker marker : mPointerMarkers) {
            marker.remove();
        }
        mPointerMarkers = null;
    }

    private void moveMarkers(final LatLng newPosition) {

        mMainMarker.setPosition(newPosition);

        for (Marker marker : mPointerMarkers) {
            marker.setPosition(newPosition);
        }
    }

    private void changeMarkerRotation(final int currentID, final int previousID) {
        mPointerMarkers.get(currentID).setVisible(true);
        mPointerMarkers.get(previousID).setVisible(false);
    }

    /** Rotate marker according to heading */
    public void rotateMarker(int currentHeading, int previousHeading) {
        if (currentHeading == previousHeading) return;

        // Swap bitmaps
        changeMarkerRotation(currentHeading, previousHeading);
    }

    /** Create a new pointer marker */
    private Marker createNewPointerMarker(Bitmap bitmap, LatLng position) {
        MarkerOptions markerOptions =
                new MarkerOptions()
                        .position(position)
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        Marker marker = mMap.addMarker(markerOptions);
        marker.setVisible(false);
        return marker;
    }

    /** Generate a new bitmat according to rotation */
    private Bitmap generateRotatedBitmap(Bitmap source, int rotation) {

        Bitmap targetBitmap =
                Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        Matrix matrix = new Matrix();
        matrix.setRotate(rotation, source.getWidth() / 2, source.getHeight() / 2);
        canvas.drawBitmap(source, matrix, new Paint());

        return targetBitmap;
    }

    private void checkIfUiThread() throws IllegalStateException {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("This call has to be made from the UI thread.");
        }
    }

    /** Update distance of main dot in humar readable format */
    public void updateDistance(int realRouteDistance) {
        String res = "";
        if (realRouteDistance > 1000) {
            realRouteDistance /= 1000;
            res = realRouteDistance + " km";
        } else {
            res = realRouteDistance + " m";
        }

        mMainMarker.setTitle("Route Distance: " + res);
    }
}
