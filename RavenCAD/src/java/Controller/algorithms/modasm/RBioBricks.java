/*
 * This class contains the SDS++ algorithm
 * 
 */
package Controller.algorithms.modasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.RGeneral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import Controller.datastructures.*;

/**
 *
 * @author evanappleton
 */
public class RBioBricks extends RGeneral {

    /**
     * Clotho part wrapper for BioBricks 3A
     */
    public ArrayList<RGraph> bioBricksClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, ArrayList<Double> costs) throws Exception {

        //Try-Catch block around wrapper method
        _maxNeighbors = 2;

        //Initialize part hash and vector set
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, discouraged, recommended);
//        ArrayList<RVector> vectorSet = ClothoReader.vectorImportClotho(vectorLibrary);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalParts);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, null, true);
        assignBioBricksOverhangs(optimalGraphs, null);
        assignScars(optimalGraphs);

        return optimalGraphs;
    }
    
    /** First step of overhang assignment - enforce numeric place holders for overhangs, ie no overhang redundancy in any step **/
    private void assignBioBricksOverhangs (ArrayList<RGraph> optimalGraphs, RVector vector) {
        
        //Initialize fields that record information to save complexity for future steps
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        if (vector == null) {
            vector = new RVector();           
        }
        vector.setLOverhang("EX");
        vector.setROverhang("SP");
        vector.setName("BBa_Vector");
        
        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            root.setVector(vector);
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);
            ArrayList<RNode> neighbors = root.getNeighbors();
            assignBioBricksOverhangsHelper(root, neighbors, root, vector);
        }

        //Determine which nodes impact which level to form the stageDirectionAssignHash
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<String> rootDir = new ArrayList<String>();
            ArrayList<String> direction = root.getDirection();
            rootDir.addAll(direction);
            ArrayList<RNode> l0Nodes = _rootBasicNodeHash.get(root);

            //Determine which levels each basic node impacts            
            for (int i = 0; i < l0Nodes.size(); i++) {

                //Determine direction of basic level 0 nodes               
                RNode l0Node = l0Nodes.get(i);               
                String l0Direction = rootDir.get(0);               
                if (l0Node.getComposition().size() == 1) {
                    ArrayList<String> l0Dir = new ArrayList<String>();
                    l0Dir.add(l0Direction);
                    l0Node.setDirection(l0Dir);
                }               
                int size = l0Node.getDirection().size();
                rootDir.subList(0, size).clear();
            }
        }
    }
    
    /** This helper method executes the loops necessary to enforce overhangs for each graph in enforceOverhangRules **/
    private void assignBioBricksOverhangsHelper (RNode parent, ArrayList<RNode> children, RNode root, RVector vector) {
        
        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            
            //Give biobricks overhangs
            child.setLOverhang("EX");
            child.setROverhang("SP");
            child.setVector(vector);

            //Make recursive call
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                assignBioBricksOverhangsHelper(child, grandChildren, root, vector);
            
            //Or record the level zero parts
            } else {
                ArrayList<RNode> l0nodes = _rootBasicNodeHash.get(root);
                l0nodes.add(child);
                _rootBasicNodeHash.put(root, l0nodes);
            }
        }
    }
       
    /** Determine overhang scars **/
    private void assignScars(ArrayList<RGraph> optimalGraphs) {
        
        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {
            
            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            assignScarsHelper(root, children);
        }        
    }
    
    /** Overhang scars helper **/
    private ArrayList<String> assignScarsHelper(RNode parent, ArrayList<RNode> children) {
        
        ArrayList<String> scars = new ArrayList<String>();
        
        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            
            if (i > 0) {
                if (child.getLOverhang().isEmpty()) {
                    scars.add("_");
                }
                scars.add("BB"); 
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
        
        parent.setScars(scars);       
        return scars;
    }
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
    
    //generates human readable instructions as well as primer sequences
    //primerParameters contains (in this order): 
    //[oligoNameRoot, forwardPrefix, reversePrefix, forwardCutSite, reverseCutSite, forwardCutDistance, reverseCutDistance,meltingTemperature, targetLength)
    public static String generateInstructions(ArrayList<RNode> roots, Collector coll, ArrayList<String> primerParameters, ArrayList<Part> partLib, ArrayList<Vector> vectorLib) {

        //initialize primer parameters
        String oligoNameRoot = "test_asm";
        String partPrimerPrefix = "nn";
        String partPrimerSuffix = "nn";
        String vectorPrimerPrefix = "gttctttactagtg";
        String vectorPrimerSuffix = "tactagtagcggccgc";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";
        String fwdEnzymeRecSite2 = "ggtctc";
        String revEnzymeRecSite2 = "gagacc";
//        int forwardEnzymeCutDistance = 0;
//        int reverseEnzymeCutDistance = 0;
        Double meltingTemp = 55.0;
        int targetLength = 20;

        boolean designPrimers = true;

//        if (primerParameters != null) {
//            designPrimers = true;
//            oligoNameRoot = primerParameters.get(0);//your oligos will be named olignoNameRoot+Number+F/R (F/R = forward/reverse)
//            meltingTemp = Double.parseDouble(primerParameters.get(1));//desired melting temperature of your primers; determines homology length
//            targetLength = Integer.parseInt(primerParameters.get(2));
//        }

        int oligoCount = 0;
        String instructions = "";

        ArrayList<String> oligoNames = new ArrayList<String>();
        ArrayList<String> oligoSequences = new ArrayList<String>();
        HashSet<RNode> seenNodes = new HashSet<RNode>();
        HashSet<RVector> newVectors = new HashSet<RVector>();
        HashSet<RNode> newNodes = new HashSet<RNode>();
        HashSet<String> libraryPartKeys = ClothoReader.getExistingPartKeys(partLib);
        HashSet<String> libraryVectorKeys = ClothoReader.getExistingVectorKeys(vectorLib);
        
        for (RNode root : roots) {

            //append header for each goal part
            instructions = instructions + "**********************************************"
                    + "\nAssembly Instructions for target part: " + coll.getPart(root.getUUID(), true).getName()
                    + "\n**********************************************\n";
            ArrayList<RNode> queue = new ArrayList<RNode>();
            queue.add(root);
            
            while (!queue.isEmpty()) {
                RNode currentNode = queue.get(0);
                queue.remove(0); 

                if (!seenNodes.contains(currentNode)) {
                    
                    //only need to generate instructions for assembling a part that has not already been encountered
                    seenNodes.add(currentNode);
                    Part currentPart = coll.getPart(currentNode.getUUID(), true);

                    //If the current node is a step in stage 1 or higher instruct the cloning steps
                    if (currentNode.getStage() > 0) {
                        
                        //If this node has a new vector, there will need to be a PCR added, but this is done at the end of the file
                        RVector vector = currentNode.getVector();
                        if (vector != null) {
                            String vectorKey = vector.getVectorKey("+");
                            if (!libraryVectorKeys.contains(vectorKey)) {
                                newVectors.add(vector);
                            }
                        }
                        
                        //append which parts to use for a moclo reaction
                        instructions = instructions + "\n\nAssemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with:\n";
                        for (RNode neighbor : currentNode.getNeighbors()) {

                            if (currentNode.getStage() > neighbor.getStage()) {
                                Part part = coll.getPart(neighbor.getUUID(), true);
                                instructions = instructions + part.getName() + "|" + part.getLeftOverhang() + "|" + part.getRightOverhang() + ", ";
                                if (!seenNodes.contains(neighbor)) {
                                    queue.add(neighbor);
                                }
                            }
                        }
                      
                        //Assuming there is a vector present, include it in the reaction (this should always be the case for MoClo assembly)
                        if (vector != null) {
                            instructions = instructions + vector.getName() + "|" + vector.getLOverhang() + "|" + vector.getROverhang() + "\n\n";
                        } else {
                            instructions = instructions.substring(0, instructions.length() - 2) + "\n\n";
                        }
                        
                    //If the node is in stage 0, it must be determined whether or not PCRs need to be done and design primers if necessary    
                    } else {
                        
                        String nodeKey = currentNode.getNodeKey("+");
                        RVector vector = currentNode.getVector();
                        String vectorKey = new String();
                        if (vector != null) {
                            vectorKey = vector.getVectorKey("+");
                            if (!libraryVectorKeys.contains(vectorKey)) {
                                newVectors.add(vector);
                            }
                        }

                        //Design part primers if this part key is not in the key list, perform asssembly step if the vector or part is not yet PCRed
                        if (!libraryPartKeys.contains(nodeKey)) {
                            newNodes.add(currentNode);
                            
                            //Assuming there is a vector present, include it in the MoClo reaction (this should always be the case for MoClo assembly)
                            if (vector != null) {
                                instructions = instructions + "\n\nAssemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with:\n";
                                instructions = instructions + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + ", ";
                                instructions = instructions + vector.getName() + "|" + vector.getLOverhang() + "|" + vector.getROverhang() + "\n\n";
                            }

                        //If the part key is in the list, determine if a steps is necessary based upon whether the vector is also present
                        } else {
                            if (!libraryVectorKeys.contains(vectorKey)) {
                                instructions = instructions + "\n\nAssemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with:\n\n";                              
                            }
                        }
                    }
                }
            }
            
            //Design primers for new level 0 nodes
            for (RNode node : newNodes) {
                                
                Part currentPart = coll.getPart(node.getUUID(), true);
                
                if (designPrimers) {
                    String forwardOligoName = (oligoNameRoot + oligoCount) + "F";
                    String reverseOligoName = (oligoNameRoot + oligoCount) + "R";
                    String forwardOligoSequence = "";
                    String reverseOligoSequence = "";
                    oligoNames.add(forwardOligoName);
                    oligoNames.add(reverseOligoName);
                    oligoSequences.add(forwardOligoSequence);
                    oligoSequences.add(reverseOligoSequence);
                    oligoCount++;
                    instructions = instructions + "\nPCR " + currentPart.getName() + " with oligos: " + forwardOligoName + " and " + reverseOligoName + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                } else {
                    instructions = instructions + "\nPCR " + currentPart.getName() + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                }
            } 
            
            //Design primers for new vectors
            for (RVector vector : newVectors) {
                
                Vector currentVector = coll.getVector(vector.getUUID(), true);
                
                if (designPrimers) {
                    String forwardOligoName = (oligoNameRoot + oligoCount) + "F";
                    String reverseOligoName = (oligoNameRoot + oligoCount) + "R";
                    
                    //Level 0 and level 2 vectors
                    String forwardOligoSequence;
                    String reverseOligoSequence;
                    if (vector.getLevel() % 3 == 0 || (vector.getLevel() - 2) % 3 == 0) {
                        forwardOligoSequence = "";
                        reverseOligoSequence = "";
                    
                    //Level 1 vectors
                    } else {
                        forwardOligoSequence = "";
                        reverseOligoSequence = "";
                    }

                    oligoNames.add(forwardOligoName);
                    oligoNames.add(reverseOligoName);
                    oligoSequences.add(forwardOligoSequence);
                    oligoSequences.add(reverseOligoSequence);
                    oligoCount++;
                    instructions = instructions + "\nPCR " + currentVector.getName() + " with oligos: " + forwardOligoName + " and " + reverseOligoName + " to get vector: " + currentVector.getName() + "|" + currentVector.getLeftoverhang() + "|" + currentVector.getRightOverhang();
                } else {
                    instructions = instructions + "\nPCR " + currentVector.getName() + " to get vector: " + currentVector.getName() + "|" + currentVector.getLeftoverhang() + "|" + currentVector.getRightOverhang();
                }
            }
            
            instructions = instructions + "\n\n";
        }

        if (designPrimers) {

            //append primer designs
            instructions = instructions + "\n**********************************************\nOLIGOS";
            for (int i = 0; i < oligoNames.size(); i++) {
                instructions = instructions + "\n>" + oligoNames.get(i);
                instructions = instructions + "\n" + oligoSequences.get(i);
            }
        }
        return instructions;
    }
    
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
}