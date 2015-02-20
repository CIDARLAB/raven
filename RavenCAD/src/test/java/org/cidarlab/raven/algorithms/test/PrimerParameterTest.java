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
public class PrimerParameterTest {
    
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
    public void libraryReUseTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/RavenTestFile.csv";
        File toLoad = new File(filePath);
        ArrayList<File> ravenFiles = new ArrayList();
        ravenFiles.add(toLoad);
        
        String[] methods = new String[]{"biobricks", "gibson", "cpec", "slic", "goldengate", "moclo"};
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);           
            Raven raven = new Raven();
            raven.assembleFile(ravenFiles, parameters, false);
        }
    }
    
    @Test
    public void endyORGateTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/endy_OR_gate.csv";
        File toLoad = new File(filePath);
        ArrayList<File> ravenFiles = new ArrayList();
        ravenFiles.add(toLoad);
        
        String[] methods = new String[]{"biobricks", "gibson", "cpec", "slic", "goldengate", "moclo"};
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);           
            Raven raven = new Raven();
            raven.assembleFile(ravenFiles, parameters, false);
        }
        
        //Primer parameter tests
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);   
            parameters.put("oligoNameRoot", "foo");
            parameters.put("meltingTemperature", "65.0");
            parameters.put("targetHomologyLength", "15");
            parameters.put("minPCRLength", "40");
            parameters.put("minCloneLength", "100");
            parameters.put("maxPrimerLength", "100");
            Raven raven = new Raven();
            raven.assembleFile(ravenFiles, parameters, false);
        }
    }
    
//    @Test
    public void benchmarkSetTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/master_8_papers_d_p_l.csv";
        File toLoad = new File(filePath);
        ArrayList<File> ravenFiles = new ArrayList();
        ravenFiles.add(toLoad);
        
        String[] methods = new String[]{"biobricks", "gibson", "cpec", "slic", "goldengate", "moclo"};
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);           
            Raven raven = new Raven();
            raven.assembleFile(ravenFiles, parameters, false);
        }
    }
}
