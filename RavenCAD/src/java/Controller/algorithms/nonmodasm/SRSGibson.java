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
            
            return optimalGraphs;
        } catch (Exception E) {
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
//            E.printStackTrace();
            return blank;
        }
    }

    public static boolean validateOverhangs(ArrayList<SRSGraph> graphs) {
        return true;
    }
}
