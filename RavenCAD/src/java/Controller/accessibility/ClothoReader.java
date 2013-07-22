/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;

import Controller.datastructures.Part;
import Controller.datastructures.RestrictionEnzyme;
import Controller.datastructures.RGraph;
import Controller.datastructures.RNode;
import Controller.datastructures.RVector;
import Controller.datastructures.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.*;

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
    public static HashMap<String, RGraph> partImportClotho(ArrayList<Part> goalParts, ArrayList<Part> partLibrary, HashSet<String> discouraged, HashSet<String> recommended) throws Exception {

        //Create library to initialize hashMem
        HashMap<String, RGraph> library = new HashMap<String, RGraph>();

        //Add goal parts to memoization hash, making new nodes with only type and composition from library
        for (Part goalPart : goalParts) {
            try {
                ArrayList<Part> basicParts = ClothoWriter.getComposition(goalPart);
                for (int i = 0; i < basicParts.size(); i++) {

                    //Initialize new graph for a basic part
                    RGraph newBasicGraph = new RGraph();
                    newBasicGraph.getRootNode().setUUID(basicParts.get(i).getUUID());

                    //Get basic part compositions and search tags relating to feature type, overhangs ignored for this step
                    ArrayList<String> composition = new ArrayList<String>();
                    composition.add(basicParts.get(i).getName());
                    ArrayList<String> sTags = basicParts.get(i).getSearchTags();
                    ArrayList<String> type = new ArrayList<String>();

                    for (int k = 0; k < sTags.size(); k++) {
                        if (sTags.get(k).startsWith("Type:")) {
                            String typeTag = sTags.get(k);
                            ArrayList<String> types = parseTags(typeTag);
                            type.addAll(types);
                        }
                    }

                    //Set type and composition
                    RNode root = newBasicGraph.getRootNode();
                    root.setName(basicParts.get(i).getName());
                    root.setComposition(composition);
                    root.setType(type);
                    library.put(root.getComposition().toString(), newBasicGraph);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //If there is an input Clotho part library, make a new node with only type and composition from library
        if (partLibrary != null) {
            if (partLibrary.size() > 0) {
                for (Part libraryPart : partLibrary) {

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

                    for (Part libPartComponent : libPartComposition) {
                        ArrayList<String> sTags = libPartComponent.getSearchTags();
                        composition.add(libPartComponent.getName());

                        //If the part has search tags
                        if (libPartComponent.getSearchTags() != null) {
                            for (int j = 0; j < sTags.size(); j++) {
                                if (sTags.get(j).startsWith("Type:")) {
                                    String typeTag = sTags.get(j);
                                    System.out.println("Search Tag: " + typeTag);
                                    ArrayList<String> types = parseTags(typeTag);
                                    type.addAll(types);
                                }
                            }
                        }
                    }

                    //Initialize new graph for library part
                    RGraph libraryPartGraph = new RGraph();
                    libraryPartGraph.getRootNode().setUUID(libraryPart.getUUID());
                    libraryPartGraph.getRootNode().setComposition(composition);
                    libraryPartGraph.getRootNode().setType(type);
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
                    library.put(libraryPartGraph.getRootNode().getComposition().toString(), libraryPartGraph);
                }
            }
        }
        return library;
    }

    /** Given a vector library, create vectorHash **/
    public static ArrayList<RVector> vectorImportClotho(ArrayList<Vector> vectorLibrary) {

        //Initialize vector library
        ArrayList<RVector> library = new ArrayList<RVector>();

        //Provided there is an input vector library
        if (vectorLibrary != null) {
            if (vectorLibrary.size() > 0) {
                for (Vector aVector : vectorLibrary) {

                    //Initialize a new vector
                    RVector vector = new RVector();

                    //If there's search tags, find overhangs
                    if (aVector.getSearchTags() != null) {
                        ArrayList<String> sTags = aVector.getSearchTags();
                        String LO = new String();
                        String RO = new String();
                        String resistance = new String();
                        int level = -1;
                        for (int i = 0; i < sTags.size(); i++) {
                            if (sTags.get(i).startsWith("LO:")) {
                                LO = sTags.get(i).substring(4);
                            } else if (sTags.get(i).startsWith("RO:")) {
                                RO = sTags.get(i).substring(4);
                            } else if (sTags.get(i).startsWith("Level:")) {
                                String aLevel = sTags.get(i).substring(7);
                                level = Integer.parseInt(aLevel);
                            } else if (sTags.get(i).startsWith("Resistance:")) {
                                resistance = sTags.get(i).substring(12);
                            }
                        }
                        vector.setLOverhang(LO);
                        vector.setROverhang(RO);
                        vector.setStringResistance(resistance);
                        vector.setLevel(level);
                    }

                    vector.setName(aVector.getName());
                    vector.setUUID(aVector.getUUID());

                    library.add(vector);
                }
            }
        }
        return library;
    }

    /** Convert goal parts into SRS nodes for the algorithm **/
    public static ArrayList<RNode> gpsToNodesClotho(ArrayList<Part> goalParts) throws Exception {
        ArrayList<RNode> gpsNodes = new ArrayList<RNode>();
        for (int i = 0; i < goalParts.size(); i++) {

            //Get goal part's composition and type (part description type)
            ArrayList<Part> basicParts = ClothoWriter.getComposition(goalParts.get(i));
            ArrayList<String> composition = new ArrayList<String>();
            ArrayList<String> type = new ArrayList<String>();
            ArrayList<String> direction = new ArrayList<String>();
            for (int j = 0; j < basicParts.size(); j++) {
                composition.add(basicParts.get(j).getName());
                ArrayList<String> sTags = basicParts.get(j).getSearchTags();
                for (int k = 0; k < sTags.size(); k++) {
                    String tag = sTags.get(k);
                    if (tag.startsWith("Type:")) {
                        ArrayList<String> list = parseTags(tag);
                        type.addAll(list);
                    } else if (tag.startsWith("Direction:")) {
                        ArrayList<String> list = parseTags(tag);
                        direction.addAll(list);
                    }
                }
            }

            //Create a new node with the specified composition, add it to goal parts, required intermediates and recommended intermediates for algorithm
            RNode gp = new RNode(false, false, null, composition, direction, type, 0, 0);
            gp.setUUID(goalParts.get(i).getUUID());
            gpsNodes.add(gp);
        }
        return gpsNodes;
    }

    /** Parse type search tags from a string into an ArrayList **/
    public static ArrayList<String> parseTags(String tag) {
        ArrayList<String> list = new ArrayList<String>();
        System.out.println("Tag length: " + tag.length());
        int tl = tag.length();
        tag.replaceAll("\\[", "");
        tag.replaceAll("\\]", "");
        int indexOf = tag.indexOf(":");
        tag = tag.substring(indexOf + 2,tl);
        System.out.println("tag: " + tag);

        String[] tokens = tag.split(",");
        list.addAll(Arrays.asList(tokens));
        return list;
    }
    
    //THIS NEXT METHOD USES RESTRICTION ENZYMES WHICH ARE OUTSIDE THE CLOTHO DATA MODEL, UNCLEAR WHERE THIS METHOD SHOULD GO
    
    /** Scan a set of parts for restriction sites **/
    //HashMap<Part, HashMap<Restriction Enzyme name, ArrayList<ArrayList<Start site, End site>>>>
    public static HashMap<Part, HashMap<String, ArrayList<ArrayList<Integer>>>> reSeqScan(ArrayList<Part> parts, ArrayList<RestrictionEnzyme> enzymes) {
        
        HashMap<Part, HashMap<String, ArrayList<ArrayList<Integer>>>> partEnzResSeqs = new HashMap<Part, HashMap<String, ArrayList<ArrayList<Integer>>>>();
        
        //For all parts
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            String name = part.getName();
            String seq = part.getSeq();
            HashMap<String, ArrayList<ArrayList<Integer>>> detectedResSeqs = new HashMap<String, ArrayList<ArrayList<Integer>>>();
            
            //Look at each enzyme's cut sites
            for (int j = 0; j < enzymes.size(); j++) {
                ArrayList<ArrayList<Integer>> matchSites = new ArrayList<ArrayList<Integer>>();
                RestrictionEnzyme enzyme = enzymes.get(j);
                String enzName = enzyme.getName();
                String fwdRec = enzyme.getFwdRecSeq();
                String revRec = enzyme.getRevRecSeq();
                
                //Compile regular expressions
                Pattern compileFwdRec = Pattern.compile(fwdRec, Pattern.CASE_INSENSITIVE);
                Pattern compileRevRec = Pattern.compile(revRec, Pattern.CASE_INSENSITIVE);
                Matcher matcherFwdRec = compileFwdRec.matcher(seq);
                Matcher matcherRevRec = compileRevRec.matcher(seq);
                
                //Find matches of forward sequence
                while (matcherFwdRec.find()) {
                    ArrayList<Integer> matchIndexes = new ArrayList<Integer>(2);
                    int start = matcherFwdRec.start();
                    int end = matcherFwdRec.end();
                    matchIndexes.add(start);
                    matchIndexes.add(end);
                    matchSites.add(matchIndexes);
                }
                
                //Find matches of reverse sequence
                while (matcherRevRec.find()) {
                    ArrayList<Integer> matchIndexes = new ArrayList<Integer>(2);
                    int start = matcherRevRec.start();
                    int end = matcherRevRec.end();
                    matchIndexes.add(start);
                    matchIndexes.add(end);
                    matchSites.add(matchIndexes);
                }
                
                detectedResSeqs.put(enzName, matchSites);
            }
            partEnzResSeqs.put(part, detectedResSeqs);
        }
        
        return partEnzResSeqs;
    }
}