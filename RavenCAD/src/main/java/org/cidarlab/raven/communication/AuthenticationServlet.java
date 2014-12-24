/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.communication;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.Assert.assertNotEquals;

/**
 *
 * @author Ernst Oberortner
 */
public class AuthenticationServlet 
	extends HttpServlet {

	private static final long serialVersionUID = -2579220291590687064L;
	
	private static final String USER_DB_NAME = "RAVENCAD";
	private static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AuthenticationServlet");
	
	// a reference to an instance 
	// of the CIDAR authenticator
	private Authenticator auth;
	
	@Override
	public void init(ServletConfig config) 
			throws ServletException {
		
	    super.init(config);
	    
	    this.auth = new Authenticator(USER_DB_NAME);
	    
	    // set a system property such that Simple Logger will include timestamp
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // set a system property such that Simple Logger will include timestamp in the given format
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd-MM-yy HH:mm:ss");

        // set minimum log level for SLF4J Simple Logger at warn
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        LOGGER.warn("[AuthenticationServlet] loaded!");	
        
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

        JSONObject jsonResponse = new JSONObject();

        try {

            // get the username and password parameter values 
            // from the request
            String command = request.getParameter("command");
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            /*
             * SIGNUP Request
             */
            if ("signup".equals(command)) {

                // if this is the initialization password
                if ("initialize".equals(username) && "initialize".equals(password)) {
                    Scanner sc = new Scanner(new File(this.getServletContext().getRealPath("/") + "/WEB-INF/restricted/login.txt"));
                    String s;
                    while (sc.hasNext()) {
                        s = sc.nextLine();
                        String user = s.split(",")[0];
                        String passwd = s.split(",")[1];

                        try {
                            auth.register(user, passwd, true);
                        } catch (AuthenticationException ae) {
                            assertNotEquals(ae.getMessage(), "The user exists already!");
                        }

                    }
                    sc.close();
                } else {

                    // register the user
                    this.auth.register(username, password, false);
                }
                
                // we automatically login the user, 
                // i.e. we do some session management 
                this.login(request, response, username);

                /*
                 * LOGIN Request
                 */
            } else if ("login".equals(command)) {

                // first, we check if the user exists and 
                // if the passwords match up
                boolean bLogin = this.auth.login(username, password);

                if (bLogin) {

                    request.getSession().invalidate();
                    //Not sure what this next line does, but seems to work without it
//                    request.changeSessionId();
                    
                    RavenLogger.setPath(this.getServletContext().getRealPath("/") + "/log/");
                    String ipAddress = request.getHeader("X-FORWARDED-FOR");
                    if (ipAddress == null) {
                        ipAddress = request.getRemoteAddr();
                    }
                    RavenLogger.logSessionIn(username, ipAddress);

                    // login the user including session management
                    this.login(request, response, username);
                }

                /*
                 *  LOGOUT Request
                 */
            } else if ("logout".equals(command)) {

                HttpSession session = request.getSession(false);
                if (session != null) {
                    // the session expires immediately
                    session.setMaxInactiveInterval(1);
                    // we remove the user information
                    session.removeAttribute("user");
                    // and invalidate the session
                    session.invalidate();
//                    response.sendRedirect("index.html");
                }

                /*
                 * Invalid Request	
                 */
            } else {
                LOGGER.warn("Invalid login! user: " + username + ", password: " + password);
                throw new AuthenticationException("Invalid Request!");
            }

            jsonResponse.put("status", "good");

        } catch (Exception e) {
            try {
                LOGGER.warn(e.getLocalizedMessage());

                jsonResponse.put("status", "exception");
                jsonResponse.put("result", e.getLocalizedMessage());

//                response.sendRedirect("index.html");
            } catch (JSONException ex) {
                Logger.getLogger(AuthenticationServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /*
         * write the response
         */
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");

        out.write(jsonResponse.toString());

        out.flush();
        out.close();
    }
    
    private void login(HttpServletRequest request, HttpServletResponse response, String user) {

        /*-------------------------------
         * VALID AUTHENTICATION 
         *-------------------------------*/

        // we create a session
        HttpSession session = request.getSession(true);

        // put the username into it
        session.setAttribute("user", user);

        // a session expires after 60 mins
        session.setMaxInactiveInterval(60 * 60);

        // also, we set two cookies
        // - the first cookie indicates of the user is authenticated
        // - the second cookie contains user information
        Cookie authenticateCookie = new Cookie("raven", "authenticated");
        Cookie userCookie = new Cookie("user", user);
        authenticateCookie.setMaxAge(60 * 60); //cookie lasts for an hour

        // add both cookies to the response
        response.addCookie(authenticateCookie);
        response.addCookie(userCookie);
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
        response.sendRedirect("login.html");
        PrintWriter out = response.getWriter();
        try {
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
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}