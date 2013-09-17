/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms.modasm;

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
public class SRSGoldenGate extends SRSGeneral{
    
    /** Clotho part wrapper for Golden Gate assembly **/
    public ArrayList<SRSGraph> goldenGateClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies) {
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
            
            //Put all parts into hash for mgp algorithm            
            ArrayList<SRSNode> gpsNodes = gpsToNodesClotho(goalParts);
            
            //Run SDS Algorithm for multiple parts
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes);
            
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
