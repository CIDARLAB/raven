/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.accessibility;

import Controller.algorithms.modasm.RBioBricks;
import Controller.algorithms.modasm.RGoldenGate;
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

    public static String generateInstructions(ArrayList<RNode> roots, Collector coll, ArrayList<Part> partLib, ArrayList<Vector> vectorLib, ArrayList<String> primerParameters, boolean designPrimers, String method) {

        int oligoCount = 1;
        String instructions = "";
        HashMap<String, String> oligoSequenceNameHash = new HashMap(); //key: sequence, value: name
        designPrimers = true;
        String oligoNameRoot = "oligo";
        Double meltingTemp = 55.0;
        int primerLength = 20;

        try {
            oligoNameRoot = primerParameters.get(0);
            meltingTemp = Double.valueOf(primerParameters.get(1));
            primerLength = Integer.valueOf(primerParameters.get(2));
        } catch (Exception e) {
            oligoNameRoot = "oligo";
            meltingTemp = 55.0;
            primerLength = 20;
        }

        ArrayList<String> oligoNames = new ArrayList<String>();
        ArrayList<String> oligoSequences = new ArrayList<String>();
        HashSet<RVector> newVectors = new HashSet<RVector>();
        HashSet<RNode> newNodes = new HashSet<RNode>();
        HashMap<String, String[]> nodeOligoHash = new HashMap();
        HashMap<String, ArrayList<String>> vectorOligoHash = new HashMap<String, ArrayList<String>>();
        HashSet<String> libraryPartKeys = ClothoReader.getExistingPartKeys(partLib);
        HashSet<String> libraryVectorKeys = ClothoReader.getExistingVectorKeys(vectorLib);

        for (RNode root : roots) {

            HashSet<RNode> l0NodesThisRoot = new HashSet<RNode>();
            HashSet<RVector> vectorsThisRoot = new HashSet<RVector>();

            //append header for each goal part
            instructions = instructions + "**********************************************"
                    + "\nAssembly Instructions for target part: " + coll.getPart(root.getUUID(), true).getName()
                    + "\n**********************************************\n";
            ArrayList<RNode> queue = new ArrayList<RNode>();
            queue.add(root);

            while (!queue.isEmpty()) {
                RNode currentNode = queue.get(0);
                queue.remove(0);

                Part currentPart = coll.getPart(currentNode.getUUID(), true);

                //If the current node is a step in stage 1 or higher instruct the cloning steps
                if (currentNode.getStage() > 0) {

                    //If this node has a new vector, there will need to be a PCR added, but this is done at the end of the file
                    RVector vector = currentNode.getVector();
                    if (vector != null) {
                        vectorsThisRoot.add(vector);
                        String vectorKey = vector.getVectorKey("+");
                        if (!libraryVectorKeys.contains(vectorKey)) {
                            newVectors.add(vector);
                        }
                    }

                    //append which parts to use for a moclo reaction
                    instructions = instructions + "\n-> Assemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a " + method + " cloning reaction with: ";
                    for (RNode neighbor : currentNode.getNeighbors()) {

                        if (currentNode.getStage() > neighbor.getStage()) {
                            Part neighborPart = coll.getPart(neighbor.getUUID(), true);
                            instructions = instructions + neighborPart.getName() + "|" + neighborPart.getLeftOverhang() + "|" + neighborPart.getRightOverhang() + ", ";
                            queue.add(neighbor);
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
                        vectorsThisRoot.add(vector);
                        vectorKey = vector.getVectorKey("+");
                        if (!libraryVectorKeys.contains(vectorKey)) {
                            newVectors.add(vector);
                        }
                    }
                    l0NodesThisRoot.add(currentNode);

                    //Design part primers if this part key is not in the key list, perform asssembly step if the vector or part is not yet PCRed
                    if (!libraryPartKeys.contains(nodeKey)) {
                        newNodes.add(currentNode);

                        //Assuming there is a vector present, include it in the MoClo reaction (this should always be the case for MoClo assembly)
                        if (vector != null) {
                            instructions = instructions + "\n-> Assemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with: ";
                            instructions = instructions + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + ", ";
                            instructions = instructions + vector.getName() + "|" + vector.getLOverhang() + "|" + vector.getROverhang() + "\n\n";
                        }

                        //If the part key is in the list, determine if a steps is necessary based upon whether the vector is also present
                    } else {
                        if (!libraryVectorKeys.contains(vectorKey)) {
                            instructions = instructions + "\n-> Assemble " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang() + " by performing a MoClo cloning reaction with: ";
                        }
                    }
                }
            }

            //Look at all level 0 nodes for this root
            for (RNode l0Node : l0NodesThisRoot) {

                Part currentPart = coll.getPart(l0Node.getUUID(), true);

                //Design primers for new level 0 nodes
                if (newNodes.contains(l0Node)) {
                    if (designPrimers) {

                        //For small part, just order annealing primers
                        boolean anneal = false;
                        if (coll.getPart(l0Node.getUUID(), true).getSeq().length() <= 24) {
                            anneal = true;
                        }

                        //If primers for this node have not yet been created (seen in the hash), create them
                        if (!nodeOligoHash.containsKey(l0Node.getNodeKey("+"))) {

                            String[] oligoNamesForNode = new String[2];


                            //Determine which kind of primers to generate
                            String[] oligos;
                            if (method.equalsIgnoreCase("MoClo")) {
                                oligos = RMoClo.generatePartPrimers(l0Node, coll, meltingTemp, primerLength);
                            } else if (method.equalsIgnoreCase("BioBricks")) {
                                oligos = RBioBricks.generatePartPrimers(l0Node, coll, meltingTemp, primerLength);
                            } else if (method.equalsIgnoreCase("GoldenGate")) {
                                oligos = RGoldenGate.generatePartPrimers(l0Node, coll, meltingTemp, primerLength);
                            } else {
                                oligos = RHomologyPrimerDesign.homologousRecombinationPrimers(l0Node, root, coll, meltingTemp, primerLength);
                            }

                            //With homologous recombination of very small parts primers for these parts is unecessary and the get implanted into other primers
                            if (oligos[0].length() > 0 && oligos[0].length() > 0) {
                                String fwdOligo = oligos[0];
                                String revOligo = oligos[1];
                                String forwardOligoName = "";
                                String reverseOligoName = "";
                                if (oligoSequenceNameHash.containsKey(fwdOligo)) {
                                    forwardOligoName = oligoSequenceNameHash.get(fwdOligo);
                                } else {
                                    forwardOligoName = oligoNameRoot + oligoCount;
                                    oligoNames.add(forwardOligoName);
                                    oligoSequences.add(fwdOligo);
                                    oligoCount++;
                                }
                                if (oligoSequenceNameHash.containsKey(revOligo)) {
                                    reverseOligoName = oligoSequenceNameHash.get(revOligo);
                                } else {
                                    reverseOligoName = oligoNameRoot + oligoCount;
                                    oligoNames.add(reverseOligoName);
                                    oligoSequences.add(revOligo);
                                    oligoCount++;
                                }
                                oligoNamesForNode[0] = forwardOligoName;
                                oligoNamesForNode[1] = reverseOligoName;
                                nodeOligoHash.put(l0Node.getNodeKey("+"), oligoNamesForNode);

                                //If the primers are small and therefore annealing primers
                                if (anneal) {
                                    instructions = instructions + "\nAnneal oligos: " + forwardOligoName + " and " + reverseOligoName + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                                } else {
                                    instructions = instructions + "\nPCR " + currentPart.getName() + " with oligos: " + forwardOligoName + " and " + reverseOligoName + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                                }
                            }

                        } else {
                            String[] nodeOligos = nodeOligoHash.get(l0Node.getNodeKey("+"));
                            if (anneal) {
                                instructions = instructions + "\nAnneal oligos: " + nodeOligos[0] + " and " + nodeOligos[1] + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                            } else {
                                instructions = instructions + "\nPCR " + currentPart.getName() + " with oligos: " + nodeOligos[0] + " and " + nodeOligos[1] + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                            }
                        }
                    } else {
                        instructions = instructions + "\nPCR " + currentPart.getName() + " to get part: " + currentPart.getName() + "|" + currentPart.getLeftOverhang() + "|" + currentPart.getRightOverhang();
                    }
                }
            }

            //Look at all vectors for this root
            for (RVector vector : vectorsThisRoot) {

                Vector currentVector = coll.getVector(vector.getUUID(), true);

                //Design primers for new vectors
                if (newVectors.contains(vector)) {
                    if (designPrimers) {

                        //If primers for this vector have not yet been created (seen in the hash), create them
                        if (!vectorOligoHash.containsKey(vector.getVectorKey("+"))) {
                            ArrayList<String> vectorOligoNamesForNode = new ArrayList<String>();


                            //Determine which kind of primers to generate
                            String[] oligos = new String[2];
                            if (method.equalsIgnoreCase("MoClo")) {
                                oligos = RMoClo.generateVectorPrimers(vector, coll);
                            } else if (method.equalsIgnoreCase("BioBricks")) {
                                oligos = RBioBricks.generateVectorPrimers(vector, coll, meltingTemp, primerLength);
                            }

                            String fwdOligo = oligos[0];
                            String revOligo = oligos[1];
                            String forwardOligoName = "";
                            String reverseOligoName = "";
                            if (oligoSequenceNameHash.containsKey(fwdOligo)) {
                                forwardOligoName = oligoSequenceNameHash.get(fwdOligo);
                            } else {
                                forwardOligoName = oligoNameRoot + oligoCount;
                                oligoNames.add(forwardOligoName);
                                oligoSequences.add(fwdOligo);
                                oligoCount++;
                            }
                            if (oligoSequenceNameHash.containsKey(revOligo)) {
                                reverseOligoName = oligoSequenceNameHash.get(revOligo);
                            } else {
                                reverseOligoName = oligoNameRoot + oligoCount;
                                oligoNames.add(reverseOligoName);
                                oligoSequences.add(revOligo);
                                oligoCount++;
                            }
                            
                            //pair of oligos used for a part
                            vectorOligoNamesForNode.add(forwardOligoName);
                            vectorOligoNamesForNode.add(reverseOligoName);

                            vectorOligoHash.put(vector.getVectorKey("+"), vectorOligoNamesForNode);
                            instructions = instructions + "\nPCR " + currentVector.getName() + " with oligos: " + forwardOligoName + " and " + reverseOligoName + " to get vector: " + currentVector.getName() + "|" + currentVector.getLeftoverhang() + "|" + currentVector.getRightOverhang();

                        } else {
                            ArrayList<String> oligoHash = vectorOligoHash.get(vector.getVectorKey("+"));
                            instructions = instructions + "\nPCR " + currentVector.getName() + " with oligos: " + oligoHash.get(0) + " and " + oligoHash.get(1) + " to get vector: " + currentVector.getName() + "|" + currentVector.getLeftoverhang() + "|" + currentVector.getRightOverhang();
                        }
                    } else {
                        instructions = instructions + "\nPCR " + currentVector.getName() + " to get vector: " + currentVector.getName() + "|" + currentVector.getLeftoverhang() + "|" + currentVector.getRightOverhang();
                    }
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
