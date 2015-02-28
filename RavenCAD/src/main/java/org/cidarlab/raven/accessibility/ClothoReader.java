/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.accessibility;

import org.cidarlab.raven.datastructures.Part;
import org.cidarlab.raven.datastructures.RGraph;
import org.cidarlab.raven.datastructures.RNode;
import org.cidarlab.raven.datastructures.RVector;
import org.cidarlab.raven.datastructures.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class ClothoReader {
    
    /**
     * ************************************************************************
     *
     * DATA IMPORT FROM CLOTHO DATA STRUCTURE
     *
     *************************************************************************
     */
    
    /** Given goal parts and library, create hashMem, key: composition with overhangs concatenated at the end, value: corresponding graph **/
    public static HashMap<String, RGraph> partImportClotho(ArrayList<Part> partLibrary, HashSet<String> discouraged, HashSet<String> recommended) throws Exception {

        //Create library to initialize hashMem
        HashMap<String, RGraph> library = new HashMap<String, RGraph>();

        //If there is an input Clotho part library, make a new node with only type and composition from library
        if (partLibrary != null) {
            for (Part libraryPart : partLibrary) {
                if (!libraryPart.getType().contains("plasmid")) {

                    //Check if basic part or not and assign composition 
                    ArrayList<Part> libPartComposition = new ArrayList<Part>();
                    if (!libraryPart.isBasic()) {
                        libPartComposition = ClothoWriter.getComposition(libraryPart);
                    } else {
                        libPartComposition.add(libraryPart);
                    }

                    //For all of this library part's components make new basic graph
                    ArrayList<String> type = new ArrayList<String>();
                    ArrayList<String> composition = new ArrayList<String>();
                    ArrayList<String> direction = libraryPart.getDirections();
                    ArrayList<String> scars = libraryPart.getScars();

                    //Get basic part types
                    for (Part libPartComponent : libPartComposition) {                        
                        composition.add(libPartComponent.getName());
                        type.addAll(libPartComponent.getType());
                    }

                    //Initialize new graph for library part
                    RGraph libraryPartGraph = new RGraph();
                    libraryPartGraph.getRootNode().setUUID(libraryPart.getUUID());
                    libraryPartGraph.getRootNode().setComposition(composition);
                    libraryPartGraph.getRootNode().setType(type);
                    libraryPartGraph.getRootNode().setDirection(direction);
                    libraryPartGraph.getRootNode().setScars(scars);
                    libraryPartGraph.getRootNode().setName(libraryPart.getName());

                    //If recommended, give graph a recommended score of 1, make root node recommended
                    if (recommended.contains(composition.toString())) {
                        libraryPartGraph.setReccomendedCount(libraryPartGraph.getReccomendedCount() + 1);
                        libraryPartGraph.getRootNode().setRecommended(true);
                    }

                    //If discouraged, give graph a recommended score of 1, make root node recommended
                    if (discouraged.contains(composition.toString())) {
                        libraryPartGraph.setDiscouragedCount(libraryPartGraph.getDiscouragedCount() + 1);
                        libraryPartGraph.getRootNode().setDiscouraged(true);
                    }

                    //Put library part into library for assembly
                    library.put(composition.toString() + direction.toString(), libraryPartGraph);
                }
            }
        }
        return library;
    }

    /* 
     * Given a vector, convert it to an RVector *
     */
    public static RVector vectorImportClotho(Vector vector) {

        //Provided there is an input vector library
        RVector rVector = null;
        if (vector != null) {

            //Initialize a new vector
            rVector = new RVector(vector.getLeftOverhang(), vector.getRightOverhang(), vector.getLevel(), vector.getName(), vector.getUUID());
            rVector.setResistance(vector.getResistance());
        }
        return rVector;
    }

    /** Convert goal parts into SRS nodes for the algorithm **/
    public static ArrayList<RNode> gpsToNodesClotho(HashSet<Part> goalParts) throws Exception {
        
        ArrayList<RNode> gpsNodes = new ArrayList<RNode>();
        for (Part goalPart : goalParts) {
            
            //Get goal part's composition and type (part description type)
            ArrayList<Part> basicParts = ClothoWriter.getComposition(goalPart);
            ArrayList<String> composition = new ArrayList<String>();
            ArrayList<String> type = new ArrayList<String>();
            ArrayList<String> direction = goalPart.getDirections();
            ArrayList<String> scars = goalPart.getScars();

            //Get basic part types
            for (int j = 0; j < basicParts.size(); j++) {
                composition.add(basicParts.get(j).getName());                
                
                //If there was no direction found, all basic parts assumed to be forward
                if (direction.isEmpty()) {
                    direction.add("+");
                }
                
                type.addAll(basicParts.get(j).getType());
            }

            //Create a new node with the specified composition, add it to goal parts, required intermediates and recommended intermediates for algorithm
            RNode gp = new RNode(false, false, composition, direction, type, scars, "", "", 0, 0, null);
            gp.setUUID(goalPart.getUUID());
            gp.setName(goalPart.getName());
            gpsNodes.add(gp);
        }
        
        //Sort nodes by part size and then composition name
        ArrayList<RNode> orderedGPSNodes = new ArrayList<RNode>();
        HashMap<String, RNode> partNameHash = new HashMap<String, RNode>();
        for (RNode gpsNode : gpsNodes) {
            String key = gpsNode.getComposition().size() + gpsNode.getNodeKey("+");
            partNameHash.put(key, gpsNode);
        }        
        Set<String> keySet = partNameHash.keySet();
        ArrayList<String> partNames = new ArrayList<String>(keySet);
        Collections.sort(partNames);
        for (String partName : partNames) {
            orderedGPSNodes.add(partNameHash.get(partName));
        }
        
        return orderedGPSNodes;
    }

    /** Parse Clotho search tags from a string into an ArrayList **/
    public static ArrayList<String> stringToList(String in) {
        
        ArrayList<String> list = new ArrayList<String>();

        //Split any arraylist-like search tag
        if (in.charAt(in.length() - 1) == ']') {
            String splitTag = in.substring(in.indexOf("[") + 1, in.length() - 1);
            String[] tokens = splitTag.split(",");
            ArrayList<String> trimmedTokens = new ArrayList<String>();

            //Trim tokens to add to final list
            for (String token : tokens) {
                String trimmedToken = token.trim();
                if (!token.equals("")) {
                    trimmedTokens.add(trimmedToken);
                }
            }
            list.addAll(trimmedTokens);

        }
        return list;
    }
    
    /**
     * Returns a part library and finds all forward and reverse characteristics
     * of each part *
     */
    public static HashSet<String> getExistingPartKeys(ArrayList<Part> partLib) {

        HashSet<String> startPartsLOcompRO = new HashSet();

        //Go through parts library, put all compositions into hash of things that already exist
        for (Part aPart : partLib) {

            //Get forward and reverse part key string
            ArrayList<String> comp = aPart.getStringComposition();
            ArrayList<String> revComp = (ArrayList<String>) comp.clone();
            Collections.reverse(revComp);

            ArrayList<String> dir = aPart.getDirections();
            ArrayList<String> scars = aPart.getScars();

            ArrayList<String> revDir = (ArrayList<String>) dir.clone();
            Collections.reverse(revDir);
            ArrayList<String> revScars = (ArrayList<String>) scars.clone();
            Collections.reverse(revScars);
            for (String aRevScar : revScars) {
                if (aRevScar.contains("*")) {
                    aRevScar = aRevScar.replace("*", "");
                } else {
                    aRevScar = aRevScar + "*";
                }
            }

            String lOverhang = aPart.getLeftOverhang();
            String rOverhang = aPart.getRightOverhang();
            String lOverhangR = rOverhang;
            String rOverhangR = lOverhang;
            
            // invert the right overhang
            if (lOverhangR.contains("*")) {
                lOverhangR = lOverhangR.replace("*", "");
            } else {
                lOverhangR = lOverhangR + "*";
            }
            if (rOverhangR.contains("*")) {
                rOverhangR = rOverhangR.replace("*", "");
            } else {
                rOverhangR = rOverhangR + "*";
            }

            String aPartCompDirScarLORO = comp + "|" + dir + "|" + scars + "|" + lOverhang + "|" + rOverhang;
            String raPartCompDirScarLORO = revComp + "|" + revDir + "|" + revScars + "|" + rOverhangR + "|" + lOverhangR;

            startPartsLOcompRO.add(aPartCompDirScarLORO);
            startPartsLOcompRO.add(raPartCompDirScarLORO);
        }

        return startPartsLOcompRO;
    }

    /**
     * Returns a part library and finds all forward and reverse characteristics
     * of each part *
     */
    public static HashSet<String> getExistingVectorKeys(ArrayList<Vector> vectorLib) {

        HashSet<String> startVectorsLOlevelRO = new HashSet<String>();

        //Go through vectors library, put all compositions into hash of things that already exist
        for (Vector aVec : vectorLib) {

            String lOverhang = aVec.getLeftOverhang();
            String rOverhang = aVec.getRightOverhang();
            String lOverhangR = aVec.getRightOverhang();
            String rOverhangR = aVec.getLeftOverhang();
            if (lOverhangR.contains("*")) {
                lOverhangR = lOverhangR.replace("*", "");
            } else {
                lOverhangR = lOverhangR + "*";
            }
            if (rOverhangR.contains("*")) {
                rOverhangR = rOverhangR.replace("*", "");
            } else {
                rOverhangR = rOverhangR + "*";
            }
            int stage = aVec.getLevel();

            String aVecLOlevelRO = aVec.getName() + "|" + lOverhang + "|" + stage + "|" + rOverhang;
            String aVecLOlevelROR = aVec.getName() + "|" + lOverhangR + "|" + stage + "|" + rOverhangR;

            startVectorsLOlevelRO.add(aVecLOlevelRO);
            startVectorsLOlevelRO.add(aVecLOlevelROR);
        }

        return startVectorsLOlevelRO;
    }
}
