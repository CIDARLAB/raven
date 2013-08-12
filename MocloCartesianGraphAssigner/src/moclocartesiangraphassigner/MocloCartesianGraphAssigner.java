/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moclocartesiangraphassigner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        root1.setLOverhang("1_");
        root1.setROverhang("4_");
        root1.setStage(1);
        partA.setLOverhang("1_");
        partA.setROverhang("2_");
        partB.setLOverhang("2_");
        partB.setROverhang("3_");
        partC.setLOverhang("3_");
        partC.setROverhang("4_");

        //for part BCD
        root2.setLOverhang("2_");
        root2.setROverhang("5_");
        root2.setStage(1);
        partD.setLOverhang("4_");
        partD.setROverhang("5_");

        //for part EF
        root3.setLOverhang("6_");
        root3.setROverhang("8_");
        root3.setStage(1);
        partE.setLOverhang("6_");
        partE.setROverhang("7_");
        partF.setLOverhang("7_");
        partF.setROverhang("8_");

        //add graphs to ArrayList
        optimalGraphs.add(graph1);
        optimalGraphs.add(graph2);
        optimalGraphs.add(graph3);

        //populate rootBasicNode hash
        _rootBasicNodeHash.put(root1, root1.getNeighbors());
        _rootBasicNodeHash.put(root2, root2.getNeighbors());
        _rootBasicNodeHash.put(root3, root3.getNeighbors());

        //generate parts
        Collector coll = new Collector();
        Part A12 = Part.generateBasic("partA", "A");
        A12.addSearchTag("LO: 1");
        A12.addSearchTag("RO: 2");
        A12.saveDefault(coll);
        Part A92 = Part.generateBasic("partA", "A");
        A92.addSearchTag("LO: 9");
        A92.addSearchTag("RO: 2");
        A92.saveDefault(coll);
        Part B23 = Part.generateBasic("partB", "B");
        B23.addSearchTag("LO: 2");
        B23.addSearchTag("RO: 3");
        B23.saveDefault(coll);
        Part B73 = Part.generateBasic("partB", "B");
        B73.addSearchTag("LO: 7");
        B73.addSearchTag("RO: 3");
        B73.saveDefault(coll);
        Part C = Part.generateBasic("partC", "C");
        C.addSearchTag("LO: 3");
        C.addSearchTag("RO: 4");
        C.saveDefault(coll);
        Part D = Part.generateBasic("partD", "D");
        D.addSearchTag("LO: 4");
        D.addSearchTag("RO: 5");
        D.saveDefault(coll);
        Part E = Part.generateBasic("partE", "E");
        E.addSearchTag("LO: 6");
        E.addSearchTag("RO: 7");
        E.saveDefault(coll);
        Part F = Part.generateBasic("partF", "F");
        F.addSearchTag("LO: 7");
        F.addSearchTag("RO: 8");
        F.saveDefault(coll);
        _partLibrary = coll.getAllParts(true);

        //build cartesian graph
        ArrayList<CartesianNode> buildCartesianGraph = assignFinalOverhangs(optimalGraphs);
        //traverse cartesian graph to assign overhangs

        //print out overhangs for verification
        System.out.println("printing out final results");
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

    public static ArrayList<CartesianNode> assignFinalOverhangs(ArrayList<RGraph> graphs) {
        //build abstractConcreteHash
        HashMap<String, HashSet<String>> abstractConcreteHash = new HashMap();
        HashMap<String, HashSet<String>> abstractLeftCompositionHash = new HashMap(); //key: abstract overhang, value: set of all compositions associated with that composition
        HashMap<String, HashSet<String>> abstractRightCompositionHash = new HashMap(); //key: composition, value: set of all abstract overhangs associated with that composition
        HashMap<String, HashSet<String>> compositionLeftConcreteHash = new HashMap();
        HashMap<String, HashSet<String>> compositionRightConcreteHash = new HashMap();
        for (RGraph graph : graphs) {
            for (RNode current : _rootBasicNodeHash.get(graph.getRootNode())) {
                if (!abstractConcreteHash.containsKey(current.getLOverhang())) {
                    abstractConcreteHash.put(current.getLOverhang(), new HashSet());
                }
                if (!abstractConcreteHash.containsKey(current.getROverhang())) {
                    abstractConcreteHash.put(current.getROverhang(), new HashSet());
                }
                if (abstractLeftCompositionHash.containsKey(current.getLOverhang())) {
                    abstractLeftCompositionHash.get(current.getLOverhang()).add(current.getComposition().toString());
                } else {
                    HashSet<String> toAddLeft = new HashSet();
                    toAddLeft.add(current.getComposition().toString());
                    abstractLeftCompositionHash.put(current.getLOverhang(), toAddLeft);
                }
                if (abstractRightCompositionHash.containsKey(current.getROverhang())) {
                    abstractRightCompositionHash.get(current.getROverhang()).add(current.getComposition().toString());
                } else {
                    HashSet<String> toAddRight = new HashSet();
                    toAddRight.add(current.getComposition().toString());
                    abstractRightCompositionHash.put(current.getROverhang(), toAddRight);
                }
            }

        }
        for (Part p : _partLibrary) {
            //populate compositionConcreteHash's
            String currentComposition = p.getStringComposition().toString();
            if (compositionLeftConcreteHash.containsKey(currentComposition)) {
                compositionLeftConcreteHash.get(currentComposition).add(p.getLeftOverhang());
                compositionRightConcreteHash.get(currentComposition).add(p.getRightOverhang());
            } else {
                HashSet<String> toAddLeft = new HashSet();
                HashSet<String> toAddRight = new HashSet();
                toAddLeft.add(p.getLeftOverhang());
                toAddRight.add(p.getRightOverhang());
                compositionLeftConcreteHash.put(currentComposition, toAddLeft);
                compositionRightConcreteHash.put(currentComposition, toAddRight);
            }
            //keep track of existing overhang pairs
        }
        for (String key : abstractLeftCompositionHash.keySet()) {
            for (String composition : abstractLeftCompositionHash.get(key)) {
                for (String concreteLeftOverhang : compositionLeftConcreteHash.get(composition)) {
                    abstractConcreteHash.get(key).add(concreteLeftOverhang);
                }
            }
            //add "new overhang" denoted by * character
            abstractConcreteHash.get(key).add("*");
        }
        for (String key : abstractRightCompositionHash.keySet()) {
            for (String composition : abstractRightCompositionHash.get(key)) {
                for (String concreteRightOverhang : compositionRightConcreteHash.get(composition)) {
                    abstractConcreteHash.get(key).add(concreteRightOverhang);
                }
            }
            //add "new overhang" denoted by * character
            abstractConcreteHash.get(key).add("*");
        }
        System.out.println("abstractConcreteHash: " + abstractConcreteHash.toString());


        //build the graph
        ArrayList<CartesianNode> previousNodes = null;
        ArrayList<CartesianNode> rootNodes = new ArrayList();
        ArrayList<String> sortedAbstractOverhangs = new ArrayList(abstractConcreteHash.keySet());
        Collections.sort(sortedAbstractOverhangs);
        int level = 0;
        for (String abstractOverhang : sortedAbstractOverhangs) {
            ArrayList<CartesianNode> currentNodes = new ArrayList();
            HashSet<String> concreteOverhangs = abstractConcreteHash.get(abstractOverhang);
            for (String overhang : concreteOverhangs) {
                CartesianNode newNode = new CartesianNode();
                newNode.setLevel(level);
                newNode.setAbstractOverhang(abstractOverhang);
                newNode.setConcreteOverhang(overhang);
                currentNodes.add(newNode);
            }
            if (previousNodes != null) {
                for (CartesianNode prev : previousNodes) {
                    for (CartesianNode node : currentNodes) {
                        if (!prev.getUsedOverhangs().contains(node.getConcreteOverhang())) {
                            prev.addNeighbor(node);
                            node.setUsedOverhangs((HashSet) prev.getUsedOverhangs().clone());
                            if (!node.getConcreteOverhang().equals("*")) {
                                node.getUsedOverhangs().add(node.getConcreteOverhang());
                            }
//                            System.out.println("linking " + prev.getAbstractOverhang() + ":" + prev.getConcreteOverhang() + " and " + node.getAbstractOverhang() + ":" + node.getConcreteOverhang());
                        }
                    }
                }
            } else {
                for (CartesianNode root : currentNodes) {
                    rootNodes.add(root);
                }
            }
            previousNodes = currentNodes;
            level++;
        }


        //find assignments
        int targetLength = abstractConcreteHash.keySet().size();
//        System.out.println("looking for this many overhangs: " + targetLength);
        ArrayList<ArrayList<String>> toReturn = new ArrayList();
        ArrayList<String> currentSolution;
        HashMap<CartesianNode, CartesianNode> parentHash = new HashMap(); //key: node, value: parent node
        for (CartesianNode root : rootNodes) {
            System.out.println("**********************");
            currentSolution = new ArrayList();
            ArrayList<CartesianNode> stack = new ArrayList();
            stack.add(root);
            boolean toParent = false; // am i returning to a parent node?
            HashSet<String> seenPaths = new HashSet();
            while (!stack.isEmpty()) {
                CartesianNode currentNode = stack.get(0);
                stack.remove(0);
                String currentPath = currentSolution.toString();
                currentPath = currentPath.substring(1, currentPath.length() - 1).replaceAll(",", "->");
                if (!toParent) {
                    currentSolution.add(currentNode.getConcreteOverhang());
                    currentPath = currentPath + "->" + currentNode.getConcreteOverhang();
                    seenPaths.add(currentPath);
                } else {
                    toParent = false;
                }
//                System.out.println("#################\ncurrent: " + currentNode.getAbstractOverhang() + ":" + currentNode.getConcreteOverhang());
//                System.out.println("current solution: " + currentSolution);
                CartesianNode parent = parentHash.get(currentNode);

                int childrenCount = 0;
                for (CartesianNode neighbor : currentNode.getNeighbors()) {
                    String edge = currentPath + "->" + neighbor.getConcreteOverhang();
                    if (!seenPaths.contains(edge)) {
//                        System.out.println(edge);
//                        System.out.println("adding edge: " + edge);
                        if (neighbor.getLevel() > currentNode.getLevel()) {
//                            System.out.println("adding: " + neighbor.getAbstractOverhang() + ":" + neighbor.getConcreteOverhang());
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
//                        System.out.println("ADDING SOLUTION");
                        toReturn.add((ArrayList<String>) currentSolution.clone());

                    } else {
                        //incomplete assignment
                    }
                    if (currentSolution.size() > 0) {
                        currentSolution.remove(currentSolution.size() - 1);
                    }
                    if (parent != null) {
                        toParent = true;
//                        System.out.println("re-adding: " + parent.getAbstractOverhang() + "|" + parent.getConcreteOverhang());
                        stack.add(0, parent);
                    }
                }

            }

        }

        //score assignments
        System.out.println("**************************************");
        for (ArrayList<String> assignment : toReturn) {
            System.out.println(assignment);
        
        }
        System.out.println("number of possible assignments: " + toReturn.size());
        //find assignments with the lowest score
        
        //assign new overhangs
        //traverse graph and assign overhangs generate vectors
        //return best assignment


        return rootNodes;
    }
    //fields
    private static HashSet<String> _encounteredCompositions; //set of part compositions that appear in the set of all graphs
    private static HashMap<RNode, RNode> _parentHash; //key: node, value: parent node
    private static HashMap<RNode, HashSet<String>> _takenParentOHs;
    private static HashMap<String, ArrayList<String>> _typeROHHash;
    private static HashMap<String, ArrayList<String>> _typeLOHHash;
    private static int _countOH;
    private static HashMap<Integer, HashMap<String, ArrayList<RNode>>> _stageDirectionAssignHash; //key: stage, value: HashMap: key: direction, value: nodes to visit
    private static HashMap<String, ArrayList<String>> _forcedOverhangHash = new HashMap(); //key: composite part composition
    private static HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash = new HashMap(); //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
    private static ArrayList<Part> _partLibrary = new ArrayList();
    private static ArrayList<Vector> _vectorLibrary = new ArrayList();
    private static HashMap<String, String> _overhangVariableSequenceHash = new HashMap(); //key:variable name, value: sequence associated with that variable
}
