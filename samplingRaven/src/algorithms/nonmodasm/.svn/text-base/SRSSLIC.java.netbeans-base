/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms.nonmodasm;

import algorithms.SRSGeneral;
import datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class SRSSLIC extends SRSGeneral{
    
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
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, discouraged, partHash, positionScores, efficiencies, false);
            optimalGraphs = assignVectors(optimalGraphs, vectorSet);
            
            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
//            E.printStackTrace();
            return blank;
        }
    }
    
    /** Optimize overhang assignments based on available parts and vectors with overhangs **/
    private ArrayList<SRSGraph> assignVectors (ArrayList<SRSGraph> optimalGraphs, ArrayList<SRSVector> vectorSet) {
        
        //If the vector set is of size one, use that vector everywhere applicable 
        SRSVector theVector = new SRSVector();
        if (vectorSet.size() == 1) {
            for (SRSVector vector : vectorSet) {
                theVector = vector;
            }
        }
        
        //For all graphs traverse nodes to count the number of reactions and assign a vector to the goal part        
        for (int i = 0; i < optimalGraphs.size(); i++) {
            
            //Only the goal part gets a vector
            SRSGraph optimalGraph = optimalGraphs.get(i);
            SRSNode root = optimalGraph.getRootNode();
            root.setVector(theVector);
            
            //Record left and right neighbors for each node... this will determine how many PCRs need to be performed
            HashSet<ArrayList<String>> neighborHash = new HashSet<ArrayList<String>>();
            ArrayList<String> rootComp = root.getComposition();
            String prev = new String();
            String next = new String();
            for (int j = 0; j < rootComp.size(); j++) {
                String current = rootComp.get(j);
                if (j == 0) {
                    next = rootComp.get(j+1);
                } else if (j == rootComp.size()-1) {
                    prev = rootComp.get(j-1);
                } else {
                    next = rootComp.get(j+1);
                    prev = rootComp.get(j-1);
                }
                ArrayList<String> seq = new ArrayList<String>();
                seq.add(prev);
                seq.add(current);
                seq.add(next);
                neighborHash.add(seq);
            }
            
            int reactions = neighborHash.size();
            
//            System.out.println("For graph number: " + i + " there will be " + reactions);
            
            optimalGraph.setReactions(reactions);
        }
        
        return optimalGraphs;
    }  
}
