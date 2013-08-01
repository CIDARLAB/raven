/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.nonmodasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.RGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class RSLIC extends RGeneral {

     /** Clotho part wrapper for SLIC assembly **/
    public ArrayList<RGraph> slicClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectors, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, HashMap<Integer, Double> efficiencies, ArrayList<Double> costs) {
        try {
            
            //Designate how many parts can be efficiently ligated in one step
            int max = 0;
            Set<Integer> keySet = efficiencies.keySet();
            for (Integer key : keySet) {
                if (key > max) {
                    max = key;
                }
            }
            _maxNeighbors = max;

            //Initialize part hash and vector set
            HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, required, recommended);

            //Put all parts into hash for mgp algorithm            
            ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalParts);
            
            //Run hierarchical Raven Algorithm
            ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, false);
            assignOverhangs(optimalGraphs);
            
            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<RGraph> blank = new ArrayList<RGraph>();
//            E.printStackTrace();
            return blank;
        }
    }
    
    /** Assign overhangs for scarless assembly **/
    private void assignOverhangs(ArrayList<RGraph> asmGraphs) {
        
        for (int i = 0; i < asmGraphs.size(); i++) {
            
            RGraph graph = asmGraphs.get(i);
            RNode root = graph.getRootNode();
            ArrayList<RNode> neighbors = root.getNeighbors();
            assignOverhangsHelper(root, neighbors);
        }
        
    }
    
    /** Overhang assignment helper **/
    private void assignOverhangsHelper(RNode parent, ArrayList<RNode> neighbors) {

        ArrayList<RNode> children = new ArrayList<RNode>();
        
        //Get children
        for (int i = 0; i < neighbors.size(); i++) {
            RNode current = neighbors.get(i);
            if (current.getStage() < parent.getStage()) {
                children.add(current);
            }            
        }
        
        //For each of the children, assign overhangs based on neighbors
        for (int j = 0; j < children.size(); j++) {
            RNode child = children.get(j);
            
            if (j == 0) {
                ArrayList<String> nextComp = children.get(j+1).getComposition();
                child.setROverhang(nextComp.get(0));
                child.setLOverhang(parent.getLOverhang());
            } else if (j == children.size() - 1) {
                ArrayList<String> prevComp = children.get(j-1).getComposition();
                child.setLOverhang(prevComp.get(prevComp.size()-1));
                child.setROverhang(parent.getROverhang());
            } else {
                ArrayList<String> nextComp = children.get(j + 1).getComposition();
                ArrayList<String> prevComp = children.get(j - 1).getComposition();
                child.setLOverhang(prevComp.get(prevComp.size() - 1));
                child.setROverhang(nextComp.get(0));
            }
            
            ArrayList<RNode> grandChildren = child.getNeighbors();
            
            assignOverhangsHelper(child, grandChildren);
        }
    }
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
    
    public static String generateInstructions(ArrayList<RNode> roots, Collector coll) {
        return null;
    }
}
