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
        HashMap<String, ArrayList<String>> compositionOverhangHash = new HashMap();
        ArrayList<String> composition = new ArrayList(Arrays.asList(new String[]{"partA", "partB", "partC", "partD"}));
        compositionOverhangHash.put("partA", new ArrayList(Arrays.asList(new String[]{"B|C", "A|B", "C|D", "D|E", "X|Y"})));
        compositionOverhangHash.put("partB", new ArrayList(Arrays.asList(new String[]{"A|B", "B|C", "C|D", "D|E", "Y|A", "Y|C", "| | |"})));
        compositionOverhangHash.put("partC", new ArrayList(Arrays.asList(new String[]{"A|B", "B|C", "C|D", "D|E"})));
        compositionOverhangHash.put("partD", new ArrayList(Arrays.asList(new String[]{"A|B", "B|C", "C|D", "D|E", "E|G", "| | |"})));
        ArrayList<SRSGraph> buildCartesianGraph = buildCartesianGraph(composition, compositionOverhangHash);
        for (ArrayList<String> solution : findOptimalAssignment(buildCartesianGraph, composition.size())) {
            System.out.println("assignment: " + solution);
        }
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
