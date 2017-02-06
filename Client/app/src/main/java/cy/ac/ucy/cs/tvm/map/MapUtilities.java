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

import static cy.ac.ucy.cs.tvm.MainActivity.localized;
import static cy.ac.ucy.cs.tvm.MainActivity.tiltHandler;
import static cy.ac.ucy.cs.tvm.tvm.CoarseLocation.REQUEST_PERMISSION_COARSE_LOCATION;
import static cy.ac.ucy.cs.tvm.tvm.CoarseLocation.REQUEST_PERMISSION_FINE_LOCATION;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.MainActivity;
import cy.ac.ucy.cs.tvm.R;
import cy.ac.ucy.cs.tvm.tvm.CoarseLocation;
import cy.ac.ucy.cs.tvm.tvm.LocalizationAlgorithms;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains everything related to the GoogleMap instance and GeoLocation API. adding/removing
 * markers, drawing routes, calculating dinstances, fancy animations, etc.
 */
public class MapUtilities {

    private static final String TAG = MapUtilities.class.getSimpleName();
    private static final int MEDIUM_ZOOM = 14;
    private static final int MIN_ZOOM = 5;
    public static int MAX_ZOOM = 20;
    public static boolean liveTilt = true;
    public static boolean touchingScreen = false;
    public static boolean cameraMoving = false;
    public static GoogleMap googleMap;
    public static RotatingMarker realPositionMarker;
    public static boolean gotCoarseLocation = false;
    public static double realRouteDistance = 0;
    public static GroundOverlay ucyBuildingGroundOverlay;
    public static ArrayList<Marker> fakeLocationMarkers;
    public static ArrayList<Marker> currentFakeLocationMarkers;
    public static ArrayList<Marker> loggerSavedSpots;
    private static double[] fakeRouteLengths;
    /** Real routes on googleMap */
    static ArrayList<Polyline> realRoutePolyline = new ArrayList<Polyline>();

    static CameraPosition previousCameraPosition;

    /** Center the googleMap in coarse location. Run on application init */
    public static void centerMapInCoarseLocation(App app) {

        // Show the marker in the center of the world!
        realPositionMarker = new RotatingMarker(googleMap, app);
        realPositionMarker.addToMap(new LatLng(0, 0));

        //Try to get coarse location from Google's Location Services using WiFi
        CoarseLocation.LocationResult locationResult =
                new CoarseLocation.LocationResult() {

                    @Override
                    public void gotLocation(Location location) {
                        gotCoarseLocation = true;

                        try {
                            // Zoom googleMap to the location
                            double gotLat = location.getLatitude();
                            double gotLon = location.getLongitude();

                            previousCameraPosition = googleMap.getCameraPosition();

                            fancyMoveToInitialLocation(
                                    new LatLng(gotLat, gotLon),
                                    500,
                                    (int) previousCameraPosition.zoom,
                                    MEDIUM_ZOOM);

                            //move to coarse location
                            realPositionMarker.moveMarker(new LatLng(gotLat, gotLon));

                        } catch (NullPointerException e) {
                            //failed to get coarse location
                        }
                    }
                };

        CoarseLocation myLocation = new CoarseLocation();
        myLocation.getLocation(app.getApplicationContext(), locationResult);
    }

    private static void fancyMoveToInitialLocation(
            final LatLng ll, final int time, final int zoomFrom, final int zoomTo) {

        // Zoom googleMap to coarse location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, zoomFrom));

        tiltHandler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {
                        cameraMoving = true;
                        tiltHandler.removeCallbacks(MainActivity.reEnableTil);

                        // Zoom in, animating the camera.
                        googleMap.animateCamera(
                                CameraUpdateFactory.zoomTo(zoomTo),
                                time,
                                new GoogleMap.CancelableCallback() {

                                    @Override
                                    public void onFinish() {
                                        cameraMoving = false;
                                    }

                                    @Override
                                    public void onCancel() {
                                        cameraMoving = false;
                                    }
                                });
                    }
                },
                1000);
    }

    /** Show marker according to user movements on googleMap */
    public static void updateMap(App app) {

        // Re-center googleMap
        if (!localized) {
            moveMapFromLocationToLocation(app.currentCoordinates, 200, 1000, MIN_ZOOM, MAX_ZOOM);
        }

        try {

            realPositionMarker.moveMarker(app.currentCoordinates);
            // if previous coordinates exist, draw real route
            if (app.drawRoutes && app.previousCoordinates.latitude != 0) {

                // Clear latest previous point from routes
                for (int i = 0; i < realRoutePolyline.size(); i++) {
                    LatLng prev = realRoutePolyline.get(i).getPoints().get(0);

                    if (prev.equals(app.previousCoordinates)) {
                        realRoutePolyline.get(i).setVisible(false);
                    }
                }

                // Calculate route dinstance
                realRouteDistance +=
                        LocalizationAlgorithms.distance(
                                app.previousCoordinates, app.currentCoordinates);

                realPositionMarker.updateDistance((int) realRouteDistance);

                // Real route polyline
                realRoutePolyline.add(
                        googleMap.addPolyline(
                                new PolylineOptions()
                                        .add(app.previousCoordinates, app.currentCoordinates)
                                        .width(5)
                                        .color(
                                                app.getResources()
                                                        .getColor(android.R.color.holo_blue_dark))
                                        .geodesic(true)));

                int fakesSize = app.fakeMatchedLocations.size();

                if (fakesSize >= 2) {

                    ArrayList<LatLng> curFakeLocs = app.fakeMatchedLocations.get(fakesSize - 1);

                    ArrayList<LatLng> prevFakeLocs = app.fakeMatchedLocations.get(fakesSize - 2);

                    int size =
                            curFakeLocs.size() < prevFakeLocs.size()
                                    ? curFakeLocs.size()
                                    : prevFakeLocs.size();

                    for (int i = 0; i < curFakeLocs.size(); i++) {
                        LatLng prevFakeLoc = prevFakeLocs.get(i);
                        LatLng curFakeLoc = curFakeLocs.get(i);

                        // Draw last 2 locs
                        googleMap.addPolyline(
                                new PolylineOptions()
                                        .add(prevFakeLoc, curFakeLoc)
                                        .width(5)
                                        .color(Color.RED)
                                        .geodesic(true));
                    }
                }
            }

        } catch (NullPointerException e) {
            // Noth - ignore first localization line draw
        }

        localized = true;
    }

    /** Fancy animation to move the map between two points */
    public static synchronized void moveMapFromLocationToLocation(
            final LatLng ll,
            final int timeZoomOut,
            final int timeZoomIn,
            final int zoomOutTo,
            final int zoomInTo) {

        cameraMoving = true;
        tiltHandler.removeCallbacks(MainActivity.reEnableTil);

        previousCameraPosition = googleMap.getCameraPosition();

        if ((int) previousCameraPosition.zoom > zoomOutTo) {

            // Zoom out camera
            googleMap.animateCamera(
                    CameraUpdateFactory.zoomTo(zoomOutTo),
                    timeZoomOut,
                    new GoogleMap.CancelableCallback() {

                        @Override
                        public void onFinish() {
                            // Zoom googleMap to coarse location
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, zoomOutTo));

                            // Zoom in, animating the camera.
                            googleMap.animateCamera(
                                    CameraUpdateFactory.zoomTo(zoomInTo),
                                    timeZoomIn,
                                    new GoogleMap.CancelableCallback() {

                                        @Override
                                        public void onFinish() {
                                            cameraMoving = false;
                                        }

                                        @Override
                                        public void onCancel() {
                                            cameraMoving = false;
                                        }
                                    });
                        }

                        @Override
                        public void onCancel() {
                            cameraMoving = false;
                        }
                    });
        } else {
            // Zoom googleMap to coarse location
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, zoomOutTo));

            // Zoom in, animating the camera.
            googleMap.animateCamera(
                    CameraUpdateFactory.zoomTo(zoomInTo),
                    timeZoomIn,
                    new GoogleMap.CancelableCallback() {

                        @Override
                        public void onFinish() {
                            cameraMoving = false;
                        }

                        @Override
                        public void onCancel() {
                            cameraMoving = false;
                        }
                    });
        }
    }

    private static synchronized void moveMapToNewCoordinate(LatLng ll, int timeZoomOut) {
        cameraMoving = true;
        tiltHandler.removeCallbacks(MainActivity.reEnableTil);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, timeZoomOut));
        googleMap.animateCamera(
                CameraUpdateFactory.zoomTo(MAX_ZOOM),
                1,
                new GoogleMap.CancelableCallback() {

                    @Override
                    public void onFinish() {
                        cameraMoving = false;
                    }

                    @Override
                    public void onCancel() {
                        cameraMoving = false;
                    }
                });
    }

    private static synchronized void moveMapToNewCoordinate(
            LatLng ll, int timeZoom, int zoomLevel) {

        cameraMoving = true;
        tiltHandler.removeCallbacks(MainActivity.reEnableTil);

        previousCameraPosition = googleMap.getCameraPosition();

        // Zoom googleMap to coarse location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, previousCameraPosition.zoom));

        googleMap.animateCamera(
                CameraUpdateFactory.zoomTo(zoomLevel),
                timeZoom,
                new GoogleMap.CancelableCallback() {

                    @Override
                    public void onFinish() {
                        cameraMoving = false;
                    }

                    @Override
                    public void onCancel() {
                        cameraMoving = false;
                    }
                });
    }

    /** Zoom out camera, and then zoom in */
    public static synchronized void cameraZoom(int zoomLevel, int time) {

        cameraMoving = true;
        tiltHandler.removeCallbacks(MainActivity.reEnableTil);

        previousCameraPosition = googleMap.getCameraPosition();

        // Zoom googleMap to coarse location
        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        previousCameraPosition.target, previousCameraPosition.zoom));

        googleMap.animateCamera(
                CameraUpdateFactory.zoomTo(zoomLevel),
                time,
                new GoogleMap.CancelableCallback() {

                    @Override
                    public void onFinish() {
                        cameraMoving = false;
                    }

                    @Override
                    public void onCancel() {
                        cameraMoving = false;
                    }
                });
    }

    public static synchronized void tiltCamera(float nadir) {
        if (googleMap == null) return; // map not ready yet

        nadir = (int) (nadir / 1.5);
        if (nadir < 0) nadir *= -1;
        if (nadir > 45) nadir = 45;
        previousCameraPosition = googleMap.getCameraPosition();

        // INFO bearing is the horizontal
        CameraPosition currentPlace =
                new CameraPosition.Builder()
                        .tilt(nadir)
                        .target(previousCameraPosition.target)
                        .zoom(previousCameraPosition.zoom)
                        .bearing(previousCameraPosition.bearing)
                        .build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void initializeMap(Context ctx, App app) {
        MapUtilities.googleMap.getUiSettings().setZoomControlsEnabled(false);
        MapUtilities.googleMap.setOnMarkerClickListener(
                new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        LatLng to = marker.getPosition();
                        moveMapFromLocationToLocation(to, 50, 100, MEDIUM_ZOOM, 22);
                        return false;
                    }
                });
        googleMap.clear(); // remove any routes, markers, etc

        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        googleMap.setPadding(5, 200, 0, 0);

        app.layoutTopMessages.setBackgroundColor(ctx.getColor(R.color.teal600));

        BitmapDescriptor image =
                BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(
                                app.getResources(), R.drawable.cs_floor2)); // get an image.

        // Ucy building
        ucyBuildingGroundOverlay =
                googleMap.addGroundOverlay(
                        new GroundOverlayOptions()
                                .image(image)
                                .anchor(0, 0)
                                .bearing(51.3f)
                                .transparency(0.4f)
                                .position(App.CS_UCY_0_0, 76.5f, 39.75f));

        if (ActivityCompat.checkSelfPermission(
                                (Activity) ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                                (Activity) ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    (Activity) ctx,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSION_COARSE_LOCATION);
            ActivityCompat.requestPermissions(
                    (Activity) ctx,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_FINE_LOCATION);
        } else {
            centerMapInCoarseLocation(app);
        }
    }

    public static void unveilMatches(App app) {
        // Clear previous markers
        for (int i = 0; i < currentFakeLocationMarkers.size(); i++) {
            currentFakeLocationMarkers.get(i).remove();
        }
        currentFakeLocationMarkers.clear();

        if (app.fakeMatchedLocations.size() >= 1) {

            // Calculate fake route lengths
            fakeRouteLengths = new double[app.fakeMatchedLocations.get(0).size()];

            for (int j = 0; j < app.fakeMatchedLocations.size() - 1; j++) {
                ArrayList<LatLng> cur = app.fakeMatchedLocations.get(j);
                ArrayList<LatLng> next = app.fakeMatchedLocations.get(j + 1);

                for (int i = 0; i < fakeRouteLengths.length; i++) {
                    fakeRouteLengths[i] += LocalizationAlgorithms.distance(cur.get(i), next.get(i));
                }
            }

            ArrayList<LatLng> tmpLatestPoints =
                    app.fakeMatchedLocations.get(app.fakeMatchedLocations.size() - 1);

            // Show new fake position markers
            for (int i = 0; i < tmpLatestPoints.size(); i++) {

                Geocoder geocoder = new Geocoder(app);
                List<Address> address = null;
                try {
                    address =
                            geocoder.getFromLocation(
                                    tmpLatestPoints.get(i).latitude,
                                    tmpLatestPoints.get(i).longitude,
                                    1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String addressString = "";

                ArrayList<String> tmpLengths = new ArrayList<String>();
                try {

                    String locality = address.get(0).getLocality();
                    String countryName = address.get(0).getCountryName();

                    if (locality == null) locality = "";
                    if (countryName == null) countryName = "";

                    addressString += ": " + locality + ", " + countryName;

                    // init lens
                    for (int j = 0; j < app.fakeMatchedLocations.get(0).size(); j++) {
                        tmpLengths.add("0");
                    }

                    for (int j = 0; j < fakeRouteLengths.length; j++) {

                        int len = (int) fakeRouteLengths[j];
                        String res = "";
                        if (len > 1000) {
                            len /= 1000;
                            res = len + " km";
                        } else {
                            res = len + " m";
                        }

                        tmpLengths.set(j, res);
                    }

                    currentFakeLocationMarkers.add(
                            googleMap.addMarker(
                                    new MarkerOptions()
                                            .position(tmpLatestPoints.get(i))
                                            .anchor(0.5f, 0.5f)
                                            .title("Route distance: " + tmpLengths.get(i))
                                            .snippet("Fake location" + addressString)
                                            .icon(
                                                    BitmapDescriptorFactory.fromResource(
                                                            R.drawable.vm_red_dot_obscured_on))));
                } catch (Exception e) {
                    // Noth
                }
            }
        }
    }
}
