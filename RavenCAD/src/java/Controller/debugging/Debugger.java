/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.debugging;

import Controller.datastructures.SRSGraph;
import Controller.datastructures.SRSNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author evanappleton
 */
public class Debugger {
    
    /** Wrapper for the Raven debugging algorithm **/
    public ArrayList<SRSGraph> debuggingWrapper (ArrayList<SRSGraph> annotatedGraphs, ArrayList<SRSNode> gps, HashMap<String, SRSGraph> partHash, ArrayList<Double> costs) {

        ArrayList<SRSNode> completedSteps = new ArrayList<SRSNode>();
        ArrayList<SRSNode> failedSteps = new ArrayList<SRSNode>();
        
        //Traverse annotated graphs to 
        for (SRSGraph graph : annotatedGraphs) {
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            SRSNode root = graph.getRootNode();
            queue.add(root);
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                
                //Tabulate failed and successful steps
                if (current.getSuccess() == true) {
                    completedSteps.add(current);
                    SRSGraph newLibPart = new SRSGraph(current);
                    partHash.put(current.getComposition().toString(), newLibPart);
                } else if (current.getSuccess() == false) {
                    failedSteps.add(current);
                }
                
                //Add unseen nodes to the queue
                ArrayList<SRSNode> neighbors = current.getNeighbors();
                for (SRSNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        gps.removeAll(completedSteps);
        ArrayList<SRSGraph> newGraphs = debug(gps, partHash, completedSteps, failedSteps, costs);
        
        return newGraphs;        
    }
 
    /** Debugging algorithm **/
    private ArrayList<SRSGraph> debug(ArrayList<SRSNode> gps, HashMap<String, SRSGraph> partHash, ArrayList<SRSNode> completedSteps, ArrayList<SRSNode> failedSteps, ArrayList<Double> costs) {
        
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
