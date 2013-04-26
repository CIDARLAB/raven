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
public class SRSGibson extends SRSGeneral {
    
    /** Clotho part wrapper for sequence independent one pot reactions **/
    public ArrayList<SRSGraph> gibsonClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectors, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies) {
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
            ArrayList<SRSVector> vectorSet = vectorImportClotho(vectors);

            //Put all parts into hash for mgp algorithm            
            ArrayList<SRSNode> gpsNodes = gpsToNodesClotho(goalParts);
            
            //Positional scoring of transcriptional units
            HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
            if (modular) {
                ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
                positionScores = getPositionalScoring(TUs);
            }

            //Run SDS Algorithm for multiple parts
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, partHash, positionScores, efficiencies);
            optimalGraphs = assignVector(optimalGraphs, vectorSet);
            
            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
//            E.printStackTrace();
            return blank;
        }
    }
    
    /** Optimize overhang assignments based on available parts and vectors with overhangs **/
    private ArrayList<SRSGraph> assignVector (ArrayList<SRSGraph> optimalGraphs, ArrayList<SRSVector> vectorSet) {
        
        //If the vector set is of size one, use that vector everywhere applicable 
        SRSVector theVector = new SRSVector();
        if (vectorSet.size() == 1) {
            for (SRSVector vector : vectorSet) {
                theVector = vector;
            }
        }
        
        //For all graphs traverse nodes of the graph and assign all nodes the biobricks vector
        for (int i = 0; i < optimalGraphs.size(); i++) {
            SRSGraph graph = optimalGraphs.get(i);
            
            //Only the goal part gets a vector
            SRSNode root = graph.getRootNode();
            root.setVector(theVector);
        }
        
        return optimalGraphs;
    }
}
