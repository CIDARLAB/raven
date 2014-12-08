/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.debugging;

import Controller.datastructures.RGraph;
import Controller.datastructures.RNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class FailureSimulator {
    
    /** Simulate experimental assembly results **/
    public void simulateResults(ArrayList<RGraph> solutionGraphs, Integer allowedAttempts) {
        
        HashMap<Integer, ArrayList<RNode>> stageHash = getStageHash(solutionGraphs);
        Set<Integer> keySet = stageHash.keySet();
        ArrayList<Integer> stages = new ArrayList<Integer>(keySet);
        Collections.sort(stages);
        
        //Simulate results one stage at a time, starting with stage 1
        if (stages.size() > 1) {
            for (int i = 1; i < stages.size(); i++) {

                ArrayList<RNode> stageNodes = stageHash.get(i);

                //For each node in this stage, 
                for (int j = 0; j < stageNodes.size(); j++) {
                    RNode node = stageNodes.get(j);
                    int successCnt = node.getSuccessCnt();
                    int failureCnt = node.getFailureCnt();
                    ArrayList<RNode> neighbors = node.getNeighbors();
                    boolean canAttempt = true;
                    
                    //Determine if all of the children have been successfully constructed... assumed to be true if stage == 0
                    for (int k = 0; k < neighbors.size(); k++) {
                        RNode neighbor = neighbors.get(k);
                        if (neighbor.getStage() < node.getStage()) {
                            if (neighbor.getStage() > 0 && neighbor.getSuccessCnt() < 1) {
                                canAttempt = false;
                            }
                        }
                    }
                    
                    //If the step has yet to be successfully completed and can be attempted
                    if (successCnt < 1 && canAttempt) {
                        int attempts = 0;
                        Random success = new Random();
                        
                        //Simulate random attempts using a gaussian simulation and a cut off
                        while (attempts < allowedAttempts && successCnt < 0) {
                            
                            //Potentially work the efficiency in here as the chance of success
                            double nextGaussian = success.nextGaussian();
                            if (nextGaussian > 1.0) {
                                successCnt++;
                            } else {
                                failureCnt++;
                            }
                        }
                    }
                    
                    node.setSuccessCnt(successCnt);
                    node.setFailureCnt(failureCnt);
                }
            }
        }
    }
    
    
    /**
     * Get the stage hash for a set of optimal graphs *
     */
    public static HashMap<Integer, ArrayList<RNode>> getStageHash(ArrayList<RGraph> optimalGraphs) {

        HashMap<Integer, ArrayList<RNode>> stageHash = new HashMap<Integer, ArrayList<RNode>>();
        ArrayList<RNode> stageNodes = new ArrayList<RNode>();

        for (int i = 0; i < optimalGraphs.size(); i++) {

            //Traverse one graph and store the nodes
            RGraph graph = optimalGraphs.get(i);
            RNode rootNode = graph.getRootNode();
            if (stageHash.containsKey(rootNode.getStage())) {
                stageNodes = stageHash.get(rootNode.getStage());
            }
            stageNodes.add(rootNode);
            stageHash.put(rootNode.getStage(), stageNodes);
            stageHash = getStageHashHelper(rootNode, rootNode.getNeighbors(), stageHash);

            //Put an extra node on the end of all stage of each optimal graph... nodes from adjacent graphs are not neighbors
            if (i != (optimalGraphs.size() - 1)) {
                Set<Integer> keySet = stageHash.keySet();
                for (Integer stage : keySet) {
                    ArrayList<RNode> finalStageNodes = new ArrayList<RNode>();
                    finalStageNodes.addAll(stageHash.get(stage));
                    RNode spacer = new RNode();
                    finalStageNodes.add(spacer);
                    stageHash.put(stage, finalStageNodes);
                }
            }
        }

        return stageHash;
    }

    private static HashMap<Integer, ArrayList<RNode>> getStageHashHelper(RNode parent, ArrayList<RNode> neighbors, HashMap<Integer, ArrayList<RNode>> stageHash) {

        //Check the current stageHash to get nodes that are already in there
        ArrayList<RNode> stageNodes = new ArrayList<RNode>();
        if (stageHash.containsKey(parent.getStage() - 1)) {
            stageNodes = stageHash.get(parent.getStage() - 1);
        }

        for (int i = 0; i < neighbors.size(); i++) {
            RNode neighbor = neighbors.get(i);
            stageNodes.add(neighbor);
            if (neighbor.getStage() > 0) {
                ArrayList<RNode> orderedChildren = new ArrayList<RNode>();
                orderedChildren.addAll(neighbor.getNeighbors());

                //Remove the current parent from the list
                if (orderedChildren.contains(parent)) {
                    orderedChildren.remove(parent);
                }
                stageHash = getStageHashHelper(neighbor, orderedChildren, stageHash);
            }
        }
        stageHash.put(parent.getStage() - 1, stageNodes);
        return stageHash;
    }
}
