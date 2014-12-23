/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.communication;

import java.io.BufferedOutputStream;
//import java.nio.file.Files;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
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
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Admin
 */
public class RavenServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
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
//            user = getUser(request).toLowerCase();
            user = getUser(request);
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
                JSONObject params = new JSONObject();
                params.put("targets", request.getParameter("targets"));
                params.put("partLibrary", request.getParameter("partLibrary"));
                params.put("vectorLibrary", request.getParameter("vectorLibrary"));
                params.put("recommended", request.getParameter("recommended"));
                params.put("required", request.getParameter("required"));
                params.put("forbidden", request.getParameter("forbidden"));
                params.put("discouraged", request.getParameter("discouraged"));
                params.put("efficiency", request.getParameter("efficiency"));
                params.put("primer", request.getParameter("primer"));
                params.put("method", request.getParameter("method"));
                
                JSONObject paramsConfig = new JSONObject();
                paramsConfig.put("recommended", request.getParameter("recommended"));
                paramsConfig.put("required", request.getParameter("required"));
                paramsConfig.put("forbidden", request.getParameter("forbidden"));
                paramsConfig.put("discouraged", request.getParameter("discouraged"));

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
                primerParameters.add(primerParam.getString("targetHomologyLength"));
                primerParameters.add(primerParam.getString("minPCRLength"));
                primerParameters.add(primerParam.getString("minCloneLength"));
                primerParameters.add(primerParam.getString("maxPrimerLength"));

                String[] stageVectorArray = request.getParameter("stageVectors").split(","); 
                HashMap<String,String> stageVectorHash = new HashMap<String, String>(); //key - stage as string, value - vector uuid
                for(int i=0 ;i<stageVectorArray.length;i++) {
                    stageVectorHash.put(String.valueOf(i), stageVectorArray[i]);
                }
                
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
                JSONObject graphData = controller.run(designCount, method, targetIDs, required, recommended, forbidden, discouraged, partLibraryIDs, vectorLibraryIDs, efficiencyHash, primerParameters, stageVectorHash);
                JSONArray partsList = controller.generatePartsList(designCount, paramsConfig.toString(), method);
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
            } else if (command.equals("saveExample")) {
                String url = request.getParameter("url");
                String path = this.getServletContext().getRealPath("/") + "/examples/";
                File dir = new File(path);
                int numExamples = dir.listFiles().length;
                path = path + "example" + (numExamples + 1) + ".png";
                saveUrl(url, path);
            } else if (command.equals("getExamples")) {
                JSONArray imageLinks = new JSONArray();
                String url = request.getParameter("url");
                String path = this.getServletContext().getRealPath("/") + "/examples/";
                File dir = new File(path);
                File[] examples = dir.listFiles();
                for (int i = 0; i < examples.length; i++) {
                    imageLinks.put(examples[i].getName());
                }
                out.write(imageLinks.toString());

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
//                String user = getUser(request).toLowerCase();
                String user = getUser(request);
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
//                new File(uploadFilePath).mkdir();
                
                try
                {
                    File temp = File.createTempFile("tempfile",".tmp");
                    System.out.println("Temp file : "+ temp.getAbsolutePath());
                    File source = new File(temp.getAbsolutePath());
                    File dest = new File(uploadFilePath+"anotherfile.tmp");
                    FileUtils.copyFile(source, dest);
                    
                
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                
                ArrayList<File> toLoad = new ArrayList();
                for (FileItem item : items) {
                    File file;
                    if (!item.isFormField()) {
                        String fileName = item.getName();
                        
//                        File.createTempFile(uploadFilePath, fileName, new File(uploadFilePath));
                        
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
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processGetRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
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

    private void saveUrl(String urlString, String localFileName) {
        int size = 1024;
        OutputStream outStream = null;
        URLConnection uCon = null;
        InputStream inputStream = null;
        try {
            URL url;
            byte[] buf;
            int ByteRead, ByteWritten = 0;
            url = new URL(urlString);
            File newFile = new File(localFileName);
            newFile.createNewFile();
            FileOutputStream fileOutStream = new FileOutputStream(newFile);
            outStream = new BufferedOutputStream(fileOutStream);
            uCon = url.openConnection();
            inputStream = uCon.getInputStream();
            buf = new byte[size];
            while ((ByteRead = inputStream.read(buf)) != -1) {
                outStream.write(buf, 0, ByteRead);
                ByteWritten += ByteRead;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //FIELDS
    private HashMap<String, RavenController> _controllerHash = new HashMap(); //key:user, value: collector assocaited with that user
}
