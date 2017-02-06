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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;

/** Base class for all initializations of HBase/Hadoop Web Interface */
public class Base {

    /** Radiomap tables */
    static final String TABLE_NAME_RADIOMAP_BASE = "rm";

    static final String TABLE_NAME_RADIOMAP_BASE1 = "rm1";
    static final String TABLE_NAME_RADIOMAP_BASE2 = "rm2";
    static final String TABLE_NAME_RADIOMAP_BASE3 = "rm3";
    static final String TABLE_NAME_RADIOMAP_BASE4 = "rm4";
    static final String TABLE_NAME_RADIOMAP_BASE5 = "rm5";

    /** Bloom filter tables */
    static final String TABLE_NAME_BLOOMFILTERS_BASE = "b";

    static final String TABLE_NAME_BLOOMFILTERS_BASE1 = "b1";
    static final String TABLE_NAME_BLOOMFILTERS_BASE2 = "b2";
    static final String TABLE_NAME_BLOOMFILTERS_BASE3 = "b3";
    static final String TABLE_NAME_BLOOMFILTERS_BASE4 = "b4";
    static final String TABLE_NAME_BLOOMFILTERS_BASE5 = "b5";

    // Types of load
    public static final String TYPE_LOAD = "load";
    public static final String TYPE_MAP_TEST = "maptest";
    public static final String TYPE_MAC = "mac";
    public static final String TYPE_BLOOM = "bloom";
    public static final String FILTER = "filter";
    public static final String TYPE_BLOOM_SIZE = "bloomsize";
    public static final int CODE_ERROR_NO_RESULTS_FOUND = 0;
    public static final int CODE_ERROR_PARAMS = -1;
    public static final int CODE_ERROR_PROGRAMMERS_FAULT = -2;
    public static final int CODE_SUCCESS = 1;
    public static final int CODE_BLOOM_BUILD_RADIOMAP = 1;
    public static final int CODE_BLOOM_MAC_ONLY = 2;
    public static final String JSON_MIME = "application/json";
    /** HBase configuration */
    static Configuration conf;

    /** HBase Table: BloomFilter */
    private static HTable htableMacAddressesDefault;

    private static HTable htableMacAddresses1;
    private static HTable htableMacAddresses2;
    private static HTable htableMacAddresses3;
    private static HTable htableMacAddresses4;
    private static HTable htableMacAddresses5;

    /** HBase Table: BloomFilter */
    private static HTable htableBloomFilterDefault;

    private static HTable htableBloomFilter1;
    private static HTable htableBloomFilter2;
    private static HTable htableBloomFilter3;
    private static HTable htableBloomFilter4;
    private static HTable htableBloomFilter5;

    /**
     * Initializes HBase classs
     *
     * @throws IOException
     */
    public static void startup() throws IOException {
        // Init HBase tables: mac, bloom
        conf = HBaseConfiguration.create();

        //Create tables
        htableMacAddressesDefault = new HTable(conf, TABLE_NAME_RADIOMAP_BASE);
        htableBloomFilterDefault = new HTable(conf, TABLE_NAME_BLOOMFILTERS_BASE);

        // INFO : the rest are groupped because if tbl doesnt exist,
        // an exception will be thrown.
        htableMacAddresses1 = new HTable(conf, TABLE_NAME_RADIOMAP_BASE1);
        htableBloomFilter1 = new HTable(conf, TABLE_NAME_BLOOMFILTERS_BASE1);
        htableMacAddresses2 = new HTable(conf, TABLE_NAME_RADIOMAP_BASE2);
        htableBloomFilter2 = new HTable(conf, TABLE_NAME_BLOOMFILTERS_BASE2);
        htableMacAddresses3 = new HTable(conf, TABLE_NAME_RADIOMAP_BASE3);
        htableBloomFilter3 = new HTable(conf, TABLE_NAME_BLOOMFILTERS_BASE3);
        htableMacAddresses4 = new HTable(conf, TABLE_NAME_RADIOMAP_BASE4);
        htableBloomFilter4 = new HTable(conf, TABLE_NAME_BLOOMFILTERS_BASE4);
        htableMacAddresses5 = new HTable(conf, TABLE_NAME_RADIOMAP_BASE5);
        htableBloomFilter5 = new HTable(conf, TABLE_NAME_BLOOMFILTERS_BASE5);
    }

    /**
     * Get HTable for MACs
     *
     * @param id
     * @return the HTable according to the ids given
     */
    public static HTable getHTableMacs(int id) {
        switch (id) {
            case 0:
                return htableMacAddressesDefault;
            case 1:
                return htableMacAddresses1;
            case 2:
                return htableMacAddresses2;
            case 3:
                return htableMacAddresses3;
            case 4:
                return htableMacAddresses4;
            case 5:
                return htableMacAddresses5;
        }
        return null; // something went wrong!
    }

    /**
     * Get HTable for Bloomfilters
     *
     * @param id
     * @return the HTable according to the ids given
     */
    public static HTable getHTableFilters(int id) {
        switch (id) {
            case 0:
                return htableBloomFilterDefault;
            case 1:
                return htableBloomFilter1;
            case 2:
                return htableBloomFilter2;
            case 3:
                return htableBloomFilter3;
            case 4:
                return htableBloomFilter4;
            case 5:
                return htableBloomFilter5;
            default:
                return null; // something went wrong
        }
    }

    /**
     * Called when servlet shuts down
     *
     * @throws IOException
     */
    public static void shutdown() throws IOException {
        //Close tables
        htableBloomFilterDefault.close();
        htableMacAddresses1.close();
        htableMacAddresses2.close();
        htableMacAddresses3.close();
        htableMacAddresses4.close();
        htableMacAddresses5.close();
    }
}
