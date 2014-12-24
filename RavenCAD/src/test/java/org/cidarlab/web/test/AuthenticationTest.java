/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.web.test;

import java.io.File;
import java.util.Scanner;
import org.cidarlab.raven.communication.AuthenticationException;
import org.cidarlab.raven.communication.Authenticator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author evanappleton
 */
public class AuthenticationTest {
    
    public AuthenticationTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    private static final String USER_DB_NAME = "RAVENCAD";
    private static Authenticator auth;

    @BeforeClass
    public static void initialize() {
        auth = new Authenticator(USER_DB_NAME);
        
    }
    
    @Test
    public void testConvertFileToSecureAuthentication() throws Exception {
        initialize();
        Scanner sc = new Scanner(new File("/Users/evanappleton/dfx_git/raven/RavenCAD/src/main/webapp/WEB-INF/restricted/login.txt"));
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
    }
}