/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moclocartesiangraphassigner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class MocloCartesianGraphAssigner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        Methods calls for old cartesian assignment
//        HashMap<String, ArrayList<String>> compositionOverhangHash = new HashMap();
//        ArrayList<String> composition = new ArrayList(Arrays.asList(new String[]{"partA", "partB", "partC", "partD"}));
//        compositionOverhangHash.put("partA", new ArrayList(Arrays.asList(new String[]{"B|C", "A|B", "C|D", "D|E", "X|Y"})));
//        compositionOverhangHash.put("partB", new ArrayList(Arrays.asList(new String[]{"A|B", "B|C", "C|D", "D|E", "Y|A", "Y|C", "| | |"})));
//        compositionOverhangHash.put("partC", new ArrayList(Arrays.asList(new String[]{"A|B", "B|C", "C|D", "D|E"})));
//        compositionOverhangHash.put("partD", new ArrayList(Arrays.asList(new String[]{"A|B", "B|C", "C|D", "D|E", "E|G", "| | |"})));
//        ArrayList<SRSGraph> buildCartesianGraph = buildCartesianGraph(composition, compositionOverhangHash);
//        for (ArrayList<String> solution : findOptimalAssignment(buildCartesianGraph, composition.size())) {
//            System.out.println("assignment: " + solution);
//        }
        //build example
        ArrayList<RGraph> optimalGraphs = new ArrayList();
        //part ABC
        RNode root1 = new RNode();
        root1.setComposition(new ArrayList(Arrays.asList(new String[]{"partA", "partB", "partC"})));
        root1.setName("ABC");
        RNode partA = new RNode();
        partA.setComposition(new ArrayList(Arrays.asList(new String[]{"partA"})));
        RNode partB = new RNode();
        partB.setComposition(new ArrayList(Arrays.asList(new String[]{"partB"})));
        RNode partC = new RNode();
        partC.setComposition(new ArrayList(Arrays.asList(new String[]{"partC"})));
        root1.addNeighbor(partA);
        root1.addNeighbor(partB);
        root1.addNeighbor(partC);
        RGraph graph1 = new RGraph(root1);

        //part BCD
        RNode root2 = new RNode();
        root2.setComposition(new ArrayList(Arrays.asList(new String[]{"partB", "partC", "partD"})));
        root2.setName("BCD");
        RNode partD = new RNode();
        partD.setComposition(new ArrayList(Arrays.asList(new String[]{"partD"})));
        root2.addNeighbor(partB);
        root2.addNeighbor(partC);
        root2.addNeighbor(partD);
        RGraph graph2 = new RGraph(root2);

        //part EF
        RNode root3 = new RNode();
        root3.setComposition(new ArrayList(Arrays.asList(new String[]{"partE", "partF"})));
        root3.setName("EF");
        RNode partE = new RNode();
        partE.setComposition(new ArrayList(Arrays.asList(new String[]{"partE"})));
        RNode partF = new RNode();
        partF.setComposition(new ArrayList(Arrays.asList(new String[]{"partF"})));
        root3.addNeighbor(partE);
        root3.addNeighbor(partF);
        RGraph graph3 = new RGraph(root3);

        //assign overhangs to the nodes
        //for part ABC
        root1.setLOverhang("1");
        root1.setROverhang("4");
        root1.setStage(1);
        partA.setLOverhang("1");
        partA.setROverhang("2");
        partB.setLOverhang("2");
        partB.setROverhang("3");
        partC.setLOverhang("3");
        partC.setROverhang("4");

        //for part BCD
        root2.setLOverhang("2");
        root2.setROverhang("5");
        root2.setStage(1);
        partD.setLOverhang("4");
        partD.setROverhang("5");

        //for part EF
        root3.setLOverhang("6");
        root3.setROverhang("8");
        root3.setStage(1);
        partE.setLOverhang("6");
        partE.setROverhang("7");
        partF.setLOverhang("7");
        partF.setROverhang("8");

        //add graphs to ArrayList
        optimalGraphs.add(graph1);
        optimalGraphs.add(graph2);
        optimalGraphs.add(graph3);

        //populate rootBasicNode hash
        _rootBasicNodeHash.put(root1, root1.getNeighbors());
        _rootBasicNodeHash.put(root2, root2.getNeighbors());
        _rootBasicNodeHash.put(root3, root3.getNeighbors());

        //build cartesian graph
        ArrayList<CartesianNode> buildCartesianGraph = buildCartesianGraph(optimalGraphs);
        //traverse cartesian graph to assign overhangs

        //print out overhangs for verification
        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> queue = new ArrayList();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                System.out.println(current.getComposition() + " " + current.getLOverhang() + "|" + current.getROverhang());
                for (RNode neighbor : current.getNeighbors()) {
                    if (current.getStage() > neighbor.getStage()) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    public static ArrayList<CartesianNode> buildCartesianGraph(ArrayList<RGraph> graphs) {
        //build abstractConcreteHash

        //build meta node graph
        HashMap<String, ArrayList<String>> relationshipHash = new HashMap(); //key: left overhang, value: right overhang
        HashMap<String, CartesianMetaNode> overhangMetaNodeHash= new HashMap(); //key: overhang, value:CartesianMetaNode that corresponds to that Hash
        for (RGraph graph : graphs) {
            RNode rootNode = graph.getRootNode();
            ArrayList<RNode> basicNodes = _rootBasicNodeHash.get(rootNode);
            
            for (RNode basicNode : basicNodes) {
                ArrayList<String> rightOverhangs = relationshipHash.get(basicNode.getLOverhang());
                if(!overhangMetaNodeHash.containsKey(basicNode.getLOverhang())) {
                    overhangMetaNodeHash.put(basicNode.getLOverhang(), new CartesianMetaNode(basicNode.getLOverhang()));
                }
                if(!overhangMetaNodeHash.containsKey(basicNode.getROverhang())) {
                    overhangMetaNodeHash.put(basicNode.getROverhang(), new CartesianMetaNode(basicNode.getROverhang()));
                }
                if (rightOverhangs != null) {
                    if(!rightOverhangs.contains(basicNode.getROverhang())) {
                        rightOverhangs.add(basicNode.getROverhang());
                    }
                } else {
                    relationshipHash.put(basicNode.getLOverhang(), new ArrayList(Arrays.asList(new String[]{basicNode.getROverhang()})));
                }
            }
        }


        return null;
    }

    public void findOptimalAssignment(ArrayList<RGraph> optimalGraphs, ArrayList<CartesianNode> cartesianGraphs) {
    }
    
    
    
    //old single graph method
    public static ArrayList<RGraph> buildCartesianGraph(ArrayList<String> composition, HashMap<String, ArrayList<String>> compositionOverhangHash) {
        ArrayList<RNode> previousNodes = null;
        ArrayList<RGraph> toReturn = new ArrayList();
        int stage = 0;
        for (String part : composition) {
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
//                System.out.println("creating node: " + part + "|" + leftOverhang + "|" + rightOverhang);
                currentNodes.add(newNode);
            }
            if (previousNodes != null) {
                for (RNode prev : previousNodes) {
                    for (RNode curr : currentNodes) {
                        if (prev.getROverhang().equals(curr.getLOverhang())) {
                            prev.addNeighbor(curr);
//                            System.out.println("linking " + prev.getName() + "|" + prev.getLOverhang() + "|" + prev.getROverhang() + " and " + curr.getName() + "|" + curr.getLOverhang() + "|" + curr.getROverhang());
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
    //old single graph method
    public static ArrayList<ArrayList<String>> findOptimalAssignment(ArrayList<RGraph> graphs, int targetLength) {
        ArrayList<ArrayList<String>> toReturn = new ArrayList();
        ArrayList<String> currentSolution;
        HashMap<RNode, RNode> parentHash = new HashMap(); //key: node, value: parent node
        for (RGraph graph : graphs) {
            System.out.println("**********************");
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
                System.out.println("#################\ncurrent: " + currentNode.getName() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                if (!toParent) {
                    currentSolution.add(currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                } else {
                    toParent = false;
                }
                System.out.println(currentSolution);
                RNode parent = parentHash.get(currentNode);

                int childrenCount = 0;
                for (RNode neighbor : currentNode.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        if (neighbor.getStage() > currentNode.getStage()) {
                            System.out.println("adding: " + neighbor.getName() + "|" + neighbor.getLOverhang() + "|" + neighbor.getROverhang());
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
                        System.out.println("ADDING SOLUTION");
                        toReturn.add((ArrayList<String>) currentSolution.clone());

                    } else {
                        //incomplete assignment
                    }
                    if (currentSolution.size() > 0) {
                        currentSolution.remove(currentSolution.size() - 1);
                    }
                    if (parent != null) {
//                        parent.getNeighbors().remove(currentNode);
//                        System.out.println("removing: " +currentNode.getName()+"|"+currentNode.getLOverhang()+"|"+currentNode.getROverhang()+" from "+ parent.getName() + "|" + parent.getLOverhang() + "|" + parent.getROverhang());
                        toParent = true;
                        System.out.println("readding: " + parent.getName() + "|" + parent.getLOverhang() + "|" + parent.getROverhang());
                        stack.add(0, parent);
                    }
                }

            }

        }
        return toReturn;
    }
    
    private static HashSet<String> _encounteredCompositions; //set of part compositions that appear in the set of all graphs
    private static HashMap<RNode, RNode> _parentHash; //key: node, value: parent node
    private static HashMap<RNode, HashSet<String>> _takenParentOHs;
    private static HashMap<String, ArrayList<String>> _typeROHHash;
    private static HashMap<String, ArrayList<String>> _typeLOHHash;
    private static int _countOH;
    private static HashMap<Integer, HashMap<String, ArrayList<RNode>>> _stageDirectionAssignHash; //key: stage, value: HashMap: key: direction, value: nodes to visit
    private static HashMap<String, ArrayList<String>> _forcedOverhangHash = new HashMap(); //key: composite part composition
    private static HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash = new HashMap(); //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
//    private ArrayList<Part> _partLibrary = new ArrayList();
//    private ArrayList<Vector> _vectorLibrary = new ArrayList();
    private static HashMap<String, String> _overhangVariableSequenceHash = new HashMap(); //key:variable name, value: sequence associated with that variable
}
