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

        //Add all basic parts in the goal parts to the memoization hash
        for (Part goalPart : goalParts) {
            try {
                
                //Add all basic parts to the memoization hash
                ArrayList<Part> basicParts = ClothoWriter.getComposition(goalPart);
                for (int i = 0; i < basicParts.size(); i++) {

                    //Initialize new graph for a basic part
                    RGraph newBasicGraph = new RGraph();
                    newBasicGraph.getRootNode().setUUID(basicParts.get(i).getUUID());

                    //Get basic part compositions and search tags relating to feature type, overhangs ignored for this step
                    ArrayList<String> composition = new ArrayList<String>();
                    ArrayList<String> direction = new ArrayList<String>();
                    composition.add(basicParts.get(i).getName());
                    ArrayList<String> sTags = basicParts.get(i).getSearchTags();
                    ArrayList<String> type = new ArrayList<String>();

                    for (int k = 0; k < sTags.size(); k++) {
                        String tag = sTags.get(k);
                        if (tag.startsWith("Type:")) {
                            ArrayList<String> list = parseTags(tag);
                            type.addAll(list);
                        }
                    }

                    //Set type and composition
                    RNode root = newBasicGraph.getRootNode();
                    root.setName(basicParts.get(i).getName());
                    root.setComposition(composition);
                    root.setDirection(direction);
                    root.setType(type);
                    
                    System.out.println("composition.toString() + direction.toString(): " + composition.toString() + direction.toString());
                    library.put(composition.toString() + direction.toString(), newBasicGraph);
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
                    ArrayList<String> direction = new ArrayList<String>();
                    ArrayList<String> composition = new ArrayList<String>();
                    ArrayList<String> tags = libraryPart.getSearchTags();
                  
                    //Get direction
                    for (String tag : tags) {
                        if (tag.startsWith("Direction:")) {
                            ArrayList<String> list = parseTags(tag);
                            direction.addAll(list);
                        }
                    }

                    //Get basic part types
                    for (Part libPartComponent : libPartComposition) {
                        ArrayList<String> sTags = libPartComponent.getSearchTags();
                        composition.add(libPartComponent.getName());
                        
                        //If there was no direction found, all basic parts assumed to be forward
                        if (libraryPart.isComposite()) {
                            if (direction.isEmpty()) {
                                direction.add("+");
                            }
                        }             

                        if (libPartComponent.getSearchTags() != null) {
                            for (int j = 0; j < sTags.size(); j++) {
                                String tag = sTags.get(j);
                                if (tag.startsWith("Type:")) {
                                    ArrayList<String> list = parseTags(tag);
                                    type.addAll(list);
                                } 
                            }                            
                        }
                    }

//                    System.out.println("direction library: " + direction);
                    
                    //Initialize new graph for library part
                    RGraph libraryPartGraph = new RGraph();
                    libraryPartGraph.getRootNode().setUUID(libraryPart.getUUID());
                    libraryPartGraph.getRootNode().setComposition(composition);
                    libraryPartGraph.getRootNode().setType(type);
                    libraryPartGraph.getRootNode().setDirection(direction);
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
                    System.out.println("composition.toString() + direction.toString(): " + composition.toString() + direction.toString());
                    library.put(composition.toString() + direction.toString(), libraryPartGraph);
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
            Part goalPart = goalParts.get(i);
            ArrayList<Part> basicParts = ClothoWriter.getComposition(goalPart);
            ArrayList<String> composition = new ArrayList<String>();
            ArrayList<String> type = new ArrayList<String>();
            ArrayList<String> direction = new ArrayList<String>();
            ArrayList<String> searchTags = goalPart.getSearchTags();
            
            //Get direction
            for (String tag : searchTags) {
                if (tag.startsWith("Direction:")) {
                    ArrayList<String> list = parseTags(tag);
                    direction.addAll(list);
                }
            }

            //Get basic part types
            for (int j = 0; j < basicParts.size(); j++) {
                composition.add(basicParts.get(j).getName());
                ArrayList<String> sTags = basicParts.get(j).getSearchTags();
                
                //If there was no direction found, all basic parts assumed to be forward
                if (direction.isEmpty()) {
                    direction.add("+");
                }

                for (int k = 0; k < sTags.size(); k++) {
                    String tag = sTags.get(k);
                    if (tag.startsWith("Type:")) {
                        ArrayList<String> list = parseTags(tag);
                        type.addAll(list);
                    } 
                }
            }

//            System.out.println("direction gpsToNodes: " + direction);
            
            //Create a new node with the specified composition, add it to goal parts, required intermediates and recommended intermediates for algorithm
            RNode gp = new RNode(false, false, null, composition, direction, type, 0, 0);
            gp.setUUID(goalParts.get(i).getUUID());
            gpsNodes.add(gp);
        }
        return gpsNodes;
    }

    /** Parse Clotho search tags from a string into an ArrayList **/
    public static ArrayList<String> parseTags(String tag) {
        ArrayList<String> list = new ArrayList<String>();
        
        //Split any arraylist-like search tag
        if (tag.charAt(tag.length()-1) == ']') {
            tag = tag.substring(0,tag.length()-1);
            String[] tokens1 = tag.split("\\[");
            String splitTag = tokens1[1];
            String[] tokens = splitTag.split(",");
            ArrayList<String> trimmedTokens = new ArrayList<String>();
            
            //Trim tokens to add to final list
            for (String token : tokens) {
                String trimmedToken = token.trim();
                trimmedTokens.add(trimmedToken);
            }
            list.addAll(trimmedTokens);
        } else {
            String[] tokens1 = tag.split(":");
            String splitTag = tokens1[1];
            splitTag = splitTag.trim();
            list.add(splitTag);
        }
        
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