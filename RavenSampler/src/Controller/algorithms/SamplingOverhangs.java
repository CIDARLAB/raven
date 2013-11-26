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
            for (RNode node : basicNodes) {
                if (finalOverhangHash.get(node.getLOverhang()) != null) {
                    currentAvailable.remove(Integer.parseInt(finalOverhangHash.get(node.getLOverhang())));
                }
                if (finalOverhangHash.get(node.getROverhang()) != null) {
                    currentAvailable.remove(Integer.parseInt(finalOverhangHash.get(node.getROverhang())));
                }
            }
//            Collections.shuffle(currentAvailable);
            for (RNode node : basicNodes) {
                if (basicNodes.indexOf(node) == 0 && finalOverhangHash.get(node.getLOverhang()) == null) {
                    //assign new left overhang
                    finalOverhangHash.put(node.getLOverhang(), String.valueOf(currentAvailable.get(0)));
                    currentAvailable.remove(0);
                } else {
                    //left overhang already assigned
                }
                if (finalOverhangHash.get(node.getROverhang()) == null) {
                    finalOverhangHash.put(node.getROverhang(), String.valueOf(currentAvailable.get(0)));
                }
                currentAvailable.remove(0);
            }
//            System.out.println("COMPOSITION: " + graph.getRootNode().getComposition());
//            for (RNode node : basicNodes) {
//                System.out.println(node.getLOverhang() + " " + node.getComposition() + " " + node.getROverhang());
//
//            }
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
