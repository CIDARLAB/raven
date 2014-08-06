/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;
import Controller.algorithms.PrimerDesign;
import java.util.ArrayList;
import java.util.HashSet;
import Controller.datastructures.*;
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

                //If the node has no uuid, make a new part
                if (currentNode.getUUID() == null) {

                    //Get new intermediate name
                    Date date = new Date();
                    String partName = nameRoot + "_intermediate_" + count + "_" + user + "_" +  date.toString().replaceAll(" ", "");
                    count++;
                    partName = partName.replaceAll("\\.", "");
                    if (partName.length() > 255) {
                        partName = partName.substring(0, 255);
                    }

                    //If there's overhangs, add search tags
                    Part newPlasmid = generateNewClothoCompositePart(coll, partName, "", composition, direction, scars, LO, RO);                    
                    newPlasmid.addSearchTag("Type: plasmid");
                    newPlasmid = newPlasmid.saveDefault(coll);
                    
                    Part newComposite = generateNewClothoCompositePart(coll, partName, "", composition, direction, scars, LO, RO);
                    newComposite.addSearchTag("Type: composite");
                    newComposite.saveDefault(coll);
                    
                    currentNode.setName(partName);
                    currentNode.setUUID(newPlasmid.getUUID());

                } else {

                    //If a part with this composition and overhangs does not exist, a new part is needed
                    String seq = "";
                    ArrayList<String> tags = new ArrayList<String>();
                    tags.add("LO: " + currentNode.getLOverhang());
                    tags.add("RO: " + currentNode.getROverhang());
                    tags.add("Type: plasmid");
                    tags.add("Direction: " + currentNode.getDirection());
                    tags.add("Scars: " + currentNode.getScars());
                    ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(currentNode.getName(), true);
                    if (!allPartsWithName.isEmpty()) {
                        seq = allPartsWithName.get(0).getSeq();
                        if (currentNode.getDirection().size() == 1) {
                            if (currentNode.getDirection().get(0).equals("-") && allPartsWithName.get(0).getSearchTags().contains("Direction: [+]")) {
                                seq = PrimerDesign.reverseComplement(seq);
                            } else if (currentNode.getDirection().get(0).equals("+") && allPartsWithName.get(0).getSearchTags().contains("Direction: [-]")) {
                                seq = PrimerDesign.reverseComplement(seq);
                            }
                        }
                    }
                    
                    currentPart = coll.getExactPart(currentNode.getName(), seq, currentNode.getComposition(), tags, false);
                    
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
                            
                            newPlasmid = Part.generateBasic(name, currentSeq, bpComposition);
                            
                            //Make a new part
                            Part newBasic = Part.generateBasic(name, currentSeq, bpComposition);
                            newBasic.addSearchTag("LO: " + LO);
                            newBasic.addSearchTag("RO: " + RO);
                            newBasic.addSearchTag("Direction: " + currentNode.getDirection().toString());
                            newBasic.addSearchTag("Scars: []");
                            
                            //Special types for merged parts 
                            if (currentNode.getSpecialSeq() != null) {
                                newBasic.addSearchTag("Type: " + currentNode.getType());
                            } else {
                                String type = currentNode.getType().toString();
                                type = type.substring(1, type.length() - 1);
                                newBasic.addSearchTag("Type: " + type);
                            }
                            
                            newBasic.saveDefault(coll);
                            
                            //Assign this basic part to the node if scarless assembly
                            if (!(method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("biobricks") || method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("gatewaygibson"))) {
                                currentNode.setUUID(newBasic.getUUID());
                                continue;
                            }

                        } else {

                            //If a new composite part needs to be made
                            ArrayList<Part> newComposition = new ArrayList<Part>();

                            for (int i = 0; i < composition.size(); i++) {
                                ArrayList<String> cSearchTags = new ArrayList<String>();
                                ArrayList<String> bpComp = new ArrayList<String>();
                                bpComp.add(composition.get(i));
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
                                if (cLO.equals("BB") || cRO.equals("BB")) {
                                    cLO = "EX";
                                    cRO = "SP";
                                }

                                cSearchTags.add("RO: " + cRO);
                                cSearchTags.add("LO: " + cLO);
                                cSearchTags.add("Type: " + cType);
                                cSearchTags.add("Direction: [" + cDir + "]");
                                cSearchTags.add("Scars: []");
                                Part exactPart = coll.getExactPart(cName, cSeq, bpComp, cSearchTags, true);
                                
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
                                    cSearchTags.clear();
                                    cSearchTags.add("LO: " + invertedcRO);
                                    cSearchTags.add("RO: " + invertedcLO);
                                    cSearchTags.add("Type: " + cType);
                                    cSearchTags.add("Direction: [" + invertedcDir + "]");
                                    cSearchTags.add("Scars: []");
                                    exactPart = coll.getExactPart(cName, rcSeq, bpComp, cSearchTags, true);
                                }

                                //In the edge case where the overhangs of a re-used composite part is changed
                                if (exactPart == null) {
                                    cSearchTags.clear();
                                    cSearchTags.add("Type: " + cType);
                                    cSearchTags.add("RO: ");
                                    cSearchTags.add("LO: ");
                                    cSearchTags.add("Direction: [" + cDir + "]" );
                                    cSearchTags.add("Scars: []");
                                    exactPart = coll.getExactPart(cName, cSeq, bpComp, cSearchTags, true);
                                }
                                
                                newComposition.add(exactPart);
                            }

                            newPlasmid = Part.generateComposite(newComposition, currentPart.getName());
                            
                            //For homologous recombination methods, a new composite part needs to be made for re-use cases
                            Part newComposite = Part.generateComposite(newComposition, currentPart.getName());
                            newComposite.addSearchTag("LO: " + LO);
                            newComposite.addSearchTag("RO: " + RO);
                            newComposite.addSearchTag("Direction: " + currentNode.getDirection().toString());
                            newComposite.addSearchTag("Scars: " + currentNode.getScars().toString());
                            newComposite.addSearchTag("Type: composite");
                            newComposite.saveDefault(coll);
                            
                            //Assign this basic part to the node if scarless assembly
                            if (!(method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("biobricks") || method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("gatewaygibson"))) {
                                if (currentNode.getStage() == 0) {
                                    currentNode.setUUID(newComposite.getUUID());
                                }
                            }
                        }

                        newPlasmid.addSearchTag("LO: " + LO);
                        newPlasmid.addSearchTag("RO: " + RO);
                        newPlasmid.addSearchTag("Direction: " + currentNode.getDirection().toString());
                        newPlasmid.addSearchTag("Scars: " + currentNode.getScars().toString());
                        newPlasmid.addSearchTag("Type: plasmid");
                        
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
    private Part generateNewClothoCompositePart(Collector coll, String name, String description, ArrayList<String> composition, ArrayList<String> direction, ArrayList<String> scars, String LO, String RO) throws Exception {
        if (_allCompositeParts.isEmpty() || _allBasicParts.isEmpty()) {
            refreshPartVectorList(coll);
        }

        //If a new composite part needs to be made
        ArrayList<Part> newComposition = new ArrayList<Part>();
        for (int i = 0; i < composition.size(); i++) {
            ArrayList<String> cSearchTags = new ArrayList<String>();
            String cName = composition.get(i);
            ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(cName, false);
            String cSeq = allPartsWithName.get(0).getSeq();
            
            //Correct for direction
            String cDir = direction.get(i);
            if (cDir.equals("-") && allPartsWithName.get(0).getSearchTags().contains("Direction: [+]")) {
                cSeq = PrimerDesign.reverseComplement(cSeq);
            } else if (cDir.equals("+") && allPartsWithName.get(0).getSearchTags().contains("Direction: [-]")) {
                cSeq = PrimerDesign.reverseComplement(cSeq);
            }

            String cType = allPartsWithName.get(0).getType();
            ArrayList<String> thisComp = new ArrayList<String>();
            thisComp.add(cName);
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
            if (cLO.equals("BB") || cRO.equals("BB")) {
                cLO = "EX";
                cRO = "SP";
            }

            cSearchTags.add("RO: " + cRO);
            cSearchTags.add("LO: " + cLO);
            cSearchTags.add("Type: " + cType);
            cSearchTags.add("Direction: [" + cDir + "]");
            cSearchTags.add("Scars: []");

            Part exactPart = coll.getExactPart(cName, cSeq, thisComp, cSearchTags, true);
            
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
                cSearchTags.clear();
                cSearchTags.add("LO: " + invertedcRO);
                cSearchTags.add("RO: " + invertedcLO);
                cSearchTags.add("Type: " + cType);
                cSearchTags.add("Direction: [" + invertedcDir + "]");
                cSearchTags.add("Scars: []");
                exactPart = coll.getExactPart(cName, cSeq, thisComp, cSearchTags, true);
            }    
            
            //In homologous recombinations, the overhangs are the neighbor, select the blank part
            if (exactPart == null) {
                cSearchTags.clear();
                cSearchTags.add("Type: " + cType);
                cSearchTags.add("RO: ");
                cSearchTags.add("LO: ");
                cSearchTags.add("Direction: [" + cDir + "]");
                cSearchTags.add("Scars: []");
                exactPart = coll.getExactPart(cName, cSeq, thisComp, cSearchTags, true);
            }
            
            newComposition.add(exactPart);
        }

        Part newPart = Part.generateComposite(newComposition, name);
        newPart.addSearchTag("LO: " + LO);
        newPart.addSearchTag("RO: " + RO);
        newPart.addSearchTag("Direction: " + direction);
        newPart.addSearchTag("Scars: " + scars);
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
        
        //If making a destination vector
        if (method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("moclo")) {
            newVector.addSearchTag("Composition: lacZ|" + LO + "|" + RO + "|+");
            newVector.addSearchTag("Type: destination vector");
            newVector.addSearchTag("Vector: " + name);
        } else {
            newVector.addSearchTag("Type: vector");
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
    //FIELDS
    private ArrayList<Part> _allCompositeParts;
    private ArrayList<Part> _allBasicParts;
    private ArrayList<Vector> _allVectors;
}
