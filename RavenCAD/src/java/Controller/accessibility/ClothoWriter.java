/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;

import static Controller.accessibility.ClothoReader.parseTags;
import java.util.ArrayList;
import java.util.HashSet;
import Controller.datastructures.*;
import java.util.HashMap;

/**
 * Provides utility methods for exporting Clotho composite parts
 *
 * @author Tao
 */
public class ClothoWriter {

    /**
     * Clotho writer constructor *
     */
    public ClothoWriter() {
        _allCompositeParts = new ArrayList<Part>();
        _allBasicParts = new ArrayList<Part>();
    }

    /**
     * Generate Clotho parts with uuids from intermediates without uuids *
     */
    public void nodesToClothoPartsVectors(Collector coll, RGraph graph, HashMap<Part, Vector> partVectorHash) throws Exception {
        String nameRoot = coll.getPart(graph.getRootNode().getUUID(), true).getName();

        ArrayList<RNode> basicNodes = new ArrayList<RNode>();
        ArrayList<RNode> stepNodes = new ArrayList<RNode>();
        ArrayList<ArrayList<RNode>> nodeOrder = new ArrayList<ArrayList<RNode>>();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        HashSet<RNode> seenNodes = new HashSet<RNode>();
        queue.add(graph.getRootNode());

        //Find all the level 0 nodes and step nodes... the new level 0 nodes need to be made first
        while (!queue.isEmpty()) {
            RNode currentNode = queue.get(0);
            seenNodes.add(currentNode);
            queue.remove(0);

            if (currentNode.getStage() == 0) {
                basicNodes.add(currentNode);
            } else {
                stepNodes.add(currentNode);
            }
            
            for (RNode neighbor : currentNode.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    if (!queue.contains(neighbor)) {
                        queue.add(neighbor);
                    }                    
                }
            }
        }
        nodeOrder.add(basicNodes);
        nodeOrder.add(stepNodes);

        for (ArrayList<RNode> nodes : nodeOrder) {
            for (RNode currentNode : nodes) {

                ArrayList<String> scars = currentNode.getScars();
                ArrayList<String> direction = currentNode.getDirection();
                ArrayList<String> composition = currentNode.getComposition();
                ArrayList<String> nodeType = currentNode.getType();
                String LO = currentNode.getLOverhang();
                String RO = currentNode.getROverhang();

                //If the node has no uuid, make a new part
                //This is pretty much only the case for composite parts
                if (currentNode.getUUID() == null) {

                    //Get new intermediate name
                    String partName = nameRoot + "_intermediate" + Math.random() * 999999999;
                    partName = partName.replaceAll("\\.", "");
                    if (partName.length() > 255) {
                        partName = partName.substring(0, 255);
                    }

                    //If there's overhangs, add search tags
                    Part newPart = generateNewClothoCompositePart(coll, partName, "", composition, direction, scars, LO, RO);
                    newPart.addSearchTag("Type: composite");
                    currentNode.setName(partName);
                    newPart = newPart.saveDefault(coll);
                    currentNode.setUUID(newPart.getUUID());

                } else {

                    //If a part with this composition and overhangs does not exist, a new part is needed
                    Part currentPart = coll.getPart(currentNode.getUUID(), true);
                    ArrayList<String> sTags = currentPart.getSearchTags();
                    ArrayList<String> stringComposition = currentPart.getStringComposition();
                    ArrayList<String> currentPartDir = parseTags(sTags, "Direction:");
                    ArrayList<String> currentPartScars = parseTags(sTags, "Scars:");
                    String currentPartLO = "";
                    String currentPartRO = "";
                    for (int k = 0; k < sTags.size(); k++) {
                        if (sTags.get(k).startsWith("LO:")) {
                            currentPartLO = sTags.get(k).substring(4);
                        } else if (sTags.get(k).startsWith("RO:")) {
                            currentPartRO = sTags.get(k).substring(4);
                        }
                    }
                    String currentPartKey = stringComposition + "|" + currentPartDir + "|" + currentPartScars + "|" + currentPartLO + "|" + currentPartRO;
                    String nodeKey = currentNode.getNodeKey("+");

                    //A new part must be created if one with the same composition and overhangs does not exist
                    if (!currentPartKey.equals(nodeKey)) {

                        //If a new part must be created
                        Part newPart;
                        if (currentPart.isBasic()) {
                            newPart = Part.generateBasic(currentPart.getName(), currentPart.getSeq());
                        } else {

                            //If a new composite part needs to be made
                            ArrayList<Part> newComposition = new ArrayList<Part>();

                            for (int i = 0; i < composition.size(); i++) {
                                ArrayList<String> cSearchTags = new ArrayList<String>();
                                String cSeq = currentPart.getComposition().get(i).getSeq();
                                String cName = composition.get(i);
                                String cDir = direction.get(i);
                                String cType = nodeType.get(i);
                                String cLO;
                                String cRO;

                                //Get internal overhangs from scars if there are any
                                if (!scars.isEmpty()) {
                                    if (i == 0) {
                                        cLO = LO;
                                        cRO = scars.get(i);
                                    } else if (i == composition.size() - 1) {
                                        cLO = scars.get(composition.size() - 2);
                                        cRO = RO;
                                    } else {
                                        cLO = scars.get(i - 1);
                                        cRO = scars.get(i);
                                    }
                                } else {
                                    if (i == 0) {
                                        cLO = LO;
                                        cRO = composition.get(1);
                                    } else if (i == composition.size() - 1) {
                                        cLO = composition.get(composition.size() - 2);
                                        cRO = RO;
                                    } else {
                                        cLO = composition.get(i - 1);
                                        cRO = composition.get(i + 1);
                                    }
                                }

                                //BioBricks scars
                                if (cLO.equals("BB") || cRO.equals("BB")) {
                                    cLO = "EX";
                                    cRO = "SP";
                                }

                                cSearchTags.add("RO: " + cRO);
                                cSearchTags.add("LO: " + cLO);
                                cSearchTags.add("Type: " + cType);
                                cSearchTags.add("Direction: [" + cDir + "]");
                                Part exactPart = coll.getExactPart(cName, cSeq, cSearchTags, true);
                                if (exactPart == null) {
                                    //find the inverted version
                                    String invertedcRO = cRO;
                                    if (invertedcRO.contains("*")) {
                                        invertedcRO = invertedcRO.substring(0, invertedcRO.length() - 1);
                                    } else {
                                        invertedcRO = invertedcRO + "*";
                                    }
                                    String invertedcLO = cLO;
                                    if (invertedcLO.contains("*")) {
                                        invertedcLO = invertedcLO.substring(0, invertedcLO.length() - 1);
                                    } else {
                                        invertedcLO = invertedcLO + "*";
                                    }
                                    String invertedcDir = cDir;
                                    if (cDir.equals("+")) {
                                        invertedcDir = "-";
                                    } else {
                                        invertedcDir = "+";
                                    }
                                    cSearchTags.clear();
                                    cSearchTags.add("LO: " + invertedcRO);
                                    cSearchTags.add("RO: " + invertedcLO);
                                    cSearchTags.add("Type: " + cType);
                                    cSearchTags.add("Direction: [" + invertedcDir + "]");
                                    exactPart = coll.getExactPart(cName, cSeq, cSearchTags, true);
                                }

                                newComposition.add(exactPart);
                            }

                            newPart = Part.generateComposite(newComposition, currentPart.getName());
                        }

                        newPart.addSearchTag("LO: " + currentNode.getLOverhang());
                        newPart.addSearchTag("RO: " + currentNode.getROverhang());
                        newPart.addSearchTag("Direction: " + currentNode.getDirection().toString());

                        String type = currentNode.getType().toString();
                        type = type.substring(1, type.length() - 1);

                        if (currentNode.getComposition().size() > 1) {
                            type = "composite";
                        }

                        if (!currentNode.getScars().isEmpty()) {
                            newPart.addSearchTag("Scars: " + currentNode.getScars().toString());
                        }

                        newPart.addSearchTag("Type: " + type);
                        newPart = newPart.saveDefault(coll);
                        currentNode.setUUID(newPart.getUUID());
                    }
                }

                //Get the vector and save a new vector if it does not have a uuid
                RVector vector = currentNode.getVector();
                if (vector != null) {

                    //Get vector parameters
                    String resistance = vector.getResistance();
                    int level = vector.getLevel();
                    String seq = "";
                    if (vector.getName().startsWith("DVL")) {
                        if (level % 3 == 0) {
                            seq = _DVL0Seq;
                        } else if (level % 3 == 1) {
                            seq = _DVL1Seq;
                        } else {
                            seq = _DVL2Seq;
                        }
                    } else if (vector.getName().equals("pSK1A2")) {
                        seq = _pSK1A2;
                    }

                    Part currentPart = coll.getPart(currentNode.getUUID(), true);
                    Vector assignedVec = partVectorHash.get(currentPart);

                    //If there is an assigned vector in the library with the appropriate resistance, do not bother making a new vector
                    if (assignedVec != null) {
                        if (assignedVec.getResistance().equals(resistance) && assignedVec.getLevel() == level) {
                            vector.setName(assignedVec.getName());
                            vector.setUUID(assignedVec.getUUID());
                            vector.setLevel(assignedVec.getLevel());

                            //Otherwise make a new vector
                        } else {
                            Vector newVector = generateNewClothoVector(coll, vector.getName(), seq, LO, RO, resistance, level);
                            newVector = newVector.saveDefault(coll);
                            vector.setName(newVector.getName());
                            vector.setUUID(newVector.getUUID());
                        }

                        //Otherwise make a new vector
                    } else {
                        Vector newVector = generateNewClothoVector(coll, vector.getName(), seq, LO, RO, resistance, level);
                        newVector = newVector.saveDefault(coll);
                        vector.setName(newVector.getName());
                        vector.setUUID(newVector.getUUID());
                    }
                }
                seenNodes.add(currentNode);
            }
        }
    }

    /**
     * Make intermediate parts of graph with no uuid into Clotho parts
     * (typically only done for solution graphs) *
     */
    private Part generateNewClothoCompositePart(Collector coll, String name, String description, ArrayList<String> composition, ArrayList<String> direction, ArrayList<String> scars, String LO, String RO) throws Exception {
        if (_allCompositeParts.isEmpty() || _allBasicParts.isEmpty()) {
            refreshPartVectorList(coll);
        }

        //Search to see there's nothing made from the same components before saving
        for (Part existingPart : _allCompositeParts) {
            ArrayList<String> existingPartComp = new ArrayList<String>();

            //Get an existing part's overhangs and direction
            ArrayList<String> sTags = existingPart.getSearchTags();
            String existingPartLO = "";
            String existingPartRO = "";
            ArrayList<String> existingPartDir = parseTags(sTags, "Direction:");

            for (int k = 0; k < sTags.size(); k++) {
                if (sTags.get(k).startsWith("LO:")) {
                    existingPartLO = sTags.get(k).substring(4);
                } else if (sTags.get(k).startsWith("RO:")) {
                    existingPartRO = sTags.get(k).substring(4);
                }
            }

            //Obtain the basic part names
            ArrayList<Part> existingPartComposition = getComposition(existingPart);
            for (Part basicPart : existingPartComposition) {
                existingPartComp.add(basicPart.getName());
            }

            //If the composition and overhangs of the new part is the same as an existing composite part, return that part
            if (composition.equals(existingPartComp) && direction.equals(existingPartDir)) {
                if (existingPartLO.equals(LO) && existingPartRO.equals(RO)) {
                    return existingPart;
                }
            }
        }

        //If a new composite part needs to be made
        ArrayList<Part> newComposition = new ArrayList<Part>();
        for (int i = 0; i < composition.size(); i++) {
            ArrayList<String> cSearchTags = new ArrayList<String>();
            String cName = composition.get(i);
            ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(cName, true);
            String cSeq = allPartsWithName.get(0).getSeq();
            String cDir = direction.get(i);
            String cType = allPartsWithName.get(0).getType();
            String cLO;
            String cRO;

            //Get internal overhangs from scars if there are any
            if (!scars.isEmpty()) {
                if (i == 0) {
                    cLO = LO;
                    cRO = scars.get(i);
                } else if (i == composition.size() - 1) {
                    cLO = scars.get(composition.size() - 2);
                    cRO = RO;
                } else {
                    cLO = scars.get(i - 1);
                    cRO = scars.get(i);
                }
            } else {
                if (i == 0) {
                    cLO = LO;
                    cRO = composition.get(1);
                } else if (i == composition.size() - 1) {
                    cLO = composition.get(composition.size() - 2);
                    cRO = RO;
                } else {
                    cLO = composition.get(i - 1);
                    cRO = composition.get(i + 1);
                }
            }

            //BioBricks scars
            if (cLO.equals("BB") || cRO.equals("BB")) {
                cLO = "EX";
                cRO = "SP";
            }

            cSearchTags.add("RO: " + cRO);
            cSearchTags.add("LO: " + cLO);
            cSearchTags.add("Type: " + cType);
            cSearchTags.add("Direction: [" + cDir + "]");

            Part exactPart = coll.getExactPart(cName, cSeq, cSearchTags, true);
            if (exactPart == null) {
                //find the inverted version
                String invertedcRO = cRO;
                if (invertedcRO.contains("*")) {
                    invertedcRO = invertedcRO.substring(0, invertedcRO.length() - 1);
                } else {
                    invertedcRO = invertedcRO + "*";
                }
                String invertedcLO = cLO;
                if (invertedcLO.contains("*")) {
                    invertedcLO = invertedcLO.substring(0, invertedcLO.length() - 1);
                } else {
                    invertedcLO = invertedcLO + "*";
                }
                String invertedcDir = cDir;
                if (cDir.equals("+")) {
                    invertedcDir = "-";
                } else {
                    invertedcDir = "+";
                }
                cSearchTags.clear();
                cSearchTags.add("LO: " + invertedcRO);
                cSearchTags.add("RO: " + invertedcLO);
                cSearchTags.add("Type: " + cType);
                cSearchTags.add("Direction: [" + invertedcDir + "]");
                exactPart = coll.getExactPart(cName, cSeq, cSearchTags, true);
            }
            newComposition.add(exactPart);
        }

        Part newPart = Part.generateComposite(newComposition, name);
        if (!LO.isEmpty()) {
            newPart.addSearchTag("LO: " + LO);
        }
        if (!RO.isEmpty()) {
            newPart.addSearchTag("RO: " + RO);
        }
        if (!direction.isEmpty()) {
            newPart.addSearchTag("Direction: " + direction);
        }
        if (!scars.isEmpty()) {
            newPart.addSearchTag("Scars: " + scars);
        }
        newPart = newPart.saveDefault(coll);
        return newPart;
    }

    /**
     * Make intermediate parts of a graph into Clotho parts (typically only done
     * for solution graphs) *
     */
    private Vector generateNewClothoVector(Collector coll, String name, String sequence, String LO, String RO, String resistance, int level) {

        _allVectors = coll.getAllVectors(true);
        String thisVecKey = name + "|" + LO + "|" + level + "|" + RO;

        //Search all existing vectors to for vectors with same overhangs and level before saving
        for (Vector vector : _allVectors) {

            //If the vector keys match, return the same vector
            String vectorKey = vector.getName() + "|" + vector.getLeftoverhang() + "|" + vector.getLevel() + "|" + vector.getRightOverhang();
            if (thisVecKey.equals(vectorKey)) {
                return vector;
            }
        }

        Vector newVector = Vector.generateVector(name, sequence);

        if (!LO.isEmpty()) {
            newVector.addSearchTag("LO: " + LO);
        }
        if (!RO.isEmpty()) {
            newVector.addSearchTag("RO: " + RO);
        }
        if (!resistance.isEmpty()) {
            newVector.addSearchTag("Resistance: " + resistance);
        }
        if (level > -1) {
            newVector.addSearchTag("Level: " + level);
        }

        return newVector;
    }

    /**
     * Refresh a part list (used by the viewer) *
     */
    private void refreshPartVectorList(Collector coll) {
        _allCompositeParts = new ArrayList<Part>();
        _allBasicParts = new ArrayList<Part>();
        _allVectors = new ArrayList<Vector>();
        ArrayList<Vector> allVectors = coll.getAllVectors(true);
        _allVectors.addAll(allVectors);
        ArrayList<Part> allParts = coll.getAllParts(true);
        for (Part somePart : allParts) {
            if (somePart.isComposite()) {
                _allCompositeParts.add(somePart);
            } else if (somePart.isBasic()) {
                _allBasicParts.add(somePart);
            }
        }
    }

    /**
     * Return the composition of a Clotho part *
     */
    public static ArrayList<Part> getComposition(Part part) throws Exception {
        ArrayList<Part> toReturn = new ArrayList<Part>();
        if (part.isBasic()) {
            toReturn.add(part);
        } else {
            ArrayList<Part> composition = part.getComposition();
            for (int i = 0; i < composition.size(); i++) {
                Part currentPart = composition.get(i);
                if (currentPart.isBasic()) {
                    toReturn.add(currentPart);
                } else {
                    toReturn = getCompositionHelper(currentPart, toReturn);
                }
            }
        }
        return toReturn;
    }

    /**
     * Helper for recursion method to discover all basic parts *
     */
    private static ArrayList<Part> getCompositionHelper(Part somePart, ArrayList<Part> partsList) throws Exception {

        ArrayList<Part> toReturn = partsList;
        Part compositePart = somePart;
        ArrayList<Part> composition = compositePart.getComposition();

        for (int i = 0; i < composition.size(); i++) {
            Part currentPart = composition.get(i);
            if (currentPart.isBasic()) {
                toReturn.add(currentPart);
            } else {
                toReturn = getCompositionHelper(currentPart, toReturn);
            }
        }
        return toReturn;
    }

    /**
     * Correct composite part UUIDs for Clotho export *
     */
    public void fixCompositeUUIDs(Collector coll, RGraph graph) throws Exception {

        ArrayList<RNode> queue = new ArrayList<RNode>();
        HashSet<RNode> seenNodes = new HashSet<RNode>();
        RNode root = graph.getRootNode();
        queue.add(root);
        ArrayList<RNode> sortedQueue = new ArrayList();
        sortedQueue.add(root);

        while (!queue.isEmpty()) {
            RNode current = queue.get(0);
            queue.remove(0);
            seenNodes.add(current);
            ArrayList<RNode> neighbors = current.getNeighbors();
            sortedQueue.add(0, current);
            for (RNode neighbor : neighbors) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        seenNodes.clear();

        while (!sortedQueue.isEmpty()) {
            RNode current = sortedQueue.get(0);
            sortedQueue.remove(0);
            seenNodes.add(current);
            Part currentPart = coll.getPart(current.getUUID(), true);
            ArrayList<RNode> neighbors = current.getNeighbors();

            //second part of if statement is for library parts with large compositions but no child neighbors
            if ((currentPart.isComposite() || current.getNeighbors().size() >= currentPart.getComposition().size()) && current.getStage() > 0) {
                ArrayList<Part> composition = new ArrayList();
                for (RNode neighbor : neighbors) {
                    if (current.getStage() > neighbor.getStage()) {
                        Part p = coll.getPart(neighbor.getUUID(), true);
                        composition.add(coll.getPart(neighbor.getUUID(), true));
                    }
                }
                currentPart.setComposition(composition);
                currentPart.setComposition(getComposition(currentPart));
            }
        }
    }
    //FIELDS
    ArrayList<Part> _allCompositeParts;
    ArrayList<Part> _allBasicParts;
    ArrayList<Vector> _allVectors;
    String _DVL0Seq = "tactagtagcggccgctgcagtccggcaaaaaagggcaaggtgtcaccaccctgccctttttctttaaaaccgaaaagattacttcgcgttatgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccacaggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaagaacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagctcgaggcttggattctcaccaataaaaaacgcccggcggcaaccgagcgttctgaacaaatccagatggagttctgaggtcattactggatctatcaacaggagtccaagcgagctcgatatcaaattacgccccgccctgccactcatcgcagtactgttgtaattcattaagcattctgccgacatggaagccatcacaaacggcatgatgaacctgaatcgccagcggcatcagcaccttgtcgccttgcgtataatatttgcccatggtgaaaacgggggcgaagaagttgtccatattggccacgtttaaatcaaaactggtgaaactcacccagggattggctgagacgaaaaacatattctcaataaaccctttagggaaataggccaggttttcaccgtaacacgccacatcttgcgaatatatgtgtagaaactgccggaaatcgtcgtggtattcactccagagcgatgaaaacgtttcagtttgctcatggaaaacggtgtaacaagggtgaacactatcccatatcaccagctcaccgtctttcattgccatacgaaattccggatgagcattcatcaggcgggcaagaatgtgaataaaggccggataaaacttgtgcttatttttctttacggtctttaaaaaggccgtaatatccagctgaacggtctggttataggtacattgagcaactgactgaaatgcctcaaaatgttctttacgatgccattgggatatatcaacggtggtatatccagtgatttttttctccattttagcttccttagctcctgaaaatctcgataactcaaaaaatacgcccggtagtgatcttatttcattatggtgaaagttggaacctcttacgtgcccgatcaactcgagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
    String _DVL1Seq = "tactagtagcggccgctgcagtccggcaaaaaagggcaaggtgtcaccaccctgccctttttctttaaaaccgaaaagattacttcgcgttatgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccacaggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaagaacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagctcgagtcccgtcaagtcagcgtaatgctctgccagtgttacaaccaattaaccaattctgattagaaaaactcatcgagcatcaaatgaaactgcaatttattcatatcaggattatcaataccatatttttgaaaaagccgtttctgtaatgaaggagaaaactcaccgaggcagttccataggatggcaagatcctggtatcggtctgcgattccgactcgtccaacatcaatacaacctattaatttcccctcgtcaaaaataaggttatcaagtgagaaatcaccatgagtgacgactgaatccggtgagaatggcaaaagcttatgcatttctttccagacttgttcaacaggccagccattacgctcgtcatcaaaatcactcgcatcaaccaaaccgttattcattcgtgattgcgcctgagcgagacgaaatacgcgatcgctgttaaaaggacaattacaaacaggaatcgaatgcaaccggcgcaggaacactgccagcgcatcaacaatattttcacctgaatcaggatattcttctaatacctggaatgctgttttcccggggatcgcagtggtgagtaaccatgcatcatcaggagtacggataaaatgcttgatggtcggaagaggcataaattccgtcagccagtttagtctgaccatctcatctgtaacatcattggcaacgctacctttgccatgtttcagaaacaactctggcgcatcgggcttcccatacaatcgatagattgtcgcacctgattgcccgacattatcgcgagcccatttatacccatataaatcagcatccatgttggaatttaatcgcggcctggagcaagacgtttcccgttgaatatggctcataacaccccttgtattactgtttatgtaagcagacagttttattgttcatgatgatatatttttatcttgtgcaatgtaacatcagagattttgagacacaacgtggctttgttgaataaatcgaacttttgctgagttgaaggatcagctcgagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
    String _DVL2Seq = "tactagtagcggccgctgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccataggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaaggacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagttaccaatgcttaatcagtgaggcacctatctcagcgatctgtctatttcgttcatccatagttgcctgactccccgtcgtgtagataactacgatacgggagggcttaccatctggccccagtgctgcaatgataccgcgagacccacgctcaccggctccagatttatcagcaataaaccagccagccggaagggccgagcgcagaagtggtcctgcaactttatccgcctccatccagtctattaattgttgccgggaagctagagtaagtagttcgccagttaatagtttgcgcaacgttgttgccattgctacaggcatcgtggtgtcacgctcgtcgtttggtatggcttcattcagctccggttcccaacgatcaaggcgagttacatgatcccccatgttgtgcaaaaaagcggttagctccttcggtcctccgatcgttgtcagaagtaagttggccgcagtgttatcactcatggttatggcagcactgcataattctcttactgtcatgccatccgtaagatgcttttctgtgactggtgagtactcaaccaagtcattctgagaatagtgtatgcggcgaccgagttgctcttgcccggcgtcaatacgggataataccgcgccacatagcagaactttaaaagtgctcatcattggaaaacgttcttcggggcgaaaactctcaaggatcttaccgctgttgagatccagttcgatgtaacccactcgtgcacccaactgatcttcagcatcttttactttcaccagcgtttctgggtgagcaaaaacaggaaggcaaaatgccgcaaaaaagggaataagggcgacacggaaatgttgaatactcatactcttcctttttcaatattattgaagcatttatcagggttattgtctcatgagcggatacatatttgaatgtatttagaaaaataaacaaataggggttccgcgcacatttccccgaaaagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
    String _pSK1A2 = "tactagtagcggccgctgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccataggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaaggacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagttaccaatgcttaatcagtgaggcacctatctcagcgatctgtctatttcgttcatccatagttgcctgactccccgtcgtgtagataactacgatacgggagggcttaccatctggccccagtgctgcaatgataccgcgagacccacgctcaccggctccagatttatcagcaataaaccagccagccggaagggccgagcgcagaagtggtcctgcaactttatccgcctccatccagtctattaattgttgccgggaagctagagtaagtagttcgccagttaatagtttgcgcaacgttgttgccattgctacaggcatcgtggtgtcacgctcgtcgtttggtatggcttcattcagctccggttcccaacgatcaaggcgagttacatgatcccccatgttgtgcaaaaaagcggttagctccttcggtcctccgatcgttgtcagaagtaagttggccgcagtgttatcactcatggttatggcagcactgcataattctcttactgtcatgccatccgtaagatgcttttctgtgactggtgagtactcaaccaagtcattctgagaatagtgtatgcggcgaccgagttgctcttgcccggcgtcaatacgggataataccgcgccacatagcagaactttaaaagtgctcatcattggaaaacgttcttcggggcgaaaactctcaaggatcttaccgctgttgagatccagttcgatgtaacccactcgtgcacccaactgatcttcagcatcttttactttcaccagcgtttctgggtgagcaaaaacaggaaggcaaaatgccgcaaaaaagggaataagggcgacacggaaatgttgaatactcatactcttcctttttcaatattattgaagcatttatcagggttattgtctcatgagcggatacatatttgaatgtatttagaaaaataaacaaataggggttccgcgcacatttccccgaaaagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
}
