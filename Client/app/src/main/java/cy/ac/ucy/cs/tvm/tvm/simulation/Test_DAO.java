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

package cy.ac.ucy.cs.tvm.tvm.simulation;

import android.os.Build;
import cy.ac.ucy.cs.tvm.App;
import java.util.ArrayList;

/** DAO class used for tests */
public class Test_DAO {

    private ArrayList<Float> mTimeDownload = new ArrayList<Float>();
    private ArrayList<Float> mTimeParse = new ArrayList<Float>();
    private ArrayList<Float> mTimeLocalise = new ArrayList<Float>();
    private ArrayList<Integer> mNumberOfMessages = new ArrayList<Integer>();
    private final String deviceUsed;
    private int mDatasetUsed;
    private App app;
    int position; // of current user

    int experimentSeries = -1;
    String anonymityAlgorithm;
    String route;

    public Test_DAO(App app, int series, String routeName) {
        this.app = app;
        this.deviceUsed = getDeviceName();
        this.mDatasetUsed = app.datasetInUse;
        this.experimentSeries = series;
        anonymityAlgorithm = app.anonymityAlgorithm.toString();
        this.route = routeName;
    }

    public void addMessagesNumber(int msgs) {
        this.mNumberOfMessages.add(msgs);
    }

    public void addLocaliseTime(float time) {
        this.mTimeLocalise.add(time);
    }

    public void addDownloadTime(float time) {
        this.mTimeDownload.add(time);
    }

    public void addParseTime(float time) {
        this.mTimeParse.add(time);
    }

    private String buildMessagesStr() {
        String result = "";

        for (float l : mNumberOfMessages) {
            result += l + ",";
        }
        if (!result.equals("")) result = result.substring(0, result.length() - 2);

        return result;
    }

    private String buildTimesDownloadStr() {
        String result = "";

        for (float l : mTimeDownload) {
            result += l + ",";
        }
        if (!result.equals("")) result = result.substring(0, result.length() - 2);

        return result;
    }

    private String buildTimesLocalizeStr() {
        String result = "";

        for (float l : mTimeLocalise) {
            result += l + ",";
        }

        if (!result.equals("")) result = result.substring(0, result.length() - 2);

        return result;
    }

    private String buildTimesParseStr() {
        String result = "";

        for (float l : mTimeParse) {
            result += l + ",";
        }
        if (!result.equals("")) result = result.substring(0, result.length() - 2);

        return result;
    }

    private float getLocaliseTimesSum() {
        float result = -1;

        for (float l : mTimeLocalise) {
            result += l;
        }

        return result;
    }

    private float getDownloadTimesSum() {
        float result = -1;

        for (float l : mTimeDownload) {
            result += l;
        }

        return result;
    }

    private float getParseTimesSum() {
        float result = -1;

        for (float l : mTimeParse) {
            result += l;
        }
        return result;
    }

    private int getTotalMessages() {
        int result = -1;

        for (int l : mNumberOfMessages) {
            result += l;
        }
        return result;
    }

    /** Return the total times for download, parse and localize */
    private float getTotalTimes() {
        return (getDownloadTimesSum() + getLocaliseTimesSum() + getParseTimesSum());
    }

    /** @return */
    public String getHttpGetRequestParameters() {

        return "experimentseries="
                + experimentSeries
                + "&device="
                + deviceUsed
                + "&dataset="
                + mDatasetUsed
                + "&kanonymity="
                + app.getkAnonymity()
                + "&algorithm="
                + anonymityAlgorithm
                + "&route="
                + route
                + "&messagesTotal="
                + getTotalMessages()
                + "&timeTotal="
                + getTotalTimes()
                + "&timeDownloadTotal="
                + getDownloadTimesSum()
                + "&timeParseTotal="
                + getParseTimesSum()
                + "&timeLocaliseTotal="
                + getLocaliseTimesSum()
                + "&messages="
                + buildMessagesStr()
                + "&timelocalize="
                + buildTimesLocalizeStr()
                + "&timedownload="
                + buildTimesDownloadStr()
                + "&timeparse="
                + buildTimesParseStr();
    }

    /** get device name */
    public String getDeviceName() {

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return (capitalize(manufacturer) + "_" + model).replace(" ", "");
        }
    }

    /** helper for device name */
    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
