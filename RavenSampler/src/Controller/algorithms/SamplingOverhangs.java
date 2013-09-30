/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import Controller.datastructures.RGraph;
import Controller.datastructures.RNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class SamplingOverhangs {

    public static ArrayList<RGraph> sampleOverhangs(ArrayList<RGraph> optimalGraphs) {
        //for each graph, compute the max number of overhangs
        
        for(RGraph graph:optimalGraphs) {
            
        }
        //for each position, assign randomly
        
        //assign overhangs
        return null;
    }
    private static HashSet encounteredCompositions;
    private static HashMap parentHash;
    private static HashMap compositionLevelHash;
    private static HashMap rootBasicNodeHash;
    
    
    
    public static void basicOverhangAssignment(ArrayList<RGraph> optimalGraphs) {

        encounteredCompositions = new HashSet();
        parentHash = new HashMap(); //key: node, value: parent node
        HashMap<RNode, RNode> previousHash = new HashMap(); //key: node, value: sibling node on the "left"
        HashMap<RNode, RNode> nextHash = new HashMap(); //key: node, value: sibling node on the "right"
        compositionLevelHash = new HashMap();
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

}
