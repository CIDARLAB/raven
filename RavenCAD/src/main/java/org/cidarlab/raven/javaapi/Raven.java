/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.javaapi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import org.cidarlab.raven.communication.RavenController;
import org.cidarlab.raven.datastructures.Part;
import org.cidarlab.raven.datastructures.RGraph;
import org.cidarlab.raven.datastructures.Vector;

/**
 *
 * @author evanappleton
 */
public class Raven {
    
    //No argument constructor
    public Raven() {        
    }
    
    //Upload a file, calculate assembly for all parts in a file that are not in the library
    //This will also automatically apply parameters at the bottom of the file for required, forbidden, recommended and discouraged
    public ArrayList<RGraph> assembleAllInFile (ArrayList<File> ravenFiles, String method) {
        
        RavenController raven = new RavenController();
        raven.loadUploadedFiles(ravenFiles);
        
        
        return null;
    }
    
    public ArrayList<RGraph> assemble (HashSet<Part> targetParts, HashSet<Part> partsLib, HashSet<Vector> vectorLib, String method) {
        return null;
    }
}
