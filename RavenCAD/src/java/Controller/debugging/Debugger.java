/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.debugging;

import Controller.datastructures.RGraph;
import Controller.datastructures.RNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author evanappleton
 */
public class Debugger {
    
    /** Wrapper for the Raven debugging algorithm **/
    public ArrayList<RGraph> debuggingWrapper (ArrayList<RGraph> annotatedGraphs, ArrayList<RNode> gps, HashMap<String, RGraph> partHash, ArrayList<Double> costs) {

        ArrayList<RNode> completedSteps = new ArrayList<RNode>();
        ArrayList<RNode> failedSteps = new ArrayList<RNode>();
        
        //Traverse annotated graphs to 
        for (RGraph graph : annotatedGraphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            RNode root = graph.getRootNode();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                
                //Tabulate failed and successful steps
                if (current.getSuccessCnt() > 0) {
                    completedSteps.add(current);
                    RGraph newLibPart = new RGraph(current);
                    partHash.put(current.getComposition().toString(), newLibPart);
                } else {
                    failedSteps.add(current);
                }
                
                //Add unseen nodes to the queue
                ArrayList<RNode> neighbors = current.getNeighbors();
                for (RNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        gps.removeAll(completedSteps);
        ArrayList<RGraph> newGraphs = debug(gps, partHash, completedSteps, failedSteps, costs);
        
        return newGraphs;        
    }
 
    /** Debugging algorithm **/
    private ArrayList<RGraph> debug(ArrayList<RNode> gps, HashMap<String, RGraph> partHash, ArrayList<RNode> completedSteps, ArrayList<RNode> failedSteps, ArrayList<Double> costs) {
        
        analyze();
        
        return null;
    }
    
    /** Analysis algorithm to produce sets of special intermediates and overhangs **/
    private void analyze() {
        
        //FAIL - one overhang (generally speaking) is bad, change one overhang
        //FAIL - combination of parts is bad, change intermediates
        //FAIL - combination of overhangs is bad, change multiple overhangs
        //FAIL - combination of parts and overhangs is bad, change everything
        
    }
    
    /** Debugging minCost function (different from regular misCost fn) **/
    private void minCost() {
        
    }
}
