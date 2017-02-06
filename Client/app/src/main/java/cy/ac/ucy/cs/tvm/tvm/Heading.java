/*
 * Copyright (c) 2013, Data Management Systems Lab (DMSL), University of Cyprus.
 *
 * Author: J. Metochi jmetoc01@cs.ucy.ac.cy (University of Cyprus)
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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import cy.ac.ucy.cs.tvm.MainActivity;

/** Heading of the device: where the device is pointing at. */
public class Heading implements SensorEventListener {

    private SensorManager mSensorManager = null;
    public static float azimuth;
    private static float nadir;
    private Sensor orientation;

    public Heading(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public static float getNadir() {
        if (nadir < 0) return -nadir;
        return nadir;
    }

    public void pause() {
        mSensorManager.unregisterListener(this);
    }

    public void resume() {
        registerSensors();
    }

    private void registerSensors() {
        orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        if (orientation == null) {
            return;
        }

        if (orientation != null) {
            mSensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {}

    @Override
    public void onSensorChanged(SensorEvent event) {

        azimuth = event.values[0];
        nadir = event.values[1];
        MainActivity.updateDeveloperInfoLabels();
    }
}
