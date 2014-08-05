/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.modasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.PrimerDesign;
import Controller.algorithms.RGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jenhantao
 */
public class RGatewayGibson extends RGeneral {

    /**
     * Clotho part wrapper for sequence dependent one pot reactions *
     */
    public ArrayList<RGraph> gatewayGibsonWrapper(HashSet<Part> gps, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies, HashMap<Integer, Vector> stageVectors, ArrayList<Double> costs, HashMap<String, String> libraryOHs) throws Exception {
        
        _partLibrary = partLibrary;
        _vectorLibrary = vectorLibrary;
        
        //Designate how many parts can be efficiently ligated in one step
        int max = 0;
        Set<Integer> keySet = efficiencies.keySet();
        for (Integer key : keySet) {
            if (key > max) {
                max = key;
            }
        }
        _maxNeighbors = max;

        //Create hashMem parameter for createAsmGraph_sgp() call
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(partLibrary, discouraged, recommended); //key: composiion, direction || value: library graph

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(gps);

        //Add single transcriptional units to the required hash
        ArrayList<ArrayList<String>> reqTUs = getSingleTranscriptionalUnits(gpsNodes);
        for (int i = 0; i < reqTUs.size(); i++) {
            required.add(reqTUs.get(i).toString());
        }

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        
//        //Pull out graphs with one node i.e. either in the library already or require only a PCR
//        ArrayList<RGraph> singlePartGraphs = new ArrayList<RGraph>();
//        for (RGraph optimalGraph : optimalGraphs) {
//            if (optimalGraph.getStages() == 0) {
//                RNode root = optimalGraph.getRootNode();
//                String OHs = libraryOHs.get(root.getUUID());
//                String[] tokens = OHs.split("\\|");
//                if (tokens.length == 2) {
//                    boolean allInts = true;
//                    for (String token : tokens) {
//                        if (!token.matches("[*]?\\d+")) {
//                            allInts = false;
//                        }
//                    }
//                    if (allInts) {
//                        singlePartGraphs.add(optimalGraph);
//                    }
//                }
//            }
//        }
//        
//        optimalGraphs.removeAll(singlePartGraphs);
//        
//        //Assign overhangs based upon input
//        for (RGraph spGraph : singlePartGraphs) {
//            RNode root = spGraph.getRootNode();
//            String OHs = libraryOHs.get(root.getUUID());
//            String[] tokens = OHs.split("\\|");
//            root.setLOverhang(tokens[0]);
//            root.setROverhang(tokens[1]);
//            RVector newVector = new RVector(tokens[0], tokens[1], 0, stageVectors.get(0).getName(), null);
//            root.setVector(newVector);
//        }
        
                
        //Pre-processing to adjust stages for Gateway steps
        for (RGraph optimalGraph : optimalGraphs) {
            if (singleBasicPartInGraph(optimalGraph)) {
                stageAdjuster(optimalGraph, -1);
            }
        }
        
        //Overhang assignment for Gibson
        if (!optimalGraphs.isEmpty()) {
            propagatePrimaryOverhangs(optimalGraphs);
            maximizeOverhangSharing(optimalGraphs);
            HashMap<String, String> forcedOverhangHash = new HashMap<String, String>();
            cartesianLibraryAssignment(optimalGraphs, forcedOverhangHash, stageVectors);
            assignScars(optimalGraphs);            
        }

        //After Gibson overhangs assigned, correct stages, assign overhangs for gateway
        gatewayOverhangs(optimalGraphs, stageVectors);
        for (RGraph optimalGraph : optimalGraphs) {
            if (singleBasicPartInGraph(optimalGraph)) {
                stageAdjuster(optimalGraph, 1);
            }
        }
        
        
//        optimalGraphs.addAll(singlePartGraphs);
        return optimalGraphs;
    }

    /**
     * Determine overhang scars *
     */
    private void assignScars(ArrayList<RGraph> optimalGraphs) {

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            root.setScars(assignScarsHelper(root, children));
        }
    }

    /**
     * Overhang scars helper *
     */
    private ArrayList<String> assignScarsHelper(RNode parent, ArrayList<RNode> children) {

        ArrayList<String> scars = new ArrayList<String>();

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);

            if (i > 0) {
                if (child.getLOverhang().isEmpty()) {
                    scars.add("_");
                }
                scars.add(child.getLOverhang());
            }

            //Make recursive call
            if (child.getStage() > 0) {

                //Remove the current parent from the list
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }

                ArrayList<String> childScars = assignScarsHelper(child, grandChildren);
                scars.addAll(childScars);
            } else {

                ArrayList<String> childScars = new ArrayList<String>();
                if (child.getComposition().size() > 1) {
                    if (!child.getScars().isEmpty()) {
                        childScars.addAll(child.getScars());
                    } else {

                        for (int j = 0; j < child.getComposition().size() - 1; j++) {
                            childScars.add("_");
                        }
                        child.setScars(childScars);
                    }
                }
                scars.addAll(childScars);
            }
        }

        //Keep scars for re-used parts with scars
        if (!scars.isEmpty()) {
            parent.setScars(scars);
            return scars;
        } else {
            return parent.getScars();
        }        
    }
    
    //Gateway overhang assignment
    private void gatewayOverhangs (ArrayList<RGraph> optimalGraphs, HashMap<Integer, Vector> stageVectors) {
        
        //Loop through each optimal graph and assign gateway overhangs for the level 0 parts
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            gatewayOverhangsHelper(root, children, stageVectors);
        }
    }
    
    //Gateway overhang assignment
    private void gatewayOverhangsHelper (RNode parent, ArrayList<RNode> children, HashMap<Integer, Vector> stageVectors) {
        
        //If this is a Gateway parent
        if (parent.getStage() == 0) {
            
            //Assign overhangs to the gateway clones
            children.get(0).setLOverhang("attL4");
            children.get(0).setROverhang("attR1");
            children.get(1).setLOverhang("attL1");
            children.get(1).setROverhang("attL2");
            
            //Convert vectors to RVectors
            HashMap<Integer, RVector> stageRVectors = new HashMap<Integer, RVector>();
            for (Integer stage : stageVectors.keySet()) {
                RVector vec = ClothoReader.vectorImportClotho(stageVectors.get(stage));
                stageRVectors.put(stage, vec);
            }
            
            //Assign entry vectors to the gateway clones
            RVector levelVector = stageRVectors.get(parent.getStage() % stageRVectors.size() + 1);
            RVector newLVector = new RVector(children.get(0).getLOverhang(), children.get(0).getROverhang(), 1, levelVector.getName(), null);
            RVector newRVector = new RVector(children.get(1).getLOverhang(), children.get(1).getROverhang(), 1, levelVector.getName(), null);
            children.get(0).setVector(newLVector);
            children.get(1).setVector(newRVector);
        
        } else {
            for (int i = 0; i < children.size(); i++) {
                RNode child = children.get(i);
                ArrayList<RNode> grandchildren = new ArrayList<RNode>();
                
                for (int j = 0; j < child.getNeighbors().size(); j++) {
                    if (child.getNeighbors().get(j).getStage() < child.getStage()) {
                        grandchildren.add(child.getNeighbors().get(j));
                    }
                }
                
                //Recursive call if there are grandchildren
                if (!grandchildren.isEmpty()) {
                    gatewayOverhangsHelper(child, grandchildren, stageVectors);
                }
            }
        }
    }
    
    //Overhang validation
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        boolean toReturn = true;
        for (RGraph graph : graphs) {
            RNode root = graph.getRootNode();
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);
                if (parent.getLOverhang().equals(parent.getROverhang())) {
                    System.out.println(parent.getComposition() + " has the same left overhang as it's right overhang");
                    toReturn = false;
                }
                if (parent.getNeighbors().size() > 1) {
                    RNode previous = null;
                    HashMap<String, Integer> leftFrequencyHash = new HashMap();
                    HashMap<String, Integer> rightFrequencyHash = new HashMap();
                    for (int i = 0; i < parent.getNeighbors().size(); i++) {
                        RNode child = parent.getNeighbors().get(i);
                        if (!seenNodes.contains(child)) {
                            if (leftFrequencyHash.get(child.getLOverhang()) != null) {
                                leftFrequencyHash.put(child.getLOverhang(), leftFrequencyHash.get(child.getLOverhang()) + 1);
                            } else {
                                leftFrequencyHash.put(child.getLOverhang(), 1);
                            }
                            if (rightFrequencyHash.get(child.getROverhang()) != null) {
                                rightFrequencyHash.put(child.getROverhang(), rightFrequencyHash.get(child.getROverhang()) + 1);
                            } else {
                                rightFrequencyHash.put(child.getROverhang(), 1);
                            }
                            if (i == 0) {
                                if (!child.getLOverhang().equals(parent.getLOverhang())) {
                                    System.out.println(child.getComposition() + ", which is the 1st part, doesnt have the same left overhang as its parent");
                                    toReturn = false;
                                }
                            }
                            if (i == parent.getNeighbors().size() - 1) {
                                if (!child.getROverhang().equals(parent.getROverhang())) {
                                    System.out.println(child.getComposition() + ", which is the last part, doesnt have the same right overhang as its parent");

                                    toReturn = false;
                                }
                            }
                            if (previous != null) {
                                if (!child.getLOverhang().equals(previous.getROverhang())) {
                                    System.out.println(child.getComposition() + " has a left overhang that doesn't match the right overhang of its neighbor");
                                    toReturn = false;
                                }
                            }

                            previous = child;
                            queue.add(child);
                        }
                    }
                    if (leftFrequencyHash.containsValue(2) || rightFrequencyHash.containsValue(2)) {
                        System.out.println("in " + parent.getComposition() + ", an overhang is used twice for the left overhang or twice for the right overhang\n");
                        System.out.println("leftFrequencyHash: " + leftFrequencyHash);
                        System.out.println("rightFrequencyHash: " + rightFrequencyHash);

                        toReturn = false;
                    }
                }
            }
        }
        return toReturn;
    }
    
    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generatePartPrimers(RNode node, Collector coll, Double meltingTemp, Integer targetLength, Integer minPCRLength, Integer maxPrimerLength) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getModularOHseqs();
        String[] oligos = new String[2];
        String partPrimerPrefix = "at";
        String partPrimerSuffix = "gt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";

        Part currentPart = coll.getPart(node.getUUID(), true);
        String seq = currentPart.getSeq();

        String fwdHomology;
        String revHomology;

        String forwardOligoSequence;
        String reverseOligoSequence;
        if (seq.length() > minPCRLength) {
            fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, seq, true)));
            revHomology = seq.substring(Math.max(0, seq.length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, PrimerDesign.reverseComplement(seq), true)));
            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()).toUpperCase() + fwdHomology;
            reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + overhangVariableSequenceHash.get(node.getROverhang()).toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix);
        
        } else {
            if (seq.equals("")) {
                fwdHomology = "[ PART " + currentPart.getName() + " FORWARD HOMOLOGY REGION ]";
                revHomology = "[ PART " + currentPart.getName() + " REVERSE HOMOLOGY REGION ]";
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()).toUpperCase() + fwdHomology;
                reverseOligoSequence = PrimerDesign.reverseComplement(overhangVariableSequenceHash.get(node.getROverhang()).toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix) + revHomology;
            } else {
                fwdHomology = seq;
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()).toUpperCase() + fwdHomology + overhangVariableSequenceHash.get(node.getROverhang()).toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix;
                reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);

            }
        }
        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }

    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generateVectorPrimers(RVector vector) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getModularOHseqs();
        String vectorPrimerPrefix = "actagtg";
        String vectorPrimerSuffix = "tactagt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";
        String fwdEnzymeRecSite2 = "ggtctc";
        String revEnzymeRecSite2 = "gagacc";

        String[] oligos = new String[2];

        //Level 0, 2, 4, 6, etc. vectors
        String forwardOligoSequence;
        String reverseOligoSequence;
        if (vector.getLevel() % 2 == 0) {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite2 + "a" + overhangVariableSequenceHash.get(vector.getLOverhang()).toUpperCase() + "at" + revEnzymeRecSite1 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(vector.getROverhang()).toUpperCase() + "a" + revEnzymeRecSite2 + vectorPrimerSuffix);

            //Level 1, 3, 5, 7, etc. vectors
        } else {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite1 + "at" + overhangVariableSequenceHash.get(vector.getLOverhang()).toUpperCase() + "a" + revEnzymeRecSite2 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite2 + "t" + overhangVariableSequenceHash.get(vector.getROverhang()).toUpperCase() + "at" + revEnzymeRecSite1 + vectorPrimerSuffix);
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }
}
