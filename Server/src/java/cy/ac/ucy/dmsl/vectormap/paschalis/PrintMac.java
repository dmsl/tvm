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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * It is used to generate a partial radiomap based on the given bloom filter. This generates the
 * body of the radiomap that consists of the rss values that match with the mac addresses in the
 * hbase database.
 */
public class PrintMac {

    /** Mac address to build radiomap */
    String macAddresses;

    /** Writer to Servlet output */
    PrintWriter writer;

    int method;
    int dataSet;

    public PrintMac(String macAddresses, int method, int dataSet, PrintWriter writer) {
        this.macAddresses = macAddresses;
        this.writer = writer;
        this.method = method;
        this.dataSet = dataSet;
    }

    /**
     * Runs the algorithms and prints matched macs or builds Partial Radiomap
     *
     * @return JSon object filled with result
     * @throws IOException
     */
    public boolean runMethod() throws IOException {

        // Use printBloom class
        PrintBloom printBloom = new PrintBloom(writer);
        String[] multiMacs = macAddresses.split(",");

        //Save all macs to arraylist
        ArrayList<String> macToFetch = new ArrayList<String>();
        for (int i = 0; i < multiMacs.length; i++) {
            macToFetch.add(multiMacs[i]);
        }

        boolean foundData = false;

        // Single mac operation
        if (multiMacs.length == 1) {
            //Print result with method 1
            if (method == 1) {
                return printMacMethod1();
            }
            // Print result with method 2
            else if (method == 2) {
                return printMacRadiomapVersion2();
            } else if (method == 3) {
                return printBloom.printPartialRadiomapSmartVersion(macToFetch, dataSet);
            }
        }
        //method = 3 . Multi mac operation
        else {
            //change this
            if (method == 1) {

                foundData = printBloom.printPartialRadiomapMacValuePairs(macToFetch);
            }
            //change this
            else if (method == 2) {

                foundData = printBloom.printPartialRadiomapVersion2(macToFetch);
            } else if (method == 3) {
                foundData = printBloom.printPartialRadiomapSmartVersion(macToFetch, dataSet);
            }
        }

        //Nothing runned
        return foundData;
    }

    private boolean printMacMethod1() throws IOException {
        boolean foundData = false;

        // new scanner
        Scan scan = new Scan();

        SingleColumnValueFilter notNullFilter =
                new SingleColumnValueFilter(
                        MapperBulkLoadRadiomap.SRV_COL_FAM,
                        Bytes.toBytes(this.macAddresses),
                        CompareOp.NOT_EQUAL,
                        Bytes.toBytes("WR")); // a wrong value
        notNullFilter.setFilterIfMissing(true);

        // Set the filter: if MAC exists in row, then print it
        scan.setFilter(notNullFilter);

        // Scanner to our table
        ResultScanner scanner = Base.getHTableMacs(dataSet).getScanner(scan);

        String key;
        for (Result result : scanner) {

            if (!foundData) {
                //Print JSON Object Header, only the first time
                Serve.printString(writer, JSonObj.printHeader(Base.CODE_SUCCESS));
            } else {
                // Print a new line
                Serve.printString(writer, "\n");
            }

            boolean keyFlag = true;
            foundData = true;

            for (KeyValue kv : result.raw()) {

                key = Bytes.toString(kv.getRow());

                if (keyFlag) {
                    // Print keys of rows
                    Serve.printString(writer, key);
                    keyFlag = false;
                }
                // Add pairs of MAC Addresses + RSS Values
                Serve.printString(
                        writer,
                        ", "
                                + Bytes.toString(kv.getQualifier())
                                + "="
                                + Bytes.toString(kv.getValue()));
            }
        }

        if (foundData)
            //Print ending of JSon Object
            Serve.printString(writer, JSonObj.printEnding());

        scanner.close();

        return foundData;
    }

    private boolean printMacRadiomapVersion2() throws IOException {
        boolean foundData = false;

        // new scanner
        Scan scan = new Scan();

        SingleColumnValueFilter notNullFilter =
                new SingleColumnValueFilter(
                        MapperBulkLoadRadiomap.SRV_COL_FAM,
                        Bytes.toBytes(this.macAddresses),
                        CompareOp.NOT_EQUAL,
                        Bytes.toBytes("WR")); // a wrong value
        notNullFilter.setFilterIfMissing(true);

        // Set the filter: if MAC exists in row, then print it
        scan.setFilter(notNullFilter);

        // Scanner to our table
        ResultScanner scanner = Base.getHTableMacs(dataSet).getScanner(scan);

        // Unique MAC Addresses for partial Radiomap
        ArrayList<String> uniqueMacs = new ArrayList<String>();
        ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
        String[] keyData;

        for (Result result : scanner) {

            if (!foundData) {
                //Print JSON Object Header, only the first time
                Serve.printString(writer, JSonObj.printHeader(Base.CODE_SUCCESS));
            }

            boolean keyFlag = true;
            foundData = true;

            //Lat, Lon, Heading values, mac
            String x = null, y = null, h = null, mac = null;

            for (KeyValue kv : result.raw()) {

                // Save key data of row
                if (keyFlag) {

                    keyData = Bytes.toString(kv.getRow()).split(":");

                    //TODO REMOVE THIS WHEN ALL RMAPS ARE LAT, LON
                    if (keyData.length == 4) {
                        x = keyData[1];
                        y = keyData[2];
                        h = keyData[3];
                    } else if (keyData.length == 3) {
                        x = keyData[0];
                        y = keyData[1];
                        h = keyData[2];
                    }

                    keyFlag = false;
                }

                mac = Bytes.toString(kv.getQualifier());
                //Save unique macs
                Coordinate.saveUniqueMac(uniqueMacs, mac);

                //Save new coordinate
                coords.add(new Coordinate(x, y, h, mac, Bytes.toString(kv.getValue())));
            }
        }

        // Print radiomap in new format
        Coordinate.printRadiomap(uniqueMacs, coords, writer);

        // Print JSon ending
        if (foundData)
            // Print ending of JSon Object
            Serve.printString(writer, JSonObj.printEnding());

        scanner.close();

        return foundData;
    }
}
