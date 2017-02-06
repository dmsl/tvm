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
package cy.ac.ucy.cs.tvm.Bloomfilter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Bloomfilter {

    private int bloomFilterSize;
    private Murmur2 murmur2;

    /**
     * kAnonymity is fixed/forced to 3, and doing selective MAC on client, solves our problems.
     * Hashfunctions num is 3.
     */
    public Bloomfilter(int bloomSize) {
        this.bloomFilterSize = bloomSize;
    }

    public String generateBloomFilter(String macAddress) {
        boolean[] result = new boolean[bloomFilterSize];

        try {
            murmur2 = new Murmur2();
            JenkinsHash jh = new JenkinsHash();

            int infiniteCnt = 0;
            int seed = 0;
            int pos1 = (int) ((jh.hash(macAddress.getBytes())) % bloomFilterSize);
            int pos2 = pos1;

            while (pos2 == pos1 || infiniteCnt > 1000) {
                pos2 = getMurmur(macAddress, seed += 143);
                infiniteCnt++;
            }

            // Flip 1st bit (MD5)
            result[pos1] = true;
            result[pos2] = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        int bitCnt = 0;
        for (int i = 0; i < result.length; i++) {
            if (result[i] == true) bitCnt++;
        }

        return bitToString(result);
    }

    private int getMurmur(String macAddress, int seed) {
        int r = murmur2.hash(macAddress, seed);
        if (r <= 0) {
            r *= -1;
        }

        return r % bloomFilterSize;
    }

    /** Converts bits array to a String */
    private String bitToString(boolean[] bits) {
        String res = "";
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == true) res += "1";
            else res += "0";
        }
        return res;
    }

    /** Hashfunction 1 */
    private int hashfunction1(String macString) throws NoSuchAlgorithmException {
        int resOfH2 = MD5(macString);
        if (resOfH2 < 0) {
            resOfH2 *= (-1);
        }
        return resOfH2 % bloomFilterSize;
    }

    /**
     * Use the hash function of MD5 and then use the result with hashCode
     *
     * @return a value
     */
    private int MD5(String macAddress) throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(macAddress.getBytes(), 0, macAddress.length());
        String str = new BigInteger(1, m.digest()).toString(16);
        return str.hashCode();
    }
}
