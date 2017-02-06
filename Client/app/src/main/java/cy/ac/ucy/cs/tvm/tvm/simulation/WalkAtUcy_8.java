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

/**
 * INFO: Class automatically generated by WalkSimulatorClassGenerator to simulate a real
 * localization scenario that was recorded at CS UCY Campus.
 */
package cy.ac.ucy.cs.tvm.tvm.simulation;

import android.util.Log;

import java.util.ArrayList;

import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;

/**
 * INFO: Class automatically generated by WalkSimulatorClassGenerator. Simulating 8 positions at
 * Computer Science dept. @ UCY Positions has real recorded data.
 */
public class WalkAtUcy_8 extends WalkSimulator {

    public static final int MAX_POSITIONS = 8;
    public static final String TAG = WalkAtUcy_8.class.getSimpleName();

    public WalkAtUcy_8(App app) {
        super("UCY_8positions", app, MAX_POSITIONS);
    }

    /**
     * @param num of position at ucy to return
     * @return real recorded data for 15 positions @ cs.ucy building
     */
    public ArrayList<LogRecord> getPosition(int num) {

        switch (num) {
            case 1:
                return getLogRecordUcyPosition1();
            case 2:
                return getLogRecordUcyPosition2();
            case 3:
                return getLogRecordUcyPosition3();
            case 4:
                return getLogRecordUcyPosition4();
            case 5:
                return getLogRecordUcyPosition5();
            case 6:
                return getLogRecordUcyPosition6();
            case 7:
                return getLogRecordUcyPosition7();
            case 8:
                return getLogRecordUcyPosition8();
            default:
                Log.e(TAG, "Gettting position null for: " + num);
                return null;
        }
    }

    /** @return position 2 in ucy Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition2() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:0b:fd:4a:71:ce", -84));
        result.add(new LogRecord("00:24:17:21:1a:95", -80));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -66));
        result.add(new LogRecord("d4:d7:48:b6:3e:b0", -83));
        result.add(new LogRecord("d4:d7:48:b0:8f:40", -72));
        result.add(new LogRecord("24:b6:57:7d:46:c0", -89));
        result.add(new LogRecord("00:25:9c:99:90:d4", -78));
        result.add(new LogRecord("24:b6:57:b4:f6:20", -90));
        result.add(new LogRecord("00:0e:84:4b:0b:e8", -89));
        result.add(new LogRecord("d4:d7:48:7d:b5:10", -72));
        result.add(new LogRecord("00:1d:45:51:38:a2", -81));
        result.add(new LogRecord("00:1d:45:51:38:a0", -81));
        result.add(new LogRecord("00:3a:98:2a:65:82", -83));
        result.add(new LogRecord("00:3a:98:2a:65:80", -83));
        result.add(new LogRecord("00:1f:6c:a8:e9:12", -89));
        result.add(new LogRecord("00:1f:6c:a8:e9:10", -90));
        result.add(new LogRecord("00:0b:fd:4a:71:b2", -86));
        result.add(new LogRecord("00:1d:45:51:38:a1", -89));
        result.add(new LogRecord("00:3a:98:2a:65:81", -83));
        result.add(new LogRecord("00:1f:6c:a8:e9:11", -90));
        result.add(new LogRecord("00:0e:84:4b:0a:f8", -89));
        result.add(new LogRecord("00:0e:84:4b:0b:f6", -92));

        return result;
    }

    /** @return position 5 in ucy. Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition5() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:24:17:21:1a:95", 0));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -64));
        result.add(new LogRecord("d4:d7:48:b6:3e:b0", -76));
        result.add(new LogRecord("d4:d7:48:b0:8f:40", -67));
        result.add(new LogRecord("24:b6:57:7d:46:c0", -77));
        result.add(new LogRecord("00:25:9c:99:90:d4", -68));
        result.add(new LogRecord("24:b6:57:b4:f6:20", -85));
        result.add(new LogRecord("d4:d7:48:7d:b5:10", -73));
        result.add(new LogRecord("00:1d:45:51:38:a2", -87));
        result.add(new LogRecord("00:3a:98:2a:65:82", -88));
        result.add(new LogRecord("00:0b:fd:4a:71:b2", -86));
        result.add(new LogRecord("00:1d:45:51:38:a1", -82));
        result.add(new LogRecord("00:3a:98:2a:65:81", -90));
        result.add(new LogRecord("00:0e:84:4b:0b:f6", -86));
        result.add(new LogRecord("24:b6:57:9f:54:10", -85));
        result.add(new LogRecord("00:3a:98:2a:65:32", -86));
        result.add(new LogRecord("d4:d7:48:7d:b2:d0", -93));
        result.add(new LogRecord("00:0b:fd:4a:71:ce", -78));
        result.add(new LogRecord("00:0e:84:4b:0b:7c", -79));
        result.add(new LogRecord("00:0e:38:7a:37:77", -81));
        result.add(new LogRecord("00:0e:84:4b:0b:ec", -84));
        result.add(new LogRecord("02:28:e8:12:62:11", -92));
        result.add(new LogRecord("00:0b:fd:f3:ab:0b", -89));
        result.add(new LogRecord("00:0e:84:4b:0b:86", -90));

        return result;
    }

    // /**
    // *
    // * @return position 3 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition3() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -89));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", -82));
    // result.add(new LogRecord("d4:d7:48:b6:3e:b0", -72));
    // result.add(new LogRecord("d4:d7:48:b0:8f:40", -83));
    // result.add(new LogRecord("24:b6:57:7d:46:c0", -77));
    // result.add(new LogRecord("00:25:9c:99:90:d4", -68));
    // result.add(new LogRecord("24:b6:57:b4:f6:20", -85));
    // result.add(new LogRecord("d4:d7:48:7d:b5:10", -88));
    // result.add(new LogRecord("00:1d:45:51:38:a2", -87));
    // result.add(new LogRecord("00:3a:98:2a:65:82", -89));
    // result.add(new LogRecord("00:0b:fd:4a:71:b2", -86));
    // result.add(new LogRecord("00:1d:45:51:38:a1", -87));
    // result.add(new LogRecord("00:3a:98:2a:65:81", -89));
    // result.add(new LogRecord("00:0e:84:4b:0b:f6", -86));
    // result.add(new LogRecord("24:b6:57:9f:54:10", -85));
    // result.add(new LogRecord("00:3a:98:2a:65:32", -76));
    // result.add(new LogRecord("d4:d7:48:7d:b2:d0", -93));
    // result.add(new LogRecord("00:0b:fd:4a:71:ce", -78));
    // result.add(new LogRecord("00:0e:84:4b:0b:7c", -79));
    // result.add(new LogRecord("00:0e:38:7a:37:77", -81));
    // result.add(new LogRecord("00:0e:84:4b:0b:ec", -84));
    // result.add(new LogRecord("02:28:e8:12:62:11", -92));
    // result.add(new LogRecord("00:0b:fd:f3:ab:0b", -89));
    // result.add(new LogRecord("00:0e:84:4b:0b:86", -90));
    // result.add(new LogRecord("00:3a:98:2a:65:30", -75));
    // result.add(new LogRecord("00:1f:6c:a8:e9:12", -84));
    // result.add(new LogRecord("00:1f:6c:a8:e9:10", -85));
    // result.add(new LogRecord("00:1d:45:51:38:a0", -87));
    // result.add(new LogRecord("00:3a:98:2a:65:80", -89));
    // result.add(new LogRecord("00:3a:98:2a:65:31", -76));
    // result.add(new LogRecord("00:1f:6c:a8:e9:11", -84));
    // result.add(new LogRecord("00:0e:84:4b:0b:e8", -83));
    // result.add(new LogRecord("00:0e:84:4b:0a:f8", -83));
    // result.add(new LogRecord("00:0e:84:4b:0a:f1", -91));
    //
    //
    //
    //
    // return result;
    // }
    //

    /** @return position 3 in ucy. Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition3() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:24:17:21:1a:95", -89));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -73));
        result.add(new LogRecord("d4:d7:48:b0:8f:40", -66));
        result.add(new LogRecord("00:25:9c:99:90:d4", 0));
        result.add(new LogRecord("d4:d7:48:7d:b5:10", -64));
        result.add(new LogRecord("00:1d:45:51:38:a2", -72));
        result.add(new LogRecord("00:3a:98:2a:65:82", -72));
        result.add(new LogRecord("00:1d:45:51:38:a1", -79));
        result.add(new LogRecord("00:3a:98:2a:65:81", -69));
        result.add(new LogRecord("00:3a:98:2a:65:32", -77));
        result.add(new LogRecord("00:3a:98:2a:65:30", -85));
        result.add(new LogRecord("00:1f:6c:a8:e9:12", -75));
        result.add(new LogRecord("00:1f:6c:a8:e9:10", -75));
        result.add(new LogRecord("00:1d:45:51:38:a0", -71));
        result.add(new LogRecord("00:3a:98:2a:65:80", -70));
        result.add(new LogRecord("00:3a:98:2a:65:31", -73));
        result.add(new LogRecord("00:1f:6c:a8:e9:11", -75));
        result.add(new LogRecord("00:0e:84:4b:0b:e8", -82));
        result.add(new LogRecord("00:0e:84:4b:0a:f8", -83));
        result.add(new LogRecord("00:0e:84:4b:0a:f1", -91));
        result.add(new LogRecord("00:0e:84:4b:0b:b5", -82));
        result.add(new LogRecord("00:0e:84:4b:0b:02", -91));

        return result;
    }

    /** @return position 1 in ucy. Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition1() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:24:17:21:1a:95", -82));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -71));
        result.add(new LogRecord("d4:d7:48:b0:8f:40", -84));
        result.add(new LogRecord("00:25:9c:99:90:d4", -90));
        result.add(new LogRecord("d4:d7:48:7d:b5:10", -64));
        result.add(new LogRecord("00:1d:45:51:38:a2", -82));
        result.add(new LogRecord("00:3a:98:2a:65:82", -85));
        result.add(new LogRecord("00:1d:45:51:38:a1", -83));
        result.add(new LogRecord("00:3a:98:2a:65:81", -86));
        result.add(new LogRecord("00:3a:98:2a:65:32", -83));
        result.add(new LogRecord("00:3a:98:2a:65:30", -80));
        result.add(new LogRecord("00:1f:6c:a8:e9:12", -79));
        result.add(new LogRecord("00:1f:6c:a8:e9:10", -79));
        result.add(new LogRecord("00:1d:45:51:38:a0", -83));
        result.add(new LogRecord("00:3a:98:2a:65:80", -85));
        result.add(new LogRecord("00:3a:98:2a:65:31", -83));
        result.add(new LogRecord("00:1f:6c:a8:e9:11", -79));
        result.add(new LogRecord("00:0e:84:4b:0b:e8", -86));
        result.add(new LogRecord("00:0e:84:4b:0a:f8", -83));
        result.add(new LogRecord("00:0e:84:4b:0a:f1", -88));
        result.add(new LogRecord("00:0e:84:4b:0b:b5", -82));
        result.add(new LogRecord("00:0e:84:4b:0b:02", -91));
        result.add(new LogRecord("d4:d7:48:b6:3e:b0", -85));
        result.add(new LogRecord("00:0e:84:4b:0a:8f", -86));
        result.add(new LogRecord("00:0e:84:4b:0b:ec", -88));
        result.add(new LogRecord("00:0e:84:4b:0b:f6", -92));

        return result;
    }

    // /**
    // *
    // * @return position 6 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition6() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    //
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -87));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", -71));
    // result.add(new LogRecord("d4:d7:48:b0:8f:40", -82));
    // result.add(new LogRecord("00:25:9c:99:90:d4", -17));
    // result.add(new LogRecord("00:1d:45:51:38:a2", -77));
    // result.add(new LogRecord("00:3a:98:2a:65:82", -81));
    // result.add(new LogRecord("00:1d:45:51:38:a1", -77));
    // result.add(new LogRecord("00:3a:98:2a:65:81", -78));
    // result.add(new LogRecord("00:3a:98:2a:65:32", -71));
    // result.add(new LogRecord("00:3a:98:2a:65:30", -71));
    // result.add(new LogRecord("00:1f:6c:a8:e9:12", -75));
    // result.add(new LogRecord("00:1f:6c:a8:e9:10", -75));
    // result.add(new LogRecord("00:1d:45:51:38:a0", -77));
    // result.add(new LogRecord("00:3a:98:2a:65:80", -78));
    // result.add(new LogRecord("00:3a:98:2a:65:31", -71));
    // result.add(new LogRecord("00:1f:6c:a8:e9:11", -75));
    // result.add(new LogRecord("00:0e:84:4b:0b:e8", -80));
    // result.add(new LogRecord("00:0e:84:4b:0a:f8", -80));
    // result.add(new LogRecord("00:0e:84:4b:0b:02", -89));
    // result.add(new LogRecord("00:0e:84:4b:0a:8f", -86));
    // result.add(new LogRecord("00:0e:84:4b:0b:ec", -92));
    // result.add(new LogRecord("d4:d7:48:7d:b5:10", -83));
    // result.add(new LogRecord("24:b6:57:b4:f6:20", -85));
    // result.add(new LogRecord("00:0e:84:4b:0a:f1", -91));
    //
    // return result;
    // }

    /** @return position 4 in ucy. Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition4() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:24:17:21:1a:95", -84));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -65));
        result.add(new LogRecord("d4:d7:48:b0:8f:40", -89));
        result.add(new LogRecord("00:25:9c:99:90:d4", -85));
        result.add(new LogRecord("00:1d:45:51:38:a2", -84));
        result.add(new LogRecord("00:3a:98:2a:65:82", -82));
        result.add(new LogRecord("00:1d:45:51:38:a1", -85));
        result.add(new LogRecord("00:3a:98:2a:65:81", -82));
        result.add(new LogRecord("00:3a:98:2a:65:32", -71));
        result.add(new LogRecord("00:3a:98:2a:65:30", -70));
        result.add(new LogRecord("00:1f:6c:a8:e9:12", -77));
        result.add(new LogRecord("00:1f:6c:a8:e9:10", -86));
        result.add(new LogRecord("00:1d:45:51:38:a0", -83));
        result.add(new LogRecord("00:3a:98:2a:65:80", -84));
        result.add(new LogRecord("00:3a:98:2a:65:31", -71));
        result.add(new LogRecord("00:1f:6c:a8:e9:11", -77));
        result.add(new LogRecord("00:0e:84:4b:0b:e8", -82));
        result.add(new LogRecord("00:0e:84:4b:0a:f8", -16));
        result.add(new LogRecord("d4:d7:48:7d:b5:10", -86));
        result.add(new LogRecord("24:b6:57:b4:f6:20", -85));
        result.add(new LogRecord("00:0e:84:4b:0a:f1", -87));
        result.add(new LogRecord("d4:d7:48:b6:3e:b0", -87));
        result.add(new LogRecord("6c:9c:ed:86:03:51", -90));

        return result;
    }

    // /**
    // *
    // * @return position 8 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition8() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -84));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", -83));
    // result.add(new LogRecord("d4:d7:48:b0:8f:40", -81));
    // result.add(new LogRecord("00:25:9c:99:90:d4", -80));
    // result.add(new LogRecord("00:1d:45:51:38:a2", -77));
    // result.add(new LogRecord("00:3a:98:2a:65:82", -80));
    // result.add(new LogRecord("00:1d:45:51:38:a1", -77));
    // result.add(new LogRecord("00:3a:98:2a:65:81", -80));
    // result.add(new LogRecord("00:3a:98:2a:65:32", -87));
    // result.add(new LogRecord("00:3a:98:2a:65:30", -86));
    // result.add(new LogRecord("00:1f:6c:a8:e9:12", -86));
    // result.add(new LogRecord("00:1f:6c:a8:e9:10", -86));
    // result.add(new LogRecord("00:1d:45:51:38:a0", -77));
    // result.add(new LogRecord("00:3a:98:2a:65:80", -80));
    // result.add(new LogRecord("00:3a:98:2a:65:31", -86));
    // result.add(new LogRecord("00:1f:6c:a8:e9:11", -86));
    // result.add(new LogRecord("00:0e:84:4b:0b:e8", -82));
    // result.add(new LogRecord("00:0e:84:4b:0a:f8", -16));
    // result.add(new LogRecord("d4:d7:48:7d:b5:10", -81));
    // result.add(new LogRecord("24:b6:57:b4:f6:20", -85));
    // result.add(new LogRecord("00:0e:84:4b:0a:f1", -87));
    // result.add(new LogRecord("d4:d7:48:b6:3e:b0", -88));
    // result.add(new LogRecord("6c:9c:ed:86:03:51", -90));
    // result.add(new LogRecord("d4:d7:48:d8:2c:70", -88));
    // result.add(new LogRecord("00:0e:84:4b:0a:8f", -93));
    //
    //
    //
    // return result;
    // }

    // /**
    // *
    // * @return position 9 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition9() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -84));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", -76));
    // result.add(new LogRecord("d4:d7:48:b0:8f:40", -81));
    // result.add(new LogRecord("00:25:9c:99:90:d4", -81));
    // result.add(new LogRecord("00:1d:45:51:38:a2", -81));
    // result.add(new LogRecord("00:3a:98:2a:65:82", -81));
    // result.add(new LogRecord("00:1d:45:51:38:a1", -81));
    // result.add(new LogRecord("00:3a:98:2a:65:81", -81));
    // result.add(new LogRecord("00:3a:98:2a:65:32", -84));
    // result.add(new LogRecord("00:3a:98:2a:65:30", -82));
    // result.add(new LogRecord("00:1f:6c:a8:e9:12", -83));
    // result.add(new LogRecord("00:1f:6c:a8:e9:10", -84));
    // result.add(new LogRecord("00:1d:45:51:38:a0", -81));
    // result.add(new LogRecord("00:3a:98:2a:65:80", -81));
    // result.add(new LogRecord("00:3a:98:2a:65:31", -86));
    // result.add(new LogRecord("00:1f:6c:a8:e9:11", -85));
    // result.add(new LogRecord("d4:d7:48:7d:b5:10", -85));
    // result.add(new LogRecord("d4:d7:48:b6:3e:b0", -88));
    // result.add(new LogRecord("d4:d7:48:d8:2c:70", -88));
    // result.add(new LogRecord("00:0e:84:4b:0a:8f", -92));
    //
    // return result;
    // }

    // /**
    // *
    // * @return position 10 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition10() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -77));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", -66));
    // result.add(new LogRecord("d4:d7:48:b0:8f:40", -70));
    // result.add(new LogRecord("00:25:9c:99:90:d4", -77));
    // result.add(new LogRecord("00:1d:45:51:38:a2", -88));
    // result.add(new LogRecord("00:3a:98:2a:65:82", -87));
    // result.add(new LogRecord("00:1d:45:51:38:a1", -88));
    // result.add(new LogRecord("00:3a:98:2a:65:81", -86));
    // result.add(new LogRecord("00:3a:98:2a:65:32", -84));
    // result.add(new LogRecord("00:3a:98:2a:65:30", -90));
    // result.add(new LogRecord("00:1f:6c:a8:e9:12", -90));
    // result.add(new LogRecord("00:1f:6c:a8:e9:10", -93));
    // result.add(new LogRecord("00:1d:45:51:38:a0", -86));
    // result.add(new LogRecord("00:3a:98:2a:65:80", -79));
    // result.add(new LogRecord("00:3a:98:2a:65:31", -85));
    // result.add(new LogRecord("00:1f:6c:a8:e9:11", -89));
    // result.add(new LogRecord("d4:d7:48:7d:b5:10", -73));
    // result.add(new LogRecord("d4:d7:48:b6:3e:b0", -79));
    // result.add(new LogRecord("d4:d7:48:d8:2c:70", -88));
    // result.add(new LogRecord("00:0e:84:4b:0a:8f", -92));
    // result.add(new LogRecord("24:b6:57:9f:54:10", -89));
    // result.add(new LogRecord("24:b6:57:b4:f6:20", -86));
    // result.add(new LogRecord("24:b6:57:7d:46:c0", -91));
    // result.add(new LogRecord("00:0e:84:4b:0b:ec", 0));
    // result.add(new LogRecord("00:0b:fd:4a:71:ce", -80));
    // result.add(new LogRecord("00:0e:84:4b:0b:7c", -82));
    // result.add(new LogRecord("00:0b:fd:4a:71:b2", -82));
    // result.add(new LogRecord("00:0e:38:7a:37:77", -83));
    // result.add(new LogRecord("00:0e:84:4b:0b:f6", -85));
    // result.add(new LogRecord("00:0b:fd:f3:ab:0b", -87));
    // result.add(new LogRecord("00:0e:84:4b:0b:02", -91));
    //
    //
    // return result;
    // }

    // /**
    // *
    // * @return position 11 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition11() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -82));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", 0));
    // result.add(new LogRecord("d4:d7:48:b0:8f:40", -80));
    // result.add(new LogRecord("00:25:9c:99:90:d4", -90));
    // result.add(new LogRecord("00:1d:45:51:38:a2", -88));
    // result.add(new LogRecord("00:3a:98:2a:65:82", -91));
    // result.add(new LogRecord("00:1d:45:51:38:a1", -88));
    // result.add(new LogRecord("00:3a:98:2a:65:81", -86));
    // result.add(new LogRecord("00:3a:98:2a:65:32", -84));
    // result.add(new LogRecord("00:3a:98:2a:65:30", -90));
    // result.add(new LogRecord("00:1f:6c:a8:e9:12", -90));
    // result.add(new LogRecord("00:1f:6c:a8:e9:10", -93));
    // result.add(new LogRecord("00:1d:45:51:38:a0", -86));
    // result.add(new LogRecord("00:3a:98:2a:65:80", -79));
    // result.add(new LogRecord("00:3a:98:2a:65:31", -85));
    // result.add(new LogRecord("00:1f:6c:a8:e9:11", -89));
    // result.add(new LogRecord("d4:d7:48:7d:b5:10", -86));
    // result.add(new LogRecord("d4:d7:48:b6:3e:b0", -73));
    // result.add(new LogRecord("24:b6:57:9f:54:10", -89));
    // result.add(new LogRecord("24:b6:57:b4:f6:20", -86));
    // result.add(new LogRecord("24:b6:57:7d:46:c0", -91));
    // result.add(new LogRecord("00:0e:84:4b:0b:ec", 0));
    // result.add(new LogRecord("00:0b:fd:4a:71:ce", -80));
    // result.add(new LogRecord("00:0e:84:4b:0b:7c", -82));
    // result.add(new LogRecord("00:0b:fd:4a:71:b2", -88));
    // result.add(new LogRecord("00:0e:38:7a:37:77", -83));
    // result.add(new LogRecord("00:0e:84:4b:0b:f6", -92));
    // result.add(new LogRecord("00:0b:fd:f3:ab:0b", -87));
    // result.add(new LogRecord("00:0e:84:4b:0b:02", -91));
    //
    //
    // return result;
    // }

    /** @return position 7 in ucy. Position is real but i save some time */
    public static ArrayList<LogRecord> getLogRecordUcyPosition7() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:24:17:21:1a:95", -75));
        result.add(new LogRecord("d4:d7:48:b6:3e:b0", -70));
        result.add(new LogRecord("d4:d7:48:b6:3b:80", -84));
        result.add(new LogRecord("d4:d7:48:d8:2c:70", -83));

        return result;
    }

    /** @return position 6 in ucy. Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition6() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("00:24:17:21:1a:95", -83));
        result.add(new LogRecord("d4:d7:48:b6:3e:b0", -85));
        result.add(new LogRecord("d4:d7:48:b6:3b:80", -70));
        result.add(new LogRecord("d4:d7:48:d8:2c:70", -76));
        result.add(new LogRecord("24:b6:57:7b:e8:a0", -81));
        result.add(new LogRecord("d4:d7:48:b6:37:c0", -86));
        result.add(new LogRecord("d4:d7:48:d8:28:30", -83));
        result.add(new LogRecord("d4:d7:48:b6:37:40", -88));
        result.add(new LogRecord("d4:d7:48:d8:28:b0", -84));
        result.add(new LogRecord("24:b6:57:ae:40:30", -84));
        result.add(new LogRecord("d4:d7:48:b0:97:10", -88));
        result.add(new LogRecord("24:b6:57:7b:ee:d0", -89));
        result.add(new LogRecord("d4:d7:48:b0:95:f0", -90));
        result.add(new LogRecord("00:0b:fd:4a:71:ce", -70));
        result.add(new LogRecord("00:16:b6:ee:00:7f", -77));
        result.add(new LogRecord("00:0b:fd:4a:71:d6", -83));
        result.add(new LogRecord("00:0e:84:4b:0b:ec", -86));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -90));
        result.add(new LogRecord("00:0e:84:4b:0c:0a", -91));

        return result;
    }

    // /**
    // *
    // * @return position 14 in ucy. Position is real but i save some time
    // */
    // public static ArrayList<LogRecord> getLogRecordUcyPosition14() {
    // ArrayList<LogRecord> result = new ArrayList<LogRecord>();
    //
    // result.add(new LogRecord("00:24:17:21:1a:95", -83));
    // result.add(new LogRecord("d4:d7:48:b6:3e:b0", -85));
    // result.add(new LogRecord("d4:d7:48:b6:3b:80", -84));
    // result.add(new LogRecord("d4:d7:48:d8:2c:70", -76));
    // result.add(new LogRecord("24:b6:57:7b:e8:a0", -81));
    // result.add(new LogRecord("d4:d7:48:b6:37:c0", -86));
    // result.add(new LogRecord("d4:d7:48:d8:28:30", -83));
    // result.add(new LogRecord("d4:d7:48:b6:37:40", -88));
    // result.add(new LogRecord("d4:d7:48:d8:28:b0", -84));
    // result.add(new LogRecord("24:b6:57:ae:40:30", -84));
    // result.add(new LogRecord("d4:d7:48:b0:97:10", -88));
    // result.add(new LogRecord("24:b6:57:7b:ee:d0", -89));
    // result.add(new LogRecord("d4:d7:48:b0:95:f0", -90));
    // result.add(new LogRecord("00:0b:fd:4a:71:ce", -66));
    // result.add(new LogRecord("00:16:b6:ee:00:7f", -77));
    // result.add(new LogRecord("00:0b:fd:4a:71:d6", -83));
    // result.add(new LogRecord("00:0e:84:4b:0b:ec", -86));
    // result.add(new LogRecord("00:0b:fd:4a:71:a2", -93));
    // result.add(new LogRecord("00:0e:84:4b:0c:0a", -91));
    //
    // return result;
    // }

    /** @return position 8 in ucy. Position is real but i save some time */
    public ArrayList<LogRecord> getLogRecordUcyPosition8() {
        ArrayList<LogRecord> result = new ArrayList<LogRecord>();

        result.add(new LogRecord("d4:d7:48:d8:2c:70", -83));
        result.add(new LogRecord("24:b6:57:7b:e8:a0", -76));
        result.add(new LogRecord("00:0b:fd:4a:71:ce", -87));
        result.add(new LogRecord("00:0e:84:4b:0b:ec", -70));
        result.add(new LogRecord("00:0b:fd:4a:71:a2", -92));

        return result;
    }
}