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

package cy.ac.ucy.cs.tvm.tvm.simulation;

import android.content.Intent;
import android.util.Log;
import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;
import java.util.ArrayList;

/** Base class of the generated walk simulator classes */
public abstract class WalkSimulator {

    private static final String TAG = WalkSimulator.class.getSimpleName();
    public final String routeName;
    App app;
    public final int max_positions;

    public WalkSimulator(String routeName, App app, int max_positions) {
        this.routeName = routeName;
        this.app = app;
        this.max_positions = max_positions;
    }

    public abstract ArrayList<LogRecord> getPosition(int num);

    public void simulatePosition(int num) {

        Log.i(TAG, "simulatePosition: " + num);

        app.currentScanListClear();
        app.currentScanListAddAll(getPosition(num));

        Intent intent = new Intent();
        intent.setAction(App.BROADCAST_RECEIVER_GET_PARTIAL_RMAP);
        app.sendBroadcast(intent);
    }
}
