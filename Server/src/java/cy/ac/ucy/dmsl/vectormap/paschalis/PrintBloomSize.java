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
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * It returns the size of the bloom filter in use. Clients need this information so the can request
 * their location by generating a bloom filter of this size.
 */
public class PrintBloomSize {

    /** Writer to Servlet output */
    PrintWriter writer;

    private int dataSet;

    public PrintBloomSize(PrintWriter writer, int dataSet) {
        this.writer = writer;
        this.dataSet = dataSet;
    }

    /**
     * Runs the algorithms and prints matched macs or builds Partial Radiomap
     *
     * @return JSon object filled with result
     * @throws IOException
     */
    public void runMethod() {

        // Match bloomfilter with MAC Addresses
        try {
            writer.println("{code: '1', message: '" + getBloomFilterSize() + "' }");
        } catch (IOException e) {
            writer.println("{code: '0', message: 'IOException" + e.getMessage() + "' }");
        }
    }

    /**
     * Match bloomfilter with MAC Addresses
     *
     * @throws IOException
     */
    private int getBloomFilterSize() throws IOException {

        // Scanner to HBase
        Scan scan = new Scan();

        // Scanner to our table
        ResultScanner scanner = Base.getHTableFilters(dataSet).getScanner(scan);

        String bloomFilter = null;
        for (Result result : scanner) {
            for (KeyValue kv : result.raw()) {

                // Get a bloom filter
                bloomFilter = Bytes.toString(kv.getValue());
                break;
            }
            break;
        }
        //Close scanner
        scanner.close();

        return bloomFilter.length();
    }
}
