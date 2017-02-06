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
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

/** Filtering the HBase results to get answers to our queries. */
public class MapredFiltering {

    private static List<String> mac_addrs;
    private static int dataset;
    private static Scan scan;
    private static ResultScanner scanner = null;

    public void setDataset(int i) {
        dataset = i;
    }

    public static class Map extends TableMapper<Text, LongWritable> {

        @Override
        protected void setup(Context context) {}

        public void setMacs(List<String> lis) {
            mac_addrs = lis;
        }

        @Override
        protected void map(ImmutableBytesWritable rowkey, Result result, Context context) {
            FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ONE);
            for (String str : mac_addrs) {

                byte[] b =
                        result.getColumnLatest(MapperBulkLoadRadiomap.SRV_COL_FAM, str.getBytes())
                                .getValue();
                if (b == null) return;

                SingleColumnValueFilter boolMatchFilter =
                        new SingleColumnValueFilter(
                                MapperBulkLoadRadiomap.SRV_COL_FAM, Bytes.toBytes(str),
                                CompareOp.NOT_EQUAL, Bytes.toBytes("WR"));

                //If mac dont exists, wont be included
                // (except if that line is covered by another matched MAC)
                boolMatchFilter.setFilterIfMissing(true);
                filters.addFilter(boolMatchFilter);
                scan.setFilter(filters);

                // Scanner to our table
                try {
                    scanner = Base.getHTableMacs(dataset).getScanner(scan);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param args ,the main method accepts an array with a signle element.The element is the name
     *     of the table to scan
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Configuration conf = HBaseConfiguration.create();

        Job job = new Job(conf, "HBase Filtering");

        job.setJarByClass(MapredFiltering.class);

        Scan scan = new Scan();
        scan.setCaching(500); // 1 is the default in Scan, which will be bad for MapReduce jobs
        scan.addFamily(MapperBulkLoadRadiomap.SRV_COL_FAM);
        //scan.addColumn(MapperBulkLoadRadiomap.SRV_COL_FAM, TwitsDAO.TWIT_COL);
        TableMapReduceUtil.initTableMapperJob(
                args[0], scan, Map.class, ImmutableBytesWritable.class, Result.class, job);

        job.setOutputFormatClass(NullOutputFormat.class);
        job.setNumReduceTasks(0);
        boolean b = job.waitForCompletion(true);
        if (!b) {
            System.err.println("Job has not been completed.Abnormal exit.");
            System.exit(1);
        }
    }
}
