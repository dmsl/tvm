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

/** Bulk loading of radiomaps into HBase. */
public class MapperBulkLoadRadiomap
        extends Mapper<LongWritable, Text, ImmutableBytesWritable, KeyValue> {
    // Set column family name
    static final byte[] SRV_COL_FAM = "m".getBytes();

    // CSV Parser TODO STATIC
    CSVParser csvParser = new CSVParser();
    String tableName = "";

    String macAddressesStr;
    String[] macAddresses;

    ImmutableBytesWritable hKey = new ImmutableBytesWritable();
    KeyValue kv;

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

        //Parse MAC Address
        String[] row = null;

        // Failed to parse line
        try {
            row = csvParser.parseLine(value.toString().replace(" ", ""));
        } catch (Exception ex) {
            context.getCounter(MapperBulkLoadRadiomap.class.getSimpleName(), "PARSE_ERRORS")
                    .increment(1);
            return;
        }

        if (row[0].charAt(0) == '#') return;

        // Save key as x:y pair (Later MAC Addresses will be used)
        hKey.set(String.format("%s", row[0] + ":" + row[1] + ":" + row[2]).getBytes());

        // Save rest of MAC Addresses with values, if value != -110
        for (int i = 3; i < row.length; i++) {
            if (!row[i].equals("-110")) {
                // Save KeyValue Pair
                kv =
                        new KeyValue(
                                hKey.get(),
                                SRV_COL_FAM,
                                macAddresses[i].getBytes(),
                                row[i].getBytes());
                // Write HFiles to HBase
                context.write(hKey, kv);
            }
        }
        context.getCounter(MapperBulkLoadRadiomap.class.getSimpleName(), "NUM_MSGS").increment(1);
    }
}
