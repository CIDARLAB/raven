/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
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
     */
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter out = null;
        String user = null;
        try {
            RavenLogger.setPath(this.getServletContext().getRealPath("/") + "/log/");
            out = response.getWriter();
            String command = request.getParameter("command");
            user = getUser(request).toLowerCase();
            RavenController controller = _controllerHash.get(user);

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
                responseString = controller.fetchData(false);
                out.write(responseString);

            } else if (command.equals("fetchScan")) {
                response.setContentType("application/json");
                String responseString = "";
                responseString = controller.fetchData(true);
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
            } else if (command.equals("run")) {
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
                primerParameters.add(primerParam.getString("meltingTemperature"));
                primerParameters.add(primerParam.getString("targetLength"));

                if (recArray.length > 0) {
                    for (int i = 0; i < recArray.length; i++) {
                        if (recArray[i].length() > 0) {
                            String rcA = recArray[i];
                            rcA = rcA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                            recommended.add(rcA);
                        }
                    }
                }

                if (reqArray.length > 0) {
                    for (int i = 0; i < reqArray.length; i++) {
                        if (reqArray[i].length() > 0) {
                            String rqA = reqArray[i];
                            rqA = rqA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                            required.add(rqA);
                        }
                    }
                }

                if (forbiddenArray.length > 0) {
                    for (int i = 0; i < forbiddenArray.length; i++) {
                        if (forbiddenArray[i].length() > 0) {
                            String fA = forbiddenArray[i];
                            fA = fA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                            forbidden.add(fA);
                        }
                    }
                }

                if (discouragedArray.length > 0) {
                    for (int i = 0; i < discouragedArray.length; i++) {
                        if (discouragedArray[i].length() > 0) {
                            String dA = discouragedArray[i];
                            dA = dA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
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
                JSONObject graphData = controller.run(designCount, method, targetIDs, required, recommended, forbidden, discouraged, partLibraryIDs, vectorLibraryIDs, efficiencyHash, primerParameters);
                JSONArray partsList = controller.generatePartsList(designCount);
                String instructions = controller.getInstructions();
                JSONObject statString = controller.generateStats();
                System.out.println("Stats: " + statString);
                instructions = instructions.replaceAll("[\r\n\t]+", "<br/>");
                if (instructions.length() < 1) {
                    instructions = "Assembly instructions for RavenCAD are coming soon! Please stay tuned.";
                }
                JSONObject toReturn = new JSONObject();
                toReturn.put("status", "good");
                toReturn.put("statistics", statString);
                toReturn.put("instructions", instructions);
                toReturn.put("partsList", partsList);
                toReturn.put("graph", graphData);
                out.println(toReturn);

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
            } else if(command.equals("saveExample")) {
                String url = request.getParameter("url");
                String examplePath = "";
                saveUrl(examplePath, url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (user == null) {
                user = "default";
            }
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            RavenLogger.setPath(this.getServletContext().getRealPath("/") + "/log/");
            e.printStackTrace(printWriter);
            RavenLogger.logError(user, request.getRemoteAddr(), stringWriter.toString());
            String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
            if (out != null) {
                out.println("{\"result\":\"" + exceptionAsString + "\",\"status\":\"bad\"}");
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * @param request servlet request
     * @param response servlet response
     *
     */
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            if (!ServletFileUpload.isMultipartContent(request)) {
                String command = request.getParameter("command");
                String user = getUser(request).toLowerCase();
                RavenController controller = _controllerHash.get(user);
                if (command.equals("importClotho")) {
                    response.setContentType("application/json");
                    String data = request.getParameter("data");
                    JSONArray devices = new JSONArray(data);
                    String toReturn = controller.importClotho(devices);
                    writer.write("{\"result\":\"" + toReturn + "\",\"status\":\"" + toReturn + "\"}");
                }


            } else {
                ServletFileUpload uploadHandler = new ServletFileUpload(new DiskFileItemFactory());
                response.setContentType("text/plain");
                response.sendRedirect("import.html");
                String user = getUser(request);
                RavenController controller = _controllerHash.get(user);
                if (controller == null) {
                    String path = this.getServletContext().getRealPath("/") + "/data/";
                    _controllerHash.put(user, new RavenController(path, user));
                    controller = _controllerHash.get(user);
                }
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

            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
            if (writer != null) {
                writer.write("{\"result\":\"" + exceptionAsString + "\",\"status\":\"bad\"}");
            }
        } finally {
            if (writer != null) {
                writer.close();
            }

        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processGetRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
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
 
    
    public void saveUrl(String filename, String urlString)
      throws MalformedURLException, IOException
    {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try
        {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new FileOutputStream(filename);

            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1)
            {
                fout.write(data, 0, count);
            }
        }
        finally
        {
            if (in != null)
                in.close();
            if (fout != null)
                fout.close();
        }
    }
    //FIELDS
    private HashMap<String, RavenController> _controllerHash = new HashMap(); //key:user, value: collector assocaited with that user
}