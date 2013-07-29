/*
 * This class contains the SDS++ algorithm
 * 
 */
package Controller.algorithms.nonmodasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.RGeneral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import Controller.datastructures.*;

/**
 *
 * @author evanappleton
 */
public class RBioBricks extends RGeneral {

/** This is the only entry point for this class. A list of goal parts is passed and a list of optimal graphs is output. 
     * PASS THE FOLLOWING ARGUMENTS: ArrayList(Goal_Parts), HashMap(A_Goal_Part, HashSet(Required_Part_Compositions)), HashMap(A_Goal_Part, HashSet(Recommended_Part_Compositions)), HashMap(Part_Compositions, Optimal_Assembly_Graphs) **/
    public ArrayList<RGraph> bioBricksClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, ArrayList<Double> costs) {

        //Try-Catch block around wrapper method
        try {

            _maxNeighbors = 2;
            
            //Initialize part hash and vector set
            HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, required, recommended);
            ArrayList<RVector> vectorSet = ClothoReader.vectorImportClotho(vectorLibrary);

            //Put all parts into hash for mgp algorithm            
            ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalParts);

            //Positional scoring of transcriptional units
            HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
            if (modular) {
                ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
                positionScores = getPositionalScoring(TUs);
            }   
            
            //Run SDS Algorithm for multiple parts
            ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, required, recommended, forbidden, discouraged, partHash, positionScores, null, true);
            optimalGraphs = assignVectors(optimalGraphs, vectorSet);
           
//            System.out.println("optimalGraphs: " + optimalGraphs);

            for (RGraph graph : optimalGraphs) {
                ArrayList<RNode> queue = new ArrayList<RNode>();
                HashSet<RNode> seenNodes = new HashSet<RNode>();
                RNode root = graph.getRootNode();
                queue.add(root);
                while (!queue.isEmpty()) {
                    RNode current = queue.get(0);
                    queue.remove(0);
                    seenNodes.add(current);
//
//                    System.out.println("*********************");
//                    System.out.println("node composition: " + current.getComposition());
//                    System.out.println("LO: " + current.getLOverhang());
//                    System.out.println("RO: " + current.getROverhang());                                      
//                    System.out.println("NodeID: " + current.getNodeID());
//                    System.out.println("uuid: " + current.getUUID());
//
//                    ArrayList<SRSNode> neighbors = current.getNeighbors();
//                    for (RNode neighbor : neighbors) {
//                        System.out.println("neighbor: " + neighbor.getComposition());
//                        if (!seenNodes.contains(neighbor)) {
//                            queue.add(neighbor);
//                        }
//                    }
//                    System.out.println("*********************");
                }
            }
            
            return optimalGraphs;
        } catch (Exception E) {
            E.printStackTrace();
            
            //Return a new graph if there is an exception
            ArrayList<RGraph> blank = new ArrayList<RGraph>();
            return blank;
        }
    }
    
    /** Optimize overhang assignments based on available parts and vectors with overhangs **/
    private ArrayList<RGraph> assignVectors (ArrayList<RGraph> optimalGraphs, ArrayList<RVector> vectorSet) {
        
        //If the vector set is of size one, use that vector everywhere applicable 
        RVector theVector = new RVector();
        if (vectorSet.size() == 1) {
            theVector = vectorSet.get(0);
        }
        theVector.setLOverhang("EX");
        theVector.setROverhang("SP");
        
        //For all graphs traverse nodes of the graph and assign all nodes the biobricks vector
        for (int i = 0; i < optimalGraphs.size(); i++) {
            RGraph graph = optimalGraphs.get(i);
            
            //Traverse nodes of a graph and assign all the selected vector
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                for (RNode neighbor : current.getNeighbors()) {
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
       
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
    
    public static String generateInstructions(ArrayList<RNode> roots, Collector coll) {
        return null;
    }
}