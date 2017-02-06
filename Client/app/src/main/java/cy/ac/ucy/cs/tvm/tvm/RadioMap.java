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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import cy.ac.ucy.cs.tvm.App;

/**
 * Radiomap consists of:
 *
 * <ul>
 *   <li>the raw file that it was created from
 *   <li>a list of the all mac addresses
 *   <li>a hashmap between the mac addresses and the RSS values
 * </ul>
 */
public class RadioMap {

    private static final String TAG = RadioMap.class.getSimpleName();
    private File rmapFile = null;
    private ArrayList<String> macAddresses = null;

    /** Table with four hashmaps. for 0, 90, 180, 270 degrees */
    private ArrayList<HashMap<String, ArrayList<String>>> locationRssHashMapTbl = null;

    private ArrayList<String> orderList = null;

    public RadioMap(Context c) {
        // MAC Addresses
        macAddresses = new ArrayList<String>();

        // Create the hasmap table container
        locationRssHashMapTbl = new ArrayList<HashMap<String, ArrayList<String>>>();

        // Table with degrees hashmaps
        for (int i = 0; i < App.MAX_DEGREES; i++) {
            // Create the hashmaps
            locationRssHashMapTbl.add(new HashMap<String, ArrayList<String>>());
        }

        orderList = new ArrayList<String>();
    }

    /**
     * Getter of MAC Address list in file order
     *
     * @return the list of MAC Addresses
     */
    public ArrayList<String> getmacAddresses() {
        return macAddresses;
    }

    public ArrayList<HashMap<String, ArrayList<String>>> getlocationRssHashMapTbl() {
        return locationRssHashMapTbl;
    }

    /**
     * Getter of Location list in file order
     *
     * @return the Location list
     */
    public ArrayList<String> getorderList() {
        return orderList;
    }

    /**
     * Getter of radio map mean filename
     *
     * @return the filename of radiomap mean used
     */
    public File getradiomapMeanFile() {
        return this.rmapFile;
    }

    /**
     * @param heading
     * @return
     */
    public HashMap<String, ArrayList<String>> getlocationRssHashMap(int heading) {
        int degreeIndex = findTableIndex(heading + "");

        return locationRssHashMapTbl.get(degreeIndex);
    }

    /** Recycles this radiomap for re-use with other rmap data */
    public void recycle() {
        rmapFile = null;
        macAddresses.clear();
        orderList.clear();

        // Clear degree hashmaps
        for (int i = 0; i < App.MAX_DEGREES; i++) {
            // Create the hashmaps
            locationRssHashMapTbl.get(i).clear();
        }
    }

    /**
     * @param headingStr heading number in a string format
     * @return HashMap according to degrees given
     */
    public HashMap<String, ArrayList<String>> getlocationRssHashMap(String headingStr) {
        return getlocationRssHashMap(Integer.parseInt(headingStr));
    }

    /** Returns the index where the line must inserted, according to the degrees */
    public static int findTableIndex(String string) {
        int degrees = Integer.parseInt(string);
        return findTableIndex(degrees);
    }

    /**
     * Calculate the index of the HashMap according to degrees
     *
     * @param degrees given to calculate HashMap Index
     */
    public static int findTableIndex(int degrees) {

        if ((degrees >= 315 && degrees <= 360) || degrees < 45) {
            return 0;
        } else if (degrees >= 45 && degrees < 135) {
            return 1;
        } else if (degrees >= 135 && degrees < 225) {
            return 2;
        } else if (degrees >= 225 && degrees < 315) {
            return 3;
        } else return -1;
    }
}
