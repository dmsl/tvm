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

import au.com.bytecode.opencsv.CSVParser;
import cy.ac.ucy.dmsl.vectormap.paschalis.RowCounter.RowCounterMapper;
import cy.ac.ucy.dmsl.vectormap.paschalis.bloom.Bloomfilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.hbase.regionserver.metrics.SchemaMetrics;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Data preparation MapReduce job driver
 *
 * <ul>
 *   <li>args[0]: Local FS input path (radiomap)
 *   <li>args[1]: Radiomap HBase table name
 *   <li>args[2]: bloom filter HBase table name
 *   <li>args[3]: b is for bulk, l is for light, bl is for bloommap table calculation only
 * </ul>
 */
public class RadiomapLoader {

    /** MAC Addresses NOTE: first 2 columns contain: #x , y */
    public static String macAddressesLine;

    /** Exension used for bloom/mac addresses files */
    public static final String BLOOM_EXTENSION = ".bloom";

    public static long TOTAL_MAC_ADDRESSES = -1;

    /** MAC Addresses of current Radiomap file */
    static String[] macAddresses;

    static CSVParser csvParser = new CSVParser();

    public static SimpleDateFormat ft = new SimpleDateFormat("MM_dd_hh_mms");

    private static HBaseAdmin hbaseAdmin;

    static Date dNow;

    // Column families for 2 tables
    private static final String CF_BLOOM_FILTER_TABLE = "m";

    /** Where data to load are in HDFS */
    private static String hdfsInputFile;

    /** WHere to save HFiles, before to flush them in HBase DB */
    private static String hdfsOutputFile;

    /** Where bloom data to load are in HDFS */
    private static String hdfsInputFileBloom;

    /** Where bloom data to load are in HDFS */
    private static String hdfsOutputFileBloom;

    public static void main(String[] args) throws Exception {

        // Wrong number of arguments
        if (args.length != 4) {
            System.err.println(
                    "Usage: radiomapFilename radiomapTable bloomTable options(b,l,bl,lnbl)");
            return;
        }

        String radiomapFileName = args[0];
        Configuration conf = new Configuration();
        hbaseAdmin = new HBaseAdmin(HBaseConfiguration.create(conf));

        // Flags for modes we use:
        // bulkLoad:	for huge datasets. uses mapred jobs
        // onlyBloom:	calculates/recalculates only the bloom filters
        // noBloom:	dont calculates the bloom filters
        boolean bulkLoad = false;
        boolean onlyBloom = false;
        boolean doBloom = true;

        //Do a bulk load
        if (args[3].equals("b")) {
            bulkLoad = true;
        }
        // Do a light load
        else if (args[3].equals("l")) {
            bulkLoad = false;
        }
        // Do only bloom filters calculation, for bloom table
        else if (args[3].equals("bl")) {
            onlyBloom = true;
        }
        // Do a light load, and dont do bloom filters calculation
        else if (args[3].equals("lnbl")) {
            doBloom = false;
            bulkLoad = false;
        }

        // following code must executed each time

        dNow = new Date();

        //If we must run all insertion algorithms
        if (!onlyBloom) {

            // Create tables if dont exist
            createTablesIfNotExists(args[1], args[2]);

            // Do bulk load method (Map Reduce)
            if (bulkLoad) {

                // Read MAC Addresses and write them to file
                prepareRadiomapFiles(radiomapFileName, conf);

                // Bulk load Vectormap data in HBase
                bulkLoadVectormap(conf, hdfsInputFile, hdfsOutputFile, args[1]);

                // Bulk load new macAddresses
                bulkLoadNewMacAddresses(conf, hdfsInputFileBloom, hdfsOutputFileBloom, args[2]);
            }
            // Do light load method
            else {
                // Load vectormap
                lightLoadVectormap(conf, radiomapFileName, args[1]);

                // Load MAC Addresses (with dummy values for now)
                lightLoadNewMacAddresses(conf, args[2]);
            }
        }

        // Do the bloom filter calculation
        if (doBloom) {
            // Count rows of all MAC Addresses
            TOTAL_MAC_ADDRESSES = simplestScanner(conf, args[2]);

            // DEBUG
            System.out.println("\n\n" + args[2] + " Total Macs: " + TOTAL_MAC_ADDRESSES + "\n\n");
            // DEBUG
            System.out.println(
                    "\n\n"
                            + args[1]
                            + " Total Rows of Radiomap: "
                            + simplestScanner(conf, args[1]));

            // Create Bloom Filter
            Bloomfilter bloomfilter =
                    new Bloomfilter(TOTAL_MAC_ADDRESSES, 5, 2); // hashfunctions num

            // Create bloom filters
            updateAllBloomFilters(conf, args[2], bloomfilter);

            // DEBUG: show bloom filters
            showBloomFilters(conf, args[2]);
        }
    }

    /**
     * Read MAC Addresses from Radiomap, save MAC Addresses to a .bloom file, so they can used as
     * InputFile for mapred jobs (for bloom filter maintenance)
     *
     * @param radiomapFileName
     * @param conf
     */
    private static void prepareRadiomapFiles(String radiomapFileName, Configuration conf) {

        // Parse first line of Radiomap: MAC Addresses
        try {

            // Read MAC Addresses from file
            BufferedReader input = new BufferedReader(new FileReader(radiomapFileName));

            macAddressesLine = input.readLine();

            input.close();

            // Parse MAC Addresses
            macAddresses = csvParser.parseLine(macAddressesLine.replace(" ", ""));

            // Write MAC Addresses to file
            BufferedWriter output =
                    new BufferedWriter(new FileWriter(radiomapFileName + BLOOM_EXTENSION));
            for (int i = 2; i < macAddresses.length; i++) {
                output.write(macAddresses[i] + "\n");
            }

            output.close();

        } catch (Exception ex) {
            System.err.println("Failed to write MAC Addresses: " + ex.getMessage());
            return;
        }

        String[] tmpPath = radiomapFileName.split("/");

        String filename = tmpPath[tmpPath.length - 1];

        String user = System.getProperty("user.name");

        // IO Files for Radiomap
        hdfsInputFile = "/user/" + user + "/" + filename;
        hdfsOutputFile = "/user/" + user + "/" + (filename.split("\\."))[0];

        // IO File for bloom filters
        hdfsInputFileBloom = hdfsInputFile + BLOOM_EXTENSION;
        //		hdfsOutputFileBloom = hdfsOutputFile + BLOOM_EXTENSION; CHECK RM

        System.out.println(
                "\n\n\n\nInputfile "
                        + hdfsInputFile
                        + "\nOUtputfile: "
                        + hdfsOutputFile
                        + "\n\n\n");

        // Copy radiomap file to HDFS
        copyFromLocal(conf, radiomapFileName, hdfsInputFile);

        // Copy mac addresses file to HDFS
        copyFromLocal(conf, radiomapFileName + BLOOM_EXTENSION, hdfsInputFileBloom);
    }

    /**
     * Copies a file from local file system to HDFS
     *
     * @param conf
     */
    private static void copyFromLocal(Configuration conf, String localPath, String hdfsPath) {
        try {
            // "hdfs://master:54310/user/hduser/input/"
            Path hdfsFile = new Path(hdfsPath);

            Path localFile = new Path(localPath);

            FileSystem fs = FileSystem.get(conf);

            // Copy file from local FS to HDFS
            fs.copyFromLocalFile(localFile, hdfsFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void lightLoadNewMacAddresses(Configuration conf, String tableName)
            throws IOException {

        // Radiomap table
        HTable htable = new HTable(conf, tableName);

        // Parse first line of Radiomap: MAC Addresses
        try {

            for (int i = 2; i < macAddresses.length; i++) {

                // Put Key Value pair to HBase
                // Key is macAddress
                Put p = new Put(Bytes.toBytes(macAddresses[i]));

                // Row to update (currenlty exists with a Nan(=0)
                // bloomfilter)
                p.add(
                        MapperBulkLoadMacAddresses.COL_FAM_BLOOM,
                        MapperBulkLoadMacAddresses.COL_NAME_BLOOM,
                        "test".getBytes());

                htable.put(p);
            }
        } catch (Exception ex) {
            System.err.println("Failed to put MAC Addresses: " + ex.getMessage());
        }

        htable.close();
    }

    /**
     * @param conf HBase configuration
     * @param radiomapFileName Filename of radiomap
     * @param tableName Name of the table
     * @throws IOException
     */
    private static void lightLoadVectormap(
            Configuration conf, String radiomapFileName, String tableName) throws IOException {

        // Radiomap table
        HTable htable = new HTable(conf, tableName);

        // Parse first line of Radiomap: MAC Addresses
        try {

            // Read MAC Addresses from file
            BufferedReader input = new BufferedReader(new FileReader(radiomapFileName));

            macAddressesLine = input.readLine();

            // Parse MAC Addresses
            macAddresses = csvParser.parseLine(macAddressesLine.replace(" ", ""));

            String line = null;
            int cnt = 0;
            while ((line = input.readLine()) != null) {
                cnt++;

                // Parse line
                String[] row = null;

                // Failed to parse line
                row = csvParser.parseLine(line.replace(" ", ""));

                // Ignore comment lines
                if (row[0].charAt(0) == '#') continue;

                for (int i = 2; i < row.length; i++) {
                    if (!row[i].equals("-110")) {

                        // Put Key Value pair to HBase
                        // Key is x:y coordinates
                        Put p = new Put(Bytes.toBytes(row[0] + ":" + row[1]));

                        // Row to update (currenlty exists with a Nan(=0)
                        // bloomfilter)
                        p.add(
                                MapperBulkLoadMacAddresses.COL_FAM_BLOOM,
                                macAddresses[i].getBytes(),
                                row[i].getBytes());

                        htable.put(p);
                    }
                }
            }

            input.close();

        } catch (Exception ex) {
            System.err.println("Failed to put Radiomap: " + ex.getMessage());
        }

        htable.close();
    }

    /**
     * Prints all keys bloom filters generated for debug purposes
     *
     * @param conf
     * @param tableName
     * @throws IOException
     */
    private static void showBloomFilters(Configuration conf, String tableName) throws IOException {
        Scan scan = new Scan();

        // Summary table
        HTable htable = new HTable(conf, tableName);

        // Scanner to our table
        ResultScanner scanner = htable.getScanner(scan);

        //		@SuppressWarnings("unused") RM
        //		String key; RM
        for (Result result : scanner) {

            for (KeyValue kv : result.raw()) {

                //				key = Bytes.toString(kv.getRow()); RM

                // Print Row Key
                System.out.print(Bytes.toString(kv.getValue()));
            }
            System.out.println("");
        }

        scanner.close();
        htable.close();
    }

    /**
     * Updates all bloomfilters in all records in Bloom Filter table (named 'b')
     *
     * @param conf
     * @param tableName
     * @param bloomfilter
     * @throws IOException
     */
    private static void updateAllBloomFilters(
            Configuration conf, String tableName, Bloomfilter bloomfilter) throws IOException {

        // Bloomfilter table
        HTable htable = new HTable(conf, tableName);
        Scan scan = new Scan();
        ResultScanner scanner = htable.getScanner(scan);
        String key;

        // Update all Bloomfilters
        for (Result result : scanner) {
            for (KeyValue kv : result.raw()) {

                // FUTURE Detect in bloomfilter size changed, and update only NaN bloomfilters
                
                // Get Row key
                key = Bytes.toString(kv.getRow());

                // Put data to key given (Update row)
                Put p = new Put(Bytes.toBytes(key));

                // Update Row
                p.add(
                        MapperBulkLoadMacAddresses.COL_FAM_BLOOM,
                        MapperBulkLoadMacAddresses.COL_NAME_BLOOM,
                        Bytes.toBytes(bloomfilter.generateBloomFilter(key)));
                htable.put(p);
            }
        }

        scanner.close();
        htable.close();
    }

    /**
     * Count the rows for the table given as input
     *
     * @param conf HBase configuration
     * @param tableName Bloomfilter table name
     * @throws IOException
     */
    private static int simplestScanner(Configuration conf, String tableName) throws IOException {

        int counter = 0;

        // Bloomfilter table
        HTable bloomHTable = new HTable(conf, tableName);

        // Scanner to mac&bloom filter pairs table
        ResultScanner scanner = bloomHTable.getScanner(new Scan());

        for (@SuppressWarnings("unused") Result result : scanner) {
            counter++;
        }

        scanner.close();
        bloomHTable.close();
        return counter;
    }

    /**
     * Count the rows of a table (Bloom filters table), and save the result using Map Reduce Job.
     * Function is unused. a simpler faster method is used
     *
     * @param conf HBase configuration
     * @param tableName table name to count its rows
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static long countTableRowsMAPRED(Configuration conf, String tableName)
            throws Exception {

        String columnName = "m";

        Job job = new Job(conf, "Count all MAC Addresses");
        job.setJarByClass(RowCounter.class);

        Scan scan = new Scan();
        scan.setFilter(new FirstKeyOnlyFilter());

        // Second argument is the table name.
        job.setOutputFormatClass(org.apache.hadoop.mapreduce.lib.output.NullOutputFormat.class);
        //
        org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil.initTableMapperJob(
                tableName,
                scan,
                RowCounterMapper.class,
                ImmutableBytesWritable.class,
                Result.class,
                job);
        job.setNumReduceTasks(0);

        job.waitForCompletion(true);

        return job.getCounters().findCounter(RowCounter.RowCounterMapper.Counters.ROWS).getValue();
    }

    private static void bulkLoadNewMacAddresses(
            Configuration conf, String inputPath, String outputPath, String tblName)
            throws Exception {

        // Pass parameters to Mad Reduce
        conf.set("hbase.table.name", tblName);
        conf.set("macs", macAddressesLine);

        // Workaround
        SchemaMetrics.configureGlobally(conf);

        // Load hbase-site.xml
        HBaseConfiguration.addHbaseResources(conf);

        // Create the job
        Job job = new Job(conf, "Load macAddresses in bloomfilters table");

        job.setJarByClass(MapperBulkLoadMacAddresses.class);
        job.setMapperClass(MapperBulkLoadMacAddresses.class);
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(KeyValue.class);

        job.setInputFormatClass(TextInputFormat.class);

        // Get the table
        HTable hTable = new HTable(conf, tblName);

        // Auto configure partitioner and reducer
        HFileOutputFormat.configureIncrementalLoad(job, hTable);

        // Save output path and input path
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        // Wait for HFiles creations
        job.waitForCompletion(true);

        // Load generated HFiles into table
        LoadIncrementalHFiles loader = new LoadIncrementalHFiles(conf);
        loader.doBulkLoad(new Path(outputPath), hTable);
    }

    /**
     * Load radiomap in HBase
     *
     * @param conf
     * @throws Exception
     */
    private static void bulkLoadVectormap(
            Configuration conf, String inputPath, String outputPath, String tblName)
            throws Exception {

        // Pass parameters to Mad Reduce
        conf.set("hbase.table.name", tblName);
        conf.set("macs", macAddressesLine);

        // Workaround
        SchemaMetrics.configureGlobally(conf);

        // Load hbase-site.xml
        HBaseConfiguration.addHbaseResources(conf);

        // Create the job
        Job job = new Job(conf, "Load radiomap in HBase");

        job.setJarByClass(MapperBulkLoadRadiomap.class);
        job.setMapperClass(MapperBulkLoadRadiomap.class);
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(KeyValue.class);

        job.setInputFormatClass(TextInputFormat.class);

        // Get the table
        HTable hTable = new HTable(conf, tblName);

        // Auto configure partitioner and reducer
        HFileOutputFormat.configureIncrementalLoad(job, hTable);

        // Save output path and input path
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        // Wait for HFiles creations
        job.waitForCompletion(true);

        // Load generated HFiles into table
        LoadIncrementalHFiles loader = new LoadIncrementalHFiles(conf);
        loader.doBulkLoad(new Path(outputPath), hTable);
    }

    /**
     * Creatse tables if they dont exist in HBase
     *
     * @param radiomap HBase table
     * @param bloom HBase table
     * @throws IOException
     */
    private static void createTablesIfNotExists(String radiomap, String bloom) throws IOException {

        // Create table if doesnt exists
        createTableIfNotExists(radiomap);
        createTableIfNotExists(bloom);
    }

    /**
     * Create table in HBase if doesnt exists
     *
     * @param tblName name
     * @throws IOException
     */
    private static void createTableIfNotExists(String tblName) throws IOException {
        // Create hbase table if doesnt exists
        if (!hbaseAdmin.tableExists(tblName)) {
            HTableDescriptor table = new HTableDescriptor(tblName);
            HColumnDescriptor family = new HColumnDescriptor(CF_BLOOM_FILTER_TABLE);
            table.addFamily(family);
            hbaseAdmin.createTable(table);
        }
    }
}
