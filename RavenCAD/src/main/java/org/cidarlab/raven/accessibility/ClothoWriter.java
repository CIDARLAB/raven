/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.accessibility;
import org.cidarlab.raven.datastructures.RGraph;
import org.cidarlab.raven.datastructures.Collector;
import org.cidarlab.raven.datastructures.RVector;
import org.cidarlab.raven.datastructures.Part;
import org.cidarlab.raven.datastructures.RNode;
import org.cidarlab.raven.datastructures.Vector;
import org.cidarlab.raven.algorithms.core.PrimerDesign;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;

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
    public void nodesToClothoPartsVectors(Collector coll, RGraph graph, HashMap<Part, Vector> partVectorHash, HashMap<Integer, Vector> stageVectors, String method, String user) throws Exception {
        String nameRoot = coll.getPart(graph.getRootNode().getUUID(), true).getName();

        Integer count = 1;
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
                Part currentPart = null;

                //Get scar sequences
                ArrayList<String> scarSeqs = scarsToSeqs(scars);
                
                //If the node has no uuid, make a new part
                //This should be the case for new intermediates
                if (currentNode.getUUID() == null) {
                    
                    //Get new intermediate name
                    Date date = new Date();
                    String partName = nameRoot + "_intermediate_" + count + "_" + user + "_" +  date.toString().replaceAll(" ", "");
                    count++;
                    partName = partName.replaceAll("\\.", "");
                    if (partName.length() > 255) {
                        partName = partName.substring(0, 255);
                    }

                    Part newPlasmid = generateNewClothoCompositePart(coll, partName, "", composition, direction, scars, LO, RO, scarSeqs);                    
                    newPlasmid.getType().add("plasmid");
                    newPlasmid = newPlasmid.saveDefault(coll);
                    
                    Part newComposite = generateNewClothoCompositePart(coll, partName, "", composition, direction, scars, LO, RO, scarSeqs);
                    newComposite.getType().add("composite");
                    newComposite.saveDefault(coll);
                    
                    currentNode.setName(partName);
                    currentNode.setUUID(newPlasmid.getUUID());

                } else {

                    //If a part with this composition and overhangs does not exist, a new part is needed
                    String seq = "";
                    ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(currentNode.getName(), true);
                    if (!allPartsWithName.isEmpty()) {
                        seq = allPartsWithName.get(0).getSeq();
                        if (currentNode.getDirection().size() == 1) {
                            if (currentNode.getDirection().get(0).equals("-") && allPartsWithName.get(0).getDirections().get(0).equals("+")) {
                                seq = PrimerDesign.reverseComplement(seq);
                            } else if (currentNode.getDirection().get(0).equals("+") && allPartsWithName.get(0).getDirections().get(0).equals("-")) {
                                seq = PrimerDesign.reverseComplement(seq);
                            }
                        }
                    }
                    
                    ArrayList<String> type = new ArrayList();
                    type.add("plasmid");
                    currentPart = coll.getExactPart(currentNode.getName(), seq, currentNode.getComposition(), currentNode.getLOverhang(), currentNode.getROverhang(), type, currentNode.getScars(), currentNode.getDirection(), false);
                    
                    if (currentPart == null) {

                        currentPart = coll.getPart(currentNode.getUUID(), true);
                        
                        //If a new part must be created
                        Part newPlasmid;
                        if (currentPart.isBasic()) {
                            
                            String name;
                            String currentSeq;
                            ArrayList<Part> bpComposition = new ArrayList<Part>();
                            
                            //Edge case for special merged nodes
                            if (currentNode.getSpecialSeq() != null) {
                                currentSeq = currentNode.getSpecialSeq();
                                name = currentNode.getName();
                                
                                ArrayList<String> nodeComposition = currentNode.getComposition();
                                for (int i = 0; i < nodeComposition.size(); i++) {

                                    //Forming the composite part composition
                                    ArrayList<Part> bpsWithName = coll.getAllPartsWithName(nodeComposition.get(i), false);
                                    Part bp = null;

                                    //Pick the part with no overhangs in the right direction
                                    for (Part bpWithName : bpsWithName) {
                                        String bpLO = bpWithName.getLeftOverhang();
                                        String bpRO = bpWithName.getRightOverhang();
                                        if (bpLO.isEmpty() && bpRO.isEmpty() && currentNode.getDirection().get(i).equals(bpWithName.getDirections().get(0))) {
                                            if (!bpWithName.getType().equals("plasmid")) {
                                                bp = bpWithName;
                                            }
                                        }
                                    }
                                    
                                    bpComposition.add(bp);
                                }
                                
                            } else {
                                currentSeq = currentPart.getSeq();
                                name = currentPart.getName();
                                bpComposition = currentPart.getComposition();
                            }
                            
                            newPlasmid = Part.generateBasic(name, currentSeq, bpComposition, new ArrayList(), new ArrayList(), "", "");                            
                            Part newBasic = Part.generateBasic(name, currentSeq, bpComposition, currentNode.getType(), currentNode.getDirection(), LO, RO);
                            newBasic.saveDefault(coll);
                            
                            //Assign this basic part to the node if scarless assembly
                            if (!(method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("biobricks") || method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("gatewaygibson"))) {
                                currentNode.setUUID(newBasic.getUUID());
                                continue;
                            }

                        } else {

                            //If a new composite part needs to be made
                            ArrayList<Part> newComposition = buildCompositePart(coll, composition, nodeType, direction, scars, LO, RO, currentPart);
                            newPlasmid = Part.generateComposite(currentPart.getName(), newComposition, scarSeqs, new ArrayList(), null, new ArrayList(), "", "", new ArrayList());
                            
                            //For homologous recombination methods, a new composite part needs to be made for re-use cases
                            ArrayList<String> typeC = new ArrayList();
                            typeC.add("composite");
                            Part newComposite = Part.generateComposite(currentPart.getName(), newComposition, scarSeqs, currentNode.getScars(), null, currentNode.getDirection(), LO, RO, typeC);
                            newComposite.saveDefault(coll);
                            
                            //Assign this basic part to the node if scarless assembly
                            if (!(method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("biobricks") || method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("gatewaygibson"))) {
                                if (currentNode.getStage() == 0) {
                                    currentNode.setUUID(newComposite.getUUID());
                                }
                            }
                        }

                        newPlasmid.setLeftOverhang(LO);
                        newPlasmid.setRightOverhang(RO);
                        newPlasmid.setDirections(currentNode.getDirection());
                        newPlasmid.setScars(currentNode.getScars());
                        newPlasmid.setType(type);
                        
                        if ((method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("biobricks") || method.equalsIgnoreCase("goldengate")) || method.equalsIgnoreCase("gatewaygibson") || currentNode.getStage() > 0) {                          

                            //Do not save basic plasmid for scarless method - they do not exist
                            newPlasmid = newPlasmid.saveDefault(coll);
                            currentNode.setUUID(newPlasmid.getUUID());
                            currentPart = newPlasmid;
                        } 
                    } else {
                        currentNode.setUUID(currentPart.getUUID());
                    }
                }

                //Get the vector and save a new vector if it does not have a uuid
                RVector vector = currentNode.getVector();
                if (vector != null) {
                    
                    //Get vector parameters
                    String seq = "";
                    String resistance = "";
                    ArrayList<Vector> allVectorsWithName = coll.getAllVectorsWithName(vector.getName(), true);
                    if (!allVectorsWithName.isEmpty()) {
                        seq = allVectorsWithName.get(0).getSeq();
                        resistance = allVectorsWithName.get(0).getResistance();
                    }
                                        
                    int level = vector.getLevel();
                    Vector existingVec = partVectorHash.get(currentPart);
                    Integer nextLevel = 0;
                    if (stageVectors.size() > 1) {
                        nextLevel = (level % stageVectors.size())+1;
                    }
                    
                    Vector nextLevelVec = stageVectors.get(nextLevel);

                    //If there is an assigned vector in the library with the appropriate resistance, do not bother making a new vector
                    if (existingVec != null) {
                        
                        //If levels are a consideration, you may reuse vectors of the same level as long as the resistance does not match the next level
                        if (method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("gatewaygibson")) {
                            
                            if ((existingVec.getLevel() % 2) == (level % 2)) {
                                
                                //As long as the resistance does not match the next level
                                if (!(existingVec.getResistance().equalsIgnoreCase(nextLevelVec.getResistance()))) {
                                    vector.setName(existingVec.getName());
                                    vector.setUUID(existingVec.getUUID());
                                    vector.setLevel(existingVec.getLevel());
                                
                                //Or if there is only one vector used
                                } else if (stageVectors.size() == 1) {
                                    vector.setName(existingVec.getName());
                                    vector.setUUID(existingVec.getUUID());
                                    vector.setLevel(existingVec.getLevel());
                                
                                //Otherwise make a new vector
                                } else {
                                    Vector newVector = generateNewClothoVector(coll, vector.getName(), seq, LO, RO, resistance, level, method);
                                    newVector = newVector.saveDefault(coll);
                                    vector.setName(newVector.getName());
                                    vector.setUUID(newVector.getUUID());
                                }
                            
                            //If there is only one node, i.e. no assembly
                            } else if (currentNode.getNeighbors().isEmpty()) {
                                vector.setName(existingVec.getName());
                                vector.setUUID(existingVec.getUUID());
                                vector.setLevel(existingVec.getLevel());
                                
                            //Otherwise make a new vector
                            } else {
                                Vector newVector = generateNewClothoVector(coll, vector.getName(), seq, LO, RO, resistance, level, method);
                                newVector = newVector.saveDefault(coll);
                                vector.setName(newVector.getName());
                                vector.setUUID(newVector.getUUID());
                            }
                        
                        //For all other assembly methods, as long as the found vector is not the same resistance of the next level, it can be re-used
                        } else {
                            
                            if (!(existingVec.getResistance().equalsIgnoreCase(nextLevelVec.getResistance())) && existingVec.getLeftOverhang().equalsIgnoreCase(nextLevelVec.getLeftOverhang()) && existingVec.getRightOverhang().equalsIgnoreCase(nextLevelVec.getRightOverhang())) {
                                vector.setName(existingVec.getName());
                                vector.setUUID(existingVec.getUUID());
                            } else if (stageVectors.size() == 1 && existingVec.getLeftOverhang().equalsIgnoreCase(nextLevelVec.getLeftOverhang()) && existingVec.getRightOverhang().equalsIgnoreCase(nextLevelVec.getRightOverhang())) {
                                vector.setName(existingVec.getName());
                                vector.setUUID(existingVec.getUUID());
                            } else {
                                Vector newVector = generateNewClothoVector(coll, vector.getName(), seq, vector.getLOverhang(), vector.getROverhang(), resistance, level, method);
                                newVector = newVector.saveDefault(coll);
                                vector.setName(newVector.getName());
                                vector.setUUID(newVector.getUUID());
                            }
                        }

                    //Otherwise make a new vector if non exists
                    } else {
                        Vector newVector = generateNewClothoVector(coll, vector.getName(), seq, vector.getLOverhang(), vector.getROverhang(), resistance, level, method);
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
    private Part generateNewClothoCompositePart(Collector coll, String name, String description, ArrayList<String> composition, ArrayList<String> direction, ArrayList<String> scars, String LO, String RO, ArrayList<String> scarSeqs) throws Exception {
        if (_allCompositeParts.isEmpty() || _allBasicParts.isEmpty()) {
            refreshPartVectorList(coll);
        }

        //If a new composite part needs to be made
        ArrayList<Part> newComposition = buildCompositePart(coll, composition, null, direction, scars, LO, RO, null);
        Part newPart = Part.generateComposite(name, newComposition, scarSeqs, scars, null, direction, LO, RO, new ArrayList());
        return newPart;
    }

    /**
     * Make intermediate parts of a graph into Clotho parts (typically only done
     * for solution graphs) *
     */
    private Vector generateNewClothoVector(Collector coll, String name, String sequence, String LO, String RO, String resistance, int level, String method) {

        _allVectors = coll.getAllVectors(true);
        String thisVecKey = name + "|" + LO + "|" + level + "|" + RO;

        //Search all existing vectors to for vectors with same overhangs and level before saving
        for (Vector vector : _allVectors) {

            //If the vector keys match, return the same vector
            String vectorKey = vector.getName() + "|" + vector.getLeftOverhang() + "|" + vector.getLevel() + "|" + vector.getRightOverhang();
            if (thisVecKey.equals(vectorKey)) {
                return vector;
            }
        }
        
        //If making a destination vector
        String composition = "";
        String type = "";
        String vector = "";
        if (method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("gatewaygibson")) {
            if (method.equalsIgnoreCase("gatewaygibson") && level > 0) {
                composition = "CmR-ccdB|" + LO + "|" + RO + "|+";
            } else {
                composition = "lacZ|" + LO + "|" + RO + "|+";
            }
            type = "destination vector";
            vector = name;
        } else {
            type = "vector";
        }

        Vector newVector = Vector.generateVector(name, sequence, LO, RO, type, vector, composition, resistance, level);
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
        ArrayList<Part> partComposition = new ArrayList<Part>();
        if (part.isBasic()) {
            partComposition.add(part.getComposition().get(0));
        } else {
            ArrayList<Part> composition = part.getComposition();
            for (int i = 0; i < composition.size(); i++) {
                Part currentPart = composition.get(i);
                if (currentPart.isBasic()) {
                    partComposition.add(currentPart);
                } else {
                    partComposition = getCompositionHelper(currentPart, partComposition);
                }
            }
        }
        return partComposition;
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
                toReturn.add(currentPart.getComposition().get(0));
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
    
    /*
     * Conver a set of scars to sequences
     */
    public static ArrayList<String> scarsToSeqs(ArrayList<String> scars) {
        
         ArrayList<String> scarSeqs = new ArrayList();
        for (String scar : scars) {
            if ("BB".equals(scar)) {
                scarSeqs.add("tactagag");
            } else if ("BBm".equals(scar)) {
                scarSeqs.add("tactag");
            } else if (PrimerDesign.getMoCloOHseqs().containsKey(scar)) {
                scarSeqs.add(PrimerDesign.getMoCloOHseqs().get(scar));
            } else if (PrimerDesign.getGatewayGibsonOHseqs().containsKey(scar)) {
                scarSeqs.add(PrimerDesign.getGatewayGibsonOHseqs().get(scar));
            } else {
                scarSeqs.add(" ");
            }
        }
        return scarSeqs;
    }
    
    /*
     * Build composite parts from the collector
     */
    private ArrayList<Part> buildCompositePart(Collector coll, ArrayList<String> composition, ArrayList<String> type, ArrayList<String> direction, ArrayList<String> scars, String LO, String RO, Part currentPart) {
        
        ArrayList<Part> newComposition = new ArrayList<Part>();
        for (int i = 0; i < composition.size(); i++) {
            
            String cName = composition.get(i);
            ArrayList<String> thisComp = new ArrayList<String>();
            thisComp.add(cName);
            String cLO;
            String cRO;
            ArrayList<String> cDirs = new ArrayList();
            String cDir = direction.get(i);
            cDirs.add(cDir);              
            String cSeq;
            ArrayList<String> cType = new ArrayList();
            
            //Get sequence and type based upon whether or not a new part is being made or one is getting overwritten
            if (currentPart == null) {
                ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(cName, false);
                Part firstPartWithName = allPartsWithName.get(0);
                cSeq = firstPartWithName.getSeq().replaceAll(" ", "");

                //Correct sequence for direction
                if (cDir.equals("-") && firstPartWithName.getDirections().get(0).equals("+")) {
                    cSeq = PrimerDesign.reverseComplement(cSeq);
                } else if (cDir.equals("+") && firstPartWithName.getDirections().get(0).equals("-")) {
                    cSeq = PrimerDesign.reverseComplement(cSeq);
                }

                cType = allPartsWithName.get(0).getType();
            } else {
                cSeq = currentPart.getComposition().get(i).getSeq();
                cType.add(type.get(i));
            }
            
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
                    cRO = composition.get(1) + direction.get(1);
                } else if (i == composition.size() - 1) {
                    cLO = composition.get(composition.size() - 2) + direction.get(direction.size() - 2);
                    cRO = RO;
                } else {
                    cLO = composition.get(i - 1) + direction.get(i - 1);
                    cRO = composition.get(i + 1) + direction.get(i + 1);
                }
            }

            //BioBricks scars
            if (cLO.equals("BB") || cRO.equals("BB") || cLO.equals("BBm") || cRO.equals("BBm")) {
                cLO = "EX";
                cRO = "SP";
            }
            
            Part exactPart = coll.getExactPart(cName, cSeq, thisComp, cLO, cRO, cType, new ArrayList(), cDirs, true);
            
            //Try to find the inverted version if this part does not exist
            if (exactPart == null) {
                
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
                String invertedcDir;
                if (cDir.equals("+")) {
                    invertedcDir = "-";
                } else {
                    invertedcDir = "+";
                }
                String rcSeq = PrimerDesign.reverseComplement(cSeq);
                
                ArrayList<String> invcDirs = new ArrayList();
                invcDirs.add(invertedcDir);
                exactPart = coll.getExactPart(cName, rcSeq, thisComp, invertedcRO, invertedcLO, cType, new ArrayList(), invcDirs,  true);
            }    
            
            //In homologous recombinations, the overhangs are the neighbor, select the blank part
            if (exactPart == null) {
                exactPart = coll.getExactPart(cName, cSeq, thisComp, "", "", cType, new ArrayList(), cDirs, true);
            }
            
            newComposition.add(exactPart);
        }
        
        return newComposition;
    }
    
    //FIELDS
    private ArrayList<Part> _allCompositeParts;
    private ArrayList<Part> _allBasicParts;
    private ArrayList<Vector> _allVectors;
}
