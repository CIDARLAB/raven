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
    public ArrayList<RGraph> mocloClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies, ArrayList<Double> costs) throws Exception {

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

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        boolean tryCartesian = false;
        enforceOverhangRules(optimalGraphs);
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
            maximizeOverhangSharing(nonCartesianGraphs);
//                optimizeOverhangVectors(nonCartesianGraphs, partHash, vectorSet, finalOverhangHash);
            optimizeOverhangVectors(nonCartesianGraphs, partHash, vectorSet, finalOverhangHash);
        } else {
            //if we're not doing the cartesian product or the cartesian products are wrong
            maximizeOverhangSharing(optimalGraphs);
            valid = validateOverhangs(optimalGraphs);
            System.out.println("##############################\nsecond pass: " + valid);
            finalOverhangHash = assignOverhangs(optimalGraphs, _forcedOverhangHash);
            optimizeOverhangVectors(optimalGraphs, partHash, vectorSet, finalOverhangHash);
            valid = validateOverhangs(optimalGraphs);
            System.out.println("##############################\nfinal pass: " + valid);
            assignScars(optimalGraphs);
        }

        return optimalGraphs;
    }

    /**
     * First step of overhang assignment - enforce numeric place holders for
     * overhangs, ie no overhang redundancy in any step *
     */
    private void enforceOverhangRules(ArrayList<RGraph> optimalGraphs) {

        //Initialize fields that record information to save complexity for future steps
        _encounteredCompositions = new HashSet<String>();
        _parentHash = new HashMap<RNode, RNode>();
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        _stageDirectionAssignHash = new HashMap<Integer, HashMap<String, ArrayList<RNode>>>();
        int count = 0;

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);
            _encounteredCompositions.add(root.getComposition().toString());
            root.setLOverhang(Integer.toString(count));
            count++;
            root.setROverhang(Integer.toString(count));
            count++;
            ArrayList<RNode> neighbors = root.getNeighbors();
            count = enforceOverhangRulesHelper(root, neighbors, root, count);
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
                int level = 0;
                RNode l0Node = l0Nodes.get(i);
                RNode parent = _parentHash.get(l0Node);

                //Go up the parent hash until the parent doesn't have an overhang impacted by the child
                while (l0Node.getLOverhang().equals(parent.getLOverhang()) || l0Node.getROverhang().equals(parent.getROverhang())) {
                    level++;
                    if (_parentHash.containsKey(parent)) {
                        parent = _parentHash.get(parent);
                    } else {
                        break;
                    }
                }

                //Determine direction and enter into hash               
//                l0Node = l0Nodes.get(i);
                String l0Direction = rootDir.get(0);
                if (l0Node.getComposition().size() == 1) {
                    ArrayList<String> l0Dir = new ArrayList<String>();
                    l0Dir.add(l0Direction);
                    l0Node.setDirection(l0Dir);
                }               
                int size = l0Node.getDirection().size();
                rootDir.subList(0, size).clear();
                
                HashMap<String, ArrayList<RNode>> directionHash;
                ArrayList<RNode> nodeList;

                if (_stageDirectionAssignHash.containsKey(level)) {
                    directionHash = _stageDirectionAssignHash.get(level);
                } else {
                    directionHash = new HashMap<String, ArrayList<RNode>>();
                }

                if (directionHash.containsKey(l0Direction)) {
                    nodeList = directionHash.get(l0Direction);
                } else {
                    nodeList = new ArrayList<RNode>();
                }

                nodeList.add(l0Node);
                directionHash.put(l0Direction, nodeList);
                _stageDirectionAssignHash.put(level, directionHash);
            }
        }
    }

    /**
     * This helper method executes the loops necessary to enforce overhangs for
     * each graph in enforceOverhangRules *
     */
    private int enforceOverhangRulesHelper(RNode parent, ArrayList<RNode> children, RNode root, int count) {

        String nextLOverhang = new String();

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            _parentHash.put(child, parent);

            //Pass numeric overhangs down from the parent to the correct child
            if (i == 0) {
                child.setLOverhang(parent.getLOverhang());
            } else if (i == children.size() - 1) {
                child.setROverhang(parent.getROverhang());
            }

            //Assign new left overhang if empty
            if (child.getLOverhang().isEmpty()) {

                //If the nextLOverhangVariable has an overhang waiting
                if (!nextLOverhang.isEmpty()) {
                    child.setLOverhang(nextLOverhang);
                    nextLOverhang = "";
                } else {
                    child.setLOverhang(Integer.toString(count));
                    count++;
                }
            }

            //Assign new right overhang if empty
            if (child.getROverhang().isEmpty()) {
                child.setROverhang(Integer.toString(count));
                nextLOverhang = Integer.toString(count);
                count++;
            }

            //Make recursive call
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                count = enforceOverhangRulesHelper(child, grandChildren, root, count);

                //Or record the level zero parts
            } else {
                ArrayList<RNode> l0nodes = _rootBasicNodeHash.get(root);
                l0nodes.add(child);
                _rootBasicNodeHash.put(root, l0nodes);
            }
        }

        return count;
    }

    /**
     * New overhang sharing method. Nodes that can be shared are determined
     * during this step. Nodes are assigned by stage of impact. Within that loop
     * are four steps: 1. All forward oriented nodes first get left overhang
     * assigned going from right to left 2. The remaining forward nodes get
     * right assigned 3. All backward oriented get assigned going from left to
     * right where the right one is assigned first 4. The remaining backwards
     * nodes get left overhang assigned
     *
     */
    private void maximizeOverhangSharing(ArrayList<RGraph> optimalGraphs) {

        ArrayList<RNode> roots = new ArrayList<RNode>();
        for (RGraph graph : optimalGraphs) {
            roots.add(graph.getRootNode());
        }

        _typeLOHHash = new HashMap<String, ArrayList<String>>(); //key: string type, value: arrayList of abstract overhangs 'reserved' for that composition
        _typeROHHash = new HashMap<String, ArrayList<String>>(); //key: string type, value: arrayList of abstract overhangs 'reserved' for that composition
        _takenParentOHs = new HashMap<RNode, HashSet<String>>(); //key: node (parent) value: all overhangs assigned to the reaction the level below
        HashMap<String, String> numberHash = new HashMap<String, String>(); //key: overhang from round 1, value: overhang from round 2
        HashSet<String> allLevelOHs = new HashSet<String>();

        Set<Integer> allLevels = _stageDirectionAssignHash.keySet();
        ArrayList<Integer> levels = new ArrayList<Integer>(allLevels);
        Collections.sort(levels);

        //Assign by levels of impact starting from lowest level of impact
        for (Integer level : levels) {
            HashMap<String, ArrayList<RNode>> directionHash = _stageDirectionAssignHash.get(level);
            HashSet<String> currentLevelOHs = new HashSet<String>();
            HashSet<String> takenOHs;
            
            //Assign nodes in the forward direction first
            ArrayList<RNode> fwdNodes = new ArrayList<RNode>();
            if (directionHash.containsKey("+")) {
                fwdNodes = directionHash.get("+");
            }

            //Assign nodes in the backward direction second
            ArrayList<RNode> bkwdNodes = new ArrayList<RNode>();
            if (directionHash.containsKey("-")) {
                bkwdNodes = directionHash.get("-");
            }

            /**
             * Assign left overhangs to forward nodes first. *
             */
            for (int j = 0; j < fwdNodes.size(); j++) {

                RNode node = fwdNodes.get(j);
                String type = node.getType().toString().toLowerCase();
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, level);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Left", "+");
                ArrayList<String> typeLeftOverhangs = _typeLOHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeROHHash.get(type);

                //Assign left overhang if it is not selected yet
                if (!numberHash.containsKey(node.getLOverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    numberHash.put(node.getLOverhang(), OH);
                    takenOHs.add(OH);
                    currentLevelOHs.add(OH);

                    //If this overhang is not contained in the part type OHs for right or left, add it to the left
                    if (!typeLeftOverhangs.contains(OH) && !typeRightOverhangs.contains(OH)) {
                        typeLeftOverhangs.add(OH);
                    }
                    assignTypeOHNeighbor(roots, node, OH, "Left", "+");
                }
            }

            /**
             * Assign right overhangs to forward nodes second. *
             */
            for (int k = 0; k < fwdNodes.size(); k++) {

                RNode node = fwdNodes.get(k);
                String type = node.getType().toString().toLowerCase();
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, level);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Right", "+");
                ArrayList<String> typeLeftOverhangs = _typeLOHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeROHHash.get(type);

                //Assign right overhang if it is not selected yet
                if (!numberHash.containsKey(node.getROverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    numberHash.put(node.getROverhang(), OH);
                    takenOHs.add(OH);
                    currentLevelOHs.add(OH);

                    //If this overhang is not contained in the part type OHs for right or left, add it to the right
                    if (!typeLeftOverhangs.contains(OH) && !typeRightOverhangs.contains(OH)) {
                        typeRightOverhangs.add(OH);
                    }
                    assignTypeOHNeighbor(roots, node, OH, "Right", "+");
                }
            }
            
            /**
             * Assign right overhangs to backwards nodes third. *
             */
            for (int k = bkwdNodes.size() - 1; k > -1; k--) {

                RNode node = bkwdNodes.get(k);
                String type = node.getType().toString().toLowerCase();
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, level);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Right", "-");
                ArrayList<String> typeLeftOverhangs = _typeROHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeLOHHash.get(type);

                //Assign right overhang if it is not selected yet
                if (!numberHash.containsKey(node.getROverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    String hashOH = OH + "*";
                    numberHash.put(node.getROverhang(), hashOH);
                    takenOHs.add(OH);
                    currentLevelOHs.add(OH);

                    //If this overhang is not contained in the part type OHs for right or left, add it to the right
                    if (!typeLeftOverhangs.contains(OH) && !typeRightOverhangs.contains(OH)) {
                        typeRightOverhangs.add(OH);
                    }
                    assignTypeOHNeighbor(roots, node, OH, "Right", "-");
                }
            }

            /**
             * Assign left overhangs to backward nodes fourth. *
             */
            for (int j = bkwdNodes.size() - 1; j > -1; j--) {

                RNode node = bkwdNodes.get(j);
                String type = node.getType().toString().toLowerCase();
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, level);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Left", "-");
                ArrayList<String> typeLeftOverhangs = _typeROHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeLOHHash.get(type);

                //Assign left overhang if it is not selected yet
                if (!numberHash.containsKey(node.getLOverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    String hashOH = OH + "*";
                    numberHash.put(node.getLOverhang(), hashOH);
                    takenOHs.add(OH);
                    currentLevelOHs.add(OH);

                    //If this overhang is not contained in the part type OHs for right or left, add it to the left
                    if (!typeLeftOverhangs.contains(OH) && !typeRightOverhangs.contains(OH)) {
                        typeLeftOverhangs.add(OH);
                    }
                    assignTypeOHNeighbor(roots, node, OH, "Left", "-");
                }
            }


            //Add all overhangs seen in this level to the taken overhang hash of each node
            allLevelOHs.addAll(currentLevelOHs);
        }

        /**
         * Assign all assignments within numberHash map. *
         */
        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            queue.add(graph.getRootNode());

            //Traverse graph and assign letter overhang to corresponding number placeholder
            while (!queue.isEmpty()) {
                RNode currentNode = queue.get(0);
                queue.remove(0);
                currentNode.setLOverhang(numberHash.get(currentNode.getLOverhang()));
                currentNode.setROverhang(numberHash.get(currentNode.getROverhang()));
                seenNodes.add(currentNode);
                ArrayList<RNode> neighbors = currentNode.getNeighbors();

                for (RNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        seenNodes.add(neighbor);
                    }
                }
            }
        }
    }

    /**
     * Find all overhangs that cannot be assigned in this iteration of part two
     * of overhang assignment *
     */
    private HashSet<String> getTakenAbstractOHs(RNode node, HashSet<String> allLevelOHs, int level) {

        HashSet<String> takenOHs;

        //Get the taken overhangs for the 'ancestor' appropriate
        RNode ancestor = _parentHash.get(node);
        while (ancestor.getStage() <= level) {
            if (_parentHash.containsKey(ancestor)) {
                ancestor = _parentHash.get(ancestor);
            } else {
                break;
            }
        }

        //Get taken overhangs from the ancestor
        if (_takenParentOHs.containsKey(ancestor)) {
            takenOHs = _takenParentOHs.get(ancestor);
        } else {
            takenOHs = new HashSet<String>();
            _takenParentOHs.put(ancestor, takenOHs);
        }

        takenOHs.addAll(allLevelOHs);
        return takenOHs;
    }

    /**
     * Get all overhangs that could be reused for a specific part type in part
     * two of overhang selection *
     */
    private ArrayList<String> getReusableAbstractOHs(RNode node, String LR, String direction) {

        //Get overhangs that have been seen before for this part type
        //NOTE: For backwards assignment, the reusable OH hashes are switched
        ArrayList<String> reusableLeftOverhangs = new ArrayList<String>();
        ArrayList<String> reusableRightOverhangs = new ArrayList<String>();
        ArrayList<String> typeRightOverhangs;
        ArrayList<String> typeLeftOverhangs;
        String type = node.getType().toString().toLowerCase();

        //If the direction is forward, return the same side overhangs, else flip the direction
        if (direction.equals("+")) {
            typeLeftOverhangs = _typeLOHHash.get(type);
            typeRightOverhangs = _typeROHHash.get(type);
        } else {
            typeLeftOverhangs = _typeROHHash.get(type);
            typeRightOverhangs = _typeLOHHash.get(type);
        }

        //If there are reusable left overhangs for this part type, add them to the reuable left overhang hash, else add new entry for this type
        if (typeLeftOverhangs != null) {
            reusableLeftOverhangs.addAll(typeLeftOverhangs);
            Collections.sort(reusableLeftOverhangs);
        } else if (typeLeftOverhangs == null) {
            typeLeftOverhangs = new ArrayList<String>();
            if (direction.equals("+")) {
                _typeLOHHash.put(type, typeLeftOverhangs);
            } else {
                _typeROHHash.put(type, typeLeftOverhangs);
            }
        }

        //If there are reusable right overhangs for this part type, add them to the reusable right overhang hash, else add new entry for this type
        if (typeRightOverhangs != null) {
            reusableRightOverhangs.addAll(typeRightOverhangs);
            Collections.sort(reusableRightOverhangs);
        } else if (typeRightOverhangs == null) {
            typeRightOverhangs = new ArrayList<String>();
            if (direction.equals("+")) {
                _typeROHHash.put(type, typeRightOverhangs);
            } else {
                _typeLOHHash.put(type, typeRightOverhangs);
            }
        }

        //Return either the either the left or right re-usable overhangs
        if (LR.equals("Left")) {
            return reusableLeftOverhangs;
        } else {
            return reusableRightOverhangs;
        }
    }

    /**
     * Given the taken and reusable overhangs and overhang count, determine a
     * new overhang to select for part two of overhang assignment *
     */
    private String selectAbstractOH(ArrayList<String> reusableOHs, HashSet<String> takenOHs) {

        int count = 0;
        String OH;
        if (!reusableOHs.isEmpty()) {
            OH = reusableOHs.get(0);
            reusableOHs.remove(0);
        } else {
            OH = count + "_";
            count++;
        }

        //Pull from the available overhang list until one does not match one already assigned to the right overhang of the current node or its parent
        //Rule checking loop
        while (takenOHs.contains(OH)) {
            if (!reusableOHs.isEmpty()) {
                OH = reusableOHs.get(0);
                reusableOHs.remove(0);
            } else {
                OH = count + "_";
                count++;
            }
        }
        return OH;
    }

    /**
     * Add the overhang of adjacent part to its type-OH hash *
     */
    private void assignTypeOHNeighbor(ArrayList<RNode> roots, RNode currentNode, String OH, String LR, String direction) {

        //Find which l0Node set this node is containted in
        for (RNode root : roots) {
            ArrayList<RNode> l0Nodes = _rootBasicNodeHash.get(root);

            if (l0Nodes.contains(currentNode)) {
                int indexOf = l0Nodes.indexOf(currentNode);

                //If assiging right overhang to the type of the node on the left
                if ("Left".equals(LR)) {
                    if (indexOf > 0) {
                        RNode l0Node = l0Nodes.get(indexOf - 1);
                        String l0Type = l0Node.getType().toString().toLowerCase();
                        ArrayList<String> l0TypeLeftOverhangs;
                        ArrayList<String> l0TypeRightOverhangs;

                        //Consider correct direction
                        if ("+".equals(direction)) {
                            l0TypeLeftOverhangs = _typeLOHHash.get(l0Type);
                            l0TypeRightOverhangs = _typeROHHash.get(l0Type);

                            if (l0TypeRightOverhangs == null) {
                                l0TypeRightOverhangs = new ArrayList<String>();
                                _typeROHHash.put(l0Type, l0TypeRightOverhangs);
                            }
                            if (l0TypeLeftOverhangs == null) {
                                l0TypeLeftOverhangs = new ArrayList<String>();
                                _typeLOHHash.put(l0Type, l0TypeLeftOverhangs);
                            }

                        } else {
                            l0TypeLeftOverhangs = _typeROHHash.get(l0Type);
                            l0TypeRightOverhangs = _typeLOHHash.get(l0Type);

                            if (l0TypeRightOverhangs == null) {
                                l0TypeRightOverhangs = new ArrayList<String>();
                                _typeLOHHash.put(l0Type, l0TypeRightOverhangs);
                            }
                            if (l0TypeLeftOverhangs == null) {
                                l0TypeLeftOverhangs = new ArrayList<String>();
                                _typeROHHash.put(l0Type, l0TypeLeftOverhangs);
                            }
                        }

                        //Add this OH to the right overhangs of the type of the node on the left
                        if (!l0TypeLeftOverhangs.contains(OH) && !l0TypeRightOverhangs.contains(OH)) {
                            l0TypeRightOverhangs.add(OH);
                        }
                    }

                    //If assiging right overhang to the type of the node on the left
                } else {
                    if (indexOf < l0Nodes.size() - 1) {
                        RNode l0Node = l0Nodes.get(indexOf + 1);
                        String l0Type = l0Node.getType().toString().toLowerCase();
                        ArrayList<String> l0TypeLeftOverhangs;
                        ArrayList<String> l0TypeRightOverhangs;

                        //Consider correct direction
                        if ("+".equals(direction)) {
                            l0TypeLeftOverhangs = _typeLOHHash.get(l0Type);
                            l0TypeRightOverhangs = _typeROHHash.get(l0Type);

                            if (l0TypeRightOverhangs == null) {
                                l0TypeRightOverhangs = new ArrayList<String>();
                                _typeROHHash.put(l0Type, l0TypeRightOverhangs);
                            }
                            if (l0TypeLeftOverhangs == null) {
                                l0TypeLeftOverhangs = new ArrayList<String>();
                                _typeLOHHash.put(l0Type, l0TypeLeftOverhangs);
                            }

                        } else {
                            l0TypeLeftOverhangs = _typeROHHash.get(l0Type);
                            l0TypeRightOverhangs = _typeLOHHash.get(l0Type);

                            if (l0TypeRightOverhangs == null) {
                                l0TypeRightOverhangs = new ArrayList<String>();
                                _typeLOHHash.put(l0Type, l0TypeRightOverhangs);
                            }
                            if (l0TypeLeftOverhangs == null) {
                                l0TypeLeftOverhangs = new ArrayList<String>();
                                _typeROHHash.put(l0Type, l0TypeLeftOverhangs);
                            }
                        }

                        //Add this OH to the left overhangs of the type of the node on the right
                        if (!l0TypeLeftOverhangs.contains(OH) && !l0TypeRightOverhangs.contains(OH)) {
                            l0TypeLeftOverhangs.add(OH);
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine overhang scars *
     */
    private void assignScars(ArrayList<RGraph> optimalGraphs) {

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            assignScarsHelper(root, children);
        }
    }

    /**
     * Overhang scars helper *
     */
    private ArrayList<String> assignScarsHelper(RNode parent, ArrayList<RNode> children) {

        ArrayList<String> scars = new ArrayList<String>();

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);

            if (i > 0) {
                if (child.getLOverhang().isEmpty()) {
                    scars.add("_");
                }
                scars.add(child.getLOverhang());
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
                    freeLeftOverhangs.addAll(0, reservedLeftOverhangs);
                } else {
                    reservedLeftOverhangs = new ArrayList();
                    reservedLeftFinalHash.put(currentNode.getType().toString().toLowerCase(), reservedLeftOverhangs);
                }

                if (reservedRightOverhangs != null) {
                    Collections.sort(reservedRightOverhangs);
                    freeRightOverhangs.addAll(0, reservedRightOverhangs);
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

                String currentLeftOverhang = currentNode.getLOverhang();
                String currentRightOverhang = currentNode.getROverhang();

                //assign left overhang
                if (!finalOverhangHash.containsKey(currentLeftOverhang)) {
                    String newOverhang = freeLeftOverhangs.get(0);
                    int counter = 1;

                    while (newOverhang.equals(finalOverhangHash.get(currentRightOverhang))
                            || newOverhang.equals(finalOverhangHash.get(parent.getROverhang()))) {
                        newOverhang = freeLeftOverhangs.get(counter);
                        counter = counter + 1;
                    }
                    //detect and handle inverted overhangs
                    if (currentLeftOverhang.indexOf("*") > -1) {
                        String starlessLeftOverhang = currentLeftOverhang.substring(0, currentLeftOverhang.indexOf("*"));
                        if (finalOverhangHash.containsKey(starlessLeftOverhang)) {
                            String starlessLeftAssignment = finalOverhangHash.get(starlessLeftOverhang);
                            if (starlessLeftAssignment.indexOf("*") > -1) {
                                newOverhang = starlessLeftAssignment.substring(0, starlessLeftOverhang.indexOf("*"));
                            } else {
                                newOverhang = starlessLeftAssignment + "*";
                            }

                        }
                    } else {
                        String starredLeftOverhang = currentLeftOverhang + "*";
                        if (finalOverhangHash.containsKey(starredLeftOverhang)) {
                            String starredLeftAssignment = finalOverhangHash.get(starredLeftOverhang);
                            if (starredLeftAssignment.indexOf("*") > -1) {
                                newOverhang = starredLeftAssignment.substring(0, starredLeftAssignment.indexOf("*"));
                            } else {
                                newOverhang = starredLeftAssignment + "*";
                            }

                        }
                    }
                    finalOverhangHash.put(currentLeftOverhang, newOverhang);
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
                if (!finalOverhangHash.containsKey(currentRightOverhang)) {
                    String newOverhang = freeRightOverhangs.get(0);
                    int counter = 1;

                    while (newOverhang.equals(finalOverhangHash.get(currentLeftOverhang))
                            || newOverhang.equals(finalOverhangHash.get(parent.getLOverhang()))) {
                        newOverhang = freeRightOverhangs.get(counter);
                        counter = counter + 1;
                    }
                    //detect and handle inverted overhangs
                    if (currentRightOverhang.indexOf("*") > -1) {
                        String starlessRightOverhang = currentRightOverhang.substring(0, currentRightOverhang.indexOf("*"));
                        if (finalOverhangHash.containsKey(starlessRightOverhang)) {
                            String starlessRightAssignment = finalOverhangHash.get(starlessRightOverhang);
                            if (starlessRightAssignment.indexOf("*") > -1) {
                                newOverhang = starlessRightAssignment.substring(0, starlessRightOverhang.indexOf("*"));
                            } else {
                                newOverhang = starlessRightAssignment + "*";
                            }

                        }
                    } else {
                        String starredRightOverhang = currentRightOverhang + "*";
                        if (finalOverhangHash.containsKey(starredRightOverhang)) {
                            String starredRightAssignment = finalOverhangHash.get(starredRightOverhang);
                            if (starredRightAssignment.indexOf("*") > -1) {
                                newOverhang = starredRightAssignment.substring(0, starredRightAssignment.indexOf("*"));
                            } else {
                                newOverhang = starredRightAssignment + "*";
                            }

                        }
                    }
                    finalOverhangHash.put(currentRightOverhang, newOverhang);
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
                String currentLeftOverhang = current.getLOverhang();
                String currentRightOverhang = current.getROverhang();
                current.setLOverhang(finalOverhangHash.get(currentLeftOverhang));
                current.setROverhang(finalOverhangHash.get(currentRightOverhang));
                currentLeftOverhang = current.getLOverhang();
                currentRightOverhang = current.getROverhang();
                RVector newVector = new RVector();
                newVector.setLOverhang(currentLeftOverhang);
                newVector.setROverhang(currentRightOverhang);
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
                String overhangString = currentLeftOverhang + "|" + currentRightOverhang;
                
                //handle inverted overhangs
                String invertedLeftOverhang = "";
                String invertedRightOverhang = "";
                if (currentLeftOverhang.indexOf("*") > -1) {
                    invertedLeftOverhang = currentLeftOverhang.substring(0, currentLeftOverhang.indexOf("*"));
                } else {
                    invertedLeftOverhang = currentLeftOverhang + "*";
                }
                if (currentRightOverhang.indexOf("*") > -1) {
                    invertedRightOverhang = currentRightOverhang.substring(0, currentRightOverhang.indexOf("*"));
                } else {
                    invertedRightOverhang = currentRightOverhang + "*";
                }
                String invertedOverhangString = invertedRightOverhang+"|"+invertedLeftOverhang;
                if (overhangOptions != null) {
                    if (!overhangOptions.contains(overhangString) && !overhangOptions.contains(invertedOverhangString)) {
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
    //[oligoNameRoot, forwardPrefix, reversePrefix, forwardCutSite, reverseCutSite, forwardCutDistance, reverseCutDistance,meltingTemperature, targetLength)
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
        int targetLength = 20;

        boolean designPrimers = false;

        if (primerParameters != null) {
            designPrimers = true;
            //TODO sort 
            oligoNameRoot = primerParameters.get(0);//your oligos will be named olignoNameRoot+Number+F/R (F/R = forward/reverse)
            forwardPrimerPrefix = primerParameters.get(1);//prepended to the 5' end of your forward primer; 
            //your primer sequence will be: forwardPrimerPrefix+forwardEnzymeCutSite+partHomology
            reversePrimerPrefix = primerParameters.get(2);//prepended to the 5' end of your reverse primer; 
            //your primer sequence will be: reversePrimerPrefix+reverseEnzymeCutSite+partHomology
            forwardEnzymeCutSite = primerParameters.get(3);//the restriction enzyme cut site that appears in the forward primer
            reverseEnzymeCutSite = primerParameters.get(4);//the restriction enzyme cut site that appears in the reverse primer
            forwardEnzymeCutDistance = Integer.parseInt(primerParameters.get(5));//distance from which forward enzyme cuts from its recognition site
            reverseEnzymeCutDistance = Integer.parseInt(primerParameters.get(6));//distance from which reverse enzyme cuts its recognition site
            meltingTemp = Double.parseDouble(primerParameters.get(7));//desired melting temperature of your primers; determines homology length
            targetLength = Integer.parseInt(primerParameters.get(8));
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
    private HashSet<String> _encounteredCompositions; //set of part compositions that appear in the set of all graphs
    private HashMap<RNode, RNode> _parentHash; //key: node, value: parent node
    private HashMap<RNode, HashSet<String>> _takenParentOHs; //key: parent node, value: all overhangs that have been seen in this step
    private HashMap<String, ArrayList<String>> _typeROHHash; //key: part type, value: all right overhangs seen for this part type
    private HashMap<String, ArrayList<String>> _typeLOHHash; //key: part type, value: all left overhangs seen for this part type
    private HashMap<Integer, HashMap<String, ArrayList<RNode>>> _stageDirectionAssignHash; //key: stage, value: HashMap: key: direction, value: nodes to visit
    private HashMap<String, ArrayList<String>> _forcedOverhangHash = new HashMap(); //key: composite part composition
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
    private ArrayList<Part> _partLibrary = new ArrayList<Part>();
    private ArrayList<Vector> _vectorLibrary = new ArrayList<Vector>();
    private static HashMap<String, String> _overhangVariableSequenceHash = new HashMap(); //key:variable name, value: sequence associated with that variable
}
