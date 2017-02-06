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

import android.os.Handler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** Settings of the communicatin to the HBase server */
public class DownloadingSettings extends Thread {

    private final String IP;
    private final String PORT;
    private final String filename_radiomap_download;
    private final String folder_path; // radiomap download location

    private String errMsg;
    private final Handler handler;

    public DownloadingSettings(
            String IP, String PORT, String folder_path, String filename, Handler handler) {
        this.filename_radiomap_download = filename;
        this.PORT = PORT;
        this.IP = IP;
        this.folder_path = folder_path;
        this.handler = handler;
    }

    public String getErrMsg() {
        return errMsg;
    }

    /** Establishes a connection on IP/PORT and download radiomap */
    public void run() {

        Socket connection = null;
        FileOutputStream fos = null;
        File root = new File(folder_path);

        String radiomap_mean = filename_radiomap_download;
        String rbf_weights = radiomap_mean + "-rbf-weights";
        String parameters = radiomap_mean + "-parameters";

        try {

            connection = new Socket(IP, Integer.parseInt(PORT));

            // path is writable
            if (root.canWrite()) {
                fos = new FileOutputStream(new File(root, radiomap_mean), false);
            } else {
                errMsg =
                        "Directory: "
                                + root.getAbsolutePath()
                                + " is not writable.\nYou may need an external memory card";
                handler.sendEmptyMessage(-2);
                return;
            }

            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine, outputLine;

            inputLine = in.readLine();

            if (!inputLine.equalsIgnoreCase("+OK READY")) {
                fos.close();
                out.close();
                in.close();
                connection.close();
                errMsg = "Server not ready.\nTry again later.";
                handler.sendEmptyMessage(-2);
                return;
            }

            outputLine = "GET radiomap";
            out.println(outputLine);
            inputLine = in.readLine();

            if (!inputLine.startsWith("RADIOMAP")) {
                fos.close();
                out.close();
                in.close();
                connection.close();
                errMsg = inputLine + "";
                handler.sendEmptyMessage(-2);
                return;
            }

            inputLine = inputLine.replaceFirst("RADIOMAP ", "");
            fos.write((inputLine + "\n").getBytes());

            // Get files: radiomap, parameters and rbf weights
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.compareTo("null") == 0 || inputLine.startsWith("CORRUPTED")) break;

                if (inputLine.equalsIgnoreCase("PARAMETERS")) {
                    fos = new FileOutputStream(new File(root, parameters), false);
                } else if (inputLine.equalsIgnoreCase("RBF_WEIGHTS")) {
                    fos = new FileOutputStream(new File(root, rbf_weights), false);
                } else {
                    fos.write((inputLine + "\n").getBytes());
                }
            }

            fos.close();
            out.close();
            in.close();
            connection.close();

            errMsg = null;
            handler.sendEmptyMessage(-2);

        } catch (Exception e) {
            errMsg = "Error: " + e.getMessage();
            handler.sendEmptyMessage(-2);
        }
    }
}
