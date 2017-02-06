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

package cy.ac.ucy.dmsl.vectormap.paschalis;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * DAO class that the information of a coordinate:
 *
 * <ul>
 *   <li>x,y (or longidute, latitude)
 *   <li>heading, as it might be obstructing the AP signal strength
 *   <li>RSS
 *   <li>mac addresss of the Access Point (AP)
 * </ul>
 */
public class Coordinate {
    /** MAC address of AP */
    public String mac;
    /** RSS */
    public String rss;
    /** X Coordinate */
    public String x = "";
    /** Y Coordinate */
    public String y = "";
    /** H Heading */
    public String h = "";

    /**
     * @param x axis (latitude)
     * @param y axis (longitude)
     * @param h Heading (accelerometer: 0, 90, 180, 270)
     * @param mac Address
     * @param rss value
     */
    public Coordinate(String x, String y, String h, String mac, String rss) {
        this.x = x;
        this.y = y;
        this.h = h;
        this.mac = mac;
        this.rss = rss;
    }

    public String toString() {
        return x + ":" + y + ":" + h + " " + mac + "=" + rss;
    }

    /**
     * @param n
     * @return true if coordinates are equal
     */
    public boolean equals(Coordinate n) {
        return n.x.equals(this.x) && n.y.equals(this.y) && n.h.equals(this.h);
    }

    /**
     * Build radiomap in our format
     *
     * @param macs unique macs
     * @param writer radiomap usefull data
     * @param map radiomap name
     */
    static void printRadiomap(
            ArrayList<String> macs, ArrayList<Coordinate> map, PrintWriter writer) {

        String line;
        line = "# X, Y, HEADING";

        //Save mac addresses
        for (String mac : macs) {
            line += ", " + mac;
        }

        //Print MAC Addresses
        writer.println(line);

        // Until all gist data are written
        while (map.size() != 0) {
            //Get a coordinates data
            ArrayList<Coordinate> coords = getSameCoordinatesData(map);
            //Print coordinates to radiomap
            printCoordinateToRadiomap(coords, macs, writer);
        }
    }

    /**
     * Save a coordinate line to the new radiomap
     *
     * @param coords a coordinate transformed to a line in final radiomap
     * @param macs unique mac address
     * @param writer To post or get requests
     */
    private static void printCoordinateToRadiomap(
            ArrayList<Coordinate> coords, ArrayList<String> macs, PrintWriter writer) {
        String line;
        line = coords.get(0).x + ", " + coords.get(0).y + ", " + coords.get(0).h;

        // Calculate the mac positions, of average coordinates
        ArrayList<Integer> macPositions = calculateMacPositions(macs, coords);
        int nextMacToWrite = macPositions.get(0);
        int size = macs.size();

        // Write RSS valeus for the line
        for (int i = 0; i < size; i++) {
            //if mac exists in averaged Coordinates, write its value
            if (i == nextMacToWrite) {
                line += "," + getRssValue(macs.get(i), coords);
                //Remove mac from macPositions
                macPositions.remove(0);

                //If all macs entered, a not NaN rss wont be writtern again
                if (macPositions.size() == 0) {
                    nextMacToWrite = -1;
                }
                //get the next mac
                else {
                    nextMacToWrite = macPositions.get(0);
                }
            }
            //otherwise write a NaN value
            else {
                line += ","; // + "-110";
            }
        }
        writer.println(line);
    }

    /**
     * Calculate indeces for MAC Addresses
     *
     * @param macs unique MAC Addresses
     * @param avgCoords Averaged Coordinates
     * @return
     */
    private static ArrayList<Integer> calculateMacPositions(
            ArrayList<String> macs, ArrayList<Coordinate> avgCoords) {
        ArrayList<Integer> indices = new ArrayList<Integer>();

        //Find MAC indices
        for (int i = 0; i < avgCoords.size(); i++) {
            indices.add(findMacPosition(macs, avgCoords.get(i).mac));
        }

        //Sort the ascending
        ArrayList<Integer> result = new ArrayList<Integer>();
        int size = indices.size();
        for (int i = 0; i < size; i++) {
            int min = Integer.MAX_VALUE;
            int index = -1;
            //Find and remove smallest
            for (int j = 0; j < indices.size(); j++) {
                //found smaller index
                if (indices.get(j) < min) {
                    //Save new min and index
                    min = indices.get(j);
                    index = j;
                }
            }

            //move smallest index to sorted arraylist
            if (min == Integer.MAX_VALUE) {
                System.err.println(
                        Coordinate.class.getSimpleName()
                                + ": Something went wrong on index sorting");
                System.exit(-1);
            }
            indices.remove(index);
            result.add(min);
        }
        return result;
    }

    /**
     * Finds a new(the first) coordinate, and returns all rows matching for that coordinate
     *
     * @param mapGist
     * @return
     */
    private static ArrayList<Coordinate> getSameCoordinatesData(ArrayList<Coordinate> mapGist) {
        ArrayList<Coordinate> coords = new ArrayList<Coordinate>();

        //Returned already all coordinates
        if (mapGist.size() == 0) return null;

        //Coordinates to return
        Coordinate coord = mapGist.get(0);
        mapGist.remove(0);
        coords.add(coord);

        // Get matching coordinates
        for (Coordinate c : mapGist) {
            //if its the same coordinate
            if (coord.equals(c)) {
                //add to returned arraylist
                coords.add(c);
            }
        }
        // Remove matched coordinates
        for (Coordinate rmc : coords) {
            //remove from mapGist
            mapGist.remove(rmc);
        }
        return coords;
    }

    /**
     * Get Coordinates RSS value
     *
     * @param mac Mac address to get its RSS value
     * @param avgCoords Coordinates with average RSS value
     * @return
     */
    private static String getRssValue(String mac, ArrayList<Coordinate> avgCoords) {
        for (int i = 0; i < avgCoords.size(); i++) {
            if (avgCoords.get(i).mac.equals(mac)) {
                return avgCoords.get(i).rss;
            }
        }
        return null;
    }

    /**
     * Returns mac position found in radiomap
     *
     * @param macs
     * @param mac
     */
    private static int findMacPosition(ArrayList<String> macs, String mac) {
        int result = -1;
        for (int i = 0; i < macs.size(); i++) {
            if (macs.get(i).equals(mac)) return i; //TODO CHECK IF HAVE TO RETURN +1
        }
        return result;
    }

    /**
     * Finds a new(the first) coordinate, and returns all rows matching for that coordinate
     *
     * @param coords
     * @return
     */
    private static Coordinate calculateMeanCoordinate(ArrayList<Coordinate> coords) {
        ArrayList<Coordinate> sameMacs = new ArrayList<Coordinate>();

        //Returned already all coordinates
        if (coords.size() == 0) return null;
        //Coordinates to return
        Coordinate coord = coords.get(0);
        coords.remove(0);
        sameMacs.add(coord);

        // Get matching coordinates
        for (Coordinate c : coords) {
            //if its the same mac address
            if (coord.mac.equals(c.mac)) {
                //add to saved macs
                sameMacs.add(c);
            }
        }

        // Remove matched macs
        for (Coordinate rmc : sameMacs) {
            //remove from mapGist
            coords.remove(rmc);
        }

        //Calculate mean for coordinate
        float meanRSS = 0;
        for (int i = 0; i < sameMacs.size(); i++) {
            float rss = Float.parseFloat(sameMacs.get(i).rss);
            //Set minimum value to -110
            if (rss < -110) rss = -110;
            meanRSS += rss;
        }

        meanRSS = meanRSS / sameMacs.size();

        // Round up
        if (Math.abs(meanRSS - (int) meanRSS) >= 0.5) {
            meanRSS -= 1; //since RSS values are negative
        }
        coord.rss = (int) meanRSS + "";
        return coord;
    }

    /**
     * @param macs Unique mac addresses arraylist
     * @param macAddress maybe a new mac address
     */
    public static void saveUniqueMac(ArrayList<String> macs, String macAddress) {
        for (String mac : macs) {
            if (mac.equals(macAddress)) {
                return;
            }
        }
        //else save new mac
        macs.add(macAddress);
    }

    /**
     * @param x coordinate
     * @param y coordinate
     * @param clear saved map
     * @param i where GPS x,y position was found
     * @param macCnt mac addresses to associate with x,y coords
     */
    private static void saveCoordinates(
            String x, String y, ArrayList<Coordinate> mapGist, int i, int macCnt, boolean clear) {

        //do save operation
        if (!clear) {
            //save all coordinates to the macCnt saved macs
            for (int j = mapGist.size() - 1; j >= mapGist.size() - macCnt; j--) {
                Coordinate c = mapGist.get(j);
                c.x = x;
                c.y = y;
            }
        }
        //Do the clear operation:
        //is used to clear the last entered values if the dont have a GPS long lat tag
        else {
            int size = mapGist.size();
            //save all coordinates to the macCnt saved macs
            for (int j = size - 1; j >= size - macCnt; j--) {
                mapGist.remove(j);
            }
        }
    }

    /**
     * Save unique MAC Addresses in a radiomap
     *
     * @param uniqueMacs Unique MAC Addresses
     * @param rowMacs to add to unique table
     */
    public static void saveUniqueMacs(ArrayList<String> uniqueMacs, ArrayList<String> rowMacs) {

        for (int i = 0; i < rowMacs.size(); i++) {
            //Save unique mac to MAC Addresses of a radiomap
            saveUniqueMac(uniqueMacs, rowMacs.get(i));
        }
    }

    public static void printRadiomaps(
            ArrayList<ArrayList<String>> rmapsUniqueMacs,
            ArrayList<ArrayList<Coordinate>> rmapsCoordinates,
            PrintWriter writer) {

        for (int i = 0; i < rmapsUniqueMacs.size(); i++) {
            writer.print("{ 'rmap': '");
            printRadiomap(rmapsUniqueMacs.get(i), rmapsCoordinates.get(i), writer);
            writer.print("' }");

            if (i < rmapsUniqueMacs.size() - 1) {
                writer.print(",\n");
            }
        }
    }
}
