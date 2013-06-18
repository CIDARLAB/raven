/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.nonmodasm;

import Controller.algorithms.SRSGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class SRSSLIC extends SRSGeneral {

     /** Clotho part wrapper for SLIC assembly **/
    public ArrayList<SRSGraph> slicClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectors, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies) {
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
            HashMap<String, SRSGraph> partHash = partImportClotho(goalParts, partLibrary, required, recommended);

            //Put all parts into hash for mgp algorithm            
            ArrayList<SRSNode> gpsNodes = gpsToNodesClotho(goalParts);

            //Positional scoring of transcriptional units
            HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
            if (modular) {
                ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
                positionScores = getPositionalScoring(TUs);
            }
            
            //Run SDS Algorithm for multiple parts
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, discouraged, partHash, positionScores, efficiencies, false);
            assignOverhangs(optimalGraphs);
            
            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
//            E.printStackTrace();
            return blank;
        }
    }
    
    /** Assign overhangs for scarless assembly **/
    private void assignOverhangs(ArrayList<SRSGraph> asmGraphs) {
        
        for (int i = 0; i < asmGraphs.size(); i++) {
            
            SRSGraph graph = asmGraphs.get(i);
            SRSNode root = graph.getRootNode();
            ArrayList<SRSNode> neighbors = root.getNeighbors();
            assignOverhangsHelper(root, neighbors);
        }
        
    }
    
    /** Overhang assignment helper **/
    private void assignOverhangsHelper(SRSNode parent, ArrayList<SRSNode> neighbors) {

        ArrayList<SRSNode> children = new ArrayList<SRSNode>();
        
        //Get children
        for (int i = 0; i < neighbors.size(); i++) {
            SRSNode current = neighbors.get(i);
            if (current.getStage() < parent.getStage()) {
                children.add(current);
            }            
        }
        
        //For each of the children, assign overhangs based on neighbors
        for (int j = 0; j < children.size(); j++) {
            SRSNode child = children.get(j);
            
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
            
            ArrayList<SRSNode> grandChildren = child.getNeighbors();
            
            assignOverhangsHelper(child, grandChildren);
        }
    }
    
    public static boolean validateOverhangs(ArrayList<SRSGraph> graphs) {
        return true;
    }
    
    public static String generateInstructions(ArrayList<SRSNode> roots, Collector coll) {
        return null;
    }
}
