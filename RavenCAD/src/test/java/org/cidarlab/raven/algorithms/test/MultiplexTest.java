/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.test;

import java.io.File;
import java.util.ArrayList;
import org.cidarlab.raven.communication.RavenController;
import org.cidarlab.raven.javaapi.Raven;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author evanappleton
 */
public class MultiplexTest {
    
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
    
    public String getFilepath() {
        String filepath="";       
        filepath = RavenController.class.getClassLoader().getResource(".").getPath();
        filepath = filepath.substring(0,filepath.indexOf("/target/"));
        return filepath;
    }
    
    @Test
    public void multiplexORGateTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/endy_OR_gate_multiplex.csv";
        File toLoad = new File(filePath);
        ArrayList<File> ravenFiles = new ArrayList();
        ravenFiles.add(toLoad);
        
        String[] methods = new String[]{"biobricks", "gibson", "cpec", "slic", "goldengate", "moclo"};
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);           
            Raven raven = new Raven();
            raven.assembleFileInput(ravenFiles, parameters, false);
        }
    }
    
//    public MultiplexTest() {        
//    }
//    
//    public static void main(String[] args) throws Exception {
//        MultiplexTest t = new MultiplexTest();
//        t.multiplexORGateTest();
//        String test = "";
//    }
}
