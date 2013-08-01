/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Admin
 */
public class RavenServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        RavenLogger.setPath(this.getServletContext().getRealPath("/") + "/log/");
        PrintWriter out = response.getWriter();
        String command = request.getParameter("command");
        String user = getUser(request).toLowerCase();
        RavenController controller = _controllerHash.get(user);

        try {
            if (controller == null) {
                String path = this.getServletContext().getRealPath("/") + "/data/";
                _controllerHash.put(user, new RavenController(path, user));
                controller = _controllerHash.get(user);
            }
            if (command.equals("dataStatus")) {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "";
                responseString = controller.getDataStatus();
                out.write(responseString);

            } else if (command.equals("load")) {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "loaded data";
                controller.saveNewDesign(request.getParameter("designCount"));
                out.write(responseString);

            } else if (command.equals("logout")) {
                response.setContentType("text/html;charset=UTF-8");
                RavenLogger.logSessionOut(user, request.getRemoteAddr());
                String responseString = "logged out";
                _controllerHash.remove(user);
                controller.clearData();
                out.write(responseString);

            } else if (command.equals("fetch")) {
                response.setContentType("application/json");
                String responseString = "";
                responseString = controller.fetchData();
                out.write(responseString);

            } else if (command.equals("purge")) {
                response.setContentType("test/plain");
                String responseString = "purged";
                controller.clearData();
                out.write(responseString);

            } else if (command.equals("delete")) {
                String[] partIDs = request.getParameter("parts").split(",");
                String[] vectorIDs = request.getParameter("vectors").split(",");
                for (int i = 0; i < partIDs.length; i++) {
                    controller._collector.removePart(partIDs[i]);
                }
                for (int i = 0; i < vectorIDs.length; i++) {
                    controller._collector.removeVector(partIDs[i]);
                }
            } else if (command.equals("importClotho")) {
                response.setContentType("application/json");
                String deviceString = request.getParameter("devices");
                JSONArray devices = new JSONArray(deviceString);
                String basicPartString = request.getParameter("basicParts");
                JSONArray basicParts = new JSONArray(basicPartString);
                String toReturn = controller.importClotho(devices, basicParts);
                out.write("{\"result\":\"" + toReturn + "\",\"status\":\"" + toReturn + "\"}");
            } else if (command.equals("run")) {
                System.out.println(request.getParameter("required"));
                response.setContentType("application/json");
                String[] targetIDs = request.getParameter("targets").split(",");
                String[] partLibraryIDs = request.getParameter("partLibrary").split(",");
                String[] vectorLibraryIDs = request.getParameter("vectorLibrary").split(",");
                String[] recArray = request.getParameter("recommended").split(";");
                String[] reqArray = request.getParameter("required").split(";");
                String[] forbiddenArray = request.getParameter("forbidden").split(";");
                String[] discouragedArray = request.getParameter("discouraged").split(";");
                String[] efficiencyArray = request.getParameter("efficiency").split(",");
                JSONObject primerParam = new JSONObject(request.getParameter("primer"));
                String method = request.getParameter("method");
                HashSet<String> required = new HashSet();
                HashSet<String> recommended = new HashSet();
                HashSet<String> forbidden = new HashSet();
                HashSet<String> discouraged = new HashSet();
                HashMap<Integer, Double> efficiencyHash = new HashMap();

                //[oligoNameRoot, forwardPrefix, reversePrefix, forwardCutSite, reverseCutSite, forwardCutDistance, reverseCutDistance,meltingTemperature, targetLength)
                ArrayList<String> primerParameters = new ArrayList();
                primerParameters.add(primerParam.getString("oligoNameRoot"));
                primerParameters.add(primerParam.getString("forwardPrefix"));
                primerParameters.add(primerParam.getString("reversePrefix"));
                primerParameters.add(primerParam.getString("forwardCutSite"));
                primerParameters.add(primerParam.getString("reverseCutSite"));
                primerParameters.add(primerParam.getString("forwardCutDistance"));
                primerParameters.add(primerParam.getString("reverseCutDistance"));
                primerParameters.add(primerParam.getString("meltingTemperature"));
                primerParameters.add(primerParam.getString("targetLength"));
                if (recArray.length > 0) {
                    for (int i = 0; i < recArray.length; i++) {
                        if (recArray[i].length() > 0) {
                            String rcA = recArray[i];
                            rcA = rcA.replaceAll("\\|", "").replaceAll("\\+", "").replaceAll("-", "");
                            recommended.add(rcA);
                        }
                    }
                }

                if (reqArray.length > 0) {
                    for (int i = 0; i < reqArray.length; i++) {
                        if (reqArray[i].length() > 0) {
                            String rqA = reqArray[i];
                            rqA = rqA.replaceAll("\\|", "").replaceAll("\\+", "").replaceAll("-", "");
                            required.add(rqA);
                        }
                    }
                }

                if (forbiddenArray.length > 0) {
                    for (int i = 0; i < forbiddenArray.length; i++) {
                        if (forbiddenArray[i].length() > 0) {
                            String fA = forbiddenArray[i];
                            fA = fA.replaceAll("\\|", "").replaceAll("\\+", "").replaceAll("-", "");
                            forbidden.add(fA);
                        }
                    }
                }

                if (discouragedArray.length > 0) {
                    for (int i = 0; i < discouragedArray.length; i++) {
                        if (discouragedArray[i].length() > 0) {
                            String dA = discouragedArray[i];
                            dA = dA.replaceAll("\\|", "").replaceAll("\\+", "").replaceAll("-", "");
                            discouraged.add(dA);
                        }
                    }
                }

                //generate efficiency hash
                if (efficiencyArray.length > 0) {
                    for (int i = 0; i < efficiencyArray.length; i++) {
                        efficiencyHash.put(i + 2, Double.parseDouble(efficiencyArray[i]));
                    }
                }

                String designCount = request.getParameter("designCount");
                String image = controller.run(designCount, method, targetIDs, required, recommended, forbidden, discouraged, partLibraryIDs, vectorLibraryIDs, efficiencyHash, primerParameters);
                String partsList = controller.generatePartsList(designCount);
                String instructions = controller.getInstructions();
                String statString = controller.generateStats();
                System.out.println("Stats: " + statString);
                instructions = instructions.replaceAll("[\r\n\t]+", "<br/>");
                if (instructions.length() < 1) {
                    instructions = "Assembly instructions for RavenCAD are coming soon! Please stay tuned.";
                }
                out.println("{\"result\":\"" + image + "\",\"statistics\":" + statString + ",\"instructions\":\"" + instructions + "\",\"status\":\"good\",\"partsList\":" + partsList + "}");

            } else if (command.equals("save")) {
                String[] partIDs = request.getParameter("partIDs").split(",");
                String[] vectorIDs = request.getParameter("vectorIDs").split(",");
                boolean writeSQL = Boolean.parseBoolean(request.getParameter("writeSQL"));
//                Boolean writeSQL= true;
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "failed save data";
                responseString = controller.save(partIDs, vectorIDs, writeSQL);
                out.write(responseString);

            } else if (command.equals("mail")) {
//                GoogleMail.Send("ravencadhelp", "Cidar1123", "eapple@bu.edu", "Guess who can send emails using a server now?", "test message");
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            RavenLogger.setPath(this.getServletContext().getRealPath("/") + "/log/");
            e.printStackTrace(printWriter);
            RavenLogger.logError(user, request.getRemoteAddr(), stringWriter.toString());
            String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
            out.println("{\"result\":\"" + exceptionAsString + "\",\"status\":\"bad\"}");
        } finally {
            out.close();
        }
    }

    /**
     * @param request servlet request
     * @param response servlet response
     * @
     * throws ServletException if a servlet -specific error occurs
     * @
     * throws IOException if an I /O error occurs
     *
     */
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new IllegalArgumentException("Request is not multipart, please 'multipart/form-data' enctype for your form.");
        }
        ServletFileUpload uploadHandler = new ServletFileUpload(new DiskFileItemFactory());
        PrintWriter writer = response.getWriter();
        response.setContentType("text/plain");
        response.sendRedirect("import.html");
        String user = getUser(request);
        RavenController controller = _controllerHash.get(user);
        if (controller == null) {
            String path = this.getServletContext().getRealPath("/") + "/data/";
            _controllerHash.put(user, new RavenController(path, user));
            controller = _controllerHash.get(user);
        }
        try {
            List<FileItem> items = uploadHandler.parseRequest(request);
            String uploadFilePath = this.getServletContext().getRealPath("/") + "/data/" + user + "/";
            RavenLogger.setPath(this.getServletContext().getRealPath("/") + "/log/");
            new File(uploadFilePath).mkdir();
            ArrayList<File> toLoad = new ArrayList();
            for (FileItem item : items) {
                File file;
                if (!item.isFormField()) {
                    String fileName = item.getName();
                    if (fileName.equals("")) {
                        System.out.println("You forgot to choose a file.");
                    }
                    if (fileName.lastIndexOf("\\") >= 0) {
                        file = new File(uploadFilePath + fileName.substring(fileName.lastIndexOf("\\")));
                    } else {
                        file = new File(uploadFilePath + fileName.substring(fileName.lastIndexOf("\\") + 1));
                    }
                    item.write(file);
                    toLoad.add(file);
                }
                writer.write("{\"result\":\"good\",\"status\":\"good\"}");
            }
            controller.loadUploadedFiles(toLoad);
        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
            writer.write("{\"result\":\"" + exceptionAsString + "\",\"status\":\"bad\"}");

        } finally {
            writer.close();

        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processGetRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processPostRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    /**
     * Get session user *
     */
    private String getUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String user = "default";
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals("user")) {
                    user = cookies[i].getValue();
                }
            }
        }
        return user;
    }
    //FIELDS
    private HashMap<String, RavenController> _controllerHash = new HashMap(); //key:user, value: collector assocaited with that user
}
