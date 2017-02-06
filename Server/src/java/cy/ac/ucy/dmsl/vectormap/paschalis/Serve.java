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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Server's frontend */
public class Serve extends HttpServlet {

    public void init() throws ServletException {
        try {
            Base.startup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints message in JSON Format with message code
     *
     * @param writer
     * @param object
     */
    private static void printJSon(PrintWriter writer, JSonObj object) {
        writer.println(object);
    }

    /**
     * Prints message in JSON Format with message code
     *
     * @param writer
     * @param str
     */
    public static void printString(PrintWriter writer, String str) {
        writer.print(str);
    }

    /*
     * Called when servlet shutsdown, or inactive for a long time
     * (so resources are saved)
     *
     * (non-Javadoc)
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy() {
        super.destroy();

        //Shutdown Base class
        try {
            Base.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {

            JSonObj json = new JSonObj();
            out = null;
            try {
                out = response.getWriter();
            } catch (IOException e) {

                e.printStackTrace();
            }

            //Responce type is plain text
            response.setContentType(Base.JSON_MIME);
            response.setCharacterEncoding("UTF-8");

            // Get request type
            // Can be: load (load data to vectormap)
            // mac: query hbase using a mac
            // bloom: query hbase using a bloom filter
            String type = request.getParameter("type");

            // Get extras for arguments.
            // if type = load, extras are: b, l, bl, nlbnl
            String extras = request.getParameter("extras");

            // IF no type is ginen, print error
            if (type == null) {
                //Set JSon objects data, and print it
                json.setData(
                        Base.CODE_ERROR_PARAMS, "You have to enter a type.\nUsage: type, extras");
                printJSon(out, json);

                return;
            }
            switch (type) {
                case Base.TYPE_LOAD:
                    //Set JSon objects data, and print it
                    json.setData(
                            Base.CODE_ERROR_PARAMS,
                            "Web load not implemented yet.\nExtras here: b, l, bl, nlbl");
                    printJSon(out, json);
                    break;
                case Base.TYPE_MAC:
                    {
                        //Get filter parameter
                        String macAddress = request.getParameter("filter");
                        String dataSet = request.getParameter("dataset");
                        // Bloom filter missing
                        if (macAddress == null || dataSet == null) {
                            // Print error message
                            json.setData(
                                    Base.CODE_ERROR_PARAMS,
                                    "Wrong parameters.\nMac Usage: type=mac, filter=macAddress[,macAddress,macAddress], dataset=b|b1..5\n"
                                            + "[extras=0|1]\nExtras explained: use method 1(mac:rss) or method 2(radiomapv2)");
                            printJSon(out, json);

                            return;
                        }
                        //Extras:
                        // 1: Build partial radiomap with MAC, RSS value pairs
                        // 2: Build partial radiomap with rmap Version 2
                        int method = 3;
                        if (extras != null) {
                            if (extras.equals("1")) method = 1;
                            else if (extras.equals("2")) method = 2;
                            else if (extras.equals("3")) method = 3;
                        }
                        // Create new object to print Mac results
                        PrintMac printMac =
                                new PrintMac(macAddress, method, Integer.parseInt(dataSet), out);
                        //Print result got
                        try {
                            //If now results found
                            if (!printMac.runMethod()) {
                                json.setNoResults();
                                printJSon(out, json);
                            }
                        } catch (IOException e) {
                            json.setData(Base.CODE_ERROR_PROGRAMMERS_FAULT, e.getMessage());
                            printJSon(out, json);
                        }
                        break;
                    }
                case Base.TYPE_BLOOM:
                    {
                        //Get filter parameter
                        String bloomFilter = request.getParameter("filter");
                        String dataSet = request.getParameter("dataset");
                        // Bloom filter missing
                        if (bloomFilter == null || dataSet == null) {
                            // Print error message
                            json.setData(
                                    Base.CODE_ERROR_PARAMS,
                                    "Wrong parameters.\nBloom Usage: type=bloom, filter=bloomFilter, dataset=b|b1..5 "
                                            + "[extras=0|1|2]\nExtras explained:\n0: show matched macs"
                                            + "\nbuild partial rmap:"
                                            + "\n1: w/ rss=mac pairs"
                                            + "\n2: radiomap version 2"
                                            + "\n3: smart json version");
                            printJSon(out, json);

                            return;
                        }
                        //Extras:
                        // 0: Show only matched MAC Addresses
                        // 1: Build partial radiomap with MAC, RSS value pairs
                        // 2: Build partial radiomap with rmap Version 2
                        // 3: Build partial radiomaps with smart json
                        int method = 0;
                        if (extras != null) {
                            if (extras.equals("1")) method = 1;
                            else if (extras.equals("2")) method = 2;
                            else if (extras.equals("3")) method = 3;
                        }
                        // Create new object to print bloom results
                        PrintBloom printBloom =
                                new PrintBloom(bloomFilter, method, Integer.parseInt(dataSet), out);
                        //Print result got
                        try {
                            //If now results found
                            if (!printBloom.runMethod()) {
                                json.setNoResults();
                                printJSon(out, json);
                            }
                        } catch (IOException e) {
                            json.setData(Base.CODE_ERROR_PROGRAMMERS_FAULT, e.getMessage());
                            printJSon(out, json);
                        }
                        break;
                    }
                case Base.TYPE_BLOOM_SIZE:
                    {
                        String dataSet = request.getParameter("dataset");
                        // Bloom filter missing
                        if (dataSet == null) {
                            // Print error message
                            json.setData(
                                    Base.CODE_ERROR_PARAMS,
                                    "Wrong parameters.\nMac Usage: dataset=b|b1..5\n");
                            printJSon(out, json);

                            return;
                        }
                        // Create new object to print Mac results
                        new PrintBloomSize(out, Integer.parseInt(dataSet)).runMethod();
                        break;
                    }
                case Base.TYPE_MAP_TEST:
                    break;
                default:
                    json.setData(
                            Base.CODE_ERROR_PARAMS,
                            "You have to enter a type.\nUsage: type [filter] extras\n\n"
                                    + "type=mac, filter=macAddress, dataset=b|b1..5\n"
                                    + "[extras=0|1]\n"
                                    + "type=bloom, filter=bloomFilter, dataset=b|b1..5 "
                                    + "[extras=0|1|2]\n"
                                    + "type=bloomsize");
                    printJSon(out, json);
                    return;
            }

        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "TVM Servlet";
    } // </editor-fold>
}
