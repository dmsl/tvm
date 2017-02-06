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

/**
 * Generator of JSon objects that contain:
 *
 * <ul>
 *   <li>a code (integer)
 *   <li>a message value (string)
 * </ul>
 */
public class JSonObj {
    private Integer code = Base.CODE_ERROR_NO_RESULTS_FOUND;
    private String message;

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        // Build String in JSon format
        return "{ code: '" + code + "', message: '" + message + "' }";
    }

    public static String printHeader(int code) {
        return "{ code: '" + code + "', message: '";
    }

    public static String printEnding() {
        return "' }";
    }

    /**
     * Set JSon objects data
     *
     * @param code
     * @param message
     */
    public void setData(Integer code, String message) {

        // If message is empty, change results
        if (message.equals("")) {
            this.code = Base.CODE_ERROR_NO_RESULTS_FOUND;
            this.message = "No results found.";
        }
        // save results as are
        else {
            this.code = code;
            this.message = message;
        }
    }

    public void setNoResults() {
        this.setData(0, "");
    }
}
