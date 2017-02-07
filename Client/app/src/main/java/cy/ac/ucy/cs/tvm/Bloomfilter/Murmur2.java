/*
 * This file is copied from the apache mahoot project.
 *
 * It is under the Apache Version 2.0 Licence:
 * URL: https://github.com/apache/mahout
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ------------------------------------------------------------------------
 *
 * See http://murmurhash.googlepages.com/ for more details. The C version of MurmurHash 2.0
 * found at that site was ported to Java by Andrzej Bialecki (ab at getopt org).
 */

package cy.ac.ucy.cs.tvm.Bloomfilter;

/** A very fast, non-cryptographic hash suitable for general hash-based lookup. */
public class Murmur2 {

    public int hash(String data, int seed) {
        return hash32(data.getBytes(), seed);
    }

    public static int hash32(byte[] data, int seed) {
        int m = 0x5bd1e995;
        int r = 24;

        int h = seed ^ data.length;

        int len = data.length;
        int len_4 = len >> 2;

        for (int i = 0; i < len_4; i++) {
            int i_4 = i << 2;
            int k = data[i_4 + 3];
            k = k << 8;
            k = k | (data[i_4 + 2] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 1] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 0] & 0xff);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        int len_m = len_4 << 2;
        int left = len - len_m;

        if (left != 0) {
            if (left >= 3) {
                h ^= (int) data[len - 3] << 16;
            }
            if (left >= 2) {
                h ^= (int) data[len - 2] << 8;
            }
            if (left >= 1) {
                h ^= (int) data[len - 1];
            }

            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }
}
