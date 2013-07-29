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
        ArrayList<SRSGraph> optimalGraphs = new ArrayList();
        //part ABC
        SRSNode root1 = new SRSNode();
        root1.setComposition(new ArrayList(Arrays.asList(new String[]{"partA", "partB", "partC"})));
        root1.setName("ABC");
        SRSNode partA = new SRSNode();
        partA.setComposition(new ArrayList(Arrays.asList(new String[]{"partA"})));
        SRSNode partB = new SRSNode();
        partB.setComposition(new ArrayList(Arrays.asList(new String[]{"partB"})));
        SRSNode partC = new SRSNode();
        partC.setComposition(new ArrayList(Arrays.asList(new String[]{"partC"})));
        root1.addNeighbor(partA);
        root1.addNeighbor(partB);
        root1.addNeighbor(partC);
        SRSGraph graph1 = new SRSGraph(root1);

        //part BCD
        SRSNode root2 = new SRSNode();
        root2.setComposition(new ArrayList(Arrays.asList(new String[]{"partB", "partC", "partD"})));
        root2.setName("BCD");
        SRSNode partD = new SRSNode();
        partD.setComposition(new ArrayList(Arrays.asList(new String[]{"partD"})));
        root2.addNeighbor(partB);
        root2.addNeighbor(partC);
        root2.addNeighbor(partD);
        SRSGraph graph2 = new SRSGraph(root2);

        //part EF
        SRSNode root3 = new SRSNode();
        root3.setComposition(new ArrayList(Arrays.asList(new String[]{"partE", "partF"})));
        root3.setName("EF");
        SRSNode partE = new SRSNode();
        partE.setComposition(new ArrayList(Arrays.asList(new String[]{"partE"})));
        SRSNode partF = new SRSNode();
        partF.setComposition(new ArrayList(Arrays.asList(new String[]{"partF"})));
        root3.addNeighbor(partE);
        root3.addNeighbor(partF);
        SRSGraph graph3 = new SRSGraph(root3);

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
        //build cartesian graph
        
        //traverse cartesian graph to assign overhangs

        //print out overhangs for verification
        for(SRSGraph graph:optimalGraphs) {
            ArrayList<SRSNode> queue = new ArrayList();
            queue.add(graph.getRootNode());
            while(!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                System.out.println(current.getComposition()+" "+current.getLOverhang()+"|"+current.getROverhang());
                for(SRSNode neighbor: current.getNeighbors()) {
                    if(current.getStage()>neighbor.getStage()) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    public ArrayList<CartesianNode> buildCartesianGraph(ArrayList<SRSGraph> graphs) {
        
        
        return null;
    }
    
    public void findOptimalAssignment(ArrayList<SRSGraph> optimalGraphs, ArrayList<CartesianNode> cartesianGraphs) {
        
    }
    public static ArrayList<SRSGraph> buildCartesianGraph(ArrayList<String> composition, HashMap<String, ArrayList<String>> compositionOverhangHash) {
        ArrayList<SRSNode> previousNodes = null;
        ArrayList<SRSGraph> toReturn = new ArrayList();
        int stage = 0;
        for (String part : composition) {
            ArrayList<SRSNode> currentNodes = new ArrayList();
            ArrayList<String> existingOverhangs = compositionOverhangHash.get(part);
            for (String overhangPair : existingOverhangs) {
                String[] tokens = overhangPair.split("\\|");
                String leftOverhang = tokens[0];
                String rightOverhang = tokens[1];
                SRSNode newNode = new SRSNode();
                newNode.setName(part);
                newNode.setLOverhang(leftOverhang);
                newNode.setROverhang(rightOverhang);
                newNode.setStage(stage);
//                System.out.println("creating node: " + part + "|" + leftOverhang + "|" + rightOverhang);
                currentNodes.add(newNode);
            }
            if (previousNodes != null) {
                for (SRSNode prev : previousNodes) {
                    for (SRSNode curr : currentNodes) {
                        if (prev.getROverhang().equals(curr.getLOverhang())) {
                            prev.addNeighbor(curr);
//                            System.out.println("linking " + prev.getName() + "|" + prev.getLOverhang() + "|" + prev.getROverhang() + " and " + curr.getName() + "|" + curr.getLOverhang() + "|" + curr.getROverhang());
                        }
                    }
                }
            } else {
                for (SRSNode root : currentNodes) {
                    toReturn.add(new SRSGraph(root));
                }
            }
            previousNodes = currentNodes;
            stage++;

        }
        return toReturn;
    }

    public static ArrayList<ArrayList<String>> findOptimalAssignment(ArrayList<SRSGraph> graphs, int targetLength) {
        ArrayList<ArrayList<String>> toReturn = new ArrayList();
        ArrayList<String> currentSolution;
        HashMap<SRSNode, SRSNode> parentHash = new HashMap(); //key: node, value: parent node
        for (SRSGraph graph : graphs) {
            System.out.println("**********************");
            currentSolution = new ArrayList();
            SRSNode root = graph.getRootNode();
            ArrayList<SRSNode> stack = new ArrayList();
            stack.add(root);
            boolean toParent = false; // am i returning to a parent node?
            HashSet<SRSNode> seenNodes = new HashSet();
            while (!stack.isEmpty()) {
                SRSNode currentNode = stack.get(0);
                stack.remove(0);
                seenNodes.add(currentNode);
                System.out.println("#################\ncurrent: " + currentNode.getName() + "|" + currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                if (!toParent) {
                    currentSolution.add(currentNode.getLOverhang() + "|" + currentNode.getROverhang());
                } else {
                    toParent = false;
                }
                System.out.println(currentSolution);
                SRSNode parent = parentHash.get(currentNode);

                int childrenCount = 0;
                for (SRSNode neighbor : currentNode.getNeighbors()) {
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
}
