/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.javaapi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import org.cidarlab.raven.accessibility.ClothoReader;
import org.cidarlab.raven.communication.RavenController;
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
    
    //Upload a file, calculate assembly for all parts in a file that are not in the library
    //This will also automatically apply parameters at the bottom of the file unless external parameters are applied
    public ArrayList<RGraph> assembleFile (ArrayList<File> ravenFiles, JSONObject parameters) throws Exception {
        
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
            parameters = new JSONObject(raven.getParameters());
        }
        
        raven.run("1", parameters, gps, null);
        
        return null;
    }
    
    public ArrayList<RGraph> assemble (HashSet<Part> targetParts, HashSet<Part> partsLib, HashSet<Vector> vectorLib, JSONObject parameters) {
        return null;
    }
}
