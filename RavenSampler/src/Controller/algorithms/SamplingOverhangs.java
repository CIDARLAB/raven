/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import Communication.RavenController;
import Controller.datastructures.RGraph;
import Controller.datastructures.RNode;
import Controller.datastructures.RVector;
import Controller.datastructures.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class SamplingOverhangs {

    public static void sampleOverhangs(ArrayList<RGraph> optimalGraphs) {
        HashMap<String, String> finalOverhangHash = new HashMap();
        //link graphs together using basic assignment
//        basicOverhangAssignment(optimalGraphs);
        enforceOverhangRules(optimalGraphs);
        //for each graph, compute the max number of overhangs
        int maxOverhangs = 1;
        for (RGraph graph : optimalGraphs) {
            //for each position, assign randomly
            ArrayList<RNode> basicNodes = rootBasicNodeHash.get(graph.getRootNode());
            maxOverhangs = maxOverhangs + basicNodes.size();
        }
        ArrayList<Integer> available = new ArrayList();
        for (int i = 0; i < maxOverhangs; i++) {
            available.add(i);
        }
        //pick overhangs
        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> basicNodes = rootBasicNodeHash.get(graph.getRootNode());
            ArrayList<Integer> currentAvailable = (ArrayList<Integer>) available.clone();
//            Collections.shuffle(currentAvailable);
            for (RNode node : basicNodes) {
                int rand = 0;//(int)(Math.random() * ((node.getComposition().size()) + 1))/2;
                if (basicNodes.indexOf(node) == 0) {
                    //assign new left overhang
                    if(String.valueOf(currentAvailable.get(0)) ==null ||node.getLOverhang()==null) {
                        System.out.println("moo");
                    }
                    finalOverhangHash.put(node.getLOverhang(), String.valueOf(currentAvailable.get(0)));
                    currentAvailable.remove(0);
                } else {
                    //left overhang already assigned
                }
                finalOverhangHash.put(node.getROverhang(), String.valueOf(currentAvailable.get(0)));
                if(String.valueOf(currentAvailable.get(0)) ==null || node.getROverhang()==null) {
                        System.out.println("moo");
                    }
                currentAvailable.remove(0);
            }

        }
        //assign overhangs
        ArrayList<String> freeAntibiotics = new ArrayList(Arrays.asList("ampicillin, kanamycin, ampicillin, kanamycin, ampicillin, kanamycin, ampicillin, kanamycin".toLowerCase().split(", "))); //overhangs that don't exist in part or vector library
        HashMap<Integer, ArrayList<String>> existingAntibioticsHash = new HashMap();
        HashMap<Integer, String> levelResistanceHash = new HashMap<Integer, String>(); // key: level, value: antibiotic resistance


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
                if (!RavenController.sampleOverhangs && !RavenController.samplePartitions) {
                    freeAntibiotics.remove(0);
                }
            }
            levelResistanceHash.put(i, resistance);
        }

        //Assign vectors for all graphs
        for (RGraph graph : optimalGraphs) {
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

    public static void basicOverhangAssignment(ArrayList<RGraph> optimalGraphs) {

        encounteredCompositions = new HashSet();
        parentHash = new HashMap(); //key: node, value: parent node
        HashMap<RNode, RNode> previousHash = new HashMap(); //key: node, value: sibling node on the "left"
        HashMap<RNode, RNode> nextHash = new HashMap(); //key: node, value: sibling node on the "right"
        rootBasicNodeHash = new HashMap();

        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            RNode root = graph.getRootNode();
            queue.add(root);
            parentHash.put(root, null);
            previousHash.put(root, null);
            nextHash.put(root, null);
            ArrayList<RNode> basic = new ArrayList();
            rootBasicNodeHash.put(root, basic);

            //Traverse the graph
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                current.setLOverhang("");
                current.setROverhang("");
                seenNodes.add(current);
                ArrayList<RNode> neighbors = current.getNeighbors();
                RNode previous = null;
                encounteredCompositions.add(current.getComposition().toString());

                for (RNode neighbor : neighbors) {

                    if (neighbor.getStage() == 0) {
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
    
    
    
     /**
     * First step of overhang assignment - enforce numeric place holders for
     * overhangs, ie no overhang redundancy in any step *
     */
    private static void enforceOverhangRules(ArrayList<RGraph> optimalGraphs) {

        //Initialize fields that record information to save complexity for future steps
        parentHash = new HashMap<RNode, RNode>();
        rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        int count = 0;

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            rootBasicNodeHash.put(root, l0nodes);
            root.setLOverhang(Integer.toString(count));
            count++;
            root.setROverhang(Integer.toString(count));
            count++;
            ArrayList<RNode> neighbors = root.getNeighbors();
            count = enforceOverhangRulesHelper(root, neighbors, root, count);
        }

    }

    /**
     * This helper method executes the loops necessary to enforce overhangs for
     * each graph in enforceOverhangRules *
     */
    private static int enforceOverhangRulesHelper(RNode parent, ArrayList<RNode> children, RNode root, int count) {

        String nextLOverhang = new String();

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            parentHash.put(child, parent);

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
                ArrayList<RNode> l0nodes = rootBasicNodeHash.get(root);
                l0nodes.add(child);
                rootBasicNodeHash.put(root, l0nodes);
            }
        }

        return count;
    }

    private static HashSet encounteredCompositions;
    private static HashMap parentHash;
    private static HashMap<RNode, ArrayList<RNode>> rootBasicNodeHash;
}
