package Communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Admin
 */
public class AuthenticationServlet extends HttpServlet {

    /**
     * Processes requests for HTTP
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        initPasswordHash();
        try {
            String user = request.getParameter("user");
            String password = request.getParameter("password");
            if (passwordHash.containsKey(user) && password.equals(passwordHash.get(user))) {
                System.out.println("authenticated");
                Cookie authenticateCookie = new Cookie("authenticate", "authenticated");
                Cookie userCookie = new Cookie("user", user);
                authenticateCookie.setMaxAge(60 * 24); //cookie lasts for an hour
                response.addCookie(authenticateCookie);
                response.addCookie(userCookie);
                response.sendRedirect("index.html");
                out.println("authenticated");
            } else {
                response.sendRedirect("login.html");
                Cookie authenticateCookie = new Cookie("authenticate", "failed");
                authenticateCookie.setMaxAge(60 * 24); //cookie lasts for an hour
                response.addCookie(authenticateCookie);
                out.println("failed");
            }
        } finally {
            out.close();
        }
    }

    /**
     * Processes requests for HTTP
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.sendRedirect("index.html");
        PrintWriter out = response.getWriter();
        try {
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                System.out.println(parameterNames.nextElement());
            }
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

    private void initPasswordHash() {
        passwordHash = new HashMap();
        passwordHash.put("admin", "admin");
        passwordHash.put("jenhan", "tao");
    }
    HashMap<String, String> passwordHash;
}
