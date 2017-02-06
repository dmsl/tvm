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
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/** Buld loading of mac addresses into HBase. */
public class MapperBulkLoadMacAddresses
        extends Mapper<LongWritable, Text, ImmutableBytesWritable, KeyValue> {

    CSVParser csvParser = new CSVParser();
    String tableName = "";
    String macAddressesStr;
    String[] macAddresses;
    ImmutableBytesWritable hKey = new ImmutableBytesWritable();
    KeyValue kv;

    // Column family name
    public static final byte[] COL_FAM_BLOOM = "m".getBytes();
    // Column name
    public static final byte[] COL_NAME_BLOOM = "v".getBytes();

    /** {@inheritDoc} */
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        Configuration c = context.getConfiguration();
        macAddressesStr = c.get("macs");
        macAddresses = csvParser.parseLine(macAddressesStr.replace(" ", ""));
        tableName = c.get("hbase.table.name");
    }

    /** {@inheritDoc} */
    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        // Save rest of MAC Addresses with values, if value != -110
        for (int i = 2; i < macAddresses.length; i++) {

            // Save key as MAC Address
            hKey.set(String.format("%s", macAddresses[i]).getBytes());

            kv = new KeyValue(hKey.get(), COL_FAM_BLOOM, COL_NAME_BLOOM, "0".getBytes());
            // Write HFiles to HBase
            context.write(hKey, kv);
        }

        context.getCounter(MapperBulkLoadMacAddresses.class.getSimpleName(), "NUM_MSGS")
                .increment(1);
    }
}
