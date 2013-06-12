/*
 * This class contains the SDS++ algorithm
 * 
 */
package Controller.algorithms.nonmodasm;

import Controller.algorithms.SRSGeneral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import Controller.datastructures.*;

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
            
            //Initialize part hash and vector set
            HashMap<String, SRSGraph> partHash = partImportClotho(goalParts, partLibrary, required, recommended);
            ArrayList<SRSVector> vectorSet = vectorImportClotho(vectorLibrary);

            //Put all parts into hash for mgp algorithm            
            ArrayList<SRSNode> gpsNodes = gpsToNodesClotho(goalParts);

            //Positional scoring of transcriptional units
            HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
            if (modular) {
                ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
                positionScores = getPositionalScoring(TUs);
            }   
            
            //Run SDS Algorithm for multiple parts
            ArrayList<SRSGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, discouraged, partHash, positionScores, null, true);
            optimalGraphs = assignVectors(optimalGraphs, vectorSet);
            
            return optimalGraphs;
        } catch (Exception E) {
            E.printStackTrace();
            
            //Return a new graph if there is an exception
            ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
            return blank;
        }
    }
    
    /** Optimize overhang assignments based on available parts and vectors with overhangs **/
    private ArrayList<SRSGraph> assignVectors (ArrayList<SRSGraph> optimalGraphs, ArrayList<SRSVector> vectorSet) {
        
        //If the vector set is of size one, use that vector everywhere applicable 
        SRSVector theVector = new SRSVector();
        if (vectorSet.size() == 1) {
            theVector = vectorSet.get(0);
        }
        theVector.setLOverhang("EX");
        theVector.setROverhang("SP");
        
        //For all graphs traverse nodes of the graph and assign all nodes the biobricks vector
        for (int i = 0; i < optimalGraphs.size(); i++) {
            SRSGraph graph = optimalGraphs.get(i);
            
            //Traverse nodes of a graph and assign all the selected vector
            ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
            HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                for (SRSNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
                
                //Give biobricks overhangs
                current.setLOverhang("EX");
                current.setROverhang("SP");
                
                //If the node is a step, it gets the biobricks vector
                if (current.getStage() > 0) {
                    current.setVector(theVector);
                }               
                seenNodes.add(current);
            }
        }
        
        return optimalGraphs;
    }  
       
    
    public static boolean validateOverhangs(ArrayList<SRSGraph> graphs) {
        return true;
    }
    
    public static String generateInstructions(ArrayList<SRSNode> roots, Collector coll) {
        return null;
    }
}