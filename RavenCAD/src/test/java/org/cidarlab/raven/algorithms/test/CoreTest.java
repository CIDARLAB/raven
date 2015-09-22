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
public class CoreTest {
    
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
            raven.assembleFileInput(ravenFiles, parameters, false);
        }
        
        //Forbidden required test
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);
            parameters.put("required", "[P7_Promoter|+, attB_TP901|+, attB_Bxb1|+, B0015|+]");
            parameters.put("forbidden", "[attP_Bxb1|-, attP_TP901|-, JBEI_RBS|+, Superfolder_gfp|+, B0015|+]");            
            Raven raven = new Raven();
            raven.assembleFileInput(ravenFiles, parameters, false);
        }
    }
    
    @Test
    public void endyORGateTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/endy_OR_gate.csv";
        File toLoad = new File(filePath);
        ArrayList<File> ravenFiles = new ArrayList();
        ravenFiles.add(toLoad);
        
        //Save test
        String[] methods = new String[]{"biobricks", "gibson", "cpec", "slic", "goldengate", "moclo"};
        for (String method : methods) {
            JSONObject parameters = new JSONObject();
            parameters.put("method", method);
            Raven raven = new Raven();
            raven.assembleFileInput(ravenFiles, parameters, true);
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
            raven.assembleFileInput(ravenFiles, parameters, false);
        }
    }
    
    @Test
    public void repressilatorBlankTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/repressilator.csv";
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
    
//    @Test
    public void repressilatorCoSBiTest() throws Exception {
        String filePath = getFilepath() + "/src/main/resources/RavenTestFiles/repressilators_CosBi.csv";
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
//    
//    public static void main (String[] args) throws Exception {
//        CoreTest coreTest = new CoreTest();
//        coreTest.benchmarkSetTest();
//    }
}
