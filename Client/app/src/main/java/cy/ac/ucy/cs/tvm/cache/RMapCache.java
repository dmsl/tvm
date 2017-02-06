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

package cy.ac.ucy.cs.tvm.cache;

import android.content.Context;
import android.util.Log;
import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.AsyncTaskHttpExecutor;
import cy.ac.ucy.cs.tvm.tvm.RadioMap;
import java.io.File;
import java.util.ArrayList;

/**
 * Radioamp cache: we cache the radiomaps either on SD card, so we don't need to download them each
 * time we want to localize in a particular area that we visited before, and in RAM, so we don't
 * have to parse the radiomap each time before localization.
 *
 * <p>There are preferences that control both RAM and SD card caching.
 */
public class RMapCache {

    private static String TAG = RMapCache.class.getSimpleName();

    /** Radiomaps Cache. When disabled only 1st place is used */
    public ArrayList<RadioMap> radiomapCache;

    /** Access point names of radiomap cache */
    public ArrayList<String> radiomapCacheAPs;

    Context context;
    App app;

    /** SD Card directory */
    File sdCardDir;

    /** Cache directory: sdDir/cache/ */
    File cacheDir;

    public enum CacheType {
        RAM,
        SD,
        MISS,
        undefined
    }

    public static class CacheData {

        public CacheType type = CacheType.undefined;
        public RadioMap currentRmap = null;

        /** Radiomap of Fake APs that 'is' currently in use */
        public ArrayList<RadioMap> currentFakeRmaps = new ArrayList<RadioMap>();

        /** Radiomap that is in SD cache */
        public File currentRmapFile = null;

        /** Current MAC address of APs range that is used for localization */
        public String currentMac = null;

        public CacheData(App app) {
            // Create the fake empty radiomaps
            for (int i = 0; i < app.getkAnonymity_m1(); i++) {
                currentFakeRmaps.add(new RadioMap(app));
            }
        }

        public void recycleFakeRmaps() {
            for (int i = 0; i < currentFakeRmaps.size(); i++) currentFakeRmaps.get(i).recycle();
        }
    }

    public RMapCache(Context context) {

        this.radiomapCache = new ArrayList<RadioMap>();
        this.context = context;
        this.app = (App) context.getApplicationContext();

        this.radiomapCache = new ArrayList<RadioMap>();
        this.radiomapCacheAPs = new ArrayList<String>();

        // Create SD card directory
        if (android.os.Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED))
            sdCardDir =
                    new File(
                            android.os.Environment.getExternalStorageDirectory(),
                            "Android/data/airplace/");
        else sdCardDir = context.getCacheDir();
        if (!sdCardDir.exists()) sdCardDir.mkdirs();

        // Create cache directory
        // Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir =
                    new File(
                            android.os.Environment.getExternalStorageDirectory(),
                            "Android/data/airplace/cache/");
        else cacheDir = context.getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();
    }

    /** Gets radiomap from server, and then cache it to SD Card and Download ram */
    public void cacheRadiomapFile(CacheData cacheData) {

        String[] parameters = {"", cacheData.currentMac};

        new AsyncTaskHttpExecutor(
                        app, App.URL_VCENTER, App.AsyncTaskType.checkVpn, parameters, null)
                .execute();
    }

    /** Returns directory name of where radiomap should be located */
    public void fillCacheData(ArrayList<String> macs) {
        String cacheDirS = getCacheDirectory();
        String filename;
        int strongestMacIndex = -1;
        int ramCacheIndex = -1;
        File file = null;

        boolean found = false;

        // IF RAM caching is enabled
        if (app.cacheRam) {

            // Find strongest mac that is RAM cached
            // For all the listening APs
            for (int i = 0; i < macs.size(); i++) {

                // And the RAM cached APs
                for (int j = 0; j < radiomapCacheAPs.size(); j++) {
                    if (radiomapCacheAPs.get(j).equals(macs.get(i))) {
                        strongestMacIndex = i;
                        ramCacheIndex = j;
                        found = true;
                        break;
                    }
                }
            }

            // Ram cache hit - Save it to current rmap cache
            if (found) {
                // Set RAM cache type, radiomap cache, and strongest index
                app.cacheData.type = CacheType.RAM;
                app.cacheData.currentRmap = this.radiomapCache.get(ramCacheIndex);
                app.cacheData.currentMac = macs.get(strongestMacIndex);

                return;
            }
        }

        if (app.cacheSDcard) {
            // Find strongest mac that is SD cached
            for (int i = 0; i < macs.size(); i++) {

                filename = cacheDirS + "/" + macs.get(i);
                file = new File(filename);

                // Check if filename exists
                if (file.exists()) {
                    // Found file
                    strongestMacIndex = i;
                    found = true;
                    break;
                }
            }

            if (found) {
                // Set SD cache type, radiomap cache, and strongest
                // index
                app.cacheData.type = CacheType.SD;
                app.cacheData.currentRmapFile = file;
                app.cacheData.currentMac = macs.get(strongestMacIndex);

                return;
            }
        }

        // Cache MISS
        // if no cache files found, use the strongest mac
        app.cacheData.currentMac = macs.get(0);

        // where file will SD cached
        app.cacheData.currentRmapFile = new File(cacheDirS + "/" + app.cacheData.currentMac);
        app.cacheData.type = CacheType.MISS;

        return;
    }

    /** Find out if problematic mac doesnt already exists */
    public boolean isProblematicMac(String problematicMac) {
        for (int i = 0; i < app.problematicAPs.size(); i++) {
            if (app.problematicAPs.get(i).equals(problematicMac)) {
                return true;
            }
        }
        return false;
    }

    /** Clears all caches */
    public void clearSDCache() {
        File[] files = cacheDir.listFiles();

        if (files == null) return;

        // Cache already empty
        if (cacheDir.listFiles().length == 0) {
            app.showToast("Cache was already empty");

            return;
        }

        for (File f : files) f.delete();

        if (cacheDir.listFiles().length != 0) {
            // Cache cleared
            app.showToast("Something went wrong. Clear cache manually");

        } else {
            Log.i(TAG, "Cleared SD Card cache");
        }
    }

    public void clearRamCache() {
        this.radiomapCacheAPs.clear();
        this.radiomapCache.clear();
    }

    /** @return caches directory */
    public String getCacheDirectory() {
        return cacheDir.toString() + "/";
    }

    /**
     * FUTURE remove and leave only the above. notice the extra slash!
     *
     * @return caches directory
     */
    public String getSDCardDirectory() {
        return cacheDir.toString();
    }

    /**
     * Creates a new slot in RAM for parsed rmap, and returns it. If RAM cache limit is reached,
     * then it removes the LRU rmap.
     */
    public RadioMap ramCacheParsedRadiomap(String macAddress) {

        RadioMap result;

        // Cache is not full yet
        if (radiomapCache.size() < app.cacheRamSlots) {
            // Create new rmap
            result = new RadioMap(app);
            // Cache radiomap + its MAC address
            radiomapCache.add(result);
            radiomapCacheAPs.add(macAddress);
        }
        // Cache is full
        else {

            // Radiomap to use is the slot last in cache
            result = radiomapCache.get(app.cacheRamSlots - 1);

            //Remove it from cache & macs too
            radiomapCache.remove(app.cacheRamSlots - 1);
            radiomapCacheAPs.remove(app.cacheRamSlots - 1);

            result.recycle();

            //Add it again in first place
            radiomapCache.add(result);
            radiomapCacheAPs.add(macAddress);
        }
        return result;
    }
}
