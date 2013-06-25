/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;

import Controller.algorithms.PrimerDesign;
import Controller.datastructures.Part;
import Controller.datastructures.RestrictionEnzyme;
import Controller.datastructures.SRSGraph;
import Controller.datastructures.SRSNode;
import Controller.datastructures.SRSVector;
import Controller.datastructures.Vector;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static HashMap<String, SRSGraph> partImportClotho(ArrayList<Part> goalParts, ArrayList<Part> partLibrary, HashSet<String> discouraged, HashSet<String> recommended) throws Exception {

        //Create library to initialize hashMem
        HashMap<String, SRSGraph> library = new HashMap<String, SRSGraph>();

        //Add goal parts to memoization hash, making new nodes with only type and composition from library
        for (Part goalPart : goalParts) {
            try {
                ArrayList<Part> basicParts = ClothoWriter.getComposition(goalPart);
                for (int i = 0; i < basicParts.size(); i++) {

                    //Initialize new graph for a basic part
                    SRSGraph newBasicGraph = new SRSGraph();
                    newBasicGraph.getRootNode().setUUID(basicParts.get(i).getUUID());

                    //Get basic part compositions and search tags relating to feature type, overhangs ignored for this step
                    ArrayList<String> composition = new ArrayList<String>();
                    composition.add(basicParts.get(i).getName());
                    ArrayList<String> sTags = basicParts.get(i).getSearchTags();
                    ArrayList<String> type = new ArrayList<String>();

                    for (int k = 0; k < sTags.size(); k++) {
                        if (sTags.get(k).startsWith("Type:")) {
                            String typeTag = sTags.get(k);
                            ArrayList<String> types = parseTypeTags(typeTag);
                            type.addAll(types);
                        }
                    }

                    //Set type and composition
                    SRSNode root = newBasicGraph.getRootNode();
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
                                    ArrayList<String> types = parseTypeTags(typeTag);
                                    type.addAll(types);
                                }
                            }
                        }
                    }

                    //Initialize new graph for library part
                    SRSGraph libraryPartGraph = new SRSGraph();
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
    public static ArrayList<SRSVector> vectorImportClotho(ArrayList<Vector> vectorLibrary) {

        //Initialize vector library
        ArrayList<SRSVector> library = new ArrayList<SRSVector>();

        //Provided there is an input vector library
        if (vectorLibrary != null) {
            if (vectorLibrary.size() > 0) {
                for (Vector aVector : vectorLibrary) {

                    //Initialize a new vector
                    SRSVector vector = new SRSVector();

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
    public static ArrayList<SRSNode> gpsToNodesClotho(ArrayList<Part> goalParts) throws Exception {
        ArrayList<SRSNode> gpsNodes = new ArrayList<SRSNode>();
        for (int i = 0; i < goalParts.size(); i++) {

            //Get goal part's composition and type (part description type)
            ArrayList<Part> basicParts = ClothoWriter.getComposition(goalParts.get(i));
            ArrayList<String> composition = new ArrayList<String>();
            ArrayList<String> type = new ArrayList<String>();
            for (int j = 0; j < basicParts.size(); j++) {
                composition.add(basicParts.get(j).getName());
                ArrayList<String> sTags = basicParts.get(j).getSearchTags();
                for (int k = 0; k < sTags.size(); k++) {
                    if (sTags.get(k).startsWith("Type:")) {
                        String typeTag = sTags.get(k);
                        ArrayList<String> types = parseTypeTags(typeTag);
                        type.addAll(types);

                    }
                }
            }

            //Create a new node with the specified composition, add it to goal parts, required intermediates and recommended intermediates for algorithm
            SRSNode gp = new SRSNode(false, false, null, composition, type, false);
            gp.setUUID(goalParts.get(i).getUUID());
            gpsNodes.add(gp);
        }
        return gpsNodes;
    }

    /** Parse type search tags from a string into an ArrayList **/
    public static ArrayList<String> parseTypeTags(String typeTag) {
        ArrayList<String> types = new ArrayList<String>();
        int length = typeTag.length();
        if (typeTag.startsWith("Type:")) {
            typeTag = typeTag.substring(6, length);
        }
        if (typeTag.startsWith("[")) {
            typeTag = typeTag.substring(1, (length - 1));
        }
        String[] tokens = typeTag.split(",");
        types.addAll(Arrays.asList(tokens));
        return types;
    }
    
    //THIS NEXT METHOD USES RESTRICTION ENZYMES WHICH ARE OUTSIDE THE CLOTHO DATA MODEL, UNCLEAR WHERE THIS METHOD SHOULD GO
    
    /** Scan a set of parts for restriction sites **/
    public static HashMap<String, ArrayList<Integer>> reSeqScan(ArrayList<Part> parts, ArrayList<RestrictionEnzyme> enzymes) {
        
        HashMap<String, ArrayList<Integer>> detectedResSeqs = new HashMap<String, ArrayList<Integer>>();
        
        //For all parts
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            String seq = part.getSeq();
            
            //Look at each enzyme's cut sites
            for (int j = 0; j < enzymes.size(); j++) {
                RestrictionEnzyme enzyme = enzymes.get(j);
                Set<String> keySet = enzyme.getRecSeq().keySet();
                ArrayList<Integer> sites = new ArrayList<Integer>();
                
                //For all of an enzyme's recognition sites (probably just one)                
                for (String recSeq : keySet) {

                    String revRecSeq = PrimerDesign.reverseComplement(recSeq);
                    ArrayList<String> strandSeqs = new ArrayList<String>();
                    strandSeqs.add(recSeq);
                    strandSeqs.add(revRecSeq);
                    ArrayList<HashMap<Integer, HashSet<String>>> strandRecognitions = new ArrayList<HashMap<Integer, HashSet<String>>>(2);
                    
                    //For each position in the recognition site, find all bases in the search sequence that satisfy the recognition site
                    for (int l = 0; l < strandRecognitions.size(); l++) {
                        HashMap<Integer, HashSet<String>> strand = new HashMap<Integer, HashSet<String>>();
                        String aStrand = strandSeqs.get(i);
                        
                        //For both the forward and reverse strand, this matching must be done
                        for (int k = 0; k < aStrand.length(); k++) {

                            String nucl;
                            if (k < aStrand.length() - 1) {
                                nucl = aStrand.substring(k, k + 1);
                            } else {
                                nucl = aStrand.substring(k);
                            }
                            
                            HashSet<String> nuclSet = new HashSet<String>();

                            if (nucl.equalsIgnoreCase("a") || nucl.equalsIgnoreCase("c") || nucl.equalsIgnoreCase("g") || nucl.equalsIgnoreCase("t") || nucl.equalsIgnoreCase("u")) {
                                nuclSet.add(nucl);
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("w")) {
                                nuclSet.add("a");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("s")) {
                                nuclSet.add("c");
                                nuclSet.add("g");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("m")) {
                                nuclSet.add("a");
                                nuclSet.add("c");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("k")) {
                                nuclSet.add("g");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("r")) {
                                nuclSet.add("a");
                                nuclSet.add("g");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("y")) {
                                nuclSet.add("c");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("w")) {
                                nuclSet.add("a");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("b")) {
                                nuclSet.add("c");
                                nuclSet.add("g");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("d")) {
                                nuclSet.add("a");
                                nuclSet.add("g");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("h")) {
                                nuclSet.add("c");
                                nuclSet.add("a");
                                nuclSet.add("t");
                                strand.put(k, nuclSet);
                            } else if (nucl.equalsIgnoreCase("v")) {
                                nuclSet.add("c");
                                nuclSet.add("g");
                                nuclSet.add("a");
                                strand.put(k, nuclSet);
                            }
                        }
                        strandRecognitions.add(strand);

                    }

                    //Scan the part sequence and look for matches that are equivalent to the length of the recognition sequence
                    for (int start = 0; start < seq.length() - recSeq.length(); start++) {
                        String fragment = seq.substring(start, start + recSeq.length());
                        
                    }

                }
            }
        }
        
        return null;
    }
}