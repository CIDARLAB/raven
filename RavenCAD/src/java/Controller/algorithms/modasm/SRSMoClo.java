/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.modasm;

import Controller.algorithms.SRSGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class SRSMoClo extends SRSGeneral {

    /**
     * Clotho part wrapper for sequence dependent one pot reactions *
     */
    public ArrayList<SRSGraph> mocloClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies) {
        try {
            _vectorLibrary = vectorLibrary;
            _partsLibrary = partLibrary;
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
            basicOverhangAssignment(optimalGraphs);
            boolean valid = validateGraphs(optimalGraphs);
            System.out.println("##############################\nfirst pass: " + valid);
            minimizeOverhangs(optimalGraphs);
            valid = validateGraphs(optimalGraphs);
            System.out.println("##############################\nsecond pass: " + valid);
            optimizeOverhangVectors(optimalGraphs, partHash, vectorSet);
            valid = validateGraphs(optimalGraphs);
            System.out.println("##############################\nfinal pass: " + valid);

            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
            E.printStackTrace();
            return blank;
        }
    }
    //assign overhangs ignoring the library of parts and vectors; overhangs are saved to graph nodes not part/vectors

    private void basicOverhangAssignment(ArrayList<SRSGraph> optimalGraphs) {
        encounteredCompositions = new HashSet();
        parentHash = new HashMap(); //key: node, value: parent node
        HashMap<SRSNode, SRSNode> previousHash = new HashMap(); //key: node, value: sibling node on the "left"
        HashMap<SRSNode, SRSNode> nextHash = new HashMap(); //key: node, value: sibling node on the "right"
        compositionLevelHash = new HashMap();
        rootBasicNodeHash = new HashMap();
        for (SRSGraph graph : optimalGraphs) {
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            SRSNode root = graph.getRootNode();
            queue.add(root);
            parentHash.put(root, null);
            previousHash.put(root, null);
            nextHash.put(root, null);
            ArrayList<SRSNode> basic = new ArrayList();
            rootBasicNodeHash.put(root, basic);
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                current.setLOverhang("");
                current.setROverhang("");
                seenNodes.add(current);
                ArrayList<SRSNode> neighbors = current.getNeighbors();
                SRSNode previous = null;
                encounteredCompositions.add(current.getComposition().toString());
                for (SRSNode neighbor : neighbors) {
                    if (neighbor.getComposition().size() == 1) {
                        basic.add(neighbor);
                    }
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        parentHash.put(neighbor, current);
                        previousHash.put(neighbor, previous);
                        if (previous != null) {
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
            String randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
            root.setLOverhang(randIndex);
            randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
            root.setROverhang(randIndex);
            ArrayList<String> toAdd = new ArrayList();
            toAdd.add(root.getLOverhang() + "|" + root.getROverhang());


            while (!queue.isEmpty()) {
                SRSNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);
                if (parent.getNeighbors().size() > 0) {
                    SRSNode previousNode;
                    SRSNode nextNode;
                    HashSet<String> neighborConflictSet = neighborConflictHash.get(parent);
                    neighborConflictSet.add(parent.getLOverhang());
                    neighborConflictSet.add(parent.getROverhang());

                    for (SRSNode currentNode : parent.getNeighbors()) {
                        if (!seenNodes.contains(currentNode)) {
                            previousNode = previousHash.get(currentNode);
                            nextNode = nextHash.get(currentNode);
                            Boolean seenFirst = true; //seen the first beighbor
                            Boolean seenLast = false; //seen the last neighbor
                            if (previousNode == null) {
                                seenFirst = false;
                            }
                            if (nextNode == null) {
                                seenLast = true;
                            }
                            //usual cases for assigning left overhang
                            if (currentNode.getLOverhang().equals("")) {
                                if (!seenFirst) {
                                    //first part automatically gets left overhang of parent
                                    currentNode.setLOverhang(parent.getLOverhang());
                                } else {
                                    randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
                                    currentNode.setLOverhang(randIndex);
                                }
                            }
                            //usual cases for assigning right overhang
                            if (currentNode.getROverhang().equals("")) {
                                if (seenLast) {
                                    // last node gets right overhang of parent
                                    currentNode.setROverhang(parent.getROverhang());
                                } else {
                                    randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
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
//        HashSet<String> hasConcreteOptionsSet = new HashSet(); //hashset of all compositions with concrete overhangs in library
        HashMap<String, ArrayList<String>> reservedAbstractHash = new HashMap(); //key: string composition, value: arrayList of abstract overhangs 'reserved' for that composition
        HashMap<String, String> numberToLetterOverhangHash = new HashMap(); //replaces the abstract numerical with abstract letter overhangs; key: abstract numerical overhang, value: abstract letter overhang
        ArrayList<String> allOverhangs = new ArrayList(Arrays.asList("A_,B_,C_,D_,E_,G_,H_,I_,J_,K_,L_,M_,N_,O_,P_,Q_,R_,S_,T_,U_,V_,W_,X_,Y_,Z_,a_,b_,c_,d_,e_,f_,g_,h_,i_,j_,k_,l_,m_,n_,o_,p_,q_,r_,s_,t_,u_,v_,w_,x_,y_,z_".split(","))); //overhangs that don't exist in part or vector library
        for (SRSGraph graph : optimalGraphs) {
            for (SRSNode currentNode : rootBasicNodeHash.get(graph.getRootNode())) {
                ArrayList<String> freeOverhangs = (ArrayList<String>) allOverhangs.clone();
                ArrayList<String> reservedOverhangs = reservedAbstractHash.get(currentNode.getComposition().toString());
                if (reservedOverhangs != null) {
                    freeOverhangs.addAll(reservedOverhangs);
                    Collections.sort(freeOverhangs);
                } else {
                    reservedOverhangs = new ArrayList();
                    reservedAbstractHash.put(currentNode.getComposition().toString(), reservedOverhangs);
                }
                if (!numberToLetterOverhangHash.containsKey(currentNode.getLOverhang())) {
                    String newOverhang = freeOverhangs.get(0);
                    int counter = 1;
                    while (newOverhang.equals(numberToLetterOverhangHash.get(currentNode.getLOverhang()))) {
                        newOverhang = freeOverhangs.get(counter);
                        counter = counter + 1;
                    }
                    numberToLetterOverhangHash.put(currentNode.getLOverhang(), newOverhang);
                    allOverhangs.remove(newOverhang);
                    if (!reservedOverhangs.contains(newOverhang)) {
                        reservedOverhangs.add(newOverhang);
                    }
                    freeOverhangs.remove(newOverhang);
                }
                if (!numberToLetterOverhangHash.containsKey(currentNode.getROverhang())) {
                    String newOverhang = freeOverhangs.get(0);
                    int counter = 1;
                    while (newOverhang.equals(numberToLetterOverhangHash.get(currentNode.getLOverhang()))) {
                        newOverhang = freeOverhangs.get(counter);
                        counter = counter + 1;
                    }
                    numberToLetterOverhangHash.put(currentNode.getROverhang(), newOverhang);
                    allOverhangs.remove(newOverhang);
                    if (!reservedOverhangs.contains(newOverhang)) {
                        reservedOverhangs.add(newOverhang);
                    }
                    freeOverhangs.remove(newOverhang);
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
                compositionLevelHash.put(currentNode.getComposition() + "|" + currentNode.getROverhang() + "|" + currentNode.getLOverhang(), currentNode.getStage());
                seenNodes.add(currentNode);
                ArrayList<SRSNode> neighbors = currentNode.getNeighbors();
                for (SRSNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);

                    }
                }
                //count instances of each part overhang combination, which will be used for optimization later
                if (currentNode.getComposition().size() == 1) {
                    String partOverhangName = currentNode.getComposition() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang();
                    if (partOverhangFrequencyHash.get(partOverhangName) != null) {
                        partOverhangFrequencyHash.put(partOverhangName, partOverhangFrequencyHash.get(partOverhangName) + 1);
                    } else {
                        partOverhangFrequencyHash.put(partOverhangName, 1);
                    }
                }
                //keep track of which compositions appear with each abstract overhang pair; used in later optimization steps
                //track only basic nodes
                if (currentNode.getComposition().size() == 1) {
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
        ArrayList<String> freeOverhangs = new ArrayList(Arrays.asList("A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))); //overhangs that don't exist in part or vector library
        HashMap<Integer, String> levelResistanceHash = new HashMap(); // key: level, value: antibiotic resistance
        HashMap<String, Integer> concreteOverhangFrequencyHash = new HashMap(); //key: concrete overhang pair, value: how often does that overhang pair appear
        HashMap<String, ArrayList<String>> abstractConcreteOptionsHash = new HashMap(); //key: abstract overhang pair, value: ArrayList<String> of concrete overhang pairs
        HashSet<String> assignedOverhangs = new HashSet<String>(); //all the overhangs that will appear in the actual output
        assignedOverhangs.addAll(finalOverhangHash.values());
        freeOverhangs.removeAll(finalOverhangHash.values());
        HashSet<String> vectorOverhangPairs = new HashSet();

        //gather all overhangs for existing vectors
        HashMap<Integer, ArrayList<String>> vectorOverhangHash = new HashMap(); //key: level, value: overhangs for that level
        ArrayList<Vector> allVectors = _vectorLibrary;
        for (Vector v : allVectors) {
//            freeOverhangs.remove(v.getLeftoverhang());
//            freeOverhangs.remove(v.getRightOverhang());
            String overhangString = v.getLeftoverhang() + "|" + v.getRightOverhang();
            vectorOverhangPairs.add(overhangString);
            ArrayList<String> levelOverhangs = vectorOverhangHash.get(v.getLevel());
            if (levelOverhangs != null) {
                if (!levelOverhangs.contains(overhangString)) {
                    levelOverhangs.add(overhangString);
                }
            } else {
                levelOverhangs = new ArrayList();
                levelOverhangs.add(overhangString);
                vectorOverhangHash.put(v.getLevel(), levelOverhangs);
            }
            if (concreteOverhangFrequencyHash.containsKey(overhangString)) {
                concreteOverhangFrequencyHash.put(overhangString, concreteOverhangFrequencyHash.get(overhangString) + 1);
            } else {
                concreteOverhangFrequencyHash.put(overhangString, 1);
            }
        }

        //gather all overhangs for existing parts
        ArrayList<Part> allParts = _partsLibrary;
        HashMap<String, ArrayList<String>> compositionConcreteOverhangHash = new HashMap(); //key: part composition, value: arrayList of concrete overhangs that appear for that compositiong
        for (Part p : allParts) {
            if (p.getLeftoverhang().length() > 0 && p.getRightOverhang().length() > 0) {
                String composition = p.getStringComposition().toString();
                if (encounteredCompositions.contains(composition)) {
                    freeOverhangs.remove(p.getLeftoverhang());
                    freeOverhangs.remove(p.getRightOverhang());
                    if (compositionConcreteOverhangHash.get(composition) == null) {
                        ArrayList<String> toAdd = new ArrayList();
                        compositionConcreteOverhangHash.put(composition, toAdd);
                    }
                    ArrayList<String> concrete = compositionConcreteOverhangHash.get(composition);
                    String concretePair = p.getLeftoverhang() + "|" + p.getRightOverhang();
                    if (!concrete.contains(concretePair)) {
                        concrete.add(concretePair);
                    }
                    //count the number of times each concrete overhang pair appears
                    if (concreteOverhangFrequencyHash.containsKey(concretePair)) {
                        concreteOverhangFrequencyHash.put(concretePair, concreteOverhangFrequencyHash.get(concretePair) + 1);
                    } else {
                        concreteOverhangFrequencyHash.put(concretePair, 1);
                    }
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
            if (compositionConcreteOverhangHash.get(composition) != null) {
                if (compositionConcreteOverhangHash.get(composition).size() > 0) {
                    partOverhangFrequencyHash.put(key, partOverhangFrequencyHash.get(key) + 1000);
                }
            } else {
                partOverhangFrequencyHash.put(key, partOverhangFrequencyHash.get(key));
            }
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
                        if (compositionConcreteOverhangHash.get(partComposition) != null) {
                            if (compositionConcreteOverhangHash.get(partComposition).contains(left + "|" + right) && !left.equals(right)) {
                                if (!right.equals(finalOverhangHash.get(leftAbstract)) && !left.equals(finalOverhangHash.get(rightAbstract)) && !left.equals(right)) { //if the right and left won't be the same
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

            //one of the overhangs is already assigned, assign the other one randomly
            if (finalOverhangHash.get(leftAbstract) == null && left.equals("")) {
                left = freeOverhangs.get(0);
                usedFreeOverhangs.add(left);
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
            if(!libraryOHOptions.contains(finalOverhangHash.get(leftAbstract) + "|" + finalOverhangHash.get(rightAbstract))) {
                libraryOHOptions.add(finalOverhangHash.get(leftAbstract) + "|" + finalOverhangHash.get(rightAbstract));
            }
            System.out.println(partComposition + " abstract " + leftAbstract + "|" + rightAbstract + " final " + finalOverhangHash.get(leftAbstract) + "|" + finalOverhangHash.get(rightAbstract) + " options " + libraryOHOptions);
        }

        //decide what antibiotic resistance goes with each level
        ArrayList<String> freeAntibiotics = new ArrayList(Arrays.asList("ampicillin, kanamycin, neomycin, puromycin, chloramphenicol, spectinomycin, streptomycin".toLowerCase().split(", "))); //overhangs that don't exist in part or vector library
        allVectors = _vectorLibrary;
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
                ArrayList<String> overhangOptions = compositionConcreteOverhangHash.get(current.getComposition().toString());
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
                    compositionConcreteOverhangHash.put(current.getComposition().toString(), toAdd);
                }
                if (!vectorOverhangPairs.contains(overhangString)) {
                    reactions = reactions + 1;
                    vectorOverhangPairs.add(overhangString);
                }
            }


            graph.setReactions(reactions);
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

    public void setForcedOverhangs(Collector coll, HashMap<String, ArrayList<String>> requiredOverhangs) {
        forcedOverhangHash = new HashMap();
        for (String key : requiredOverhangs.keySet()) {
            Part part = coll.getPartByName(key);
            
            forcedOverhangHash.put(part.getStringComposition().toString(), requiredOverhangs.get(key));
        }
    }

    private boolean validateGraphs(ArrayList<SRSGraph> graphs) {
        boolean toReturn = true;
        for (SRSGraph graph : graphs) {
            SRSNode root = graph.getRootNode();
            HashSet<SRSNode> seenNodes = new HashSet();
            ArrayList<SRSNode> queue = new ArrayList();
            queue.add(root);
            while (!queue.isEmpty()) {
                SRSNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);
//                System.out.println(parent.getComposition() + parent.getLOverhang() + "|" + parent.getROverhang());
                if (parent.getLOverhang().equals(parent.getROverhang())) {
                    System.out.println("parent failed: " + parent.getComposition() + " " + parent.getLOverhang() + "|" + parent.getROverhang());
                    return false;
                }
                if (parent.getNeighbors().size() > 1) {
                    SRSNode previous = null;
                    for (int i = 0; i < parent.getNeighbors().size(); i++) {
                        SRSNode child = parent.getNeighbors().get(i);
                        if (!seenNodes.contains(child)) {
                            if (i == 0) {
                                if (!child.getLOverhang().equals(parent.getLOverhang())) {
                                    System.out.println(child.getComposition() + " left caused failure " + child.getLOverhang());
                                    System.out.println("parent: " + parent.getComposition() + " " + parent.getLOverhang() + "|" + parent.getROverhang());
                                    return false;
                                }
                            }
                            if (i == parent.getNeighbors().size() - 1) {
                                if (!child.getROverhang().equals(parent.getROverhang())) {
                                    System.out.println(child.getComposition() + " right caused failure " + child.getROverhang());
                                    System.out.println("parent: " + parent.getComposition() + " " + parent.getLOverhang() + "|" + parent.getROverhang());
                                    return false;
                                }
                            }
                            if (previous != null) {
                                if (!child.getLOverhang().equals(previous.getROverhang())) {
                                    System.out.println(child.getComposition() + " previous caused failure " + child.getLOverhang());
                                    System.out.println("previous: " + previous.getComposition() + " " + previous.getLOverhang() + "|" + previous.getROverhang());

                                    return false;
                                }
                            }

                            previous = child;
                            queue.add(child);
                        }
                    }

                }
            }
        }
        return toReturn;
    }
    private HashMap<String, ArrayList<String>> abstractOverhangCompositionHash; //key: overhangs delimited by "|", value: compositions with overhangs indicated by keys
    private HashMap<String, Integer> partOverhangFrequencyHash; //key: part composition concatenated with abstract overhang with "_" delimited with "|", value: number of occurences of part with given overhangs
    private HashSet<String> encounteredCompositions; //set of part compositions that appear in the set of all graphs
    private HashMap<SRSNode, SRSNode> parentHash; //key: node, value: parent node
    private HashMap<String, Integer> compositionLevelHash; //key: string composition with overhangs, value; arrayList of nodes with the given composition
    private HashMap<String, ArrayList<String>> forcedOverhangHash; //key: composite part composition
    private HashMap<SRSNode, ArrayList<SRSNode>> rootBasicNodeHash; //key: root node, value: ordered arrayList of basic nodes in graph that root node belongs to
    private ArrayList<Part> _partsLibrary = new ArrayList();
    private ArrayList<Vector> _vectorLibrary = new ArrayList();

}
