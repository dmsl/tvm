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
package cy.ac.ucy.cs.tvm.tvm;

/** A DAO class that is used by the Weighted variants of KNN and MMSE */
public class WeightRecord {
    private float wx;
    private float wy;

    public WeightRecord(float weightX, float weightY) {
        this.wx = weightX;
        this.wy = weightY;
    }

    public float getWeightX() {
        return wx;
    }

    public float getWeightY() {
        return wy;
    }
}
