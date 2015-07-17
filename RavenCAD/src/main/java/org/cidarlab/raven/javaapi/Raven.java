/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.javaapi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.cidarlab.raven.accessibility.ClothoReader;
import org.cidarlab.raven.communication.RavenController;
import org.cidarlab.raven.communication.Statistics;
import org.cidarlab.raven.datastructures.Part;
import org.cidarlab.raven.datastructures.RGraph;
import org.cidarlab.raven.datastructures.Vector;
import org.json.JSONObject;

/**
 *
 * @author evanappleton
 */
public class Raven {
    
    //No argument constructor
    public Raven() {        
    }
    
    //Upload a file, calculate assembly for all plasmids in a file that are not in the library
    //This will also automatically apply parameters at the bottom of the file unless external parameters are applied
    public ArrayList<RGraph> assembleFileInput (ArrayList<File> ravenFiles, JSONObject parameters, boolean save) throws Exception {
        
        //Load data
        HashSet<Part> gps = new HashSet();
        RavenController raven = new RavenController(null);
        raven.loadUploadedFiles(ravenFiles);
        for (Part p : raven.getCollector().getAllParts(true)) {
            if (p.getType().contains("plasmid") && p.isComposite()) {
                gps.add(p);
            }
        }
        
        //Assembly parameters
        if (parameters == null) {
            String parametersString = raven.getParameters();
            if (parametersString == null) {
                parameters = new JSONObject();
            } else {
                parameters = new JSONObject(parametersString);
            }
        } else {
            String parametersString = raven.getParameters();
            if (parametersString != null) {
                JSONObject newParameters = new JSONObject(parametersString);
                parameters = mergeParameters(parameters, newParameters);
            }
        }
        
        raven.run(null, parameters, gps, null, null);
        if (save) {
            raven.save(new HashSet(raven.getCollector().getAllParts(true)), new HashSet(raven.getCollector().getAllVectors(true)), false);
        }
        
        ArrayList<RGraph> assemblyGraphs = raven.getAssemblyGraphs();
        Statistics statistics = raven.getStatistics();
        
        return assemblyGraphs;
    }
    
    //This will run Raven for a given library, set of targets and design parameters
    public ArrayList<RGraph> assemble (HashSet<Part> targetParts, HashSet<Part> partsLib, HashSet<Vector> vectorLib, HashMap<Part, Part> partVectorPairs, HashMap<Integer, Vector> stageVectors, JSONObject parameters) throws Exception {
        
        //Add library to Raven collector and save parts to library
        RavenController raven = new RavenController(null);
        for (Part p : partsLib) {
            p.saveDefault(raven.getCollector());
            raven.addToPartLibrary(p);
        }
        for (Vector v : vectorLib) {
            v.saveDefault(raven.getCollector());
            raven.addToVectorLibrary(v);
        }
        
        raven.run(null, parameters, targetParts, stageVectors, null);
        ArrayList<RGraph> assemblyGraphs = raven.getAssemblyGraphs();
        
        return assemblyGraphs;
    }
    
    //This will run Raven for a given library, set of targets and design parameters and return the instruction file
    public File assemblyInstructions (HashSet<Part> targetParts, HashSet<Part> partsLib, HashSet<Vector> vectorLib, HashMap<Part, Vector> partVectorPairs, HashMap<Integer, Vector> stageVectors, JSONObject parameters, String filePath) throws Exception {
        
        //Add library to Raven collector and save parts to library
        RavenController raven = new RavenController(filePath);
        for (Part p : partsLib) {
            p = p.saveDefault(raven.getCollector());
            raven.addToPartLibrary(p);            
        }
        for (Vector v : vectorLib) {
            v = v.saveDefault(raven.getCollector());
            raven.addToVectorLibrary(v);
        }
        for (Part t : targetParts) {
            t = t.saveDefault(raven.getCollector());
        }
        
        HashMap<String, String> makeLibraryOHHash = raven.makeLibraryOHHash(partsLib);
        makeLibraryOHHash.putAll(raven.makeLibraryOHHash(targetParts));
        raven.setLibraryOHHash(makeLibraryOHHash);
        raven.setPartVectorPairs(partVectorPairs);
        raven.run(null, parameters, targetParts, stageVectors, filePath);
//        String instructions = raven.getInstructions();
        File assmInstructions = raven.getInstructionsFile();
        
        return assmInstructions;
    }
    
    //Merge parameters from multiple input files
    public JSONObject mergeParameters (JSONObject existing, JSONObject merge) {
        
        for (Object objKey : merge.keySet()) {
            String key = objKey.toString();
            
            //Replace primer parameters
            if (key.equalsIgnoreCase("meltingTemperature") || key.equalsIgnoreCase("targetHomologyLength") || key.equalsIgnoreCase("minPCRLength") || key.equalsIgnoreCase("minCloneLength") || key.equalsIgnoreCase("maxPrimerLength") || key.equalsIgnoreCase("oligoNameRoot")) {
                existing.put(key, merge.get(key));
            
            //Replace assembly method
            } else if (key.equalsIgnoreCase("method")) {
                if (existing.has("method")) {
                    existing.remove("method");
                    existing.put(key, merge.get(key));
                }
                if (merge.has("efficiency")) {
                    existing.remove("efficiency");
                    if (existing.has("efficiency")) {
                        existing.put("efficiency", merge.get("efficiency"));
                    }
                }

            //Merge required, recommended, discouraged, forbidden
            } else if (key.equalsIgnoreCase("required") || key.equalsIgnoreCase("recommended") || key.equalsIgnoreCase("discouraged") || key.equalsIgnoreCase("forbidden")) {
                
                //Get existing inermediates
                Object get = existing.get(key);
                String existingVal = get.toString();
                String[] tokens = existingVal.split(";");
                HashSet<String> intermediateSet = new HashSet();
                for (String token : tokens) {
                    intermediateSet.add(token);
                }
                
                //Get merged intermediates
                Object get2 = merge.get(key);
                String existingVal2 = get2.toString();
                String[] tokens2 = existingVal2.split(";");
                for (String token : tokens2) {
                    intermediateSet.add(token);
                }
                
                String finalSet = "";
                for (String intermediate : intermediateSet) {
                    finalSet = finalSet + intermediate + ";";
                }
                finalSet = finalSet.substring(0, finalSet.length()-2);
                existing.put(key, finalSet);
            }
        }
        
        return existing;
    }
}
