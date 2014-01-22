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
    public ArrayList<RGraph> mocloClothoWrapper(HashMap<Part, Vector> goalPartsVectors, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies, ArrayList<Double> costs) throws Exception {

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
        ArrayList<Part> goalParts = new ArrayList<Part>(goalPartsVectors.keySet());

        //Create hashMem parameter for createAsmGraph_sgp() call
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, required, recommended); //key: composiion, direction || value: library graph

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalPartsVectors, false);

        //Positional scoring of transcriptional units
//            HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
//            if (modular) {
//                ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
//                positionScores = getPositionalScoring(TUs);
//            }

        //Add single transcriptional units to the required hash
//            ArrayList<ArrayList<String>> reqTUs = getSingleTranscriptionalUnits(gpsNodes, 2);
//            for (int i = 0; i < reqTUs.size(); i++) {
//                required.add(reqTUs.get(i).toString());
//            }

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        enforceOverhangRules(optimalGraphs);
        maximizeOverhangSharing(optimalGraphs);
        HashMap<String, String> finalOverhangHash = assignOverhangs(optimalGraphs, _forcedOverhangHash);
        assignFinalOverhangs(optimalGraphs, finalOverhangHash);
        assignScars(optimalGraphs);

        return optimalGraphs;

    }

    /**
     * First step of overhang assignment - enforce numeric place holders for
     * overhangs, ie no overhang redundancy in any step *
     */
    private void enforceOverhangRules(ArrayList<RGraph> optimalGraphs) {

        //Initialize fields that record information to save complexity for future steps
        _parentHash = new HashMap<RNode, RNode>();
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        _stageDirectionAssignHash = new HashMap<Integer, HashMap<String, ArrayList<RNode>>>();
        _OHexclusionHash = new HashMap<String, HashSet<String>>(); //key: 1st pass overhang value: all overhangs that cannot be the same
        int count = 0;

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);
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
                
                //Start OH exclusivity set... start with all of parent's children
                HashSet<String> exclusiveL = new HashSet<String>();
                HashSet<String> exclusiveR = new HashSet<String>();
                for (RNode neighbor: parent.getNeighbors()) {
                    if (neighbor.getStage() < parent.getStage()) {
                        exclusiveL.add(neighbor.getLOverhang());
                        exclusiveL.add(neighbor.getROverhang());
                        exclusiveR.add(neighbor.getLOverhang());
                        exclusiveR.add(neighbor.getROverhang());
                    }
                }

                //Go up the parent hash until the parent doesn't have an overhang impacted by the child
                RNode ancestor = parent;
                while (l0Node.getLOverhang().equals(ancestor.getLOverhang()) || l0Node.getROverhang().equals(ancestor.getROverhang())) {                    
                    level = ancestor.getStage();
                    
                    if (_parentHash.containsKey(ancestor)) {
                        ancestor = _parentHash.get(ancestor);
                        
                        //Add exclusive OHs for each relevant ancestor
                        for (RNode neighbor : ancestor.getNeighbors()) {
                            if (neighbor.getStage() < ancestor.getStage()) {
                                exclusiveL.add(neighbor.getLOverhang());
                                exclusiveL.add(neighbor.getROverhang());
                                exclusiveR.add(neighbor.getLOverhang());
                                exclusiveR.add(neighbor.getROverhang());
                            }
                        }     
                        
                    } else {
                        break;
                    }
                }

                //Add to exclusion hash for the left OH
                if (_OHexclusionHash.containsKey(l0Node.getLOverhang())) {
                    HashSet<String> exL = _OHexclusionHash.get(l0Node.getLOverhang());
                    exL.addAll(exclusiveL);
                } else {
                    _OHexclusionHash.put(l0Node.getLOverhang(), exclusiveL);
                }
                
                //Add to exclusion hash for the right OH
                if (_OHexclusionHash.containsKey(l0Node.getROverhang())) {
                    HashSet<String> exR = _OHexclusionHash.get(l0Node.getROverhang());
                    exR.addAll(exclusiveR);
                } else {
                    _OHexclusionHash.put(l0Node.getROverhang(), exclusiveR);
                }
                
                //Determine direction and enter into hash               
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
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, "Left", numberHash);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Left", "+");
                ArrayList<String> typeLeftOverhangs = _typeLOHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeROHHash.get(type);

                //Assign left overhang if it is not selected yet
                if (!numberHash.containsKey(node.getLOverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    numberHash.put(node.getLOverhang(), OH);
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
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, "Right", numberHash);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Right", "+");
                ArrayList<String> typeLeftOverhangs = _typeLOHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeROHHash.get(type);

                //Assign right overhang if it is not selected yet
                if (!numberHash.containsKey(node.getROverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    numberHash.put(node.getROverhang(), OH);
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
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, "Right", numberHash);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Right", "-");
                ArrayList<String> typeLeftOverhangs = _typeROHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeLOHHash.get(type);

                //Assign right overhang if it is not selected yet
                if (!numberHash.containsKey(node.getROverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    String hashOH = OH + "*";
                    numberHash.put(node.getROverhang(), hashOH);
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
                takenOHs = getTakenAbstractOHs(node, allLevelOHs, "Left", numberHash);
                ArrayList<String> reusableOHs = getReusableAbstractOHs(node, "Left", "-");
                ArrayList<String> typeLeftOverhangs = _typeROHHash.get(type);
                ArrayList<String> typeRightOverhangs = _typeLOHHash.get(type);

                //Assign left overhang if it is not selected yet
                if (!numberHash.containsKey(node.getLOverhang())) {

                    String OH = selectAbstractOH(reusableOHs, takenOHs);
                    String hashOH = OH + "*";
                    numberHash.put(node.getLOverhang(), hashOH);
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
    private HashSet<String> getTakenAbstractOHs(RNode node, HashSet<String> allLevelOHs, String LR, HashMap<String, String> numberHash) {

        HashSet<String> takenOHs = new HashSet<String>();
        String OH;
        
        if (LR.equals("Left")) {
            OH = node.getLOverhang();
        } else {
            OH = node.getROverhang();
        }
        
        //Search all other overhangs exclusive to this one and 
        HashSet<String> exclusiveOHs = _OHexclusionHash.get(OH);
        for (String exclusiveOH : exclusiveOHs) {
            if (numberHash.containsKey(exclusiveOH)) {
                
                String exclude = numberHash.get(exclusiveOH);
                takenOHs.add(exclude);
                
                if (exclude.contains("*")) {
                    takenOHs.add(exclude.substring(0, exclude.length()-1));
                } else {
                    takenOHs.add(exclude+"*");
                }
            }
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
            root.setScars(assignScarsHelper(root, children));
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

    /* 
     * Third step of overhang assignment - A partial cartesian product given a library of parts
     */   
    public void assignFinalOverhangs(ArrayList<RGraph> graphs, HashMap<String, String> finalOverhangHash) {
        
        //Initialize all hashes for the third pass of overhang assignment
        HashMap<String, HashSet<String>> nodeOHlibraryOHHash = new HashMap<String, HashSet<String>>();
        HashMap<String, HashSet<String>> nodeLCompHash = new HashMap<String, HashSet<String>>(); //key: abstract overhang, value: set of all compositions associated with that overhang
        HashMap<String, HashSet<String>> nodeRCompHash = new HashMap<String, HashSet<String>>(); //key: abstract overhang, value: set of all compositions associated with that overhang
        
        HashMap<String, HashSet<String>> libraryLCompHash = new HashMap<String, HashSet<String>>(); //key: composition, value: set of all abstract overhangs associated with that composition
        HashMap<String, HashSet<String>> libraryRCompHash = new HashMap<String, HashSet<String>>(); //key: composition, value: set of all abstract overhangs associated with that composition
        
        HashSet<String> libCompOHDirHash = new HashSet<String>(); //concatentation of composition Overhang and direction seen in the partLibrary
        HashMap<Integer, String> levelResistanceHash = new HashMap<Integer, String>(); //key: level, value: antibiotic resistance
        HashSet<String> invertedOverhangs = new HashSet<String>();

        /*
         * Build map of node overhangs (i.e. LO & RO) to library overhangs
         */
        
        //For each of the graphs in the solution set, create a hash of overhangs (left and right) seen for each level 0 node's composition
        for (RGraph graph : graphs) {
            
            //For each of the l0Nodes in the graph, build hash of overhangs for each composition, ignoring reverse overhangs 
            for (RNode l0Node : _rootBasicNodeHash.get(graph.getRootNode())) {
                
                String LO = l0Node.getLOverhang();
                String RO = l0Node.getROverhang();
                
                //If the left overhang is forward (i.e. not inverted), put it in the abstractConcrete hash and the abstractLeftComposisiton hash
                if (LO.indexOf("*") < 0) {

                    nodeOHlibraryOHHash.put(LO, new HashSet());

                    //Add left overhang to abstractLeftComposisiton hash if this overhang is already in the map, otherwise initialize with this composition
                    if (nodeLCompHash.containsKey(LO)) {
                        nodeLCompHash.get(LO).add(l0Node.getComposition().toString());
                    } else {
                        HashSet<String> toAddLeft = new HashSet();
                        toAddLeft.add(l0Node.getComposition().toString());
                        nodeLCompHash.put(LO, toAddLeft);
                    }
                
                } else {
                    invertedOverhangs.add(LO);
                }
                
                //If the right overhang is forward (i.e. not inverted), put it in the abstractConcrete hash and the abstractRightComposisiton hash
                if (RO.indexOf("*") < 0) {

                    nodeOHlibraryOHHash.put(RO, new HashSet());
                    
                    //Add right overhang to abstractRightComposisiton hash if this overhang is already in the map, otherwise initialize with this composition
                    if (nodeRCompHash.containsKey(RO)) {
                        nodeRCompHash.get(RO).add(l0Node.getComposition().toString());
                    } else {
                        HashSet<String> toAddRight = new HashSet();
                        toAddRight.add(l0Node.getComposition().toString());
                        nodeRCompHash.put(RO, toAddRight);
                    }
                
                } else {
                    invertedOverhangs.add(RO);
                }
            }
        }
        
        //For each part in the library, build hash of overhangs for each composition
        for (Part libraryPart : _partLibrary) {
            
            libCompOHDirHash.add(libraryPart.getStringComposition() + "|" + libraryPart.getLeftOverhang() + "|" + libraryPart.getRightOverhang() + "|" + libraryPart.getDirections());
            String composition = libraryPart.getStringComposition().toString();
            
            //If the library part composition is seen in the left hash, add it or put a new entry for the composition
            if (libraryLCompHash.containsKey(composition)) {
                libraryLCompHash.get(composition).add(libraryPart.getLeftOverhang());                
            } else {
                HashSet<String> toAddLeft = new HashSet();
                toAddLeft.add(libraryPart.getLeftOverhang());
                libraryLCompHash.put(composition, toAddLeft);
            }
                
            //If the library part composition is seen in the right hash, add it or put a new entry for the composition    
            if (libraryRCompHash.containsKey(composition))  {
                libraryRCompHash.get(composition).add(libraryPart.getRightOverhang());            
            } else {
                HashSet<String> toAddRight = new HashSet();
                toAddRight.add(libraryPart.getRightOverhang());
                libraryRCompHash.put(composition, toAddRight);
            }
        }
        
        //For each left overhang in the node hash, loop through associated node compositions and for each composition, build the nodeLO to libraryLO hash 
        for (String nodeLO : nodeLCompHash.keySet()) {
            
            for (String nodeComp : nodeLCompHash.get(nodeLO)) {
                
                //If any of the node composition are seen in the library, add all library LOs to the node-library hash map that are not blank
                if (libraryLCompHash.get(nodeComp) != null) {
                    
                    for (String libraryLO : libraryLCompHash.get(nodeComp)) {
                        if (!libraryLO.equals("")) {
                            nodeOHlibraryOHHash.get(nodeLO).add(libraryLO);
                        }
                    }
                }
            }
            
            //Each node LO gets all library matches based on composition and one additional placeholder
            nodeOHlibraryOHHash.get(nodeLO).add("#");
        }
        
        //For each right overhang in the node hash, loop through associated node compositions and for each composition, build the nodeRO to libraryRO hash
        for (String nodeRO : nodeRCompHash.keySet()) {
            
            for (String nodeComp : nodeRCompHash.get(nodeRO)) {
                
                //If any of the node composition are seen in the library, add all library ROs to the node-library hash map that are not blank
                if (libraryRCompHash.get(nodeComp) != null) {
                    
                    for (String libraryRO : libraryRCompHash.get(nodeComp)) {
                        if (!libraryRO.equals("")) {
                            nodeOHlibraryOHHash.get(nodeRO).add(libraryRO);
                        }
                    }
                }
            }
            
            //Each node RO gets all library matches based on composition and one additional placeholder
            nodeOHlibraryOHHash.get(nodeRO).add("#");
        }

        /*
         * Begin Cartesian Graph Space
         */         
        
        ArrayList<CartesianNode> previousNodes = null;
        ArrayList<CartesianNode> cartestianRootNodes = new ArrayList<CartesianNode>();
        
        //Make a sorted list of the key values of the nodeOH to library OH map, from 0 to highest seen node OH
        ArrayList<String> sortedNodeOverhangs = new ArrayList(nodeOHlibraryOHHash.keySet());
        Collections.sort(sortedNodeOverhangs);
        
        int level = 0;

        //For each of the node overhangs,
        for (String nodeOverhang : sortedNodeOverhangs) {
            
            ArrayList<CartesianNode> currentNodes = new ArrayList<CartesianNode>();
            HashSet<String> libraryOverhangs = nodeOHlibraryOHHash.get(nodeOverhang);
            
            //For all library overhangs mapping to this node overhang, make cartesian nodes
            for (String libraryOverhang : libraryOverhangs) {
                CartesianNode cartesian = new CartesianNode();
                cartesian.setLevel(level);
                cartesian.setNodeOverhang(nodeOverhang);
                cartesian.setLibraryOverhang(libraryOverhang.trim());
                currentNodes.add(cartesian);
            }
            
            //If this is not the first cartesian node seen, add to space
            if (previousNodes != null) {
                
                //For all current and previous nodes expand space that is not redundant
                for (CartesianNode previousNode : previousNodes) {
                    for (CartesianNode currentNode : currentNodes) {
                        
                        //If the current and previous node aren't matching or the current node is a blank, make current node a neighbor of the previous node
                        if (!previousNode.getLibraryOverhang().equals(currentNode.getLibraryOverhang()) || currentNode.getLibraryOverhang().equals("#")) {
                            previousNode.addNeighbor(currentNode);
                        }
                    }
                }
            } else {
                
                for (CartesianNode root : currentNodes) {
                    cartestianRootNodes.add(root);
                }
            }
            
            previousNodes = currentNodes;
            level++;
        }
        
        /*
         * Traverse Cartesian graph to find best score
         */
        
        int targetLength = sortedNodeOverhangs.size();
        ArrayList<ArrayList<String>> completeAssignments = new ArrayList<ArrayList<String>>();
        HashMap<CartesianNode, CartesianNode> parentHash = new HashMap<CartesianNode, CartesianNode>(); //key: node, value: parent node
        ArrayList<String> currentSolution;
        
        //Traverse the Cartesian graph starting at the Cartesian roots
        for (CartesianNode cartesianRoot : cartestianRootNodes) {
            
            currentSolution = new ArrayList<String>();
            ArrayList<CartesianNode> stack = new ArrayList<CartesianNode>();
            stack.add(cartesianRoot);
            boolean toParent = false; // am i returning to a parent node?
            HashSet<String> seenPaths = new HashSet<String>();
            
            while (!stack.isEmpty()) {
                
                CartesianNode currentNode = stack.get(0);
                stack.remove(0);
                String currentPath = currentSolution.toString();
                currentPath = currentPath.substring(1, currentPath.length() - 1).replaceAll(",", "->").replaceAll(" ", "");
                
                //
                if (!toParent) {
                    currentSolution.add(currentNode.getLibraryOverhang());
                    currentPath = currentPath + "->" + currentNode.getLibraryOverhang();
                    seenPaths.add(currentPath);
                } else {
                    toParent = false;
                }
                
                CartesianNode parent = parentHash.get(currentNode);
                int childrenCount = 0;
                
                //Build paths to complete assignment
                for (CartesianNode neighbor : currentNode.getNeighbors()) {
                    
                    //If the current path does not contain this neighbor's overhang or the neighbor is a blank, make a new edge
                    if (currentPath.indexOf(neighbor.getLibraryOverhang()) < 0 || neighbor.getLibraryOverhang().equals("#")) {
                        String edge = currentPath + "->" + neighbor.getLibraryOverhang();
                        
                        //Add to stack and parent hash if the edge hasn't been seen and the neighbor is the next level
                        if (!seenPaths.contains(edge)) {                            
                            if (neighbor.getLevel() > currentNode.getLevel()) {
                                stack.add(0, neighbor);
                                parentHash.put(neighbor, currentNode);
                                childrenCount++;
                            }
                        }
                    }

                }
                
                //If there are no more children, i.e. we've reached the end of a branch
                if (childrenCount == 0) {
                    
                    //If this solution equals the target length (another check), it is a coplete assignment
                    if (currentSolution.size() == targetLength) {
                        completeAssignments.add((ArrayList<String>) currentSolution.clone());
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
        
        //Get all basic nodes into a list and initialize arbitrarily high best score
        ArrayList<RNode> basicNodes = new ArrayList();
        for (RNode rootNodes : _rootBasicNodeHash.keySet()) {
            for (RNode basicNode : _rootBasicNodeHash.get(rootNodes)) {
                if (!basicNodes.contains(basicNode)) {
                    basicNodes.add(basicNode);
                }
            }
        }
        int bestScore = 1000000000;
        HashMap<String, String> bestAssignment = null;
        
        //Loop through each complete assignment and score the solutions
        for (ArrayList<String> assignment : completeAssignments) {
            HashMap<String, String> currentAssignment = new HashMap();
            int currentScore = 0;
            
            //Forced overhangs
            for (int i = 0; i < sortedNodeOverhangs.size(); i++) {
                String currentNodeOverhang = sortedNodeOverhangs.get(i);
      
                //If final overhang hash includes this node overhang, give the current assignment that value otherwise a new compelte assignment value
                if (finalOverhangHash.containsKey(currentNodeOverhang)) {
                    currentAssignment.put(currentNodeOverhang, finalOverhangHash.get(currentNodeOverhang));
                } else {
                    currentAssignment.put(sortedNodeOverhangs.get(i), assignment.get(i));
                }
            }
            
            //TODO: DEAL WITH THIS
            //handle inverted overhangs
            for (String invertedOverhang : invertedOverhangs) {
                
                //If the inverted overhang is forced put it in the current assignment
                if (finalOverhangHash.containsKey(invertedOverhang)) {
                    currentAssignment.put(invertedOverhang, finalOverhangHash.get(invertedOverhang));
                
                } else {
                    String uninvertedOverhang = invertedOverhang.substring(0, invertedOverhang.indexOf("*"));
                    
                    if (currentAssignment.containsKey(uninvertedOverhang)) {
                        String uninvertedOverhangAssignment = currentAssignment.get(uninvertedOverhang);
                        String invertedOverhangAssignment = "";
                        
                        if (uninvertedOverhangAssignment.equals("#")) {
                            //# markes new overhang
                            currentAssignment.put(invertedOverhang, "#");
                        } else {
                            
                            if (uninvertedOverhangAssignment.indexOf("*") > -1) {
                                invertedOverhangAssignment = uninvertedOverhangAssignment.substring(0, uninvertedOverhangAssignment.indexOf("*"));
                            } else {
                                invertedOverhangAssignment = uninvertedOverhangAssignment + "*";
                            }
                            currentAssignment.put(invertedOverhang, invertedOverhangAssignment);
                        }
                    } else {
                        //# marks new overhang
                        currentAssignment.put(invertedOverhang, "#");
                    }
                }
            }
            
            HashSet<String> matched = new HashSet();
            
            //Score this assignment for each basic node
            for (RNode basicNode : basicNodes) {
                String compositionOverhangDirectionString = basicNode.getComposition() + "|" + currentAssignment.get(basicNode.getLOverhang()) + "|" + currentAssignment.get(basicNode.getROverhang()) + "|" + basicNode.getDirection();
                if (!libCompOHDirHash.contains(compositionOverhangDirectionString)) {
                    currentScore++;
                } else {
                    matched.add(compositionOverhangDirectionString);
                }
            }
            currentScore = currentScore - matched.size();
            
            //If this is the new best score, replace the former best score and assignment
            if (currentScore < bestScore) {
                bestScore = currentScore;
                bestAssignment = currentAssignment;
            } 
        }

        //generate new overhangs
        HashSet<String> assignedOverhangs = new HashSet(bestAssignment.values());
        int newOverhang = 0;
        
        for (String starAbstract : sortedNodeOverhangs) {
            if (bestAssignment.get(starAbstract).equals("#")) {
                while (assignedOverhangs.contains(String.valueOf(newOverhang))) {
                    newOverhang++;
                }
                bestAssignment.put(starAbstract, String.valueOf(newOverhang));
                assignedOverhangs.add(String.valueOf(newOverhang));
            }
        }

        //generate matching new overhangs for inverted overhans
        for (String invertedOverhang : invertedOverhangs) {
            
            if (bestAssignment.get(invertedOverhang).equals("#")) {
                String uninvertedOverhang = invertedOverhang.substring(0, invertedOverhang.indexOf("*"));
                
                if (bestAssignment.containsKey(uninvertedOverhang)) {
                    bestAssignment.put(invertedOverhang, bestAssignment.get(uninvertedOverhang) + "*");
                } else {
                    while (assignedOverhangs.contains(String.valueOf(newOverhang))) {
                        newOverhang++;
                    }
                    bestAssignment.put(invertedOverhang, String.valueOf(newOverhang));
                    assignedOverhangs.add(String.valueOf(newOverhang));
                }
            }
        }
        
        //traverse graph and assign overhangs generate vectors
        finalOverhangHash = bestAssignment;
        ArrayList<String> freeAntibiotics = new ArrayList(Arrays.asList("ampicillin, kanamycin, ampicillin, kanamycin, ampicillin, kanamycin, ampicillin, kanamycin".toLowerCase().split(", "))); //overhangs that don't exist in part or vector library
        ArrayList<String> existingAntibiotics = new ArrayList<String>();
        HashMap<Integer, ArrayList<String>> existingAntibioticsHash = new HashMap();
        
        for (Vector v : _vectorLibrary) {
            
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

        for (RGraph graph : graphs) {
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

        //Assign vectors for all graphs
        for (RGraph graph : graphs) {
            ArrayList<RNode> queue = new ArrayList();
            HashSet<RNode> seenNodes = new HashSet();
            queue.add(graph.getRootNode());

            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }

                String currentLeftOverhang = current.getLOverhang();
                String currentRightOverhang = current.getROverhang();
                current.setLOverhang(finalOverhangHash.get(currentLeftOverhang));
                current.setROverhang(finalOverhangHash.get(currentRightOverhang));
                currentLeftOverhang = current.getLOverhang();
                currentRightOverhang = current.getROverhang();

                RVector newVector = new RVector(currentLeftOverhang, currentRightOverhang, current.getStage(), "DVL" + current.getStage(), null);
                newVector.setStringResistance(levelResistanceHash.get(current.getStage()));
                current.setVector(newVector);
            }
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
                Part part = coll.getAllPartsWithName(key, false).get(0);
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

    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generatePartPrimers(RNode node, Collector coll, Double meltingTemp, Integer targetLength) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getModularOHseqs();
        String[] oligos = new String[2];
        String partPrimerPrefix = "at";
        String partPrimerSuffix = "gt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";

        Part currentPart = coll.getPart(node.getUUID(), true);
        String seq = currentPart.getSeq();
        ArrayList<String> direction = node.getDirection();
        if ("-".equals(direction.get(0))) {
            seq = PrimerDesign.reverseComplement(seq);
        }

        String fwdHomology;
        String revHomology;

        String forwardOligoSequence;
        String reverseOligoSequence;
        if (seq.length() > 24) {
            if (seq.equals("")) {
                fwdHomology = "[ PART " + currentPart.getName() + " FORWARD HOMOLOGY REGION ]";
                revHomology = "[ PART " + currentPart.getName() + " REVERSE HOMOLOGY REGION ]";
            } else {
                fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, seq, true, true)));
                revHomology = seq.substring(Math.max(0, seq.length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(seq), true, true)));
            }

            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()) + fwdHomology;
            reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + overhangVariableSequenceHash.get(node.getROverhang()) + "ag" + revEnzymeRecSite1 + partPrimerSuffix);
        } else {
            if (seq.equals("")) {
                fwdHomology = "[ PART " + currentPart.getName() + " FORWARD HOMOLOGY REGION ]";
                revHomology = "[ PART " + currentPart.getName() + " REVERSE HOMOLOGY REGION ]";
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()) + fwdHomology + overhangVariableSequenceHash.get(node.getROverhang()) + "gt" + revEnzymeRecSite1 + partPrimerSuffix;
                reverseOligoSequence = PrimerDesign.reverseComplement(overhangVariableSequenceHash.get(node.getROverhang()) + "ag" + revEnzymeRecSite1 + partPrimerSuffix) + revHomology + PrimerDesign.reverseComplement(partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()));
            } else {
                fwdHomology = seq;
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(node.getLOverhang()) + fwdHomology + overhangVariableSequenceHash.get(node.getROverhang()) + "ag" + revEnzymeRecSite1 + partPrimerSuffix;
                reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);

            }
        }
        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }

    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generateVectorPrimers(RVector vector, Collector coll) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getModularOHseqs();
        String vectorPrimerPrefix = "actagtg";
        String vectorPrimerSuffix = "tactagt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";
        String fwdEnzymeRecSite2 = "ggtctc";
        String revEnzymeRecSite2 = "gagacc";

        String[] oligos = new String[2];

        //Level 0, 2, 4, 6, etc. vectors
        String forwardOligoSequence;
        String reverseOligoSequence;
        if (vector.getLevel() % 2 == 0) {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite2 + "a" + overhangVariableSequenceHash.get(vector.getLOverhang()) + "at" + revEnzymeRecSite1 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(vector.getROverhang()) + "a" + revEnzymeRecSite2 + vectorPrimerSuffix);

            //Level 1, 3, 5, 7, etc. vectors
        } else {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite1 + "at" + overhangVariableSequenceHash.get(vector.getLOverhang()) + "a" + revEnzymeRecSite2 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite2 + "t" + overhangVariableSequenceHash.get(vector.getROverhang()) + "at" + revEnzymeRecSite1 + vectorPrimerSuffix);
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }
    //FIELDS
    private HashMap<RNode, RNode> _parentHash; //key: node, value: parent node
    private HashMap<String, HashSet<String>> _OHexclusionHash; //key: parent node, value: all overhangs that have been seen in this step
    private HashMap<String, ArrayList<String>> _typeROHHash; //key: part type, value: all right overhangs seen for this part type
    private HashMap<String, ArrayList<String>> _typeLOHHash; //key: part type, value: all left overhangs seen for this part type
    private HashMap<Integer, HashMap<String, ArrayList<RNode>>> _stageDirectionAssignHash; //key: stage, value: HashMap: key: direction, value: nodes to visit
    private HashMap<String, ArrayList<String>> _forcedOverhangHash = new HashMap<String, ArrayList<String>>(); //key: composite part composition
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
    private ArrayList<Part> _partLibrary = new ArrayList<Part>();
    private ArrayList<Vector> _vectorLibrary = new ArrayList<Vector>();
}
