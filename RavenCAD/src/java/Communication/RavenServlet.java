/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String command = request.getParameter("command");
        String user = getUser(request);
        RavenController controller = _collectorHash.get(user);
        if (controller == null) {
            String path = this.getServletContext().getRealPath("/") + "/data/";
            _collectorHash.put(user, new RavenController(path, user));
            controller = _collectorHash.get(user);
        }
        if (command.equals("dataStatus")) {
            try {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "";
                responseString = controller.getDataStatus();
                out.write(responseString);
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
                out.write(exceptionAsString);
            } finally {
                out.close();
            }
        } else if (command.equals("load")) {
            try {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "loaded data";
                controller.loadData();
                out.write(responseString);
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
                out.write(exceptionAsString);
            } finally {
                out.close();
            }
        } else if (command.equals("logout")) {
            try {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "logged out";
                _collectorHash.remove(user);
                controller.clearData();
                out.write(responseString);
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
                out.write(exceptionAsString);
            } finally {
                out.close();
            }
        } else if (command.equals("fetch")) {
            try {

                response.setContentType("application/json");
                String responseString = "";
                responseString = controller.fetchData();
                out.write(responseString);
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
                out.write(exceptionAsString);
            } finally {
                out.close();
            }
        } else if (command.equals("purge")) {
            try {
                response.setContentType("test/plain");
                String responseString = "purged";
                controller.clearData();
                out.write(responseString);
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
                out.write(exceptionAsString);
            } finally {
                out.close();
            }
        } else if (command.equals("run")) {
            try {
                response.setContentType("application/json");
                //run(String method, HashMap<Part, ArrayList<Part>> goalParts, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden)
                String[] targetIDs = request.getParameter("targets").split(",");
                String[] partLibraryIDs = request.getParameter("partLibrary").split(",");
                String[] vectorLibraryIDs = request.getParameter("vectorLibrary").split(",");
                String[] recArray = request.getParameter("recommended").split(";");
                String[] reqArray = request.getParameter("required").split(";");
                String[] forbiddenArray = request.getParameter("forbidden").split(";");
                String[] discouragedArray = request.getParameter("discouraged").split(";");
                String method = request.getParameter("method");
                HashSet<String> required = new HashSet();
                HashSet<String> recommended = new HashSet();
                HashSet<String> forbidden = new HashSet();
                HashSet<String> discouraged = new HashSet();

                if (recArray.length > 0) {
                    for (int i = 0; i < recArray.length; i++) {
                        if (recArray[i].length() > 0) {
                            recommended.add(recArray[i]);
                        }
                    }
                }
                if (reqArray.length > 0) {
                    for (int i = 0; i < reqArray.length; i++) {
                        if (reqArray[i].length() > 0) {
                            required.add(reqArray[i]);
                        }
                    }
                }
                if (forbiddenArray.length > 0) {
                    for (int i = 0; i < forbiddenArray.length; i++) {
                        if (forbiddenArray[i].length() > 0) {
                            forbidden.add(forbiddenArray[i]);
                        }
                    }
                }
                if (discouragedArray.length > 0) {
                    for (int i = 0; i < discouragedArray.length; i++) {
                        if (discouragedArray[i].length() > 0) {
                            discouraged.add(discouragedArray[i]);
                        }
                    }
                }
                String designCount = request.getParameter("designCount");
                String image = controller.run(method, targetIDs, required, recommended, forbidden, discouraged, partLibraryIDs, vectorLibraryIDs);
                controller.generatePartsListFile(designCount);
                String instructions = controller.generateInstructionsFile(designCount);
                String statString = controller.generateStats();
                instructions = instructions.replaceAll("[\r\n\t]+", "<br/>");
                if (instructions.length() < 1) {
                    instructions = "Assembly instructions for RavenCAD are coming soon! Please stay tuned.";
                }

                out.println("{\"result\":\"" + image + "\",\"statistics\":" + statString + ",\"instructions\":\"" + instructions + "\",\"status\":\"good\"}");

            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
                out.println("{\"result\":\"" + exceptionAsString + "\",\"status\":\"bad\"}");
            } finally {
                out.close();
            }
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
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
        } finally {
            out.close();
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

//private String run() {
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
    private HashMap<String, RavenController> _collectorHash = new HashMap(); //key:user, value: collector assocaited with that user
}
