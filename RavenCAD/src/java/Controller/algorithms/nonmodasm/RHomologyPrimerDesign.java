/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.nonmodasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.PrimerDesign;
import Controller.datastructures.Collector;
import Controller.datastructures.Part;
import Controller.datastructures.RNode;
import Controller.datastructures.RVector;
import Controller.datastructures.Vector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author evanappleton
 */
public class RHomologyPrimerDesign {
    
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
                      
                        //Assuming there is a vector present, include it in the MoClo reaction (this should always be the case for MoClo assembly)
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
    
}
