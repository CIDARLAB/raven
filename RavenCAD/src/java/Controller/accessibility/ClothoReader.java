/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;

import java.util.ArrayList;
import java.util.HashSet;
import Controller.datastructures.*;

/**
 * Provides utility methods for interpreting Clotho composite parts
 *
 * @author Tao
 */
public class ClothoReader {

    /**
     * Clotho reader constructor *
     */
    public ClothoReader() {
        _allCompositeParts = new ArrayList<Part>();
        _allBasicParts = new ArrayList<Part>();
    }

    /**
     * Generate Clotho parts with uuids from intermediates without uuids *
     */
    public void nodesToClothoPartsVectors(Collector coll, SRSGraph graph) throws Exception {
        String nameRoot = coll.getPart(graph.getRootNode().getUUID()).getName();
        ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
        HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
        queue.add(graph.getRootNode());
        while (!queue.isEmpty()) {
            SRSNode currentNode = queue.get(0);
            seenNodes.add(currentNode);
            queue.remove(0);

            for (SRSNode neighbor : currentNode.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }

            //If the node has no uuid
            if (currentNode.getUUID() == null) {
                //Get new intermediate name
                String partName = nameRoot + "_intermediate" + Math.random() * 999999999;
                partName = partName.replaceAll("\\.", "");
                if (partName.length() > 255) {
                    partName = partName.substring(0, 255);
                }

                //Get new intermediate composition
                ArrayList<String> UUIDcomposition = new ArrayList();
                for (String s : currentNode.getComposition()) {
                    UUIDcomposition.add(Part.retrieveByExactName(coll, s).getUUID());
                }

                //Get new intermediate overhangs
                String LO = currentNode.getLOverhang();
                String RO = currentNode.getROverhang();

                //If there's overhangs, add search tags
                Part newPart = generateNewClothoPart(coll, partName, "", UUIDcomposition, LO, RO);
                currentNode.setName(partName);
                newPart.saveDefault(coll);
                currentNode.setUUID(newPart.getUUID());

            }


            //create new part and change node uuid if overhangs not match
            Part currentPart = coll.getPart(currentNode.getUUID());
            if (!currentNode.getLOverhang().equals(currentPart.getLeftOverhang()) || !currentNode.getROverhang().equals(currentPart.getRightOverhang())) {
                //current part is not an exact match for the node in terms of over hang, find a better match or create a new part
                Part betterPart = coll.getPartByName(currentPart.getName() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang()); //search for a better match
                if (betterPart == null || !currentNode.getLOverhang().equals(betterPart.getLeftOverhang()) || !currentNode.getROverhang().equals(betterPart.getRightOverhang())) {
                    //if no better part exists, create a new one
                    if (currentPart.isBasic()) {
                        betterPart = Part.generateBasic(currentPart.getName(), currentPart.getSeq());

                    } else if (currentPart.isComposite()) {
                        betterPart = Part.generateComposite(currentPart.getComposition(), currentPart.getName());
                    }
                    betterPart.addSearchTag("LO: " + currentNode.getLOverhang());
                    betterPart.addSearchTag("RO: " + currentNode.getROverhang());
                    betterPart.addSearchTag("Type: " + currentPart.getType());
                    betterPart.saveDefault(coll);
                }
                currentNode.setUUID(betterPart.getUUID());

            }



            //Get the vector and save a new vector if it does not have a uuid
            SRSVector vector = currentNode.getVector();
            if (vector != null) {
                //Get new intermediate name
                String vecName = (nameRoot + "_vector" + Math.random() * 999999999);
                vecName = vecName.replaceAll("\\.", "");
                if (vecName.length() > 255) {
                    vecName = vecName.substring(0, 255);
                }
                //Get vector overhangs
                String LO = vector.getLOverhang();
                String RO = vector.getROverhang();
                String resistance = vector.getResistance();
                int level = vector.getLevel();
                Vector newVector = generateNewClothoVector(coll, vecName, "", LO, RO, resistance, level);
                newVector.saveDefault(coll);
                vector.setName(newVector.getName());
                vector.setUUID(newVector.getUUID());
                currentNode.setVector(vector);
            }
            seenNodes.add(currentNode);
        }

    }

    /**
     * Make intermediate parts of a graph into Clotho parts (typically only done
     * for solution graphs) *
     */
    private Part generateNewClothoPart(Collector coll, String name, String description, ArrayList<String> UUIDcomposition, String LO, String RO) {
        if (_allCompositeParts == null || _allBasicParts == null) {
            refreshPartVectorList(coll);
        }
        ArrayList<String> inputPartUUIDComp = new ArrayList<String>();

        //For each composite part, get the basic part uuids
        for (String uuid : UUIDcomposition) {
            Part inputPart = coll.getPart(uuid);
            if (inputPart.isComposite()) {
                try {
                    ArrayList<Part> inputPartComposition = getComposition(inputPart);
                    for (Part basicPart : inputPartComposition) {
                        inputPartUUIDComp.add(basicPart.getUUID());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                inputPartUUIDComp.add(uuid);
            }
        }

        //Every time a new composite part can be made, search to see there's nothing made from the same components before saving
        for (Part existingPart : _allCompositeParts) {
            ArrayList<String> existingPartUUIDComp = new ArrayList<String>();

            //Get an existing part's overhangs
            ArrayList<String> sTags = existingPart.getSearchTags();
            String existingPartLO = new String();
            String existingPartRO = new String();
            for (int k = 0; k < sTags.size(); k++) {
                if (sTags.get(k).startsWith("LO:")) {
                    existingPartLO = sTags.get(k).substring(4);
                } else if (sTags.get(k).startsWith("RO:")) {
                    existingPartRO = sTags.get(k).substring(4);
                }
            }

            //Obtain the basic part uuids
            try {
                ArrayList<Part> existingPartComposition = getComposition(existingPart);
                for (Part basicPart : existingPartComposition) {
                    existingPartUUIDComp.add(basicPart.getUUID());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            //If the number of uuids is the same as the number of input composition uuids and the number of uuids in the composition of somePart and the overhangs match, return the part
            if (inputPartUUIDComp.equals(existingPartUUIDComp)) {
                if (existingPartLO.equalsIgnoreCase(LO) && existingPartRO.equalsIgnoreCase(RO)) {
                    return existingPart;
                }
            }
        }


        //If a new composite part needs to be made
        if (inputPartUUIDComp.size() > 1) {
            ArrayList<Part> newComposition = new ArrayList<Part>();
            for (String uuid : inputPartUUIDComp) {
                newComposition.add(coll.getPart(uuid));
            }
            Part newPart = Part.generateComposite(newComposition, name);
            if (!LO.isEmpty()) {
                newPart.addSearchTag("LO: " + LO);
            }
            if (!RO.isEmpty()) {
                newPart.addSearchTag("RO: " + RO);
            }
//            newPart.saveDefault();
            return newPart;

            //Make a new basic part
        } else {
            Part newPart = Part.generateBasic(name, coll.getPart(inputPartUUIDComp.get(0)).getSeq());
            if (!LO.isEmpty()) {
                newPart.addSearchTag("LO: " + LO);
            }
            if (!RO.isEmpty()) {
                newPart.addSearchTag("RO: " + RO);
            }
            return newPart;
        }

    }

    /**
     * Make intermediate parts of a graph into Clotho parts (typically only done
     * for solution graphs) *
     */
    private Vector generateNewClothoVector(Collector coll, String name, String sequence, String LO, String RO, String resistance, int level) {
        _allVectors = coll.getAllVectors();
        //Search all existing vectors to for vectors with same overhangs and level before saving
        for (Vector vector : _allVectors) {
            //Get an existing part's overhangs
            ArrayList<String> sTags = vector.getSearchTags();
            String existingVecLO = vector.getLeftoverhang();
            String existingVecRO = vector.getRightOverhang();
            String existResistance = vector.getResistance();
            int existLevel = vector.getLevel();
            //If all of these things match, just return the vector that is found
            if (existingVecLO.equalsIgnoreCase(LO) && existingVecRO.equalsIgnoreCase(RO)) {
                if (existResistance.equalsIgnoreCase(resistance) && existLevel == level) {
                    return vector;
                }
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
    private static void refreshPartVectorList(Collector coll) {
        _allCompositeParts = new ArrayList<Part>();
        _allBasicParts = new ArrayList<Part>();
        _allVectors = new ArrayList<Vector>();
        ArrayList<Vector> allVectors = coll.getAllVectors();
        _allVectors.addAll(allVectors);
        ArrayList<Part> allParts = coll.getAllParts();
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

    public void fixCompositeUUIDs(Collector coll, SRSGraph graph) throws Exception {
        ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
        HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
        SRSNode root = graph.getRootNode();
        queue.add(root);
        ArrayList<SRSNode> sortedQueue = new ArrayList();
        sortedQueue.add(root);
        while (!queue.isEmpty()) {
            SRSNode current = queue.get(0);
            queue.remove(0);
            seenNodes.add(current);
            ArrayList<SRSNode> neighbors = current.getNeighbors();
            for (SRSNode neighbor : neighbors) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                    sortedQueue.add(0, neighbor);
                }
            }
        }
        seenNodes.clear();
        while (!sortedQueue.isEmpty()) {
            SRSNode current = sortedQueue.get(0);
            sortedQueue.remove(0);
            seenNodes.add(current);
            Part currentPart = coll.getPart(current.getUUID());
            ArrayList<SRSNode> neighbors = current.getNeighbors();
            ArrayList<Part> composition = new ArrayList();
            if (currentPart.getComposition().size() > 1) {
                for (SRSNode neighbor : neighbors) {
                    if (current.getComposition().toString().length() > neighbor.getComposition().toString().length()) {
                        composition.add(coll.getPart(neighbor.getUUID()));
                    }
                }
                currentPart.setComposition(composition);
                currentPart.setComposition(getComposition(currentPart));
            }
        }




    }
    //Fields
    static ArrayList<Part> _allCompositeParts;
    static ArrayList<Part> _allBasicParts;
    static ArrayList<Vector> _allVectors;
}