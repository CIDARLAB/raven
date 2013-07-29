/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.modasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.PrimerDesign;
import Controller.algorithms.RGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jenhantao
 */
public class RMoClo extends RGeneral {

    /**
     * Clotho part wrapper for sequence dependent one pot reactions *
     */
    public ArrayList<RGraph> mocloClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies, ArrayList<Double> costs) {
        try {
            _partLibrary = partLibrary;
            _vectorLibrary = vectorLibrary;
            if (_partLibrary == null) {
                _partLibrary = new ArrayList();
            }
            if (_vectorLibrary == null) {
                _vectorLibrary = new ArrayList();
            }

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
            HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, required, recommended); //key: composiion, direction || value: library graph
            ArrayList<RVector> vectorSet = ClothoReader.vectorImportClotho(vectorLibrary);

            //Put all parts into hash for mgp algorithm            
            ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalParts);

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
            ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, discouraged, partHash, positionScores, efficiencies, true);
            boolean tryCartesian = true;
            basicOverhangAssignment(optimalGraphs);
            boolean valid = validateOverhangs(optimalGraphs);
            System.out.println("##############################\nfirst pass: " + valid);

            HashMap<String, String> finalOverhangHash = new HashMap();
            if (tryCartesian) {
                //gather some info
                ArrayList<RGraph> nonCartesianGraphs = new ArrayList(); //graphs without valid cartesian products
                HashMap<String, ArrayList<String>> availableOverhangs = new HashMap(); //key: composition, value: arrayList containing available overhangs
                for (Part p : _partLibrary) {
                    if (p.getLeftOverhang().length() > 0 && p.getRightOverhang().length() > 0) {
                        String composition = p.getStringComposition().toString();
                        if (_encounteredCompositions.contains(composition)) {
                            ArrayList<String> existingOverhangs = availableOverhangs.get(composition);
                            if (existingOverhangs != null) {
                                existingOverhangs.add(p.getLeftOverhang() + "|" + p.getRightOverhang());
                            } else {
                                availableOverhangs.put(composition, new ArrayList(Arrays.asList(new String[]{p.getLeftOverhang() + "|" + p.getRightOverhang()}))); //create new array list
                            }
                        }
                    }
                }
                HashMap<String, ArrayList<String>> cartesianOverhangs = new HashMap();
                for (RGraph graph : optimalGraphs) {
                    RNode root = graph.getRootNode();
                    ArrayList<RNode> composition = _rootBasicNodeHash.get(root);
                    //use cartesian product methods to find an assignment
                    ArrayList<ArrayList<String>> optimalAssignments = findOptimalAssignment(buildCartesianGraph(composition, availableOverhangs), composition.size());
                    //iterate through each cartesian assignment and see if they are valid
                    if (optimalAssignments.size() > 0) {
                        for (ArrayList<String> cartesianAssignment : optimalAssignments) {
                            cartesianOverhangs.put(root.getComposition().toString(), cartesianAssignment);
                            HashMap<String, String> graphOverhangAssignment = assignOverhangs(optimalGraphs, cartesianOverhangs);
                            //force overhangs each time
                            graphOverhangAssignment.putAll(assignOverhangs(optimalGraphs, _forcedOverhangHash));
                            //traverse graph and assign overhangs
                            ArrayList<RNode> queue = new ArrayList<RNode>();
                            HashSet<RNode> seenNodes = new HashSet();
                            queue.add(graph.getRootNode());
                            while (!queue.isEmpty()) {
                                RNode current = queue.get(0);
                                queue.remove(0);
                                seenNodes.add(current);
                                current.setLOverhang(graphOverhangAssignment.get(current.getLOverhang()));
                                current.setROverhang(graphOverhangAssignment.get(current.getROverhang()));
                                for (RNode neighbor : current.getNeighbors()) {
                                    if (!seenNodes.contains(neighbor)) {
                                        queue.add(neighbor);
                                    }
                                }
                            }
                            //if graph is valid, no need to check other cartesian assignments for graph
                            //otherwise try another assignment
                            if (validateOverhangs(optimalGraphs)) {
                                finalOverhangHash.putAll(graphOverhangAssignment);
                                break;
                            }
                            if (optimalAssignments.indexOf(cartesianAssignment) == optimalAssignments.size() - 1) {
                                //if no cartesian assignments are valid, we have to do things the old fashioned way
                                nonCartesianGraphs.add(graph);
                            }
                        }
                    } else {
                        nonCartesianGraphs.add(graph);
                        //if no cartesian product then do things the old fashioned way
                    }

                }
//                finalOverhangHash.putAll(assignOverhangs(optimalGraphs, _forcedOverhangHash));
                //regular asssignment for graphs with no cartesian assignment
                minimizeOverhangs(nonCartesianGraphs);
//                optimizeOverhangVectors(nonCartesianGraphs, partHash, vectorSet, finalOverhangHash);
                optimizeOverhangVectors(nonCartesianGraphs, partHash, vectorSet, finalOverhangHash);
            } else {
                //if we're not doing the cartesian product or the cartesian products are wrong
                minimizeOverhangs(optimalGraphs);
                valid = validateOverhangs(optimalGraphs);
                System.out.println("##############################\nsecond pass: " + valid);
                finalOverhangHash = assignOverhangs(optimalGraphs, _forcedOverhangHash);
                optimizeOverhangVectors(optimalGraphs, partHash, vectorSet, finalOverhangHash);
                valid = validateOverhangs(optimalGraphs);
                System.out.println("##############################\nfinal pass: " + valid);
            }
            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<RGraph> blank = new ArrayList<RGraph>();
            E.printStackTrace();
            return blank;
        }
    }

    /**
     * Assign overhangs ignoring the library of parts and vectors; overhangs are
     * saved to graph nodes not part/vectors *
     */
    private void basicOverhangAssignment(ArrayList<RGraph> optimalGraphs) {

        _encounteredCompositions = new HashSet();
        _parentHash = new HashMap(); //key: node, value: parent node
        HashMap<RNode, RNode> previousHash = new HashMap(); //key: node, value: sibling node on the "left"
        HashMap<RNode, RNode> nextHash = new HashMap(); //key: node, value: sibling node on the "right"
        _compositionLevelHash = new HashMap();
        _rootBasicNodeHash = new HashMap();

        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            RNode root = graph.getRootNode();
            queue.add(root);
            _parentHash.put(root, null);
            previousHash.put(root, null);
            nextHash.put(root, null);
            ArrayList<RNode> basic = new ArrayList();
            _rootBasicNodeHash.put(root, basic);

            //Traverse the graph
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                current.setLOverhang("");
                current.setROverhang("");
                seenNodes.add(current);
                ArrayList<RNode> neighbors = current.getNeighbors();
                RNode previous = null;
                _encounteredCompositions.add(current.getComposition().toString());

                for (RNode neighbor : neighbors) {

                    if (neighbor.getStage() == 0) {
                        basic.add(neighbor);
                    }

                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        _parentHash.put(neighbor, current);
                        previousHash.put(neighbor, previous);

                        if (previous != null) {
                            nextHash.put(previous, neighbor);
                        }
                        previous = neighbor;
                    }
                }
            }

        }

        for (RGraph graph : optimalGraphs) {

            HashMap<RNode, HashSet<String>> neighborConflictHash = new HashMap();
            RNode root = graph.getRootNode();
            neighborConflictHash.put(root, new HashSet());
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList<RNode>();
            queue.add(root);

            String randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
            root.setLOverhang(randIndex);
            randIndex = String.valueOf((int) (Math.random() * ((1000000000 - 1) + 1)));
            root.setROverhang(randIndex);
            ArrayList<String> toAdd = new ArrayList();
            toAdd.add(root.getLOverhang() + "|" + root.getROverhang());

            //Travere the graph
            while (!queue.isEmpty()) {
                RNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);

                if (parent.getNeighbors().size() > 0) {
                    RNode previousNode;
                    RNode nextNode;
                    HashSet<String> neighborConflictSet = neighborConflictHash.get(parent);
                    neighborConflictSet.add(parent.getLOverhang());
                    neighborConflictSet.add(parent.getROverhang());

                    for (RNode currentNode : parent.getNeighbors()) {
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

    private void minimizeOverhangs(ArrayList<RGraph> optimalGraphs) {
        _abstractOverhangCompositionHash = new HashMap();
        _partOverhangFrequencyHash = new HashMap();
        HashMap<String, ArrayList<String>> reservedLeftAbstractHash = new HashMap(); //key: string composition, value: arrayList of abstract overhangs 'reserved' for that composition
        HashMap<String, ArrayList<String>> reservedRightAbstractHash = new HashMap(); //key: string composition, value: arrayList of abstract overhangs 'reserved' for that composition

        HashMap<String, String> numberToLetterOverhangHash = new HashMap(); //replaces the abstract numerical with abstract letter overhangs; key: abstract numerical overhang, value: abstract letter overhang
        ArrayList<String> allOverhangs = new ArrayList(Arrays.asList("A_,B_,C_,D_,E_,G_,H_,I_,J_,K_,L_,M_,N_,O_,P_,Q_,R_,S_,T_,U_,V_,W_,X_,Y_,Z_,a_,b_,c_,d_,e_,f_,g_,h_,i_,j_,k_,l_,m_,n_,o_,p_,q_,r_,s_,t_,u_,v_,w_,x_,y_,z_".split(","))); //overhangs that don't exist in part or vector library
        //aa_,ba_,ca_,da_,ea_,fa_,ga_,ha_,ia_,ja_,ka_,la_,ma_,na_,oa_,pa_,qa_,ra_,sa_,ta_,ua_,va_,wa_,xa_,ya_,za_,ab_,bb_,cb_,db_,eb_,fb_,gb_,hb_,ib_,jb_,kb_,lb_,mb_,nb_,ob_,pb_,qb_,rb_,sb_,tb_,ub_,vb_,wb_,xb_,yb_,zb_

        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> compositionNodes = _rootBasicNodeHash.get(graph.getRootNode());

            for (RNode currentNode : compositionNodes) {
                ArrayList<String> freeLeftOverhangs = (ArrayList<String>) allOverhangs.clone();
                ArrayList<String> freeRightOverhangs = (ArrayList<String>) allOverhangs.clone();
                ArrayList<String> reservedLeftOverhangs = reservedLeftAbstractHash.get(currentNode.getType().toString().toLowerCase());
                ArrayList<String> reservedRightOverhangs = reservedRightAbstractHash.get(currentNode.getType().toString().toLowerCase());
                RNode parent = _parentHash.get(currentNode);

                if (reservedLeftOverhangs != null) {
                    freeLeftOverhangs.addAll(reservedLeftOverhangs);
                    Collections.sort(freeLeftOverhangs);
                } else {
                    reservedLeftOverhangs = new ArrayList();
                    reservedLeftAbstractHash.put(currentNode.getType().toString().toLowerCase(), reservedLeftOverhangs);
                }

                if (reservedRightOverhangs != null) {
                    freeRightOverhangs.addAll(reservedRightOverhangs);
                    Collections.sort(freeRightOverhangs);
                } else {
                    reservedRightOverhangs = new ArrayList();
                    reservedRightAbstractHash.put(currentNode.getType().toString().toLowerCase(), reservedRightOverhangs);
                }

                for (RNode node : parent.getNeighbors()) {
                    if (node.getComposition().toString().length() < parent.getComposition().toString().length()) {
                        freeLeftOverhangs.remove(numberToLetterOverhangHash.get(node.getLOverhang()));
                        freeLeftOverhangs.remove(numberToLetterOverhangHash.get(node.getROverhang()));
                        freeRightOverhangs.remove(numberToLetterOverhangHash.get(node.getLOverhang()));
                        freeRightOverhangs.remove(numberToLetterOverhangHash.get(node.getROverhang()));
                    }

                }

                int partIndex = -1;
                ArrayList<String> parentNeighbors = parent.getComposition();

                for (int i = 0; i < parentNeighbors.size(); i++) {
                    String compositionString = currentNode.getName();

                    if (compositionString.equals(parentNeighbors.get(i))) {
                        partIndex = i;
                    }
                }

                if (partIndex == 0 || partIndex == parentNeighbors.size() - 1) {
                    RNode grandParent = _parentHash.get(parent);
                    if (grandParent != null) {
                        for (RNode uncle : grandParent.getNeighbors()) {
                            freeLeftOverhangs.remove(numberToLetterOverhangHash.get(uncle.getLOverhang()));
                            freeLeftOverhangs.remove(numberToLetterOverhangHash.get(uncle.getROverhang()));
                            freeRightOverhangs.remove(numberToLetterOverhangHash.get(uncle.getLOverhang()));
                            freeRightOverhangs.remove(numberToLetterOverhangHash.get(uncle.getROverhang()));
                        }
                    }
                }

                //assign left overhang
                if (!numberToLetterOverhangHash.containsKey(currentNode.getLOverhang())) {
                    String newOverhang = freeLeftOverhangs.get(0);
                    int counter = 1;

                    while (newOverhang.equals(numberToLetterOverhangHash.get(currentNode.getROverhang()))
                            || newOverhang.equals(numberToLetterOverhangHash.get(parent.getROverhang()))) {
                        newOverhang = freeLeftOverhangs.get(counter);
                        counter = counter + 1;
                    }

                    numberToLetterOverhangHash.put(currentNode.getLOverhang(), newOverhang);
                    allOverhangs.remove(newOverhang);

                    if (!reservedLeftOverhangs.contains(newOverhang) && !reservedRightOverhangs.contains(newOverhang)) {
                        reservedLeftOverhangs.add(newOverhang);
                    }
                    freeLeftOverhangs.remove(newOverhang);
                    freeRightOverhangs.remove(newOverhang);
                }

                //assign right overhang
                if (!numberToLetterOverhangHash.containsKey(currentNode.getROverhang())) {
                    String newOverhang = freeRightOverhangs.get(0);
                    int counter = 1;

                    while (newOverhang.equals(numberToLetterOverhangHash.get(currentNode.getLOverhang()))
                            || newOverhang.equals(numberToLetterOverhangHash.get(parent.getLOverhang()))) {
                        newOverhang = freeRightOverhangs.get(counter);
                        counter = counter + 1;
                    }
                    numberToLetterOverhangHash.put(currentNode.getROverhang(), newOverhang);
                    allOverhangs.remove(newOverhang);

                    if (!reservedLeftOverhangs.contains(newOverhang) && !reservedRightOverhangs.contains(newOverhang)) {
                        reservedRightOverhangs.add(newOverhang);
                    }
                    freeRightOverhangs.remove(newOverhang);
                    freeLeftOverhangs.remove(newOverhang);
                }
            }
        }

        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            queue.add(graph.getRootNode());

            //traverse graph and assign abstract letter overhangs
            while (!queue.isEmpty()) {
                RNode currentNode = queue.get(0);
                queue.remove(0);
                currentNode.setLOverhang(numberToLetterOverhangHash.get(currentNode.getLOverhang()));
                currentNode.setROverhang(numberToLetterOverhangHash.get(currentNode.getROverhang()));
                _compositionLevelHash.put(currentNode.getComposition() + "|" + currentNode.getROverhang() + "|" + currentNode.getLOverhang(), currentNode.getStage());
                seenNodes.add(currentNode);
                ArrayList<RNode> neighbors = currentNode.getNeighbors();

                for (RNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);
                    }
                }

                //count instances of each part overhang combination, which will be used for optimization later
                if (currentNode.getComposition().size() == 1) {
                    String partOverhangName = currentNode.getComposition() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang();
                    if (_partOverhangFrequencyHash.get(partOverhangName) != null) {
                        _partOverhangFrequencyHash.put(partOverhangName, _partOverhangFrequencyHash.get(partOverhangName) + 1);
                    } else {
                        _partOverhangFrequencyHash.put(partOverhangName, 1);
                    }
                }

                //keep track of which compositions appear with each abstract overhang pair; used in later optimization steps
                //track only basic nodes
                if (currentNode.getComposition().size() == 1) {
                    String overhangPair = currentNode.getLOverhang() + "|" + currentNode.getROverhang();
                    ArrayList<String> existingCompositions = _abstractOverhangCompositionHash.get(overhangPair);
                    if (existingCompositions != null) {
                        if (!existingCompositions.contains(currentNode.getComposition().toString())) {
                            existingCompositions.add(currentNode.getComposition().toString());
                        }
                    } else {
                        ArrayList<String> toAdd = new ArrayList();
                        toAdd.add(currentNode.getComposition().toString());
                        _abstractOverhangCompositionHash.put(overhangPair, toAdd);
                    }
                }
            }

        }
    }

    //optimizes overhang assignment based on frequency of a parts appearance and the availability of existing overhangs
//concurrent optimizes vector assignment based on vector assignment
//prioritize existing parts with correct overhangs
//next priority is overhangs that vectors already have
    private void optimizeOverhangVectors(ArrayList<RGraph> optimalGraphs, HashMap<String, RGraph> partHash, ArrayList<RVector> vectorSet, HashMap<String, String> finalOverhangHash) {
        ArrayList<String> allOverhangs = new ArrayList(Arrays.asList("A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))); //overhangs that don't exist in part or vector library
        //aa,ba,ca,da,ea,fa,ga,ha,ia,ja,ka,la,ma,na,oa,pa,qa,ra,sa,ta,ua,va,wa,xa,ya,za
        HashMap<Integer, String> levelResistanceHash = new HashMap(); // key: level, value: antibiotic resistance
        HashMap<String, Integer> concreteOverhangFrequencyHash = new HashMap(); //key: concrete overhang pair, value: how often does that overhang pair appear
        allOverhangs.removeAll(finalOverhangHash.values());
        HashSet<String> vectorOverhangPairs = new HashSet();

        //gather all overhangs for existing vectors
        HashMap<Integer, ArrayList<String>> vectorOverhangHash = new HashMap(); //key: level, value: overhangs for that level
        ArrayList<Vector> allVectors = _vectorLibrary;

        for (Vector v : allVectors) {
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
        ArrayList<Part> allParts = _partLibrary;
        HashMap<String, ArrayList<String>> compositionConcreteOverhangHash = new HashMap(); //key: part composition, value: arrayList of concrete overhangs that appear for that compositiong
        HashMap<String, ArrayList<String>> reservedLeftFinalHash = new HashMap(); //key: string composition, value: arrayList of abstract overhangs 'reserved' for that composition
        HashMap<String, ArrayList<String>> reservedRightFinalHash = new HashMap(); //key: string composition, value: arrayList of abstract overhangs 'reserved' for that composition

        for (Part p : allParts) {

            if (p.getLeftOverhang().length() > 0 && p.getRightOverhang().length() > 0) {
                String composition = p.getStringComposition().toString();

                if (_encounteredCompositions.contains(composition)) {
                    allOverhangs.remove(p.getLeftOverhang());
                    allOverhangs.remove(p.getRightOverhang());
                    ArrayList<String> reservedLeftOverhangs = reservedLeftFinalHash.get("[" + p.getType().toLowerCase() + "]");
                    ArrayList<String> reservedRightOverhangs = reservedRightFinalHash.get("[" + p.getType().toLowerCase() + "]");

                    if (reservedLeftOverhangs != null) {

                        if (!reservedLeftOverhangs.contains(p.getLeftOverhang())) {
                            reservedLeftOverhangs.add(p.getLeftOverhang());
                        }
                    } else {
                        reservedLeftOverhangs = new ArrayList();
                        reservedLeftFinalHash.put("[" + p.getType().toLowerCase() + "]", reservedLeftOverhangs);

                        if (!reservedLeftOverhangs.contains(p.getLeftOverhang())) {
                            reservedLeftOverhangs.add(p.getLeftOverhang());
                        }
                    }

                    if (reservedRightOverhangs != null) {

                        if (!reservedRightOverhangs.contains(p.getRightOverhang())) {
                            reservedRightOverhangs.add(p.getRightOverhang());
                        }
                    } else {
                        reservedRightOverhangs = new ArrayList();
                        reservedRightFinalHash.put("[" + p.getType().toLowerCase() + "]", reservedRightOverhangs);

                        if (!reservedRightOverhangs.contains(p.getRightOverhang())) {
                            reservedRightOverhangs.add(p.getRightOverhang());
                        }
                    }
                }
            }
        }

        //pick overhangs
        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> compositionNodes = _rootBasicNodeHash.get(graph.getRootNode());
            for (RNode currentNode : compositionNodes) {
                ArrayList<String> freeLeftOverhangs = (ArrayList<String>) allOverhangs.clone();
                ArrayList<String> freeRightOverhangs = (ArrayList<String>) allOverhangs.clone();
                ArrayList<String> reservedLeftOverhangs = reservedLeftFinalHash.get(currentNode.getType().toString().toLowerCase());
                ArrayList<String> reservedRightOverhangs = reservedRightFinalHash.get(currentNode.getType().toString().toLowerCase());
                RNode parent = _parentHash.get(currentNode);

                if (reservedLeftOverhangs != null) {
                    Collections.sort(reservedLeftOverhangs);
                    freeLeftOverhangs.addAll(0,reservedLeftOverhangs);
//                    freeLeftOverhangs.addAll(reservedLeftOverhangs);
//                    Collections.sort(freeLeftOverhangs);
                } else {
                    reservedLeftOverhangs = new ArrayList();
                    reservedLeftFinalHash.put(currentNode.getType().toString().toLowerCase(), reservedLeftOverhangs);
                }

                if (reservedRightOverhangs != null) {
                    Collections.sort(reservedRightOverhangs);
                    freeRightOverhangs.addAll(0,reservedRightOverhangs);
//                    freeRightOverhangs.addAll(reservedRightOverhangs);
//                    Collections.sort(freeRightOverhangs);
                } else {
                    reservedRightOverhangs = new ArrayList();
                    reservedRightFinalHash.put(currentNode.getType().toString().toLowerCase(), reservedRightOverhangs);
                }

                //find basic parts position in the composition of its parent
                int partIndex = -1;
                ArrayList<String> parentNeighbors = parent.getComposition();
                for (int i = 0; i < parentNeighbors.size(); i++) {
                    String compositionString = currentNode.getName();
                    if (compositionString.equals(parentNeighbors.get(i))) {
                        partIndex = i;
                    }
                }

                if (partIndex == 0 || partIndex == parentNeighbors.size() - 1) {
                    RNode grandParent = _parentHash.get(parent);

                    if (grandParent != null) {

                        for (RNode uncle : grandParent.getNeighbors()) {
                            freeLeftOverhangs.remove(finalOverhangHash.get(uncle.getLOverhang()));
                            freeLeftOverhangs.remove(finalOverhangHash.get(uncle.getROverhang()));
                            freeRightOverhangs.remove(finalOverhangHash.get(uncle.getLOverhang()));
                            freeRightOverhangs.remove(finalOverhangHash.get(uncle.getROverhang()));
                        }
                    }
                }

                //assign left overhang
                if (!finalOverhangHash.containsKey(currentNode.getLOverhang())) {
                    String newOverhang = freeLeftOverhangs.get(0);
                    int counter = 1;

                    while (newOverhang.equals(finalOverhangHash.get(currentNode.getROverhang()))
                            || newOverhang.equals(finalOverhangHash.get(parent.getROverhang()))) {
                        newOverhang = freeLeftOverhangs.get(counter);
                        counter = counter + 1;
                    }

                    finalOverhangHash.put(currentNode.getLOverhang(), newOverhang);
                    allOverhangs.remove(newOverhang);

                    if (!reservedLeftOverhangs.contains(newOverhang) && !reservedRightOverhangs.contains(newOverhang)) {
                        reservedLeftOverhangs.add(newOverhang);
                    }
                    freeLeftOverhangs.remove(newOverhang);
                    freeRightOverhangs.remove(newOverhang);

                    for (String type : reservedLeftFinalHash.keySet()) {
                        reservedLeftFinalHash.get(type).remove(newOverhang);
                        reservedRightFinalHash.get(type).remove(newOverhang);
                    }
                }

                //assign right overhang
                if (!finalOverhangHash.containsKey(currentNode.getROverhang())) {
                    String newOverhang = freeRightOverhangs.get(0);
                    int counter = 1;

                    while (newOverhang.equals(finalOverhangHash.get(currentNode.getLOverhang()))
                            || newOverhang.equals(finalOverhangHash.get(parent.getLOverhang()))) {
                        newOverhang = freeRightOverhangs.get(counter);
                        counter = counter + 1;
                    }
                    finalOverhangHash.put(currentNode.getROverhang(), newOverhang);
                    allOverhangs.remove(newOverhang);

                    if (!reservedLeftOverhangs.contains(newOverhang) && !reservedRightOverhangs.contains(newOverhang)) {
                        reservedRightOverhangs.add(newOverhang);
                    }
                    freeLeftOverhangs.remove(newOverhang);
                    freeRightOverhangs.remove(newOverhang);

                    for (String type : reservedLeftFinalHash.keySet()) {
                        reservedLeftFinalHash.get(type).remove(newOverhang);
                        reservedRightFinalHash.get(type).remove(newOverhang);
                    }
                }
            }
        }

        //decide what antibiotic resistance goes with each level
        ArrayList<String> freeAntibiotics = new ArrayList(Arrays.asList("chloramphenicol, kanamycin, ampicillin, chloramphenicol, kanamycin, ampicillin, chloramphenicol, kanamycin, ampicillin, chloramphenicol, kanamycin, ampicillin, neomycin, puromycin, spectinomycin, streptomycin".toLowerCase().split(", "))); //overhangs that don't exist in part or vector library
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

        for (RGraph graph : optimalGraphs) {
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
        for (RGraph graph : optimalGraphs) {
            System.out.println("finalizing: " + graph.getRootNode().getComposition());
            int reactions = 0;
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet();
            queue.add(graph.getRootNode());

            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                current.setLOverhang(finalOverhangHash.get(current.getLOverhang()));
                current.setROverhang(finalOverhangHash.get(current.getROverhang()));

                RVector newVector = new RVector();
                newVector.setLOverhang(current.getLOverhang());
                newVector.setROverhang(current.getROverhang());
                newVector.setLevel(current.getStage());
                newVector.setStringResistance(levelResistanceHash.get(current.getStage()));
                current.setVector(newVector);

                for (RNode neighbor : current.getNeighbors()) {
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
    private HashMap<String, String> assignOverhangs(ArrayList<RGraph> optimalGraphs, HashMap<String, ArrayList<String>> forcedHash) {
        HashMap<String, String> toReturn = new HashMap(); //precursor for the finalOverhangHash used in the optimizeOverhangVectors method
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            if (forcedHash.containsKey(root.getComposition().toString())) {
                //traverse the graph and find all of the basic parts and then put them in order
                ArrayList<RNode> stack = new ArrayList();
                HashSet<RNode> seenNodes = new HashSet();
                ArrayList<RNode> basicParts = new ArrayList();
                stack.add(root);

                while (!stack.isEmpty()) {
                    RNode current = stack.get(0);
                    stack.remove(0);
                    seenNodes.add(current);

                    if (current.getStage() == 0) {
                        basicParts.add(0, current);
                    }

                    for (RNode neighbor : current.getNeighbors()) {
                        if (!seenNodes.contains(neighbor)) {
                            stack.add(0, neighbor);
                        }
                    }
                }
                ArrayList<String> forcedOverhangs = forcedHash.get(root.getComposition().toString());
                for (int i = 0; i < basicParts.size(); i++) {
                    String[] forcedTokens = forcedOverhangs.get(i).split("\\|");
                    String forcedLeft = forcedTokens[0].trim();
                    String forcedRight = forcedTokens[1].trim();
                    RNode basicNode = basicParts.get(i);
                    if (forcedLeft.length() > 0) {
                        toReturn.put(basicNode.getLOverhang(), forcedLeft);
                    }
                    if (forcedRight.length() > 0) {
                        toReturn.put(basicNode.getROverhang(), forcedRight);
                    }
                }
            }
        }

        return toReturn;
    }

    public void setForcedOverhangs(Collector coll, HashMap<String, ArrayList<String>> requiredOverhangs) {
        if (requiredOverhangs != null) {
            _forcedOverhangHash = new HashMap();
            for (String key : requiredOverhangs.keySet()) {
                Part part = coll.getPartByName(key, false);
                if (part != null) {
                    _forcedOverhangHash.put(part.getStringComposition().toString(), requiredOverhangs.get(key));
                }
            }
        }
    }

    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        boolean toReturn = true;
        for (RGraph graph : graphs) {
            RNode root = graph.getRootNode();
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);
                if (parent.getLOverhang().equals(parent.getROverhang())) {
                    System.out.println(parent.getComposition() + " has the same left overhang as it's right overhang");
                    toReturn = false;
                }
                if (parent.getNeighbors().size() > 1) {
                    RNode previous = null;
                    HashMap<String, Integer> leftFrequencyHash = new HashMap();
                    HashMap<String, Integer> rightFrequencyHash = new HashMap();
                    for (int i = 0; i < parent.getNeighbors().size(); i++) {
                        RNode child = parent.getNeighbors().get(i);
                        if (!seenNodes.contains(child)) {
                            if (leftFrequencyHash.get(child.getLOverhang()) != null) {
                                leftFrequencyHash.put(child.getLOverhang(), leftFrequencyHash.get(child.getLOverhang()) + 1);
                            } else {
                                leftFrequencyHash.put(child.getLOverhang(), 1);
                            }
                            if (rightFrequencyHash.get(child.getROverhang()) != null) {
                                rightFrequencyHash.put(child.getROverhang(), rightFrequencyHash.get(child.getROverhang()) + 1);
                            } else {
                                rightFrequencyHash.put(child.getROverhang(), 1);
                            }
                            if (i == 0) {
                                if (!child.getLOverhang().equals(parent.getLOverhang())) {
                                    System.out.println(child.getComposition() + ", which is the 1st part, doesnt have the same left overhang as its parent");
                                    toReturn = false;
                                }
                            }
                            if (i == parent.getNeighbors().size() - 1) {
                                if (!child.getROverhang().equals(parent.getROverhang())) {
                                    System.out.println(child.getComposition() + ", which is the last part, doesnt have the same right overhang as its parent");

                                    toReturn = false;
                                }
                            }
                            if (previous != null) {
                                if (!child.getLOverhang().equals(previous.getROverhang())) {
                                    System.out.println(child.getComposition() + " has a left overhang that doesn't match the right overhang of its neighbor");
                                    toReturn = false;
                                }
                            }

                            previous = child;
                            queue.add(child);
                        }
                    }
                    if (leftFrequencyHash.containsValue(2) || rightFrequencyHash.containsValue(2)) {
                        System.out.println("in " + parent.getComposition() + ", an overhang is used twice for the left overhang or twice for the right overhang\n");
                        System.out.println("leftFrequencyHash: " + leftFrequencyHash);
                        System.out.println("rightFrequencyHash: " + rightFrequencyHash);

                        toReturn = false;
                    }
                }
            }
        }
        return toReturn;
    }

    //generates human readable instructions as well as primer sequences
    //primerParameters contains (in this order): 
    //[primerNameRoot, forwardPrimerPrefix, reversePrimerPrefix, forwardEnzymeCutSite, reverseEnzymeCutSite, forwardEnzymeCutDistance, reverseEnzymeCutDistance,meltingTemperature)
    public static String generateInstructions(ArrayList<RNode> roots, Collector coll, ArrayList<String> primerParameters) {
        
        //initialize primer parameters
        String oligoNameRoot = "";
        String forwardPrimerPrefix = "";
        String reversePrimerPrefix = "";
        String forwardEnzymeCutSite = "";
        String reverseEnzymeCutSite = "";
        int forwardEnzymeCutDistance = 0;
        int reverseEnzymeCutDistance = 0;
        Double meltingTemp = 0.0;
        boolean designPrimers = false;
        if (primerParameters != null) {
            designPrimers = true;
            oligoNameRoot = primerParameters.get(0); //your oligos will be named olignoNameRoot+Number+F/R (F/R = forward/reverse)
            forwardPrimerPrefix = primerParameters.get(1); //prepended to the 5' end of your forward primer; 
            //your primer sequence will be: forwardPrimerPrefix+forwardEnzymeCutSite+partHomology
            reversePrimerPrefix = primerParameters.get(2); //prepended to the 5' end of your reverse primer; 
            //your primer sequence will be: reversePrimerPrefix+reverseEnzymeCutSite+partHomology
            forwardEnzymeCutSite = primerParameters.get(3); //the restriction enzyme cut site that appears in the forward primer
            reverseEnzymeCutSite = primerParameters.get(4); //the restriction enzyme cut site that appears in the reverse primer
            forwardEnzymeCutDistance = Integer.parseInt(primerParameters.get(5)); //distance from which forward enzyme cuts from its recognition site
            reverseEnzymeCutDistance = Integer.parseInt(primerParameters.get(6)); //distance from which reverse enzyme cuts its recognition site
            meltingTemp = Double.parseDouble(primerParameters.get(7)); //desired melting temperature of your primers; determines homology length
        }

        int oligoCount = 0;
        String toReturn = "";

        ArrayList<String> oligoNames = new ArrayList();
        ArrayList<String> oligoSequences = new ArrayList();
        HashSet<RNode> seenNodes = new HashSet();
        for (RNode root : roots) {
            
            //append header for each goal part
            toReturn = toReturn + "**********************************************"
                    + "\nAssembly Instructions for target part: " + coll.getPart(root.getUUID(), true).getName()
                    + "\n**********************************************";
            ArrayList<RNode> queue = new ArrayList();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode currentNode = queue.get(0);
                queue.remove(0); //queue for traversing graphs (bfs)

                if (!seenNodes.contains(currentNode)) {
                    //only need to generate instructions for assembling a part that has not already been encountered
                    seenNodes.add(currentNode);
                    Part currentPart = coll.getPart(currentNode.getUUID(), true);

                    if (currentPart.getComposition().size() > 1) {
                        //append which parts to use for a moclo reaction
                        toReturn = toReturn + "\nAssemble " + currentPart.getName() + " by performing a MoClo reaction with: ";
                        for (RNode neighbor : currentNode.getNeighbors()) {
                            if (currentNode.getComposition().size() > neighbor.getComposition().size()) {
                                toReturn = toReturn + coll.getPart(neighbor.getUUID(), true).getName() + ", ";
                                if (!seenNodes.contains(neighbor)) {
                                    queue.add(neighbor);
                                }
                            }
                        }
                    } else {
        
                        //design primers
                        if (designPrimers) {
                            String forwardOligoName = (oligoNameRoot + oligoCount) + "F";
                            String reverseOligoName = (oligoNameRoot + oligoCount) + "R";
                            String forwardOligoSequence = forwardPrimerPrefix + forwardEnzymeCutSite + PrimerDesign.generateRandomSequence(forwardEnzymeCutDistance) + _overhangVariableSequenceHash.get(currentNode.getLOverhang()) + currentPart.getSeq().substring(0, PrimerDesign.getPrimerHomologyLength(meltingTemp, currentPart.getSeq()));
                            String reverseOligoSequence = PrimerDesign.reverseComplement(reversePrimerPrefix + reverseEnzymeCutSite + PrimerDesign.generateRandomSequence(reverseEnzymeCutDistance) + _overhangVariableSequenceHash.get(currentNode.getROverhang()) + currentPart.getSeq().substring(currentPart.getSeq().length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, PrimerDesign.reverseComplement(currentPart.getSeq()))));
                            oligoNames.add(forwardOligoName);
                            oligoNames.add(reverseOligoName);
                            oligoSequences.add(forwardOligoSequence);
                            oligoSequences.add(reverseOligoSequence);
                            oligoCount++;
                            toReturn = toReturn + "\nPCR " + currentPart.getName() + " with oligos: " + forwardOligoName + " and " + reverseOligoName;
                        } else {
                            toReturn = toReturn + "\nPCR " + currentPart.getName() + " to prepend overhang " + currentNode.getLOverhang() + " and append overhang " + currentNode.getROverhang();
                        }
                    }

                }
            }
            toReturn = toReturn + "\n\n";
        }

        if (designPrimers) {
            
            //append primer designs
            toReturn = toReturn + "\n**********************************************\nOLIGOS";
            for (int i = 0; i < oligoNames.size(); i++) {
                toReturn = toReturn + "\n>" + oligoNames.get(i);
                toReturn = toReturn + "\n" + oligoSequences.get(i);
            }
        }
        return toReturn;
    }

    //given a part composition and an hashmap containing existing overhangs, build a directed graph representing all overhang assignment choices
    private ArrayList<RGraph> buildCartesianGraph(ArrayList<RNode> composition, HashMap<String, ArrayList<String>> compositionOverhangHash) {
        ArrayList<RNode> previousNodes = null;
        ArrayList<RGraph> toReturn = new ArrayList();
        int stage = 0;
        for (RNode node : composition) {
            String part = node.getComposition().toString();
            ArrayList<RNode> currentNodes = new ArrayList();
            ArrayList<String> existingOverhangs = compositionOverhangHash.get(part);
            for (String overhangPair : existingOverhangs) {
                String[] tokens = overhangPair.split("\\|");
                String leftOverhang = tokens[0];
                String rightOverhang = tokens[1];
                RNode newNode = new RNode();
                newNode.setName(part);
                newNode.setLOverhang(leftOverhang);
                newNode.setROverhang(rightOverhang);
                newNode.setStage(stage);
                currentNodes.add(newNode);
            }
            if (previousNodes != null) {
                for (RNode prev : previousNodes) {
                    for (RNode curr : currentNodes) {
                        if (prev.getROverhang().equals(curr.getLOverhang())) {
                            prev.addNeighbor(curr);
                        }
                    }
                }
            } else {
                for (RNode root : currentNodes) {
                    toReturn.add(new RGraph(root));
                }
            }
            previousNodes = currentNodes;
            stage++;

        }
        return toReturn;
    }

    //given a cartesian product graph, traverse a graph to see if a complete assignment exists
    //returns null if no such assignment exists
    private ArrayList<ArrayList<String>> findOptimalAssignment(ArrayList<RGraph> graphs, int targetLength) {
        ArrayList<ArrayList<String>> toReturn = new ArrayList();
        ArrayList<String> currentSolution;
        HashMap<RNode, RNode> parentHash = new HashMap(); //key: node, value: parent node
        for (RGraph graph : graphs) {
            currentSolution = new ArrayList();
            RNode root = graph.getRootNode();
            ArrayList<RNode> stack = new ArrayList();
            stack.add(root);
            boolean toParent = false; // am i returning to a parent node?
            HashSet<RNode> seenNodes = new HashSet();
            while (!stack.isEmpty()) {
                RNode currentNode = stack.get(0);
                stack.remove(0);
                seenNodes.add(currentNode);
                if (!toParent) {
                    currentSolution.add(currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                } else {
                    toParent = false;
                }
                RNode parent = parentHash.get(currentNode);

                int childrenCount = 0;
                for (RNode neighbor : currentNode.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        if (neighbor.getStage() > currentNode.getStage()) {
                            stack.add(0, neighbor);
                            parentHash.put(neighbor, currentNode);
                            childrenCount++;
                        }
                    }
                }
                if (childrenCount == 0) {
                    //no children means we've reached the end of a branch
                    if (currentSolution.size() == targetLength) {
                        //yay complete assignment
                        toReturn.add((ArrayList<String>) currentSolution.clone());

                    } else {
                        //incomplete assignment
                    }
                    if (currentSolution.size() > 0) {
                        currentSolution.remove(currentSolution.size() - 1);
                    }
                    if (parent != null) {
                        toParent = true;
                        stack.add(0, parent);
                    }
                }

            }

        }
        return toReturn;
    }
    private HashMap<String, ArrayList<String>> _abstractOverhangCompositionHash; //key: overhangs delimited by "|", value: compositions with overhangs indicated by keys
    private HashMap<String, Integer> _partOverhangFrequencyHash; //key: part composition concatenated with abstract overhang with "_" delimited with "|", value: number of occurences of part with given overhangs
    private HashSet<String> _encounteredCompositions; //set of part compositions that appear in the set of all graphs
    private HashMap<RNode, RNode> _parentHash; //key: node, value: parent node
    private HashMap<String, Integer> _compositionLevelHash; //key: string composition with overhangs, value; arrayList of nodes with the given composition
    private HashMap<String, ArrayList<String>> _forcedOverhangHash = new HashMap(); //key: composite part composition
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
    private ArrayList<Part> _partLibrary = new ArrayList();
    private ArrayList<Vector> _vectorLibrary = new ArrayList();
    private static HashMap<String, String> _overhangVariableSequenceHash = new HashMap(); //key:variable name, value: sequence associated with that variable
}