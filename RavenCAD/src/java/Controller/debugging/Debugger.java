/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.debugging;

import Controller.datastructures.rGraph;
import Controller.datastructures.rNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author evanappleton
 */
public class Debugger {
    
    /** Wrapper for the Raven debugging algorithm **/
    public ArrayList<rGraph> debuggingWrapper (ArrayList<rGraph> annotatedGraphs, ArrayList<rNode> gps, HashMap<String, rGraph> partHash, ArrayList<Double> costs) {

        ArrayList<rNode> completedSteps = new ArrayList<rNode>();
        ArrayList<rNode> failedSteps = new ArrayList<rNode>();
        
        //Traverse annotated graphs to 
        for (rGraph graph : annotatedGraphs) {
            ArrayList<rNode> queue = new ArrayList<rNode>();
            HashSet<rNode> seenNodes = new HashSet<rNode>();
            rNode root = graph.getRootNode();
            queue.add(root);
            while (!queue.isEmpty()) {
                rNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                
                //Tabulate failed and successful steps
                if (current.getSuccessCnt() > 0) {
                    completedSteps.add(current);
                    rGraph newLibPart = new rGraph(current);
                    partHash.put(current.getComposition().toString(), newLibPart);
                } else {
                    failedSteps.add(current);
                }
                
                //Add unseen nodes to the queue
                ArrayList<rNode> neighbors = current.getNeighbors();
                for (rNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        gps.removeAll(completedSteps);
        ArrayList<rGraph> newGraphs = debug(gps, partHash, completedSteps, failedSteps, costs);
        
        return newGraphs;        
    }
 
    /** Debugging algorithm **/
    private ArrayList<rGraph> debug(ArrayList<rNode> gps, HashMap<String, rGraph> partHash, ArrayList<rNode> completedSteps, ArrayList<rNode> failedSteps, ArrayList<Double> costs) {
        
        analyze();
        
        return null;
    }
    
    /** Analysis algorithm to produce sets of special intermediates and overhangs **/
    private void analyze() {
        
    }
    
    /** Debugging minCost function (different from regular misCost fn) **/
    private void minCost() {
        
    }
}
