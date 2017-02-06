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

/**
 * A log record consists of:
 *
 * <ul>
 *   <li>coordinates (latitude, longitude)
 *   <li>heading of the device
 *   <li>rss and bssid values of the Access Points (APs)
 *   <li>the timestamp when the above values were observed
 * </ul>
 */
public class LogRecord {

    private long ts; // timestamp
    private double lng; // longitude
    private double lat; // latitude
    private float heading;
    private String bssid;
    private int rss;

    public LogRecord(String bssid, int rss) {
        super();
        this.bssid = bssid;
        this.rss = rss;
    }

    public LogRecord(long ts, double lat, double lng, float heading, String bssid, int rss) {
        super();
        this.ts = ts;
        this.lng = lng;
        this.lat = lat;
        this.heading = heading;
        this.bssid = bssid;
        this.rss = rss;
    }

    public String toString() {
        String str =
                String.valueOf(ts)
                        + " "
                        + String.valueOf(lat)
                        + " "
                        + String.valueOf(lng)
                        + " "
                        + String.valueOf(heading)
                        + " "
                        + String.valueOf(bssid)
                        + " "
                        + String.valueOf(rss)
                        + "\n";
        return str;
    }

    public String getBssid() {
        return bssid;
    }

    public int getRss() {
        return rss;
    }
}
