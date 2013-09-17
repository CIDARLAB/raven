/*
 * This class contains the SDS++ algorithm
 * 
 */
package algorithms.nonmodasm;

import algorithms.SRSGeneral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import datastructures.*;

/**
 *
 * @author evanappleton
 */
public class SRSBioBricks extends SRSGeneral {

    /** This is the only entry point for this class. A list of goal parts is passed and a list of optimal graphs is output. 
     * PASS THE FOLLOWING ARGUMENTS: ArrayList(Goal_Parts), HashMap(A_Goal_Part, HashSet(Required_Part_Compositions)), HashMap(A_Goal_Part, HashSet(Recommended_Part_Compositions)), HashMap(Part_Compositions, Optimal_Assembly_Graphs) **/
    public ArrayList<SRSGraph> bioBricksClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular) {

        //Try-Catch block around wrapper method
        try {

            _maxNeighbors = 2;

            //Put all parts into hash for mgp algorithm            
            ArrayList<SRSNode> gpsNodes = gpsToNodesClotho(goalParts);
            
            //Run SDS Algorithm for multiple parts
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes);
            
            return optimalGraphs;
        } catch (Exception E) {
            E.printStackTrace();
            
            //Return a new graph if there is an exception
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
            return blank;
        }
    }
    
    /** Get all subsets of a set for a specific sized subset **/
//    @Override
//    protected ArrayList<int[]> getSubsets(int[] set, int k) {
//        int[] subset = new int[k];
//        ArrayList<int[]> breaks = new ArrayList<int[]>();
//        getSubsetsHelper(set, subset, 0, 0, breaks);
//        return breaks;
//    }
//    
//    /** getSubsets helper function **/
//    private void getSubsetsHelper(int[] set, int[] subset, int subsetSize, int nextIndex, ArrayList<int[]> breaks) { 
//        if (subsetSize == subset.length) {
//            breaks.add(subset);
//        } else {
//            for (int j = nextIndex; j < set.length; j++) {
//                int[] sub = subset.clone();
//                sub[subsetSize] = set[j];
//                getSubsetsHelper(set, sub, subsetSize + 1, j + 1, breaks);
//            }
//        }
//    }
    public static boolean validateOverhangs(ArrayList<SRSGraph> graphs) {
        return true;
    }
}