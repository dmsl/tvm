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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import cy.ac.ucy.cs.tvm.Bloomfilter.Bloomfilter;
import cy.ac.ucy.cs.tvm.cache.RMapCache;
import cy.ac.ucy.cs.tvm.tvm.Heading;
import cy.ac.ucy.cs.tvm.tvm.LightWifiManager;
import cy.ac.ucy.cs.tvm.tvm.LocalizationAlgorithms;
import cy.ac.ucy.cs.tvm.tvm.LocalizationService;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;
import cy.ac.ucy.cs.tvm.tvm.simulation.TestTVM;
import cy.ac.ucy.cs.tvm.tvm.simulation.WalkAtUcy_8;
import cy.ac.ucy.cs.tvm.tvm.simulation.WalkSimulator;
import cy.ac.ucy.cs.tvm.tvm.simulation.WalkSimulatorClassGenerator;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Application class. All information that should be shared between activities/classes is kept here.
 */
public class App extends Application {

    public static final String URL_VCENTER = "http://vcenter.in.cs.ucy.ac.cy";
    public static final String URL_SERVER = "http://vectormap1.in.cs.ucy.ac.cy";
    public static final String URL_SERVER_PORT = ":8080";
    public static final String URL_SERVER_PATH_VMSERVER = "/VMServer/Serve?";
    public static final String URL_SERVER_PATH_EXPERIMENTS = "/Experiments?";
    public static final String URL_TYPE = "type=";
    public static final String TYPE_BLOOMSIZE = "bloomsize";
    public static final String URL_FILTER = "&filter=";
    public static final String URL_EXTRAS = "&extras=";
    public static final String URL_DATASET = "&dataset=";
    public static final int CODE_FAILED_TO_PARSE_JSON = -10;
    public static final int CODE_UNKNOWN_ERR0R = -11;
    public static final int CODE_WRONG_ARGS = -1;
    public static final int CODE_NO_RESULTS = 0;
    public static final int CODE_SUCCESS = 1;
    public static final LatLng CS_UCY_0_0 = new LatLng(35.14487, 33.41115);
    public static final String RMAP_MEAN = "rm-mean.txt";
    public static final int URL_EXTRAS_V1 = 1;
    public static final int URL_EXTRAS_V2 = 2;
    public static final int URL_EXTRAS_V3 = 3;
    /** Delay for the location service to run */
    public static final long DELAY_LOCATION_SERVICE = 3000;
    /** algorithm */
    public static final float EXIT_RSS_THRESHOLD = 10.0f;
    /** Max Anonimity Level. 20 prefs */
    public static final int MAXIMUM_ANONYMITY = 20;
    /** Min Anonimity Level. 2 prefs */
    public static final int MINIMUM_ANONYMITY = 2;

    public static final int K_ANONYMITY_DEFAULT = 3;
    static final String TAG = App.class.getSimpleName();
    public static final String PACKAGE_POWER_TUTOR = "edu.umich.PowerTutor";
    public static final String ALL_RADIOMAP = "all";
    public static String NanValueUsed = "-110";
    public static String BROADCAST_PARSE_RMAP_AND_FIND_LOCATION;
    public static String BROADCAST_RECEIVER_GET_PARTIAL_RMAP;
    public static String BROADCAST_EXPERIMENT_LOCALISE_INSTANCE;
    public static String sdCardPath;
    /** Degrees groups we are processing (0, 90, 180, 270) */
    public static int MAX_DEGREES = 4;

    public static Random random;
    public ProgressBar progressBarBgTasks;
    public TextView textViewMessage3;
    public TextView textViewMessage4;
    public boolean wasWifiEnabled = false;
    public TextView textViewMessage1;
    public LinearLayout layoutTopMessages;
    public TextView textViewMessage2;
    public static MenuItem menuItemTrackMe;
    public static MenuItem menuItemFindMe;
    public LatLng currentCoordinates = new LatLng(0, 0);
    public LatLng previousCoordinates;
    public ArrayList<LatLng> locationHistory;
    public String previousMac;
    /** Access points that dont have any values in Server's radiomap */
    public ArrayList<String> problematicAPs;

    public LocalizationAlgorithms algorithms;

    /** Radiomap Cache */
    public RMapCache rmapCache;
    /** TextView showing the current scan results */
    public TextView scanAPs;

    public int wifiScanInterval = -1;
    public int bloomSize = -1;
    public Bloomfilter bloomfilter = null;
    /** Download RAM for real radiomap: used just before parsing */
    public String[] downloadRamCache = null;
    /** Download RAM for fake radiomaps: used for calculating new fake neighbors */
    public ArrayList<String[]> downloadRamFakesCache = null;
    /** All fake matching macs CHECK FOR REDUDUNCY IN CACHE ! */
    public ArrayList<String> currentFakeMatchedMacs;

    /** Application is in tracking process. Only one instrance must run at a time */
    /** */
    public ArrayList<ArrayList<LatLng>> fakeMatchedLocations;
    /** Unveil matches: where server thinks we are */
    public Button buttonUnveilMatches;
    /** Fakes a location within UCY for developing purposes */
    public Button buttonWalkingSimulator;
    /** Use SD Card caching */
    public boolean cacheSDcard = false;
    /** Use SD Card caching */
    public boolean cacheRam = false;
    /** K Parameter used in localization algorithms */
    public int kParameter;

    public LocalizationAlgorithm localizationAlgorithm;
    /** Anonymity Algorithm in use */
    public AnonymityAlgorithm anonymityAlgorithm;

    public RMapCache.CacheData cacheData;
    /** Get scan results from Wifi card */
    public LightWifiManager lightWifiManager;

    public boolean alwaysScan;
    /**
     * To indicate if the first bloom filter is send. From now on TVM1/2 algorithms will be used in
     * the getMultiMac function
     */
    public boolean firstBloomDone;

    public boolean realSimulation;
    public boolean drawRoutes;
    /** How many slots for RMaps will have in RAM */
    public int cacheRamSlots = 0;

    public Heading h;
    public String tvm1pRequests;
    public int datasetInUse;
    public boolean runningExperiment = false;
    public int currentSimulationPosition = -1;
    public boolean singleStepSimulation = false;
    /** first screen appearing is Tracker */
    AppType appType = AppType.Tracker;

    boolean vpnIsOkay = false;
    /** Use application in developer mode */
    boolean developerMode;
    /** A background process is running */
    private boolean inBackgroundProcess;
    /** The latest scan list of APs FUTURE COUNT TIME TO FIND THOSE! */
    private ArrayList<LogRecord> currentScanList;
    /** The previously scanned APs */
    private ArrayList<LogRecord> previousScanList;

    private ArrayList<LogRecord> enteredScanList;
    private ArrayList<LogRecord> exitedScanList;
    /** Flag for the TrackMe service */
    private boolean isTrackmeEnabled = false;
    /** Flag for the FindMe service */
    private boolean isFindmeEnabled = false;

    private ArrayList<CustomNavDrawer.ListItemObject> mListItemObjects;
    private Toast toast;
    private int kAnonymity = 0;
    public boolean alwaysScanWasOff = false; // Check if alwaysScan was on or off before
    public WalkSimulator simulateWalking;

    public static String getVMServerURL() {
        return URL_SERVER + URL_SERVER_PORT + URL_SERVER_PATH_VMSERVER;
    }

    public static String getExperimentsURL() {
        return URL_SERVER + URL_SERVER_PORT + URL_SERVER_PATH_EXPERIMENTS;
    }

    /**
     * Execute Post Request
     *
     * @param link of the PHP Script
     * @param values to build the PHP Post (eg. username=abc)
     * @return JSON Object with the result
     */
    public static String executePost(String link, ArrayList<NameValuePair> values) {
        String result = null;
        InputStream inputStream = null;

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(link);
            httppost.setEntity(new UrlEncodedFormEntity(values));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, "US-ASCII"));

            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            inputStream.close();
            result = stringBuilder.toString();

        } catch (Exception e1) {
            Log.e(TAG, "Error in Http.Post " + e1.toString());
            return null;
        }
        return result;
    }

    /**
     * Execute Get Request
     *
     * @param link of the http get
     * @param params parameters for http get
     * @return JSON Object with the result
     */
    public static String executeGet(String link, String params, AsyncTaskType type) {

        String result = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(link + params);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(300000000);
            conn.setReadTimeout(300000000);

            inputStream = conn.getInputStream();

            if (type.equals(AsyncTaskType.checkVpn)) {
                return "1";
            }

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, "US-ASCII"));

            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            inputStream.close();
            result = stringBuilder.toString();

        } catch (Exception e1) {
            Log.d(TAG, "Error in Http.Get " + e1.toString());
            return null;
        }

        return result;
    }

    /** Show K Anonymity Number Picker dialog */
    public static void showDialogRunExperimentSeries1(final Activity activity) {
        final App app = (App) activity.getApplication();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder
                .setTitle("Run Experiment for Series 1")
                .setPositiveButton(
                        "Run",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TestTVM.RunExperiment((MainActivity) activity, app);
                            }
                        })
                .setNegativeButton(
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /** Show K Anonymity Number Picker dialog */
    public static void showChangeDatasetDialog(final Activity activity) {
        final App app = (App) activity.getApplication();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        CharSequence descriptions[] =
                app.getResources().getStringArray(R.array.array_datasets_descriptions);
        final CharSequence values[] =
                app.getResources().getStringArray(R.array.array_datasets_values);
        String curVal = app.datasetInUse + "";
        String curDesc = "";

        for (int i = 0; i < values.length; i++) {
            if (curVal.equals(values[i])) {
                curDesc = descriptions[i].toString();
                break;
            }
        }

        alertDialogBuilder.setTitle("Datasets");
        alertDialogBuilder.setTitle("Currently selected: " + curDesc);
        alertDialogBuilder.setItems(
                descriptions,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        SharedPreferences.Editor editor =
                                activity.getSharedPreferences(
                                                activity.getString(R.string.preferences),
                                                MODE_PRIVATE)
                                        .edit();

                        editor.putString(
                                activity.getString(R.string.pref_key_datasets),
                                values[item].toString());
                        editor.commit();

                        //save it in app too
                        app.datasetInUse = Integer.parseInt(values[item].toString());
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /** Show K Anonymity Number Picker dialog */
    public static void showKAnonymityPickerDialog(final Activity activity, int min, int max) {
        final NumberPicker np = new NumberPicker(activity);
        final App app = (App) activity.getApplication();
        np.setMaxValue(max);
        np.setMinValue(min);
        np.setValue(app.getkAnonymity());
        np.setFocusable(true);
        np.setFocusableInTouchMode(true);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle("Choose K Anonymity parameter");

        alertDialogBuilder
                .setPositiveButton(
                        "Change",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                app.kAnonymity = np.getValue();

                                SharedPreferences.Editor editor =
                                        activity.getSharedPreferences(
                                                        activity.getString(R.string.preferences),
                                                        MODE_PRIVATE)
                                                .edit();

                                editor.putInt(
                                        activity.getString(R.string.pref_key_k_anonymity),
                                        np.getValue());
                                editor.commit();
                            }
                        })
                .setNegativeButton(
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                .setCancelable(true);

        alertDialogBuilder.setView(np);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /** @return K Anonymity -1, */
    public int getkAnonymity_m1() {
        return kAnonymity - 1;
    }

    public ArrayList<CustomNavDrawer.ListItemObject> getListItemObjects() {
        return mListItemObjects;
    }

    public CustomNavDrawer.ListItemObject getListItemObject(int position) {
        return mListItemObjects.get(position);
    }

    private ArrayList<CustomNavDrawer.ListItemObject> initListItemObjects() {

        ArrayList<CustomNavDrawer.ListItemObject> arrayList =
                new ArrayList<CustomNavDrawer.ListItemObject>();

        // Settings title
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Quick Settings", CustomNavDrawer.ListItemType.Label, null));
        // Settings items
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Control privacy",
                        CustomNavDrawer.ListItemType.Setting,
                        getString(R.string.pref_key_k_anonymity)));
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Change dataset",
                        CustomNavDrawer.ListItemType.Setting,
                        getString(R.string.pref_key_datasets)));

        //Experiments title
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Experiments", CustomNavDrawer.ListItemType.Label, null));
        //Experiments items
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Series 1", CustomNavDrawer.ListItemType.Experiment, "series1"));
        // arrayList.add(new CustomNavDrawer.ListItemObject("Series 2", CustomNavDrawer.ListItemType.Experiment, "series2"));

        //Utilities title
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Advanced", CustomNavDrawer.ListItemType.Label, null));
        arrayList.add(
                new CustomNavDrawer.ListItemObject(
                        "Record route", CustomNavDrawer.ListItemType.Experiment, "buildroutes"));

        return arrayList;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        BROADCAST_EXPERIMENT_LOCALISE_INSTANCE =
                getString(R.string.broadcast_receiver_experiment_localise_instance);
        BROADCAST_PARSE_RMAP_AND_FIND_LOCATION =
                getString(R.string.broadcast_receiver_rmapfile_fetched);
        BROADCAST_RECEIVER_GET_PARTIAL_RMAP =
                getString(R.string.broadcast_receiver_get_partial_radiomap);
        currentScanList = new ArrayList<LogRecord>();
        previousScanList = new ArrayList<LogRecord>();
        enteredScanList = new ArrayList<LogRecord>();
        exitedScanList = new ArrayList<LogRecord>();
        random = new Random();
        problematicAPs = new ArrayList<String>();
        lightWifiManager = new LightWifiManager(this);
        algorithms = new LocalizationAlgorithms(this);
        // Matched MAC Addresses from bloom algorithm
        currentFakeMatchedMacs = new ArrayList<String>();
        // Matches locations(to above macs) from bloom algorithm
        fakeMatchedLocations = new ArrayList<ArrayList<LatLng>>();
        downloadRamFakesCache = new ArrayList<String[]>();
        locationHistory = new ArrayList<LatLng>();

        initializePreferences();
        // this is inited each time k anonymity changes (it depends on it)
        cacheData = new RMapCache.CacheData(this);

        this.mListItemObjects = initListItemObjects();

        simulateWalking = new WalkAtUcy_8(this);
    }

    /** Initialize preferences from Shared Preferences */
    private void initializePreferences() {

        SharedPreferences sharedPreferences =
                getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);

        initializeDefaultValues(sharedPreferences);

        // Save sd card cache status
        cacheSDcard =
                sharedPreferences.getBoolean(
                        getString(R.string.pref_key_sd_card_cache_status), true);

        // Save RAM cache status
        cacheRam =
                sharedPreferences.getBoolean(getString(R.string.pref_key_ram_cache_status), true);

        alwaysScan = sharedPreferences.getBoolean(getString(R.string.pref_key_always_scan), false);

        String localizationAlgo =
                sharedPreferences.getString(
                        getString(R.string.pref_key_localization_algorithm), "");

        String anonymityAlgo =
                sharedPreferences.getString(getString(R.string.pref_key_anonymity_algorithm), "");

        developerMode =
                sharedPreferences.getBoolean(getString(R.string.pref_key_developer_mode), false);

        realSimulation =
                sharedPreferences.getBoolean(getString(R.string.pref_key_real_simulation), false);

        drawRoutes = sharedPreferences.getBoolean(getString(R.string.pref_key_draw_routes), true);

        setLocalizationAlgorithm(localizationAlgo);
        setAnonymityAlgorithm(anonymityAlgo);

        wifiScanInterval =
                Integer.parseInt(
                        sharedPreferences.getString(
                                getString(R.string.pref_key_samples_interval), ""));

        kParameter =
                Integer.parseInt(
                        sharedPreferences.getString(
                                getString(R.string.pref_key_samples_interval), "0"));
        // Ram cache slots
        cacheRamSlots =
                Integer.parseInt(
                        sharedPreferences.getString(
                                getString(R.string.pref_key_ram_cache_slots), "0"));

        // Ram cache slots
        datasetInUse =
                Integer.parseInt(
                        sharedPreferences.getString(getString(R.string.pref_key_datasets), "0"));

        kAnonymity = sharedPreferences.getInt(getString(R.string.pref_key_k_anonymity), 0);

        Log.i(TAG, "Localization algo: " + localizationAlgorithm.toString());
        Log.i(TAG, "Anonymity algo: " + anonymityAlgorithm.toString());
        Log.i(TAG, "SD Cache: " + cacheSDcard);
        Log.i(TAG, "Ram Cache: " + cacheRam);
        Log.i(TAG, "Wifi Scan interval: " + wifiScanInterval);
        Log.i(TAG, "KNN parameter(k): " + kParameter);
        Log.i(TAG, "Always scan: " + alwaysScan);
        Log.i(TAG, "Ram slots: " + cacheRamSlots);
        Log.i(TAG, "Dataset in use: " + datasetInUse);
        Log.i(TAG, "K Anonymity in use: " + kAnonymity);
    }

    /** Initialize default values of Shared Preferences */
    private void initializeDefaultValues(SharedPreferences s) {

        String noInit = s.getString(getString(R.string.pref_key_localization_algorithm), "noinit");

        if (noInit.equals("noinit")) {
            toast.setText("Please setup your preferences");
            toast.show();

        } else return;

        // Initialize default values
        PreferenceManager.setDefaultValues(
                this, getString(R.string.preferences), MODE_PRIVATE, R.xml.preferences, true);

        //Set default value for k anonymity to 3
        // special handling case here!
        SharedPreferences.Editor editor = s.edit();

        editor.putInt(this.getString(R.string.pref_key_k_anonymity), App.K_ANONYMITY_DEFAULT);
        editor.commit();
    }

    /** Clearing current scan list of WiFi Access Points */
    public void currentScanListClear() {

        try {

            previousScanListAddAll(currentScanList);
            this.currentScanList.clear();

        } catch (NullPointerException e) {
        }
    }

    /** Add a log record to current scan list */
    void currentScanListAdd(LogRecord logRecord) {
        this.currentScanList.add(logRecord);
    }

    /** Add an arraylist of LogRecords to current Scan List */
    public void currentScanListAddAll(ArrayList<LogRecord> logRecords) {
        this.currentScanList.addAll(logRecords);
    }

    /** @return the current scan list of WiFi Access Points */
    public ArrayList<LogRecord> currentScanListGet() {
        return this.currentScanList;
    }

    /** @return the previous scan list of WiFi Access Points */
    ArrayList<LogRecord> previousScanListGet() {
        return this.previousScanList;
    }

    public ArrayList<LogRecord> enteredScanListGet() {
        return this.enteredScanList;
    }

    public ArrayList<LogRecord> exitedScanListGet() {
        return this.exitedScanList;
    }

    boolean currentScanListIsEmpty() {
        return this.currentScanList.isEmpty();
    }

    public void enteredScanListAddAll(ArrayList<LogRecord> previousData) {
        this.enteredScanList.clear();
        this.enteredScanList.addAll(previousData);
    }

    public void exitedScanListAddAll(ArrayList<LogRecord> previousData) {
        this.exitedScanList.clear();
        this.exitedScanList.addAll(previousData);
    }

    void previousScanListAddAll(ArrayList<LogRecord> previousData) {
        this.previousScanList.clear();
        this.previousScanList.addAll(previousData);
    }

    public void setAnonymityAlgorithm(String algoStr) {
        if (algoStr.equals("FRMAP")) {
            anonymityAlgorithm = AnonymityAlgorithm.FRMAP;
        } else if (algoStr.equals("PRMAP")) {
            anonymityAlgorithm = AnonymityAlgorithm.PRMAP;
        } else if (algoStr.equals("TVM0")) {
            anonymityAlgorithm = AnonymityAlgorithm.TVM0;
        } else if (algoStr.equals("TVM1")) {
            anonymityAlgorithm = AnonymityAlgorithm.TVM1;
        } else if (algoStr.equals("TVM2")) {
            anonymityAlgorithm = AnonymityAlgorithm.TVM2;
        } else if (algoStr.equals("GMAPS")) {
            anonymityAlgorithm = AnonymityAlgorithm.GMAPS;
        }
    }

    public void setLocalizationAlgorithm(String algoStr) {
        if (algoStr.equals("WKNN")) {
            localizationAlgorithm = LocalizationAlgorithm.WKNN;
        } else if (algoStr.equals("KNN")) {
            localizationAlgorithm = LocalizationAlgorithm.KNN;
        } else if (algoStr.equals("WMMSE")) {
            localizationAlgorithm = LocalizationAlgorithm.WMMSE;
        } else if (algoStr.equals("MMSE")) {
            localizationAlgorithm = LocalizationAlgorithm.MMSE;
        }
    }

    public void setVpnOkay() {
        vpnIsOkay = true;
    }

    public void setVpnNotAvailable() {
        vpnIsOkay = false;
    }

    public boolean isInVpn() {
        return vpnIsOkay;
    }

    /** @return true if AppType is tracker false if its logger */
    boolean isTracker() {
        return appType.equals(AppType.Tracker) ? true : false;
    }

    public boolean isFindmeEnabled() {
        return isFindmeEnabled;
    }

    public synchronized void setFindmeOn() {
        isFindmeEnabled = true;
        textViewMessage1.setText(getString(R.string.getting_location));
    }

    public synchronized void setFindmeOff() {
        isFindmeEnabled = false;
        textViewMessage1.setText("");
    }

    public boolean isTrackmeEnabled() {
        return isTrackmeEnabled;
    }

    public synchronized void setTrackmeOn() {
        isTrackmeEnabled = true;
        textViewMessage1.setText(getString(R.string.tracking));
    }

    public synchronized void setTrackmeOff() {
        isTrackmeEnabled = false;
        textViewMessage1.setText("");
    }

    public boolean isInBgProgress() {
        return inBackgroundProcess;
    }

    public synchronized void setBgProgressOn() {
        inBackgroundProcess = true;
    }

    public synchronized void setBgProgressOff() {
        inBackgroundProcess = false;
    }

    void startLocalizationService() {
        startService(new Intent(this, LocalizationService.class));
    }

    void stopLocalizationService() {
        stopService(new Intent(this, LocalizationService.class));
    }

    /** @return true if TVM1 or TVM2 algorithms are used */
    public boolean isTVMenabled() {
        if (anonymityAlgorithm.equals(AnonymityAlgorithm.TVM1)
                || anonymityAlgorithm.equals(AnonymityAlgorithm.TVM2)) return true;

        return false;
    }

    /** @return true if TVM1 or TVM2 algorithms are used */
    public boolean isTVM0() {
        if (anonymityAlgorithm.equals(AnonymityAlgorithm.TVM0)) return true;
        return false;
    }

    /** Show toast notification */
    public void showToast(String string) {
        toast.setText(string);
        toast.show();
    }

    public boolean isTablet() {
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_LARGE) return true;
        else if ((getResources().getConfiguration().screenLayout
                        & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE) return true;
        return false;
    }

    public int getkAnonymity() {
        return kAnonymity;
    }

    public void setkAnonymity(int KAnonymity) {
        kAnonymity = KAnonymity;
    }

    /** Initialize receiver for experiments */
    public void initExperimentSeries1() {
        this.currentSimulationPosition = 1;
        this.runningExperiment = true;
    }

    public void buildValuesClassInSdCard() {

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
            Calendar cal = Calendar.getInstance();
            String timeStr = dateFormat.format(cal.getTime());

            File file = new File(rmapCache.getCacheDirectory() + timeStr);
            file.createNewFile();

            PrintWriter pw = new PrintWriter(file);
            WalkSimulatorClassGenerator.Generate(pw);
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static enum AppType {
        Tracker,
        Logger,
        Unknown
    }

    public static enum LocalizationAlgorithm {
        KNN,
        WKNN,
        MMSE,
        WMMSE
    }

    public static enum AnonymityAlgorithm {
        FRMAP,
        PRMAP,
        TVM0,
        TVM1,
        TVM2,
        GMAPS
    }

    public enum AsyncTaskType {
        getMac,
        getBloom,
        getBloomSize,
        getMultiMacs,
        checkVpn,
        notset,
        saveExperimentData
    }

    /** Restart the application */
    public void restartApp() {
        Intent i = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(i);
    }
}
