/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;

import Controller.algorithms.modasm.RBioBricks;
import Controller.algorithms.modasm.RMoClo;
import Controller.algorithms.nonmodasm.RHomologyPrimerDesign;
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
public class RInstructions {
    
    public static String generateInstructions (ArrayList<RNode> roots, Collector coll, ArrayList<Part> partLib, ArrayList<Vector> vectorLib, ArrayList<String> primerParameters, boolean designPrimers, String method) {
        
        int oligoCount = 0;
        String instructions = "";
        String oligoNameRoot = "test_asm";
        Double meltingTemp = 50.0;
        int primerLength = 24;
        
        if (primerParameters != null) {
            designPrimers = true;
            oligoNameRoot = primerParameters.get(0);
            meltingTemp = Double.valueOf(primerParameters.get(1));
            primerLength = Integer.valueOf(primerParameters.get(2));
        }

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
                        instructions = instructions + "\n-> Assemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a " + method + " cloning reaction with:\n";
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
                                instructions = instructions + "\n-> Assemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with:\n";
                                instructions = instructions + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + ", ";
                                instructions = instructions + vector.getName() + "|" + vector.getLOverhang() + "|" + vector.getROverhang() + "\n\n";
                            }

                        //If the part key is in the list, determine if a steps is necessary based upon whether the vector is also present
                        } else {
                            if (!libraryVectorKeys.contains(vectorKey)) {
                                instructions = instructions + "\n-> Assemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with:\n\n";                              
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
                    
                    //Determine which kind of primers to generate
                    ArrayList<String> oligos;
                    if (method.equalsIgnoreCase("MoClo")) {
                        oligos = RMoClo.generatePartPrimers(node, coll, meltingTemp, primerLength);
                    } else if (method.equalsIgnoreCase("BioBricks")) {
                        oligos = RBioBricks.generatePartPrimers(node, coll, meltingTemp, primerLength);
                    } else {
                        oligos = RHomologyPrimerDesign.homologousRecombinationPrimers(node, root, coll, meltingTemp, primerLength);
                    }
                    
                    oligoNames.add(forwardOligoName);
                    oligoNames.add(reverseOligoName);
                    oligoSequences.add(oligos.get(0));
                    oligoSequences.add(oligos.get(1));
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
                    
                    //Determine which kind of primers to generate
                    ArrayList<String> oligos = new ArrayList<String>();
                    if (method.equalsIgnoreCase("MoClo")) {
                        oligos = RMoClo.generateVectorPrimers(vector, coll);
                    } else if (method.equalsIgnoreCase("BioBricks")) {
                        oligos = RBioBricks.generateVectorPrimers(vector, coll, meltingTemp, primerLength);
                    }

                    oligoNames.add(forwardOligoName);
                    oligoNames.add(reverseOligoName);
                    oligoSequences.add(oligos.get(0));
                    oligoSequences.add(oligos.get(1));
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
