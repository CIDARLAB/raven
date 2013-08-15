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
        partF.setROverhang("1_*");

        //add graphs to ArrayList
        graph1.setStages(1);
        graph2.setStages(1);
        graph3.setStages(1);
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
//        A12.addSearchTag("LO: 1");
//        A12.addSearchTag("RO: 2");
        A12.saveDefault(coll);
//        Part A92 = Part.generateBasic("partA", "A");
//        A92.addSearchTag("LO: 9");
//        A92.addSearchTag("RO: 2");
//        A92.saveDefault(coll);
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
        assignFinalOverhangs(optimalGraphs, new HashMap());
        //traverse cartesian graph to assign overhangs     
    }

    public static void assignFinalOverhangs(ArrayList<RGraph> graphs, HashMap<String, String> finalOverhangHash) {
        HashMap<String, HashSet<String>> abstractConcreteHash = new HashMap();
        HashMap<String, HashSet<String>> abstractLeftCompositionHash = new HashMap(); //key: abstract overhang, value: set of all compositions associated with that composition
        HashMap<String, HashSet<String>> abstractRightCompositionHash = new HashMap(); //key: composition, value: set of all abstract overhangs associated with that composition
        HashMap<String, HashSet<String>> compositionLeftConcreteHash = new HashMap();
        HashMap<String, HashSet<String>> compositionRightConcreteHash = new HashMap();
        HashSet<String> compositionOverhangDirections = new HashSet(); //concatentation of compositionOverhang and direction seen in the partLibrary
        HashMap<Integer, String> levelResistanceHash = new HashMap(); // key: level, value: antibiotic resistance
        HashSet<String> invertedOverhangs = new HashSet();

        for (RGraph graph : graphs) {
            for (RNode current : _rootBasicNodeHash.get(graph.getRootNode())) {
                String currentLeftOverhang = current.getLOverhang();
                String currentRightOverhang = current.getROverhang();
                if (currentLeftOverhang.indexOf("*") < 0) { //ignore inverted overhangs
                    if (!abstractConcreteHash.containsKey(currentLeftOverhang)) {
                        abstractConcreteHash.put(currentLeftOverhang, new HashSet());
                    }
                    if (abstractLeftCompositionHash.containsKey(currentLeftOverhang)) {
                        abstractLeftCompositionHash.get(currentLeftOverhang).add(current.getComposition().toString());
                    } else {
                        HashSet<String> toAddLeft = new HashSet();
                        toAddLeft.add(current.getComposition().toString());
                        abstractLeftCompositionHash.put(currentLeftOverhang, toAddLeft);
                    }
                } else {
                    invertedOverhangs.add(currentLeftOverhang);
                }
                if (currentRightOverhang.indexOf("*") < 0) { //ignore inverted overhangs
                    if (!abstractConcreteHash.containsKey(currentRightOverhang)) {
                        abstractConcreteHash.put(currentRightOverhang, new HashSet());
                    }
                    if (abstractRightCompositionHash.containsKey(currentRightOverhang)) {
                        abstractRightCompositionHash.get(currentRightOverhang).add(current.getComposition().toString());
                    } else {
                        HashSet<String> toAddRight = new HashSet();
                        toAddRight.add(current.getComposition().toString());
                        abstractRightCompositionHash.put(currentRightOverhang, toAddRight);
                    }
                } else {
                    invertedOverhangs.add(currentRightOverhang);
                }
            }
        }
        for (Part p : _partLibrary) {
            if (p.getDirections().isEmpty()) {
                //TODO shouldn't need this once part import is fixed
                p.addSearchTag("Direction: [+]");
            }
            compositionOverhangDirections.add(p.getStringComposition() + "|" + p.getLeftOverhang() + "|" + p.getRightOverhang() + "|" + p.getDirections());
            //populate compositionConcreteHash's
            String currentComposition = p.getStringComposition().toString();
            if (compositionLeftConcreteHash.containsKey(currentComposition) || compositionRightConcreteHash.containsKey(currentComposition)) {
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
                    if (!concreteLeftOverhang.equals("")) {
                        abstractConcreteHash.get(key).add(concreteLeftOverhang);
                    }
                }
            }
            abstractConcreteHash.get(key).add("*");
        }
        for (String key : abstractRightCompositionHash.keySet()) {
            for (String composition : abstractRightCompositionHash.get(key)) {
                for (String concreteRightOverhang : compositionRightConcreteHash.get(composition)) {
                    if (!concreteRightOverhang.equals("")) {
                        abstractConcreteHash.get(key).add(concreteRightOverhang);
                    }
                }
            }
            //add "new overhang" denoted by * character
            abstractConcreteHash.get(key).add("*");
        }

        //build the graph
        ArrayList<CartesianNode> previousNodes = null;
        ArrayList<CartesianNode> rootNodes = new ArrayList();
        ArrayList<String> sortedAbstractOverhangs = new ArrayList(abstractConcreteHash.keySet());
        Collections.sort(sortedAbstractOverhangs);
        int level = 0;
//        System.out.println("digraph{");
        for (String abstractOverhang : sortedAbstractOverhangs) {
            ArrayList<CartesianNode> currentNodes = new ArrayList();
            HashSet<String> concreteOverhangs = abstractConcreteHash.get(abstractOverhang);
            for (String overhang : concreteOverhangs) {
                CartesianNode newNode = new CartesianNode();
                newNode.setLevel(level);
                newNode.setAbstractOverhang(abstractOverhang);
                newNode.setConcreteOverhang(overhang.trim());
                currentNodes.add(newNode);
            }
            if (previousNodes != null) {
                for (CartesianNode prev : previousNodes) {
                    for (CartesianNode current : currentNodes) {
                        if (!prev.getConcreteOverhang().equals(current.getConcreteOverhang()) || current.getConcreteOverhang().equals("*")) {
//                            System.out.println("\"" + prev.id + ": " + prev.getAbstractOverhang() + "-" + prev.getConcreteOverhang() + "\" -> \"" + current.id + ": " + current.getAbstractOverhang() + "-" + current.getConcreteOverhang() + "\"");
                            prev.addNeighbor(current);
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
//        System.out.println("}");
        //find assignments
        int targetLength = sortedAbstractOverhangs.size(); //number of abstract overhangs
        //each value is a potential concrete assignment, 
        //the first value in each assignment corresponds to the first sortedAbstractOverhang
        ArrayList<ArrayList<String>> completeAssignments = new ArrayList();
        ArrayList<String> currentSolution;
        HashMap<CartesianNode, CartesianNode> parentHash = new HashMap(); //key: node, value: parent node
        for (CartesianNode root : rootNodes) {
            currentSolution = new ArrayList();
            ArrayList<CartesianNode> stack = new ArrayList();
            stack.add(root);
            boolean toParent = false; // am i returning to a parent node?
            HashSet<String> seenPaths = new HashSet();
            while (!stack.isEmpty()) {
                CartesianNode currentNode = stack.get(0);
                stack.remove(0);
                String currentPath = currentSolution.toString();
                currentPath = currentPath.substring(1, currentPath.length() - 1).replaceAll(",", "->").replaceAll(" ", "");
                if (!toParent) {
                    currentSolution.add(currentNode.getConcreteOverhang());
                    currentPath = currentPath + "->" + currentNode.getConcreteOverhang();
                    seenPaths.add(currentPath);
                } else {
                    toParent = false;
                }
                CartesianNode parent = parentHash.get(currentNode);
                int childrenCount = 0;
                for (CartesianNode neighbor : currentNode.getNeighbors()) {
                    if (currentPath.indexOf(neighbor.getConcreteOverhang()) < 0 || neighbor.getConcreteOverhang().equals("*")) {
                        String edge = currentPath + "->" + neighbor.getConcreteOverhang();
                        if (!seenPaths.contains(edge)) {
                            if (neighbor.getLevel() > currentNode.getLevel()) {
                                stack.add(0, neighbor);
                                parentHash.put(neighbor, currentNode);
                                childrenCount++;
                            }
                        }
                    }

                }
                if (childrenCount == 0) {
                    //no children means we've reached the end of a branch
                    if (currentSolution.size() == targetLength) {
                        //yay complete assignment
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
        //score assignments
        ArrayList<RNode> basicNodes = new ArrayList();
        for (RNode key : _rootBasicNodeHash.keySet()) {
            for (RNode basicNode : _rootBasicNodeHash.get(key)) {
                if (!basicNodes.contains(basicNode)) {
                    basicNodes.add(basicNode);
                }
            }
        }
        int bestScore = 1000000000;
        HashMap<String, String> bestAssignment = null;
        for (ArrayList<String> assignment : completeAssignments) {
            HashMap<String, String> currentAssignment = new HashMap();
            int currentScore = 0;
            //handle forced overhangs
            for (int i = 0; i < sortedAbstractOverhangs.size(); i++) {
                String currentAbstractOverhang = sortedAbstractOverhangs.get(i);
                if (finalOverhangHash.containsKey(currentAbstractOverhang)) {
                    currentAssignment.put(currentAbstractOverhang, finalOverhangHash.get(currentAbstractOverhang));
                } else {
                    currentAssignment.put(sortedAbstractOverhangs.get(i), assignment.get(i));
                }
            }
            //handle inverted overhangs
            for (String invertedOverhang : invertedOverhangs) {
                if (finalOverhangHash.containsKey(invertedOverhang)) {
                    currentAssignment.put(invertedOverhang, finalOverhangHash.get(invertedOverhang));
                } else {
                    String uninvertedOverhang = invertedOverhang.substring(0, invertedOverhang.indexOf("*"));
                    if (currentAssignment.containsKey(uninvertedOverhang)) {
                        String uninvertedOverhangAssignment = currentAssignment.get(uninvertedOverhang);
                        String invertedOverhangAssignment = "";
                        if (uninvertedOverhangAssignment.equals("*")) {
                            currentAssignment.put(invertedOverhang, "*");
                        } else {
                            if (uninvertedOverhangAssignment.indexOf("*") > -1) {
                                invertedOverhangAssignment = uninvertedOverhangAssignment.substring(0, uninvertedOverhangAssignment.indexOf("*"));
                            } else {
                                invertedOverhangAssignment = uninvertedOverhangAssignment + "*";
                            }
                            currentAssignment.put(invertedOverhang, invertedOverhangAssignment);
                        }
                    } else {
                        currentAssignment.put(invertedOverhang, "*");
                    }
                }
            }
            HashSet<String> matched = new HashSet();
            for (RNode basicNode : basicNodes) {
                String compositionOverhangDirectionString = basicNode.getComposition() + "|" + currentAssignment.get(basicNode.getLOverhang()) + "|" + currentAssignment.get(basicNode.getROverhang()) + "|" + basicNode.getDirection();
                if (!compositionOverhangDirections.contains(compositionOverhangDirectionString)) {
                    currentScore++;
                } else {
                    matched.add(compositionOverhangDirectionString);
                }
            }
            currentScore = currentScore - matched.size();
            if (currentScore < bestScore) {
                bestScore = currentScore;
                bestAssignment = currentAssignment;
            } else {
                //just to save memory
//                assignment.clear();
            }
        }
        //generate new overhangs
        HashSet<String> assignedOverhangs = new HashSet(bestAssignment.values());
        int newOverhang = 0;
        for (String starAbstract : sortedAbstractOverhangs) {
            if (bestAssignment.get(starAbstract).equals("*")) {
                while (assignedOverhangs.contains(String.valueOf(newOverhang))) {
                    newOverhang++;
                }
                bestAssignment.put(starAbstract, String.valueOf(newOverhang));
                assignedOverhangs.add(String.valueOf(newOverhang));
            }
        }
        //generate matching new overhangs for inverted overhans
        for (String invertedOverhang : invertedOverhangs) {
            if (bestAssignment.get(invertedOverhang).equals("*")) {
                String uninvertedOverhang = invertedOverhang.substring(0, invertedOverhang.indexOf("*"));
                bestAssignment.put(invertedOverhang, bestAssignment.get(uninvertedOverhang)+"*");
            }
        }
        //assign new overhangs
        finalOverhangHash = bestAssignment;
        //traverse graph and assign overhangs generate vectors
        ArrayList<String> freeAntibiotics = new ArrayList(Arrays.asList("chloramphenicol, kanamycin, ampicillin, chloramphenicol, kanamycin, ampicillin, chloramphenicol, kanamycin, ampicillin, chloramphenicol, kanamycin, ampicillin, neomycin, puromycin, spectinomycin, streptomycin".toLowerCase().split(", "))); //overhangs that don't exist in part or vector library
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

        for (RGraph graph : graphs) {
            ArrayList<RNode> queue = new ArrayList();
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
                newVector.setName("DVL" + current.getStage());
                newVector.setStringResistance(levelResistanceHash.get(current.getStage()));
                current.setVector(newVector);
                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
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
