/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.modasm;

import Controller.algorithms.SRSGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class SRSMoClo extends SRSGeneral {

    private ArrayList<Part> partLibrary = new ArrayList();

    /**
     * Clotho part wrapper for sequence dependent one pot reactions *
     */
    public ArrayList<SRSGraph> mocloClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies) {
        try {
            partLibrary = partLibrary;
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
            HashMap<String, SRSGraph> partHash = partImportClotho(goalParts, partLibrary, required, recommended); //key: composiion, value: corresponding graph; contains just basic parts and imported intermediates
            ArrayList<SRSVector> vectorSet = vectorImportClotho(vectorLibrary);

            //Put all parts into hash for mgp algorithm            
            ArrayList<SRSNode> gpsNodes = gpsToNodesClotho(goalParts);

            //Positional scoring of transcriptional units
            HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
            if (modular) {
                ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
                positionScores = getPositionalScoring(TUs);
            }
            //Add single transcriptional units to the required hash
//            ArrayList<ArrayList<String>> reqTUs = getSingleTranscriptionalUnits(gpsNodes, 2);
//            for (int i = 0; i < reqTUs.size(); i++) {
//                required.add(reqTUs.get(i).toString());
//            }

            //Run SDS Algorithm for multiple parts
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, partHash, positionScores, efficiencies);
            assignSharingOverhangs(optimalGraphs);
            minimizeOverhangs(optimalGraphs);
            optimizeOverhangVectors(optimalGraphs, partHash, vectorSet);
            //            crudeOptimize(optimalGraphs);
            //Remove transcriptional units from the required set
//            for (int j = 0; j < reqTUs.size(); j++) {
//                required.remove(reqTUs.get(j).toString());
//            }

            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
            E.printStackTrace();
            return blank;
        }
    }
    //assign overhangs ignoring the library of parts and vectors; overhangs are saved to graph nodes not part/vectors

    private void assignSharingOverhangs(ArrayList<SRSGraph> optimalGraphs) {
        compositionLevelHash = new HashMap();
        HashMap<String, ArrayList<String>> usedOverhangHash = new HashMap();
        parentHash = new HashMap(); //key: node, value: parent node
        HashMap<SRSNode, SRSNode> previousHash = new HashMap(); //key: node, value: sibling node on the "left"
        HashMap<SRSNode, SRSNode> nextHash = new HashMap(); //key: node, value: sibling node on the "right"
        HashMap<String, Integer> compositionFrequencyHash = new HashMap();
        compositionNodeHash = new HashMap();
        for (SRSGraph graph : optimalGraphs) {
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            queue.add(graph.getRootNode());
            parentHash.put(graph.getRootNode(), null);
            previousHash.put(graph.getRootNode(), null);
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                current.setLOverhang("");
                current.setROverhang("");
                seenNodes.add(current);
                if (!compositionLevelHash.containsKey(current.getComposition().toString())) {
                    compositionLevelHash.put(current.getComposition().toString(), current.getStage());
                }
                if (compositionFrequencyHash.get(current.getComposition().toString()) != null) {
                    compositionFrequencyHash.put(current.getComposition().toString(), compositionFrequencyHash.get(current.getComposition().toString()) + 1);
                } else {
                    compositionFrequencyHash.put(current.getComposition().toString(), 1);
                }
                ArrayList<SRSNode> neighbors = current.getNeighbors();
                SRSNode previous = null;
                for (SRSNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);
                        parentHash.put(neighbor, current);
                        previousHash.put(neighbor, previous);
                        //keep track of which nodes contain the current composition
                        ArrayList<SRSNode> containsThisComposition = compositionNodeHash.get(neighbor.getComposition().toString());
                        if (containsThisComposition != null) {
                            if (!containsThisComposition.contains(current)) {
                                containsThisComposition.add(current);
                            }
                        } else {
                            ArrayList<SRSNode> toAdd = new ArrayList();
                            toAdd.add(current);
                            compositionNodeHash.put(neighbor.getComposition().toString(), toAdd);
                        }

                        if (previousHash != null) {
                            nextHash.put(previous, neighbor);
                        }
                        previous = neighbor;
                    }
                }
            }

        }


        for (SRSGraph graph : optimalGraphs) {
            HashMap<SRSNode, HashSet<String>> neighborConflictHash = new HashMap();
            SRSNode root = graph.getRootNode();
            neighborConflictHash.put(root, new HashSet());
            HashSet<SRSNode> seenNodes = new HashSet();
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            queue.add(root);
            if (usedOverhangHash.get(root.getComposition().toString()) != null) {
                ArrayList<String> existingOverhangs = usedOverhangHash.get(root.getComposition().toString());
                String[] tokens = existingOverhangs.get(0).split("\\|");
                root.setLOverhang(tokens[0]);
                root.setROverhang(tokens[1]);
            } else {
                String randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
                root.setLOverhang(randIndex);
                randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
                root.setROverhang(randIndex);
                ArrayList<String> toAdd = new ArrayList();
                toAdd.add(root.getLOverhang() + "|" + root.getROverhang());
                usedOverhangHash.put(root.getComposition().toString(), toAdd);
            }

            while (!queue.isEmpty()) {
                SRSNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);
                if (parent.getNeighbors().size() > 0) {
                    SRSNode previousNode = null;
                    SRSNode nextNode = null;
                    HashSet<String> neighborConflictSet = neighborConflictHash.get(parent);
                    neighborConflictSet.add(parent.getLOverhang());
                    neighborConflictSet.add(parent.getROverhang());
                    //sort neighbors according to how frequently their composition appears
                    ArrayList<SRSNode> sortedNeighbors = new ArrayList();
                    ArrayList<SRSNode> clonedNeighbors = (ArrayList<SRSNode>) parent.getNeighbors().clone();
                    while (sortedNeighbors.size() < parent.getNeighbors().size()) {
                        int maxFrequency = -1;
                        SRSNode maxNode = null;
                        for (SRSNode node : clonedNeighbors) {
                            int frequency = compositionFrequencyHash.get(node.getComposition().toString());
                            if (frequency > maxFrequency) {
                                maxFrequency = frequency;
                                maxNode = node;
                            }
                        }
                        clonedNeighbors.remove(maxNode);
                        sortedNeighbors.add(maxNode);
                    }

                    for (int i = 0; i < parent.getNeighbors().size(); i++) {
                        SRSNode currentNode = sortedNeighbors.get(i);
                        if (!seenNodes.contains(currentNode)) {
                            ArrayList<String> existingOverhangs = usedOverhangHash.get(currentNode.getComposition().toString());
                            String left = "";
                            String right = "";
                            Boolean seenFirst = true; //seen the first beighbor
                            Boolean seenLast = false; //seen the last neighbor
                            previousNode = previousHash.get(currentNode);
                            nextNode = nextHash.get(currentNode);
                            if (previousNode == null) {
                                seenFirst = false;
                            }
                            if (nextNode == null) {
                                seenLast = true;
                            }
                            if (existingOverhangs != null) {
                                // iterate through all pairs of existing overhangs and see if a pair can be used
                                for (int j = 0; j < existingOverhangs.size(); j++) {
                                    left = "";
                                    right = "";
                                    String[] tokens = existingOverhangs.get(j).split("\\|");
                                    if (!neighborConflictSet.contains(tokens[0]) && !neighborConflictSet.contains(tokens[1])) {
                                        left = tokens[0];
                                        right = tokens[1];
                                    }
                                    if (!seenFirst && tokens[0].equals(parent.getLOverhang())) {
                                        left = tokens[0];
                                    }
                                    if (seenLast && tokens[1].equals(parent.getROverhang())) {
                                        right = tokens[1];
                                    }
                                    if (previousHash.get(currentNode) != null && !neighborConflictSet.contains(tokens[1])) {
                                        if (tokens[0].equals(previousHash.get(currentNode).getROverhang())) {
                                            left = tokens[0];
                                        }
                                    }
                                    if (nextHash.get(currentNode) != null && !neighborConflictSet.contains(tokens[0])) {
                                        if (tokens[1].equals(nextHash.get(currentNode).getLOverhang())) {
                                            right = tokens[1];
                                        }
                                    }
                                    if (left.length() > 0 && right.length() > 0) {
                                        break;
                                    }
                                }

                            }

                            if (left.length() > 0 && right.length() > 0 && seenFirst && !seenLast) {
                                //overwrite overhangs to enhance sharing
                                currentNode.setLOverhang(left);
                                currentNode.setROverhang(right);

                            } else {
                                //usual cases for assigning left overhang
                                if (!seenFirst) {
                                    //first part automatically gets left overhang of parent
                                    currentNode.setLOverhang(parent.getLOverhang());
                                } else {
                                    if (previousNode.getROverhang().length() > 0) {
                                        currentNode.setLOverhang(previousNode.getROverhang());
                                    } else {
                                        //if left overhang hasn't had its overhangs assigned yet, assign it
                                        String randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
                                        previousNode.setROverhang(randIndex);
                                        currentNode.setLOverhang(randIndex);
                                    }
                                }
                                //usual cases for assigning right overhang
                                if (seenLast) {
                                    // last node gets right overhang of parent
                                    currentNode.setROverhang(parent.getROverhang());
                                } else {
                                    String randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
                                    currentNode.setROverhang(randIndex);
                                }
                            }
                            //assign overhangs to neighbors if necessary 
                            if (nextNode != null && currentNode.getROverhang().length() > 0) {
                                if (nextNode.getLOverhang().length() > 0) {
                                    currentNode.setROverhang(nextNode.getLOverhang());
                                } else {
                                    nextNode.setLOverhang(currentNode.getROverhang());
                                }
                            }
                            if (previousNode != null && currentNode.getLOverhang().length() > 0) {
                                if (previousNode.getROverhang().length() > 0) {
                                    currentNode.setLOverhang(previousNode.getROverhang());
                                } else {
                                    previousNode.setROverhang(currentNode.getLOverhang());
                                }
                            }
                            //store overhang choice
                            existingOverhangs = usedOverhangHash.get(currentNode.getComposition().toString());
                            if (existingOverhangs != null) {
                                if (!existingOverhangs.contains(currentNode.getLOverhang() + "|" + currentNode.getROverhang())) {
                                    existingOverhangs.add(currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                                }
                            } else {
                                ArrayList<String> toAdd = new ArrayList();
                                toAdd.add(currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                                usedOverhangHash.put(currentNode.getComposition().toString(), toAdd);
                            }
                            compositionFrequencyHash.put(currentNode.getComposition().toString(), compositionFrequencyHash.get(currentNode.getComposition().toString()) + 1);
                            //the overhangs of neighbors cannot overlap at all
                            if (currentNode.getLOverhang().length() > 0) {
                                neighborConflictSet.add(currentNode.getLOverhang());
                            }
                            if (currentNode.getROverhang().length() > 0) {
                                neighborConflictSet.add(currentNode.getROverhang());
                            }
                            neighborConflictHash.put(currentNode, new HashSet());
                            queue.add(currentNode);
                        }
                    }
                }
            }
        }
    }

    private void minimizeOverhangs(ArrayList<SRSGraph> optimalGraphs) {
        abstractOverhangCompositionHash = new HashMap();
        partOverhangFrequencyHash = new HashMap();
        encounteredCompositions = new HashSet();
        HashMap<String, String> numberToLetterOverhangHash = new HashMap(); //replaces the abstract numerical with abstract letter overhangs; key: abstract numerical overhang, value: abstract letter overhang
        ArrayList<String> allOverhangs = new ArrayList(Arrays.asList("A_,B_,C_,D_,E_,G_,H_,I_,J_,K_,L_,M_,N_,O_,P_,Q_,R_,S_,T_,U_,V_,W_,X_,Y_,Z_,a_,b_,c_,d_,e_,f_,g_,h_,i_,j_,k_,l_,m_,n_,o_,p_,q_,r_,s_,t_,u_,v_,w_,x_,y_,z_".split(","))); //overhangs that don't exist in part or vector library
        for (SRSGraph graph : optimalGraphs) {
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            SRSNode root = graph.getRootNode();
            //count instances of each part overhang combination, which will be used for optimization later

            queue.add(root);

            while (!queue.isEmpty()) {
                boolean removeUncles = false;

                ArrayList<String> freeOverhangs = (ArrayList<String>) allOverhangs.clone();
                SRSNode currentNode = queue.get(0);
                queue.remove(0);
                SRSNode parent = parentHash.get(currentNode);
                if (parent != null) {
                    int partIndex = 0;
                    for (SRSNode node : parent.getNeighbors()) {
                        if (currentNode.getComposition().toString().equals(node.getComposition().toString())) {
                            break;
                        } else {
                            partIndex = partIndex + 1;
                        }
                    }
                    if ((partIndex == 0 || partIndex == parent.getComposition().size() - 1) && parent.getComposition().size() > 2) {
                        removeUncles = true;
                    }
                }
                if (parentHash.get(currentNode) != null) {
                    ArrayList<SRSNode> siblings = parent.getNeighbors();
                    //overhang can't be the same as neighbors that are not immediately adjacent
                    for (SRSNode neighbor : siblings) {
                        if (neighbor.getComposition().toString().length() < parent.getComposition().toString().length()) {
                            freeOverhangs.remove(numberToLetterOverhangHash.get(neighbor.getLOverhang()));
                            freeOverhangs.remove(numberToLetterOverhangHash.get(neighbor.getROverhang()));
                        }
                    }
                }
                //parents overhangs should only be assigned on the outside, so remove them
                if (parent != null) {
                    freeOverhangs.remove(numberToLetterOverhangHash.get(parent.getLOverhang()));
                    freeOverhangs.remove(numberToLetterOverhangHash.get(parent.getROverhang()));
                }

                if (parent != null && removeUncles) {
                    SRSNode grandParent = parentHash.get(parent);
                    if (grandParent != null) {
                        ArrayList<SRSNode> uncles = grandParent.getNeighbors();
                        for (SRSNode node : uncles) {
                            freeOverhangs.remove(numberToLetterOverhangHash.get(node.getLOverhang()));
                            freeOverhangs.remove(numberToLetterOverhangHash.get(node.getROverhang()));
                        }
                    }
                }

                //pick overhang for current node
                if (!numberToLetterOverhangHash.containsKey(currentNode.getLOverhang())) {
                    numberToLetterOverhangHash.put(currentNode.getLOverhang(), freeOverhangs.get(0));
                    freeOverhangs.remove(0);
                }
                if (!numberToLetterOverhangHash.containsKey(currentNode.getROverhang())) {
                    numberToLetterOverhangHash.put(currentNode.getROverhang(), freeOverhangs.get(0));
                    freeOverhangs.remove(0);
                }
                if (numberToLetterOverhangHash.get(currentNode.getLOverhang()).equals(numberToLetterOverhangHash.get(currentNode.getROverhang()))) {
                    numberToLetterOverhangHash.put(currentNode.getLOverhang(), freeOverhangs.get(freeOverhangs.size() - 1));
                    freeOverhangs.remove(freeOverhangs.size() - 1);
                }
                seenNodes.add(currentNode);
                ArrayList<SRSNode> neighbors = currentNode.getNeighbors();
                for (SRSNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);

                    }
                }
            }
        }
        //traverse graph and assign abstract letter overhangs
        for (SRSGraph graph : optimalGraphs) {
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                SRSNode currentNode = queue.get(0);
                queue.remove(0);
                currentNode.setLOverhang(numberToLetterOverhangHash.get(currentNode.getLOverhang()));
                currentNode.setROverhang(numberToLetterOverhangHash.get(currentNode.getROverhang()));
                seenNodes.add(currentNode);
                ArrayList<SRSNode> neighbors = currentNode.getNeighbors();
                for (SRSNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);

                    }
                }
                //count instances of each part overhang combination, which will be used for optimization later
                String partOverhangName = currentNode.getComposition() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang();
                partOverhangName = currentNode.getComposition() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang();
                if (partOverhangFrequencyHash.get(partOverhangName) != null) {
                    partOverhangFrequencyHash.put(partOverhangName, partOverhangFrequencyHash.get(partOverhangName) + 1);
                } else {
                    partOverhangFrequencyHash.put(partOverhangName, 1);
                }

                //keep track of which compositions appear with each abstract overhang pair; used in later optimization steps
                String overhangPair = currentNode.getLOverhang() + "|" + currentNode.getROverhang();
                ArrayList<String> existingCompositions = abstractOverhangCompositionHash.get(overhangPair);
                if (existingCompositions != null) {
                    if (!existingCompositions.contains(currentNode.getComposition().toString())) {
                        existingCompositions.add(currentNode.getComposition().toString());
                    }
                } else {
                    ArrayList<String> toAdd = new ArrayList();
                    toAdd.add(currentNode.getComposition().toString());
                    abstractOverhangCompositionHash.put(overhangPair, toAdd);
                }
                if (!encounteredCompositions.contains(currentNode.getComposition().toString())) {
                    encounteredCompositions.add(currentNode.getComposition().toString());
                }
            }

        }
    }

//optimizes overhang assignment based on frequency of a parts appearance and the availability of existing overhangs
//concurrent optimizes vector assignment based on vector assignment
//prioritize existing parts with correct overhangs
//next priority is overhangs that vectors already have
    private void optimizeOverhangVectors(ArrayList<SRSGraph> optimalGraphs, HashMap<String, SRSGraph> partHash, ArrayList<SRSVector> vectorSet) {
        HashMap<String, String> finalOverhangHash; //key: abstract overhang assignment with "_" character, value: final overhang
        HashMap<String, String> abstractFinalPairHash = new HashMap(); //key: abstract pair, value: concrete pair
        finalOverhangHash = preAssignOverhangs(optimalGraphs);
        HashSet<String> usedFreeOverhangs = new HashSet();//contains all new overhangs that don't appear in library
        HashSet<String> lengthBonusSet = new HashSet(); //parts between 30 and 80 base pairs get a boost in frequency so that they are considered first
        ArrayList<String> freeOverhangs = new ArrayList(Arrays.asList("A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))); //overhangs that don't exist in part or vector library
        HashMap<Integer, String> levelResistanceHash = new HashMap(); // key: level, value: antibiotic resistance
        HashMap<String, Integer> concreteOverhangFrequencyHash = new HashMap(); //key: concrete overhang pair, value: how often does that overhang pair appear
        HashMap<String, ArrayList<String>> abstractConcreteOptionsHash = new HashMap(); //key: abstract overhang pair, value: ArrayList<String> of concrete overhang pairs
        HashSet<String> libraryCompositions = new HashSet(); //compositions with overangs indicated in the library
        HashSet<String> assignedOverhangs = new HashSet<String>(); //all the overhangs that will appear in the actual output
        assignedOverhangs.addAll(finalOverhangHash.values());
        freeOverhangs.removeAll(finalOverhangHash.values());
        HashSet<String> vectorOverhangPairs = new HashSet();
        //gather all overhangs for existing vectors
        HashMap<Integer, ArrayList<String>> vectorOverhangHash = new HashMap(); //key: level, value: overhangs for that level
        ArrayList<Vector> allVectors = Collector.getAllVectors();
        for (Vector v : allVectors) {
            freeOverhangs.remove(v.getLeftoverhang());
            freeOverhangs.remove(v.getRightOverhang());
            String overhangString = v.getLeftoverhang() + "|" + v.getRightOverhang();
            vectorOverhangPairs.add(overhangString);
            ArrayList<String> levelOverhangs = vectorOverhangHash.get(v.getLevel());
            if (levelOverhangs != null) {
                if (!levelOverhangs.contains(overhangString)) {
                    levelOverhangs.add(overhangString);
                }
            } else {
                levelOverhangs = new ArrayList();
                vectorOverhangHash.put(v.getLevel(), levelOverhangs);
            }
            if (concreteOverhangFrequencyHash.containsKey(overhangString)) {
                concreteOverhangFrequencyHash.put(overhangString, concreteOverhangFrequencyHash.get(overhangString) + 1);
            } else {
                concreteOverhangFrequencyHash.put(overhangString, 1);
            }
        }

        //gather all overhangs for existing parts
//        ArrayList<Part> allParts = Collector.getAllParts();
        ArrayList<Part> allParts = partLibrary;
        HashMap<String, ArrayList<String>> compositionConcreteOverhangHash = new HashMap(); //key: part composition, value: arrayList of concrete overhangs that appear for that compositiong
        HashMap<String, ArrayList<String>> libraryCompositionConcreteOverhangHash = new HashMap();

        for (Part p : allParts) {
            if (p.getSeq().length() > 29 && p.getSeq().length() < 81) { //parts in this range are hard to construct, and so assign their overhangs first
                lengthBonusSet.add(p.getStringComposition().toString());
            }

            if (encounteredCompositions.contains(p.getStringComposition().toString())) {
                //only consider library parts that apppear in the actual graph
                freeOverhangs.remove(p.getLeftoverhang());
                freeOverhangs.remove(p.getRightOverhang());
                ArrayList<String> overhangsForComposition = compositionConcreteOverhangHash.get(p.getStringComposition().toString());
                ArrayList<String> overhangsForComposition2 = libraryCompositionConcreteOverhangHash.get(p.getStringComposition().toString());
                String overhangString = p.getLeftoverhang() + "|" + p.getRightOverhang();
                if (overhangString.length() > 2) {
                    if (overhangsForComposition != null) {
                        if (!overhangsForComposition.contains(overhangString)) {
                            overhangsForComposition.add(overhangString);
                            overhangsForComposition2.add(overhangString);
                        }
                    } else {
                        overhangsForComposition = new ArrayList<String>();
                        overhangsForComposition2 = new ArrayList<String>();
                        overhangsForComposition2.add(overhangString);
                        overhangsForComposition.add(overhangString);
                        libraryCompositionConcreteOverhangHash.put(p.getStringComposition().toString(), overhangsForComposition2);
                        compositionConcreteOverhangHash.put(p.getStringComposition().toString(), overhangsForComposition);
                    }
                    //count the number of times each concrete overhang pair appears
                    if (concreteOverhangFrequencyHash.containsKey(overhangString)) {
                        concreteOverhangFrequencyHash.put(overhangString, concreteOverhangFrequencyHash.get(overhangString) + 1);
                    } else {
                        concreteOverhangFrequencyHash.put(overhangString, 1);
                    }
                    libraryCompositions.add(p.getStringComposition().toString());
                }
            }
        }
        //vector overhangs are available to all parts as well
        for (String composition : encounteredCompositions) {
            ArrayList<String> concreteOverhangs = compositionConcreteOverhangHash.get(composition);
            if (concreteOverhangs == null) {
                concreteOverhangs = new ArrayList();
                compositionConcreteOverhangHash.put(composition, concreteOverhangs);
            }
            ArrayList<String> levelVectorOverhangs = vectorOverhangHash.get(compositionLevelHash.get(composition));
            if (levelVectorOverhangs != null) {
                for (String vectorPair : levelVectorOverhangs) {
                    concreteOverhangs.add(vectorPair);
                }
            }
        }


        //complete concreteCompositionOverhanghash; parts with the same abstract overhangs should have access to their combined set of concrete overhangs
        for (String abstractOverhangPair : abstractOverhangCompositionHash.keySet()) {
            ArrayList<String> shouldShareCompositions = abstractOverhangCompositionHash.get(abstractOverhangPair);
            ArrayList<String> sharedConcrete = new ArrayList();
            for (String composition : shouldShareCompositions) {
                ArrayList<String> concreteOverhangs = compositionConcreteOverhangHash.get(composition);
                if (concreteOverhangs != null) {
                    for (String concreteOverhangPair : concreteOverhangs) {
                        if (!sharedConcrete.contains(concreteOverhangPair) && concreteOverhangPair.length() > 2) {
                            if (sharedConcrete.size() > 0) {
                                int size = sharedConcrete.size();
                                for (int i = 0; i < size; i++) {
                                    String comparedOverhangPair = sharedConcrete.get(i);
                                    if (concreteOverhangFrequencyHash.get(concreteOverhangPair) >= concreteOverhangFrequencyHash.get(comparedOverhangPair)) {
                                        sharedConcrete.add(i, concreteOverhangPair);
                                        break;
                                    }
                                }
                            } else {
                                sharedConcrete.add(concreteOverhangPair);
                            }
                        }
                    }
                }
                abstractConcreteOptionsHash.put(abstractOverhangPair, sharedConcrete);
            }
        }

        //sort part overhangs combinations according to frequency with a bias towards existing  parts
        ArrayList<String> keyArray = new ArrayList();
        keyArray.addAll(partOverhangFrequencyHash.keySet());
        ArrayList<String> sortedKeys = new ArrayList();
        for (String key : partOverhangFrequencyHash.keySet()) {
            String composition = key.substring(0, key.indexOf("]") + 1);
            int numParts = composition.split(",").length;
            if (libraryCompositions.contains(composition)) {
                partOverhangFrequencyHash.put(key, partOverhangFrequencyHash.get(key) + 1000);
            }
            if (lengthBonusSet.contains(composition)) { //short parts should be considered first
                partOverhangFrequencyHash.put(key, partOverhangFrequencyHash.get(key) + 5); //5 is arbtrary
            }
            partOverhangFrequencyHash.put(key, partOverhangFrequencyHash.get(key) - numParts); //consider lower level parts first
        }


        while (sortedKeys.size() != partOverhangFrequencyHash.keySet().size()) {
            String maxKey = "";
            int maxFreq = -1000000000;
            for (String key : keyArray) {
                if (partOverhangFrequencyHash.get(key) >= maxFreq) {
                    maxFreq = partOverhangFrequencyHash.get(key);
                    maxKey = key;
                }
            }
            sortedKeys.add(maxKey);
            keyArray.remove(maxKey);
        }
        //match abstract overhangs to final overhangs
        for (String partCompositionOverhang : sortedKeys) {
            String[] partTokens = partCompositionOverhang.split("\\|");
            String partComposition = partTokens[0];
            String leftAbstract = partTokens[1];
            String rightAbstract = partTokens[2];
            String abstractOverhangPair = partTokens[1] + "|" + partTokens[2];
            ArrayList<String> libraryOHOptions = abstractConcreteOptionsHash.get(abstractOverhangPair); //pairs of overhangs that exist in library for given part
            String left = ""; //overhang from library that will be assigned with right as a pair if it exists and is available
            String right = ""; //overhang from library that will be assigned with left as a pair if it exists and is available
            //pick a new pair of overhangs
            if (libraryOHOptions != null) {
                //library contains part, check which overhang option is suitable
                String firstLeft = "";
                String firstRight = "";
                boolean partMatch = false;
                for (String option : libraryOHOptions) {
                    String[] tokens = option.split("\\|");
                    assignedOverhangs.remove(finalOverhangHash.get(leftAbstract));
                    assignedOverhangs.remove(finalOverhangHash.get(rightAbstract));
                    if (!assignedOverhangs.contains(tokens[0]) && !assignedOverhangs.contains(tokens[1])) {
                        left = tokens[0];
                        right = tokens[1];
                        if (firstLeft.equals("") && firstRight.equals("") && !firstLeft.equals(firstRight)) {
                            firstLeft = left;
                            firstRight = right;
                        }
                        if (libraryCompositionConcreteOverhangHash.get(partComposition) != null) {
                            if (libraryCompositionConcreteOverhangHash.get(partComposition).contains(left + "|" + right) && !left.equals(right)) {
                                if (!right.equals(finalOverhangHash.get(leftAbstract)) && !left.equals(finalOverhangHash.get(rightAbstract))) { //if the right and left won't be the same
                                    if (finalOverhangHash.get(leftAbstract) == null && finalOverhangHash.get(rightAbstract) == null) { //if either overhang is assigned, pick a pair that matches the assignment
                                        partMatch = true;
                                        break;
                                    } else if (finalOverhangHash.get(leftAbstract) != null) {
                                        if (finalOverhangHash.get(leftAbstract).equals(left)) {
                                            partMatch = true;
                                            break;

                                        }
                                    } else if (finalOverhangHash.get(rightAbstract) != null) {
                                        if (finalOverhangHash.get(rightAbstract).equals(right)) {
                                            partMatch = true;
                                            break;

                                        }

                                    }
                                }
                            }
                        }
                    }
                }
                if (!partMatch) {
                    left = firstLeft;
                    right = firstRight;
                }

            }

            //one of the overhangs is already assigned, assign the other one
            if (finalOverhangHash.get(leftAbstract) == null && left.equals("")) {
                left = freeOverhangs.get(0);
                usedFreeOverhangs.add(left+"*"); //asterisk marks new overhang
                freeOverhangs.remove(0);
            }
            if (finalOverhangHash.get(rightAbstract) == null && right.equals("")) {
                right = freeOverhangs.get(0); 
                usedFreeOverhangs.add(right);
                freeOverhangs.remove(0);
            }

            if (!left.equals("") && (finalOverhangHash.get(leftAbstract) == null || left.equals(finalOverhangHash.get(leftAbstract)))) {
                finalOverhangHash.put(leftAbstract, left);
                assignedOverhangs.add(left);
            }
            if (!right.equals("") && (finalOverhangHash.get(rightAbstract) == null || right.equals(finalOverhangHash.get(rightAbstract)))) {
                finalOverhangHash.put(rightAbstract, right);
                assignedOverhangs.add(right);
            }

            if (!abstractFinalPairHash.containsKey(leftAbstract + "|" + rightAbstract)) {
                abstractFinalPairHash.put(partComposition + "|" + leftAbstract + "|" + rightAbstract, left + "|" + right);
            }
        }

        //iterate through overhang pairs and see if there is a pair that should be replaced with a vector pair
        //first remove all vector pairs that would create a conflict
        for (Integer level : vectorOverhangHash.keySet()) {
            ArrayList<String> levelVectorOverhangs = vectorOverhangHash.get(level);
            ArrayList<String> clone = (ArrayList<String>) levelVectorOverhangs.clone();
            for (String vectorPair : clone) {
                String[] tokens = vectorPair.split("\\|");
                if (assignedOverhangs.contains(tokens[0]) || assignedOverhangs.contains(tokens[1])) {
                    levelVectorOverhangs.remove(vectorPair);
                }
            }

        }
        for (String abstractPair : abstractFinalPairHash.keySet()) {
            String[] tokens = abstractPair.split("\\|");
            ArrayList<String> freeLevelOverhangs = vectorOverhangHash.get(compositionLevelHash.get(tokens[0]));
            String left = finalOverhangHash.get(tokens[1]);
            String right = finalOverhangHash.get(tokens[2]);
            if (freeLevelOverhangs != null) {
                if (freeLevelOverhangs.size() > 0 && usedFreeOverhangs.contains(left) && usedFreeOverhangs.contains(right)) {
                    String[] vectorTokens = freeLevelOverhangs.get(0).split("\\|");
                    finalOverhangHash.put(tokens[0], vectorTokens[0]);
                    finalOverhangHash.put(tokens[1], vectorTokens[1]);
                    freeLevelOverhangs.remove(0);
                }
            }
        }

        //decide what antibiotic resistance goes with each level
        ArrayList<String> freeAntibiotics = new ArrayList(Arrays.asList("ampicillin, kanamycin, neomycin, puromycin, chloramphenicol, spectinomycin, streptomycin".toLowerCase().split(", "))); //overhangs that don't exist in part or vector library
        allVectors = Collector.getAllVectors();
        ArrayList<String> existingAntibiotics = new ArrayList<String>();
        HashMap<Integer, ArrayList<String>> existingAntibioticsHash = new HashMap();
        for (Vector v : allVectors) {
            if (!existingAntibiotics.contains(v.getResistance())) {
                existingAntibiotics.add(v.getResistance());
                if (existingAntibioticsHash.get(v.getLevel()) == null) {
                    existingAntibioticsHash.put(v.getLevel(), new ArrayList());
                }
                if (!existingAntibioticsHash.get(v.getLevel()).contains(v.getResistance())) {
                    existingAntibioticsHash.get(v.getLevel()).add(v.getResistance());
                }
                freeAntibiotics.remove(v.getResistance());
            }
        }
        int maxStage = 0;
        for (SRSGraph graph : optimalGraphs) {
            if (graph.getStages() > maxStage) {
                maxStage = graph.getStages();
            }
        }
        for (int i = 0; i <= maxStage; i++) {
            String resistance = "";
            if (existingAntibioticsHash.get(i) != null) {
                if (existingAntibioticsHash.get(i).size() > 0) {
                    resistance = existingAntibioticsHash.get(i).get(0);
                    existingAntibioticsHash.get(i).remove(0);
                }
            } else {
                resistance = freeAntibiotics.get(0);
                freeAntibiotics.remove(0);
            }
            levelResistanceHash.put(i, resistance);
        }
        //traverse graphs and assign appropriate overhangs and vectors
        for (SRSGraph graph : optimalGraphs) {
            int reactions = 0;
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                current.setLOverhang(finalOverhangHash.get(current.getLOverhang()));
                current.setROverhang(finalOverhangHash.get(current.getROverhang()));
                SRSVector newVector = new SRSVector();
                newVector.setLOverhang(current.getLOverhang());
                newVector.setROverhang(current.getROverhang());
                newVector.setLevel(current.getStage());
                newVector.setStringResistance(levelResistanceHash.get(current.getStage()));
                current.setVector(newVector);
                for (SRSNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
                //count the number of reactions
                ArrayList<String> overhangOptions = libraryCompositionConcreteOverhangHash.get(current.getComposition().toString());
                String overhangString = current.getLOverhang() + "|" + current.getROverhang();
                if (overhangOptions != null) {
                    if (!overhangOptions.contains(overhangString)) {
                        reactions = reactions + 1;
                        overhangOptions.add(overhangString);
                    }
                } else {
                    if (current.getComposition().size() == 1) {
                        reactions = reactions + 1;
                    }
                    ArrayList<String> toAdd = new ArrayList();
                    toAdd.add(overhangString);
                    libraryCompositionConcreteOverhangHash.put(current.getComposition().toString(), toAdd);
                }
                if (!vectorOverhangPairs.contains(overhangString)) {
                    reactions = reactions + 1;
                    vectorOverhangPairs.add(overhangString);
                }
            }


            graph.setReactions(reactions);
        }
    }
    //unbiased assignment of overhangs utilized for testing purposes

    private void crudeOptimize(ArrayList<SRSGraph> optimalGraphs) {
        HashMap<String, String> finalOverhangHash = new HashMap();
        ArrayList<String> freeOverhangs = new ArrayList(Arrays.asList("A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))); //overhangs that don't exist in part or vector library
        for (SRSGraph graph : optimalGraphs) {
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                String left = "";
                String right = "";
                if (finalOverhangHash.containsKey(current.getLOverhang())) {
                    left = finalOverhangHash.get(current.getLOverhang());
                } else {
                    left = freeOverhangs.get(0);
                    finalOverhangHash.put(current.getLOverhang(), left);
                    freeOverhangs.remove(0);
                }
                if (finalOverhangHash.containsKey(current.getROverhang())) {
                    right = finalOverhangHash.get(current.getROverhang());
                } else {
                    right = freeOverhangs.get(0);
                    freeOverhangs.remove(0);
                    finalOverhangHash.put(current.getROverhang(), right);
                }
                current.setLOverhang(left);
                current.setROverhang(right);
                seenNodes.add(current);

                ArrayList<SRSNode> neighbors = current.getNeighbors();
                for (SRSNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);

                    }
                }
            }

        }

    }

    //sets user specified overhangs before algorithm computes the rest
    private HashMap<String, String> preAssignOverhangs(ArrayList<SRSGraph> optimalGraphs) {
        HashMap<String, String> toReturn = new HashMap(); //precursor for the finalOverhangHash used in the optimizeOverhangVectors method
        for (SRSGraph graph : optimalGraphs) {
            SRSNode root = graph.getRootNode();
            if (forcedOverhangHash.containsKey(root.getComposition().toString())) {
                //traverse the graph and find all of the basic parts and then put them in order
                ArrayList<SRSNode> queue = new ArrayList();
                HashSet<SRSNode> seenNodes = new HashSet();
                ArrayList<SRSNode> basicParts = new ArrayList();
                queue.add(root);
                while (!queue.isEmpty()) {
                    SRSNode current = queue.get(0);
                    queue.remove(0);
                    seenNodes.add(current);
                    if (current.getNeighbors().size() == 1) {
                        basicParts.add(current);
                    }
                    for (SRSNode neighbor : current.getNeighbors()) {
                        if (!seenNodes.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
                ArrayList<String> forcedOverhangs = forcedOverhangHash.get(root.getComposition().toString());
                while (!forcedOverhangs.isEmpty()) {
                    String[] forcedTokens = forcedOverhangs.get(0).split("\\|");
                    int composition = Integer.parseInt(forcedTokens[0]);
                    String forcedLeft = forcedTokens[1];
                    String forcedRight = forcedTokens[2];
                    for (int i = 0; i < basicParts.size(); i++) {
                        SRSNode basicNode = basicParts.get(i);
                        String nodeCompositionString = basicNode.getComposition().toString();
                        if (i == composition) {
                            toReturn.put(basicNode.getLOverhang(), forcedLeft);
                            toReturn.put(basicNode.getROverhang(), forcedRight);
                            forcedOverhangs.remove(0);
                            break;
                        }

                    }
                }
            }
        }

        return toReturn;
    }
    HashMap<String, ArrayList<String>> abstractOverhangCompositionHash; //key: overhangs delimited by "|", value: compositions with overhangs indicated by keys
    HashMap<String, Integer> partOverhangFrequencyHash; //key: part composition concatenated with abstract overhang with "_" delimited with "|", value: number of occurences of part with given overhangs
    HashSet<String> encounteredCompositions; //set of part compositions that appear in the set of all graphs
    HashMap<SRSNode, SRSNode> parentHash; //key: node, value: parent node
    HashMap<String, ArrayList<SRSNode>> compositionNodeHash; //key: string composition, value; arrayList of nodes with the given composition
    HashMap<String, ArrayList<String>> forcedOverhangHash; //key: composite part composition
    HashMap<String, Integer> compositionLevelHash; //key: composition, value: level

    public void setForcedOverhangs(HashMap<String, ArrayList<String>> requiredOverhangs) {
        forcedOverhangHash = new HashMap();
        for (String key : requiredOverhangs.keySet()) {
            Part part = Collector.getPartByName(key);
            forcedOverhangHash.put(part.getStringComposition().toString(), requiredOverhangs.get(key));
        }
    }
    public String generateInstructions(ArrayList<SRSGraph> graphs) {
        String toReturn = "";
        String header = "You are trying to assemble the following goal parts:\n";
        for(SRSGraph graph: graphs) {
            header = header+"*"+graph.getRootNode().getName()+"\n";
            
        }
        for(Part p:partLibrary) {
            
        }
        toReturn = header+"\n"+toReturn;
        return toReturn;
    }
   
}
