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
    public ArrayList<RGraph> assembleFile (ArrayList<File> ravenFiles, JSONObject parameters, boolean save) throws Exception {
        
        //Load data
        HashSet<Part> gps = new HashSet();
        RavenController raven = new RavenController();
        raven.loadUploadedFiles(ravenFiles);
        for (Part p : raven.getCollector().getAllParts(true)) {
            if (ClothoReader.parseTags(p.getSearchTags(), "Type: ").contains("plasmid") && p.isComposite()) {
                gps.add(p);
            }
        }
        
        //If there are no parameters specified
        if (parameters == null) {
            String parametersString = raven.getParameters();
            if (parametersString == null) {
                parameters = new JSONObject();
            } else {
                parameters = new JSONObject(parametersString);
            }
        }
        
        raven.run(null, parameters, gps, null);
        if (save) {
            raven.save(new HashSet(raven.getCollector().getAllParts(true)), new HashSet(raven.getCollector().getAllVectors(true)), false);
        }
        
        ArrayList<RGraph> assemblyGraphs = raven.getAssemblyGraphs();
        Statistics statistics = raven.getStatistics();
        
        return assemblyGraphs;
    }
    
    //This will run Raven for a given library, set of targets and design parameters
    public ArrayList<RGraph> assemble (HashSet<Part> targetParts, HashSet<Part> partsLib, HashSet<Vector> vectorLib, HashMap<Integer, Vector> stageVectors, JSONObject parameters) throws Exception {
        
        //Add library to Raven collector and save parts to library
        RavenController raven = new RavenController();
        for (Part p : partsLib) {
            p.saveDefault(raven.getCollector());
            raven.addToPartLibrary(p);
        }
        for (Vector v : vectorLib) {
            v.saveDefault(raven.getCollector());
            raven.addToVectorLibrary(v);
        }
        
        raven.run(null, parameters, targetParts, stageVectors);
        ArrayList<RGraph> assemblyGraphs = raven.getAssemblyGraphs();
        
        return assemblyGraphs;
    }
}
