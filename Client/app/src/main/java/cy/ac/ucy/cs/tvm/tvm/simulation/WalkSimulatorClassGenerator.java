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

import cy.ac.ucy.cs.tvm.App;
import cy.ac.ucy.cs.tvm.tvm.LogRecord;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Generates a WalkSimulator class. This allows us to test the application by simulating real data
 * that were recorded at CS UCY campus.
 */
public class WalkSimulatorClassGenerator {

    public static final String CLASS_HEADER =
            "/**\n"
                    + " * INFO: 	Class automatically generated by "
                    + WalkSimulatorClassGenerator.class.getSimpleName()
                    + " to simulate a real \n"
                    + " * localization scenario that was recorded at CS UCY Campus.\n"
                    + " */\n\n"
                    + "package cy.ac.ucy.dmsl.vectormap.helpers;\n\n"
                    + "import java.util.ArrayList;\n\n"
                    + "import cy.ac.ucy.dmsl.vectormap.LogRecord;\n"
                    + "import cy.ac.ucy.dmsl.vectormap.main.App;\n\n\n"
                    + "/**\n"
                    + " * Simulating __POS_NUM__ positions at Computer Science dept. @ UCY Positions has real\n"
                    + " * recorded data.\n\n"
                    + " * @author paschalis\n\n"
                    + "public class WalkAtUcy___POS_NUM__ extends WalkSimulator {\n\n\n"
                    + "\tpublic WalkAtUcy___POS_NUM__(App app) {\n"
                    + "\t\tsuper(\"UCY__POS_NUM__positions\", app);\n"
                    + "\t}\n";

    public static final String CLASS_FOOTER = "\n\n" + "}\n";

    public static final String METHOD_GET_POSITION_HEADER =
            "\n\npublic ArrayList<LogRecord> getPosition(int num) {\n"
                    + "\n\n"
                    + "\t\tswitch (num) {\n\t\t";

    public static final String METHOD_GET_POSITION_FOOTER =
            "\n"
                    + "\t\t\tdefault:\n"
                    + "\t\t\t\tbreak;\n"
                    + "\t\t}\n\n"
                    + "\t\treturn null;\n"
                    + "\t}";

    private static App app;
    private static int positionSaved = 0;
    private static ArrayList<String> methodGetPositionCaseBody = new ArrayList<String>();
    private static ArrayList<String> methodsOfMesurements = new ArrayList<String>();
    public static boolean recordingRoute = false;

    public static void init(App app) {
        WalkSimulatorClassGenerator.positionSaved = 0; //start from position 0
        recordingRoute = true;
        WalkSimulatorClassGenerator.app = app;
    }

    /** Add a new position to the string-creating DAO class */
    public static void addNewPosition(ArrayList<LogRecord> recordedRssValues) {

        app.showToast("Recorded positions: " + positionSaved);
        methodGetPositionCaseBody.add(
                "case "
                        + positionSaved
                        + ": return getLogRecordUcyPosition"
                        + positionSaved
                        + "();\n");

        String methodOfMeusurmentsString =
                "public ArrayList<LogRecord> getLogRecordUcyPosition"
                        + positionSaved
                        + "() {\n"
                        + "\t\tArrayList<LogRecord> result = new ArrayList<LogRecord>();\n\n";

        //add body
        for (LogRecord lr : recordedRssValues) {
            methodOfMeusurmentsString +=
                    "result.add(new LogRecord(\"" + lr.getBssid() + "\", " + lr.getRss() + "));\n";
        }

        methodOfMeusurmentsString += "\n\nreturn result;\n" + "\t}";

        methodsOfMesurements.add(methodOfMeusurmentsString);

        positionSaved++;
    }

    /** Build a class in string Generate a WalkSimulator class */
    public static void Generate(PrintWriter pw) {
        pw.print(CLASS_HEADER.replace("__POS_NUM__", Integer.toString((positionSaved) - 1)));
        pw.print(METHOD_GET_POSITION_HEADER);

        for (String caseRows : methodGetPositionCaseBody) {
            pw.print(caseRows);
        }

        pw.print(METHOD_GET_POSITION_FOOTER);

        //Add the methods that contain measurements
        for (String methods : methodsOfMesurements) {
            pw.print("\n\n");
            pw.print(methods);
        }

        pw.write("\n\n\tpublic static final int MAX_POSITIONS = " + (positionSaved - 1) + ";");
        pw.write(CLASS_FOOTER);
        WalkSimulatorClassGenerator.recordingRoute = false; // dont save any other positions
    }
}
