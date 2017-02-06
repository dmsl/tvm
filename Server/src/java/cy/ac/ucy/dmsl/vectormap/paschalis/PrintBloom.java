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
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * It is used to generate a partial radiomap based on the given bloom filter. This generates the
 * header of the radiomap that consists of the mac addresses that match with the given bloom filter.
 */
public class PrintBloom {

    /** Build radiomap if true, otherwise show matched MAC Addresses */
    int method;

    public static final int METHOD_MAC_RSS_PAIR = 1;
    public static final int METHOD_RMAP_V2 = 2;

    /** Bloom filter to get MAC Addresses or build partial radiomap */
    private String bloomFilter = null;

    /** Writer to Servlet output */
    PrintWriter writer;

    boolean isMapRed = false;

    /** Which dataset is chosen. 0: real 1: 3mbs? 2: ?mbs 3: ?mbs 4: ?mbs 5: ?mbs */
    private int dataSet;

    public PrintBloom(String bloomFilter, int method, int dataSet, PrintWriter writer) {

        this.bloomFilter = bloomFilter;
        this.method = method;
        this.writer = writer;
        this.dataSet = dataSet;
    }

    public PrintBloom(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Runs the algorithms and prints matched macs or builds Partial Radiomap
     *
     * @return JSon object filled with result
     * @throws IOException
     */
    public boolean runMethod() throws IOException {
        boolean foundData = false;

        // MAC Addresses matched with the bloom filter
        ArrayList<String> macMatched = new ArrayList<String>();

        // Match bloomfilter with MAC Addresses
        matchMACSwithBloomfilter(macMatched);

        //Print only macs that matched
        if (method == 0) {
            String message = "";
            for (String mac : macMatched) {
                if (foundData) {
                    message += "\n";
                }

                foundData = true;
                message += mac;
            }

            //If result found
            if (foundData) {
                //Print JSON Object Header
                Serve.printString(writer, JSonObj.printHeader(Base.CODE_BLOOM_MAC_ONLY));
                Serve.printString(writer, message);

                //Print ending of JSon Object
                Serve.printString(writer, JSonObj.printEnding());
            }
        }
        // Print radiomap
        else if (method == 1) {
            foundData = printPartialRadiomapMacValuePairs(macMatched);
        } else if (method == 2) {
            foundData = printPartialRadiomapVersion2(macMatched);
        } else if (method == 3) {
            foundData = printPartialRadiomapSmartVersion(macMatched, dataSet);
        }

        return foundData;
    }

    private static final String ALL = "all";

    /**
     * @param macAddressInput MACs to build the smart JSon object (v3)
     * @return
     * @throws IOException
     */
    public boolean printPartialRadiomapSmartVersion(
            ArrayList<String> macAddressInput, int datasetToUse) throws IOException {
        boolean foundData = false;
        ResultScanner scanner;

        /// start of of MR job

        // Scan to HBase
        Scan scan = new Scan();

        // Apply filters if its not equal to all
        // otherwise the whole radiomap will produced! (CSR)
        if (!macAddressInput.get(0).equals(ALL)) {

            // One of the filters must pass
            FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ONE);

            //Create filters for all matched MAC Addresses
            for (String macAddress : macAddressInput) {
                // If the MACaddress: WR pair is found, then dont include it
                // This pair wont found because WR will never be as a value in our tables
                // So if a MAC exists, will be included, and if dont, wont
                SingleColumnValueFilter boolMatchFilter =
                        new SingleColumnValueFilter(
                                MapperBulkLoadRadiomap.SRV_COL_FAM,
                                Bytes.toBytes(macAddress),
                                CompareOp.NOT_EQUAL,
                                Bytes.toBytes("WR")); // a dummy value

                //If mac dont exists, wont be included
                // (except if that line is covered by another matched MAC)
                boolMatchFilter.setFilterIfMissing(true);
                filters.addFilter(boolMatchFilter);
            }

            // Set the filter list to scanner
            scan.setFilter(filters);
        }

        // Scanner to our table
        scanner = Base.getHTableMacs(datasetToUse).getScanner(scan);
        //end of mapred job

        // Contains all coordinates for a row
        ArrayList<Coordinate> rowCoordinates = new ArrayList<Coordinate>();
        // Contains all macs for a row
        ArrayList<String> rowMacs = new ArrayList<String>();

        // Contains coordinates to be written on each produced radiomap
        ArrayList<ArrayList<Coordinate>> rmapsCoordinates = new ArrayList<ArrayList<Coordinate>>();
        ArrayList<ArrayList<String>> rmapsUniqueMacs = new ArrayList<ArrayList<String>>();

        // create tables to store each radiomap its coordinates
        for (int i = 0; i < macAddressInput.size(); i++) {
            rmapsCoordinates.add(new ArrayList<Coordinate>());
            rmapsUniqueMacs.add(new ArrayList<String>());
        }

        // In what radiomap to insert each data
        // (it may has to inserted in more than one rmap)
        ArrayList<Integer> whereToInsert = new ArrayList<Integer>();
        ArrayList<String> macsCopy = new ArrayList<String>();
        ArrayList<String> macsNCoords = new ArrayList<String>();
        macsCopy.addAll(macAddressInput); // copy macs to new arraylist
        macsNCoords.addAll(macAddressInput); //add macs values for now

        //Lat, Lon, Heading values, mac
        String x = null, y = null, h = null, mac = null;
        String[] keyData = null;
        boolean keyFlag;

        // Run scanner in hbase, and sort results in K RMaps(in memory)
        // All the results that match with filters
        // == all lines that have values for mac addresses given
        for (Result result : scanner) { // outer for: all matching rows of HBase

            //Clear indices
            whereToInsert.clear();
            //Clear coordinates of a row
            rowCoordinates.clear();
            rowMacs.clear();

            // Flag to avoid multiple inserts of key (x,y,h)
            keyFlag = true;
            // Found results in HBase
            foundData = true;

            // Process each row
            for (KeyValue kv : result.raw()) { //inner row: keyValue pairs matched
                //== xyh and mac/Rss matched
                // each line can have eg 10 valuePairs

                // Save key data of row
                if (keyFlag) {
                    keyData = Bytes.toString(kv.getRow()).split(":");

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

                // Get MAC Address
                mac = Bytes.toString(kv.getQualifier());

                // Find in which radiomaps this row has to entered
                if (updateWhereToInsertRow(whereToInsert, macAddressInput, mac)) {
                    //Print header macs
                    saveHeaderMacs(macAddressInput, macsCopy, macsNCoords, mac, x, y);
                }

                //add mac addresses of a row
                rowMacs.add(mac);

                //Save new coordinate
                rowCoordinates.add(
                        new Coordinate(x, y, h, mac, Bytes.toString(kv.getValue())) /*RSS*/);
            } // Row processed

            //Save data to arraylists
            saveDataOfARowToEachRadiomap(
                    rowMacs, rowCoordinates, rmapsUniqueMacs, rmapsCoordinates, whereToInsert);
        } // Scanner to whole hbase

        printHeaderAndMacsWithCoords(macAddressInput, macsNCoords);

        // Print radiomaps
        Coordinate.printRadiomaps(rmapsUniqueMacs, rmapsCoordinates, writer);
        writer.print("\n]\n}");
        scanner.close();
        return foundData;
    }

    private void printHeaderAndMacsWithCoords(
            ArrayList<String> macsMatched, ArrayList<String> macsNCoords) {

        //Print JSon header + Json array of bloom matches
        writer.print("{ 'code': '" + Base.CODE_BLOOM_BUILD_RADIOMAP + "', 'matches': [\n");

        for (int i = 0; i < macsNCoords.size(); i++) {

            writer.print(
                    "{ 'mac':'" + macsMatched.get(i) + "' , 'loc':'" + macsNCoords.get(i) + "' } ");

            if (i < macsNCoords.size() - 1) {
                writer.print(",\n");
            }
        }

        // Close JSONArray
        writer.print("\n],\n");

        // Print JSON Array for partial rmaps
        writer.print("'rmaps': [\n");
    }

    private void saveHeaderMacs(
            final ArrayList<String> matchedMacs,
            ArrayList<String> macsCopy,
            ArrayList<String> macsNcoords,
            String mac,
            String x,
            String y) {

        for (int i = 0; i < macsCopy.size(); i++) {
            if (macsCopy.get(i).equals(mac)) {

                int index = findMacIndex(matchedMacs, mac);
                macsNcoords.set(index, x + "," + y);
                macsCopy.remove(i); //remove mac
                break;
            }
        }
    }

    /**
     * Returns the index of the mac, in matched macs table
     *
     * @param matchedMacs
     * @param mac
     * @return
     */
    private int findMacIndex(ArrayList<String> matchedMacs, String mac) {

        for (int i = 0; i < matchedMacs.size(); i++) {
            if (matchedMacs.get(i).equals(mac)) return i;
        }
        return -1;
    }

    /**
     * Save all data to appropriate radiomaps
     *
     * @param rowMacs All MAC Addresses of a row
     * @param rowCoordinates All Coordinates of a row
     * @param rmapsUniqueMacs All radiomaps with all unique MAC Addresses tables
     * @param rmapsCoordinates All radiomaps with all coordinates
     * @param whereToInsert the indices of rows to insert rowMacs and rowCoordinates
     */
    private void saveDataOfARowToEachRadiomap(
            ArrayList<String> rowMacs,
            ArrayList<Coordinate> rowCoordinates,
            ArrayList<ArrayList<String>> rmapsUniqueMacs,
            ArrayList<ArrayList<Coordinate>> rmapsCoordinates,
            ArrayList<Integer> whereToInsert) {

        //Iterate over all indices
        for (int i = 0; i < whereToInsert.size(); i++) {
            int index = whereToInsert.get(i);

            //Add unique macs to i-index radiomap
            Coordinate.saveUniqueMacs(rmapsUniqueMacs.get(index), rowMacs);
            //Add coordinates to i-index radiomap
            rmapsCoordinates.get(index).addAll(rowCoordinates);
        }
    }

    /**
     * Updates where the indices of radiomaps that this row has to be inserted
     *
     * @param whereToInsert indices
     * @param macAddressInput Matched MAC Addresses
     * @param mac new mac's row
     */
    private boolean updateWhereToInsertRow(
            ArrayList<Integer> whereToInsert, ArrayList<String> macAddressInput, String mac) {

        boolean found = false;

        //get the whole radiomap
        if (macAddressInput.get(0).equals(ALL)) {
            whereToInsert.add(0); // insert in the first, and only radiomap
            return true; // that contains the whole worlds radiomap
        }

        //Iterate over all matched macs
        for (int i = 0; i < macAddressInput.size(); i++) {
            // MAC matches
            if (macAddressInput.get(i).equals(mac)) {
                //save its index
                whereToInsert.add(i);
                found = true; //Found where to add line
            }
        }
        return found;
    }

    /**
     * x:y, MAC:RSS, MAC:RSS, MAC:RSS
     *
     * @param macAddressInput MAC Addresses matched from bloom filter
     * @throws IOException
     */
    public boolean printPartialRadiomapMacValuePairs(ArrayList<String> macAddressInput)
            throws IOException {

        boolean foundData = false;

        // Scan to HBase
        Scan scan = new Scan();

        // One of the filters must pass
        FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ONE);

        //Create filters for all matched MAC Addresses
        for (String macAddress : macAddressInput) {

            // If the MACaddress: WR pair is found, then dont include it
            // This pair wont found because WR will never be as a value in our tables
            // So if a MAC exists, will be included, and if dont, wont
            SingleColumnValueFilter boolMatchFilter =
                    new SingleColumnValueFilter(
                            MapperBulkLoadRadiomap.SRV_COL_FAM,
                            Bytes.toBytes(macAddress),
                            CompareOp.NOT_EQUAL,
                            Bytes.toBytes("WR")); // a dummy value

            //If mac dont exists, wont be included
            // (except if that line is covered by another matched MAC)
            boolMatchFilter.setFilterIfMissing(true);

            //Add filter to list
            filters.addFilter(boolMatchFilter);
        }

        // Set the filter list to scanner
        scan.setFilter(filters);

        // Scanner to our table
        ResultScanner scanner = Base.getHTableMacs(dataSet).getScanner(scan);

        String key;
        for (Result result : scanner) {

            if (!foundData) {
                //Print JSON Object Header
                Serve.printString(writer, JSonObj.printHeader(Base.CODE_BLOOM_BUILD_RADIOMAP));
            } else {
                Serve.printString(writer, "\n");
            }

            boolean keyFlag = true;
            foundData = true;

            for (KeyValue kv : result.raw()) {

                key = Bytes.toString(kv.getRow());

                // Show the key (x:y) only once for a row
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

    /**
     * # x,y, HEADING, mac, mac ,mac x:y:h, rss, rss, rss
     *
     * @param macAddressInput MAC Addresses matched from bloom filter
     * @throws IOException
     */
    public boolean printPartialRadiomapVersion2(ArrayList<String> macAddressInput)
            throws IOException {

        boolean foundData = false;

        // Scan to HBase
        Scan scan = new Scan();

        // One of the filters must pass
        FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ONE);

        //Create filters for all matched MAC Addresses
        for (String macAddress : macAddressInput) {

            // If the MACaddress: WR pair is found, then dont include it
            // This pair wont found because WR will never be as a value in our tables
            // So if a MAC exists, will be included, and if dont, wont
            SingleColumnValueFilter boolMatchFilter =
                    new SingleColumnValueFilter(
                            MapperBulkLoadRadiomap.SRV_COL_FAM,
                            Bytes.toBytes(macAddress),
                            CompareOp.NOT_EQUAL,
                            Bytes.toBytes("WR")); // a dummy value

            //If mac dont exists, wont be included
            // (except if that line is covered by another matched MAC)
            boolMatchFilter.setFilterIfMissing(true);

            //Add filter to list
            filters.addFilter(boolMatchFilter);
        }

        // Set the filter list to scanner
        scan.setFilter(filters);

        // Scanner to our table
        ResultScanner scanner = Base.getHTableMacs(dataSet).getScanner(scan);

        // Unique MAC Addresses for partial Radiomap
        ArrayList<String> uniqueMacs = new ArrayList<String>();
        ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
        String[] keyData;

        for (Result result : scanner) {

            if (!foundData) {
                //Print JSON Object Header
                Serve.printString(writer, JSonObj.printHeader(Base.CODE_BLOOM_BUILD_RADIOMAP));
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

    /**
     * Match bloomfilter with MAC Addresses
     *
     * @param macMatched MAC Addresses Matched
     * @throws IOException
     */
    private void matchMACSwithBloomfilter(ArrayList<String> macMatched) throws IOException {

        // Scanner to HBase
        Scan scan = new Scan();

        // Scanner to our table
        ResultScanner scanner = Base.getHTableFilters(dataSet).getScanner(scan);

        String key, value;
        for (Result result : scanner) {
            for (KeyValue kv : result.raw()) {

                // Get key and value
                key = Bytes.toString(kv.getRow());
                value = Bytes.toString(kv.getValue());

                if (value.equals(bloomFilter.toString())) {
                    macMatched.add(key);
                }
            }
        }
        scanner.close();
    }
}
