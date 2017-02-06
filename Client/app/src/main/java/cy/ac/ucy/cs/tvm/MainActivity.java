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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cy.ac.ucy.cs.tvm.cache.RMapCache;
import cy.ac.ucy.cs.tvm.map.MapUtilities;
import cy.ac.ucy.cs.tvm.tvm.Heading;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;
import cy.ac.ucy.cs.tvm.tvm.simulation.TestTVM;
import cy.ac.ucy.cs.tvm.tvm.simulation.WalkSimulatorClassGenerator;

import static cy.ac.ucy.cs.tvm.App.menuItemFindMe;
import static cy.ac.ucy.cs.tvm.App.menuItemTrackMe;
import static cy.ac.ucy.cs.tvm.R.id.map;
import static cy.ac.ucy.cs.tvm.R.id.textViewMessage2;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.cameraMoving;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.cameraZoom;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.centerMapInCoarseLocation;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.currentFakeLocationMarkers;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.fakeLocationMarkers;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.googleMap;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.initializeMap;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.liveTilt;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.loggerSavedSpots;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.realPositionMarker;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.tiltCamera;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.touchingScreen;
import static cy.ac.ucy.cs.tvm.map.MapUtilities.unveilMatches;
import static cy.ac.ucy.cs.tvm.tvm.CoarseLocation.REQUEST_PERMISSION_COARSE_LOCATION;
import static cy.ac.ucy.cs.tvm.tvm.CoarseLocation.REQUEST_PERMISSION_FINE_LOCATION;

/**
 * MainActivity of the application: Shows a map fragment, with localization options in the Toolbar:
 *
 * <ul>
 *   <li>Find Me (localize once)
 *   <li>Track Me (Continues localization)
 * </ul>
 *
 * It also has a drawer menu in the left side which allows access to quick settings, running
 * experiments, and recording real user routes (with RSS values)
 */
public class MainActivity extends AppCompatActivity {

    public static final int MENU_TRACKME_OFF = R.drawable.ic_navigation_black_24dp;
    private static final int ID_PROGRESS_DIALOG = 0;
    private static final int MENU_TRACKME_ON = R.drawable.ic_near_me_black_24dp;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static Runnable reEnableTil =
            new Runnable() {

                public void run() {
                    touchingScreen = false;
                }
            };
    public static Handler tiltHandler = new Handler();
    public static boolean localized;
    /** */
    static boolean bFind = false;

    static int currentHeading = 0;
    static float currentNadir = 0;
    static int previousHeading = 360;
    private static TextView info;
    private static App app;
    private static int icon = R.drawable.vm_blue_dot_off;
    // Samples list
    private final ArrayList<ArrayList<LogRecord>> samples = new ArrayList<ArrayList<LogRecord>>();
    public MultiBroadcastReceiver mreceiver;
    MenuItem menuItemAppToggle;
    MenuItem menuItemPreferences;
    MenuItem menuItemRecord;
    Marker loggerTempMarker;
    TextView tvDeveloperMode;
    /** Dev purposes. Toggles between positions at UCY */
    private ProgressDialog progressDialog;

    final Handler handler =
            new Handler() {

                public void handleMessage(Message msg) {
                    int total = msg.arg1;
                    progressDialog.setProgress(total);

                    // Check to dismiss
                    if (total >= progressDialog.getMax()) {
                        dismissDialog(ID_PROGRESS_DIALOG);
                        progressDialog.setProgress(0);
                    }
                }
            };
    private String samplesNum = null;
    private String filename_rss, folderPath;
    private SharedPreferences sharedPreferences;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mDrawerItemStrings;

    /**
     * Update labels with current longitude, latitude, heading. These are shown on the bottom left
     * corner of the screen
     */
    public static synchronized void updateDeveloperInfoLabels() {
        StringBuilder sb = new StringBuilder();

        sb.append("H:   ");
        sb.append(currentHeading);

        if (realPositionMarker != null && !touchingScreen) {
            previousHeading = currentHeading;
            currentHeading = (int) Heading.azimuth;
            realPositionMarker.rotateMarker(currentHeading, previousHeading);
        }

        currentNadir = Heading.getNadir();

        // Tilt camera
        if (liveTilt && !cameraMoving && !touchingScreen) {
            tiltCamera(currentNadir);
        }
        sb.append("\nLat: ");
        sb.append(app.currentCoordinates.latitude);
        sb.append("\nLon: ");
        sb.append(app.currentCoordinates.longitude);
        if (info != null) {
            info.setText(sb.toString());
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();
        setContentView(R.layout.activity_main);
        forceOrientation();

        app.layoutTopMessages = (LinearLayout) findViewById(R.id.linearLayoutTopMessages);
        app.textViewMessage1 = (TextView) findViewById(R.id.textViewMessage1);
        app.layoutTopMessages.setBackgroundColor(getColor(R.color.yellow300));
        app.textViewMessage1.setText(getString(R.string.loading));

        // Read shared preferences
        sharedPreferences = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);

        mTitle = mDrawerTitle = getTitle();
        mDrawerItemStrings = getResources().getStringArray(R.array.array_experiments);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        app.textViewMessage2 = (TextView) findViewById(textViewMessage2);
        app.textViewMessage3 = (TextView) findViewById(R.id.textViewMessage3);
        app.textViewMessage4 = (TextView) findViewById(R.id.textViewMessage4);
        tvDeveloperMode = (TextView) findViewById(R.id.tvDeveloper_mode);

        app.progressBarBgTasks = (ProgressBar) findViewById(R.id.progressBarNetwork);
        app.scanAPs = (TextView) findViewById(R.id.detectedAPs);
        app.buttonWalkingSimulator = (Button) findViewById(R.id.buttonPosition2);
        app.buttonUnveilMatches = (Button) findViewById(R.id.buttonUnveilMatches);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        mDrawerList.setAdapter(new CustomNavDrawer(app, app.getListItemObjects()));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawerToggle =
                new ActionBarDrawerToggle(
                        this,
                        mDrawerLayout,
                        mToolbar,
                        R.string.drawer_open,
                        R.string.drawer_close) {
                    public void onDrawerClosed(View view) {
                        getSupportActionBar().setTitle(mTitle);
                        supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    }

                    public void onDrawerOpened(View drawerView) {
                        getSupportActionBar().setTitle(mDrawerTitle);
                        supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    }
                };

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        currentFakeLocationMarkers = new ArrayList<Marker>();

        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(map))
                .getMapAsync(
                        new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(GoogleMap googleMap) {
                                MapUtilities.googleMap = googleMap;

                                initializeMap(MainActivity.this, app);

                                if (app.isTracker()) {
                                    initializeTracker();
                                } else {
                                    initializeLogger();
                                }

                                updateDeveloperInfoLabels();
                            }
                        });

        app.setVpnNotAvailable();
        localized = false;
        app.h = new Heading(this);
        info = (TextView) findViewById(R.id.trackingInfoLabel);

        fakeLocationMarkers = new ArrayList<Marker>();

        // If its not dev mode, hide elements
        toggleDevElements();
        mreceiver = new MultiBroadcastReceiver();

        app.buttonWalkingSimulator.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        // Restart positions
                        if (app.currentSimulationPosition >= 8) {
                            TestTVM.FinalizeWalkingSimulator(app);
                            return;
                        }
                        if (app.currentSimulationPosition <= 0) {
                            TestTVM.InitWalkingSimulator(MainActivity.this, app);
                        }

                        app.currentSimulationPosition++;

                        app.simulateWalking.simulatePosition(app.currentSimulationPosition);
                        Log.e(TAG, "Walking step: " + app.currentSimulationPosition);
                        app.buttonWalkingSimulator.setText("" + app.currentSimulationPosition);
                    }
                });

        //run real simulation
        app.buttonWalkingSimulator.setOnLongClickListener(
                new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {

                        if (app.currentSimulationPosition > 8) {
                            app.currentSimulationPosition = 1;
                        }

                        // Fake N location
                        Log.e(TAG, "Walk simulator. Position: " + app.currentSimulationPosition);
                        app.simulateWalking.simulatePosition(app.currentSimulationPosition);
                        Log.e(TAG, "Walk simulator. Position: " + app.currentSimulationPosition);
                        app.buttonWalkingSimulator.setText("" + app.currentSimulationPosition);

                        return true;
                    }
                });

        //Show the fake and real matches
        app.buttonUnveilMatches.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        localized = false;
                        cameraZoom(2, 500);
                        unveilMatches(app);
                    }
                });

        // Radiomap loader
        app.rmapCache = new RMapCache(getApplicationContext());
        App.sdCardPath = app.rmapCache.getSDCardDirectory();

        if (app.alwaysScan) {
            // Scart localization service
            app.startLocalizationService();
        }

        // Force screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /** Run an experiments, or change a settting */
    private void selectListItem(int position) {

        CustomNavDrawer.ListItemObject item = app.getListItemObject(position);
        mDrawerList.setItemChecked(position, true);

        switch (item.listItemType) {
            case Setting:
                //Change settings
                if (item.key.equals(getString(R.string.pref_key_k_anonymity))) {
                    App.showKAnonymityPickerDialog(
                            this, App.MINIMUM_ANONYMITY, App.MAXIMUM_ANONYMITY);
                } else if (item.key.equals(getString(R.string.pref_key_datasets))) {
                    App.showChangeDatasetDialog(this);
                }

                //If its a setting unckeck it!
                mDrawerList.setItemChecked(position, false);
                break;
            case Experiment:
                // Series 1
                if (item.key.equals("series1")) {
                    try {
                        Intent i;
                        PackageManager manager = getPackageManager();
                        i = manager.getLaunchIntentForPackage(App.PACKAGE_POWER_TUTOR);
                        i.addCategory(Intent.CATEGORY_LAUNCHER);
                        startActivity(i);

                        app.showDialogRunExperimentSeries1(this);

                        app.showToast("Press Start Profiler");

                    } catch (NullPointerException e) {
                        app.showToast("Running experiments requires PowerTutor!");
                    }
                } else if (item.key.equals("buildroutes")) {

                    //Stop saving values, export to sd card the data
                    if (WalkSimulatorClassGenerator.recordingRoute) {
                        app.showToast("Route (RSS values) stored on SD card");
                        app.buildValuesClassInSdCard();
                        if (app.alwaysScanWasOff) {
                            app.alwaysScanWasOff = false;
                            app.alwaysScan = false; //disable it
                        }
                    } else { //start saving values
                        app.showToast("When done, press again to store route.");

                        if (!app.alwaysScan) {
                            app.alwaysScanWasOff = true;
                            app.alwaysScan = true; //enable it
                        }
                        WalkSimulatorClassGenerator.init(app);
                    }
                }
                break;
        }
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void initializeTracker() {
        app.textViewMessage1.setText(getString(R.string.tracker));
        app.layoutTopMessages.setBackgroundColor(getColor(R.color.teal600));
        googleMap.setOnMapClickListener(
                new OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng point) {
                        return; // do noth!
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void initializeLogger() {
        app.textViewMessage1.setText(getString(R.string.logger));
        app.layoutTopMessages.setBackgroundColor(getColor(R.color.teal600));
        loggerSavedSpots = new ArrayList<Marker>();
        app.currentCoordinates = new LatLng(0, 0);
        googleMap.setOnMapClickListener(
                new OnMapClickListener() {

                    @Override
                    public void onMapClick(LatLng point) {

                        // Update coordinates
                        app.currentCoordinates = point;
                        updateDeveloperInfoLabels();

                        // Draw marker on this point

                        // If previous point exists, remove it
                        if (loggerTempMarker != null) {
                            loggerTempMarker.setPosition(point);
                            return;
                        }

                        // Add marker
                        loggerTempMarker =
                                googleMap.addMarker(
                                        new MarkerOptions()
                                                .position(point)
                                                .anchor(0.5f, 0.5f)
                                                .icon(
                                                        BitmapDescriptorFactory.fromResource(
                                                                R.drawable
                                                                        .vm_blue_dot_obscured_on)));
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Restart localization service
        if (app.isTrackmeEnabled()) {
            // Stop wifi manager
            app.lightWifiManager.stop();

            // Stop Localization Service
            app.stopLocalizationService();
        }
        app.h.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart localization service
        if (app.isTrackmeEnabled() || app.alwaysScan) {
            app.lightWifiManager.start();
            app.startLocalizationService();
        }
        updateDeveloperInfoLabels();
        app.h.resume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Restart localization service
        if (app.isTrackmeEnabled() || app.alwaysScan) {
            app.lightWifiManager.start();
            app.startLocalizationService();
        }

        // If its not dev mode, hide elements
        toggleDevElements();
        app.setVpnNotAvailable();
        updateDeveloperInfoLabels();
    }

    /** Hide/Show developer elements */
    private void toggleDevElements() {
        LinearLayout layoutBottomMessages = (LinearLayout) findViewById(R.id.linearLayoutDownStats);

        if (app.realSimulation) {
            app.buttonWalkingSimulator.setVisibility(View.VISIBLE);
        } else {
            app.buttonWalkingSimulator.setVisibility(View.INVISIBLE);
        }

        if (app.developerMode) {
            layoutBottomMessages.setVisibility(View.VISIBLE);
            tvDeveloperMode.setVisibility(View.VISIBLE);
        } else {
            layoutBottomMessages.setVisibility(View.INVISIBLE);
            tvDeveloperMode.setVisibility(View.INVISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        app.layoutTopMessages.setBackgroundColor(app.getColor(R.color.teal600));

        switch (item.getItemId()) {
            case android.R.id.home: // drawer
                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                    mDrawerLayout.closeDrawer(mDrawerList);
                } else {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
                return true;
                // Find me
            case R.id.itemFindMe:
                app.startLocalizationService();
                app.setTrackmeOff();
                app.setFindmeOn();
                bFind = true;
                menuItemTrackMe.setChecked(false);
                menuItemTrackMe.setIcon(MENU_TRACKME_OFF);

                break;
            case R.id.itemTrackMe:

                // Disable track me
                if (menuItemTrackMe.isChecked()) {
                    app.setTrackmeOff();
                    menuItemTrackMe.setIcon(MENU_TRACKME_OFF);
                    menuItemTrackMe.setChecked(false);
                    app.stopLocalizationService();
                }
                // Enable track me
                else {
                    icon = R.drawable.nav_notification_icon_active;
                    app.startLocalizationService();
                    app.setTrackmeOn();
                    menuItemTrackMe.setIcon(MENU_TRACKME_ON);
                    menuItemTrackMe.setChecked(true);
                }
                break;
            case R.id.itemAppToggle:

                // Remove markers
                initializeMap(MainActivity.this, app);
                localized = false;

                // Switch app in Logger Mode
                if (app.isTracker()) {
                    app.appType = App.AppType.Logger;
                    menuItemRecord.setVisible(true);
                    menuItemFindMe.setVisible(false);
                    menuItemTrackMe.setVisible(false);
                    menuItemAppToggle.setTitle(
                            "Mode: " + getApplicationContext().getString(R.string.tracker));
                    initializeLogger();
                }
                // Switch app in Tracker Mode
                else {
                    app.appType = App.AppType.Tracker;
                    menuItemRecord.setVisible(false);
                    menuItemFindMe.setVisible(true);
                    menuItemTrackMe.setVisible(true);
                    menuItemAppToggle.setTitle(
                            "Mode: " + getApplicationContext().getString(R.string.logger));
                    initializeTracker();
                }
                break;

            case R.id.itemPreferences:
                // Show preferences activity
                Intent prefs = new Intent(MainActivity.this, Preferences.class);
                startActivity(prefs);
                break;

            case R.id.itemRecord:
                startWifiRecording();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        app.lightWifiManager.start();

        // Get menu items
        menuItemFindMe = menu.findItem(R.id.itemFindMe);
        menuItemTrackMe = menu.findItem(R.id.itemTrackMe);
        menuItemAppToggle = menu.findItem(R.id.itemAppToggle);
        menuItemPreferences = menu.findItem(R.id.itemPreferences);
        menuItemRecord = menu.findItem(R.id.itemRecord);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Create the dialog
     *
     * @param id the id of dialog to create
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ID_PROGRESS_DIALOG:
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("Scanning in progress...");
                progressDialog.setCancelable(false);
                return progressDialog;
            default:
                return null;
        }
    }

    /**
     * Prepare the dialog. Sets progress to 0 and max to samples number
     *
     * @param id the id of dialog to prepare
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case ID_PROGRESS_DIALOG:
                progressDialog.setProgress(0);
                progressDialog.setMax(Integer.parseInt(samplesNum));
                break;
        }
    }

    public void startWifiRecording() {

        samplesNum = sharedPreferences.getString("samplesNum", "");
        if (samplesNum.trim().equals("")) {
            app.showToast(
                    "Samples number not specified\n"
                            + "Go to Menu::Preferences::"
                            + "Sampling Settings::Samples Number");

            return;
        } else if ((!(new File(folderPath).canWrite()))) {

            app.showToast(
                    "Folder path is not writable\n"
                            + "Go to Menu::Preferences::"
                            + "Storing Settings::Folder");

            return;
        }

        filename_rss = (String) sharedPreferences.getString("filename_log", "");
        if (filename_rss.equals("")) {

            app.showToast(
                    "Filename of RSS log not specified\n"
                            + "Go to Menu::Preferences::Storing "
                            + "Settings::Filename");

            return;
        }

        folderPath = app.rmapCache.getSDCardDirectory();
        menuItemRecord.setCheckable(false);
        app.textViewMessage1.setText("Start Recording WiFi Fingerprint");
        menuItemRecord.setCheckable(false);
        showDialog(ID_PROGRESS_DIALOG);
    }

    public void writeToLog() {
        String header = "# Timestamp, X, Y, HEADING, MAC Address of AP, RSS\n";
        ArrayList<LogRecord> writeRecords;
        LogRecord writeLR;
        int N;
        synchronized (samples) {
            try {
                Boolean write_mode = sharedPreferences.getBoolean("write_mode", false);

                N = Integer.parseInt(samplesNum);
                File root = new File(folderPath);

                if (root.canWrite()) {
                    FileOutputStream fos =
                            new FileOutputStream(new File(root, filename_rss), !write_mode);

                    for (int i = 0; i < N; ++i) {
                        fos.write(header.getBytes());
                        writeRecords = samples.get(i);
                        for (int j = 0; j < writeRecords.size(); ++j) {
                            writeLR = writeRecords.get(j);
                            fos.write(writeLR.toString().getBytes());
                        }
                    }
                    if (!samples.isEmpty()) {
                        samples.clear();
                        menuItemRecord.setCheckable(true);
                    }

                    fos.close();
                }
            } catch (ClassCastException cce) {
                app.showToast("Error: " + cce.getMessage());
            } catch (NumberFormatException nfe) {
                app.showToast("Error: " + nfe.getMessage());
            } catch (FileNotFoundException fnfe) {
                app.showToast("Error: " + fnfe.getMessage());
            } catch (IOException ioe) {
                app.showToast("Error: " + ioe.getMessage());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_COARSE_LOCATION:
            case REQUEST_PERMISSION_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    centerMapInCoarseLocation(app);
                    return;
                }
        }
    }

    /** Force orientation according to device type (smartphone, tablet) */
    public void forceOrientation() {
        if (app.isTablet()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectListItem(position);
        }
    }

    /**
     * Updates the UI w/ information regarding: widi access points, heading, latitude, and longitude
     * This information is shown at the bottom of the google map when developer mode is enabled.
     */
    public class SimpleWifiReceiverLogger extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent == null || context == null || intent.getAction() == null) return;

                List<ScanResult> wifiList = app.lightWifiManager.getScanResults();
                app.scanAPs.setText("AP:  " + wifiList.size());
                if (menuItemRecord.isCheckable()) return;

                ArrayList<LogRecord> Records = new ArrayList<LogRecord>();
                Records.clear();

                Date date = new Date();
                long timestamp = date.getTime();

                if ((app.currentCoordinates.longitude != 0 && app.currentCoordinates.latitude != 0)
                        && !wifiList.isEmpty()) {

                    for (int i = 0; i < wifiList.size(); i++) {

                        LogRecord lr =
                                new LogRecord(
                                        timestamp,
                                        app.currentCoordinates.latitude,
                                        app.currentCoordinates.longitude,
                                        Heading.azimuth,
                                        wifiList.get(i).BSSID,
                                        wifiList.get(i).level);
                        Records.add(lr);
                    }

                    synchronized (samples) {
                        samples.add(0, Records);

                        Message msg = handler.obtainMessage();
                        msg.arg1 = samples.size();
                        handler.sendMessage(msg);

                        if (samples.size() >= Integer.parseInt(samplesNum)) {
                            writeToLog();
                            // If sammples successfully written
                            // hide temp marker
                            loggerTempMarker.remove();
                            loggerTempMarker = null;

                            // Add new marker to googleMap
                            googleMap.addMarker(
                                    new MarkerOptions()
                                            .position(app.currentCoordinates)
                                            .anchor(0.5f, 0.5f)
                                            .icon(
                                                    BitmapDescriptorFactory.fromResource(
                                                            R.drawable.vm_red_dot_obscured_on)));
                        }
                    }
                }
            } catch (RuntimeException e) {
                return;
            }
        }
    }
}
