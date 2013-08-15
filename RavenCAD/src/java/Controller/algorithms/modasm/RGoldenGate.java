/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.modasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.RGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jenhantao,evanappleton
 */
public class RGoldenGate extends RGeneral {

    /**
     * Clotho part wrapper for Golden Gate assembly *
     */
    public ArrayList<RGraph> goldenGateClothoWrapper(HashMap<Part, Vector> goalPartsVectors, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, HashMap<Integer, Double> efficiencies, ArrayList<Double> costs) throws Exception {

        //Designate how many parts can be efficiently ligated in one step
        int max = 0;
        Set<Integer> keySet = efficiencies.keySet();
        for (Integer key : keySet) {
            if (key > max) {
                max = key;
            }
        }
        _maxNeighbors = max;
        ArrayList<Part> goalParts = new ArrayList<Part>(goalPartsVectors.keySet());

        //Create hashMem parameter for createAsmGraph_sgp() call
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, discouraged, recommended);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalPartsVectors);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        optimalGraphs = assignOverhangs(optimalGraphs);

        return optimalGraphs;
    }

    /**
     * Optimize overhang assignments based on available parts and vectors with
     * overhangs *
     */
    private ArrayList<RGraph> assignOverhangs(ArrayList<RGraph> optimalGraphs) {
        return optimalGraphs;
    }

    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
}
