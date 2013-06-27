/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.debugging;

import Controller.datastructures.rGraph;
import Controller.algorithms.Modularity;
import Controller.datastructures.rNode;
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
    public void simulateResults(ArrayList<rGraph> solutionGraphs, Integer allowedAttempts) {
        
        HashMap<Integer, ArrayList<rNode>> stageHash = Modularity.getStageHash(solutionGraphs);
        Set<Integer> keySet = stageHash.keySet();
        ArrayList<Integer> stages = new ArrayList<Integer>(keySet);
        Collections.sort(stages);
        
        //Simulate results one stage at a time, starting with stage 1
        if (stages.size() > 1) {
            for (int i = 1; i < stages.size(); i++) {

                ArrayList<rNode> stageNodes = stageHash.get(i);

                //For each node in this stage, 
                for (int j = 0; j < stageNodes.size(); j++) {
                    rNode node = stageNodes.get(j);
                    int successCnt = node.getSuccessCnt();
                    int failureCnt = node.getFailureCnt();
                    ArrayList<rNode> neighbors = node.getNeighbors();
                    boolean canAttempt = true;
                    
                    //Determine if all of the children have been successfully constructed... assumed to be true if stage == 0
                    for (int k = 0; k < neighbors.size(); k++) {
                        rNode neighbor = neighbors.get(k);
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
    
}
