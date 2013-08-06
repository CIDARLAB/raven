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

    /**
     * Clotho part wrapper for BioBricks 3A
     */
    public ArrayList<RGraph> bioBricksClothoWrapper(ArrayList<Part> goalParts, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, ArrayList<Double> costs) throws Exception {

        //Try-Catch block around wrapper method
        _maxNeighbors = 2;

        //Initialize part hash and vector set
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, discouraged, recommended);
        ArrayList<RVector> vectorSet = ClothoReader.vectorImportClotho(vectorLibrary);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalParts);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, null, true);
        enforceOverhangRules(optimalGraphs, null);
        assignScars(optimalGraphs);

        return optimalGraphs;
    }
    
    /** First step of overhang assignment - enforce numeric place holders for overhangs, ie no overhang redundancy in any step **/
    private void enforceOverhangRules (ArrayList<RGraph> optimalGraphs, RVector vector) {
        
        //Initialize fields that record information to save complexity for future steps
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        if (vector == null) {
            vector = new RVector();           
        }
        vector.setLOverhang("EX");
        vector.setROverhang("SP");
        vector.setName("BBa_Vector");
        
        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            root.setVector(vector);
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);
            ArrayList<RNode> neighbors = root.getNeighbors();
            enforceOverhangRulesHelper(root, neighbors, root, vector);
        }

        //Determine which nodes impact which level to form the stageDirectionAssignHash
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<String> rootDir = new ArrayList<String>();
            ArrayList<String> direction = root.getDirection();
            rootDir.addAll(direction);
            ArrayList<RNode> l0Nodes = _rootBasicNodeHash.get(root);

            //Determine which levels each basic node impacts            
            for (int i = 0; i < l0Nodes.size(); i++) {

                //Determine direction of basic level 0 nodes               
                RNode l0Node = l0Nodes.get(i);               
                String l0Direction = rootDir.get(0);               
                if (l0Node.getComposition().size() == 1) {
                    ArrayList<String> l0Dir = new ArrayList<String>();
                    l0Dir.add(l0Direction);
                    l0Node.setDirection(l0Dir);
                }               
                int size = l0Node.getDirection().size();
                rootDir.subList(0, size).clear();
            }
        }
    }
    
    /** This helper method executes the loops necessary to enforce overhangs for each graph in enforceOverhangRules **/
    private void enforceOverhangRulesHelper (RNode parent, ArrayList<RNode> children, RNode root, RVector vector) {
        
        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            
            //Give biobricks overhangs
            child.setLOverhang("EX");
            child.setROverhang("SP");
            child.setVector(vector);

            //Make recursive call
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                enforceOverhangRulesHelper(child, grandChildren, root, vector);
            
            //Or record the level zero parts
            } else {
                ArrayList<RNode> l0nodes = _rootBasicNodeHash.get(root);
                l0nodes.add(child);
                _rootBasicNodeHash.put(root, l0nodes);
            }
        }
    }
       
    /** Determine overhang scars **/
    private void assignScars(ArrayList<RGraph> optimalGraphs) {
        
        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {
            
            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            assignScarsHelper(root, children);
        }        
    }
    
    /** Overhang scars helper **/
    private ArrayList<String> assignScarsHelper(RNode parent, ArrayList<RNode> children) {
        
        ArrayList<String> scars = new ArrayList<String>();
        
        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            
            if (i > 0) {
                if (child.getLOverhang().isEmpty()) {
                    scars.add("_");
                }
                scars.add("BB"); 
            }   
            
            //Make recursive call
            if (child.getStage() > 0) {
                
                //Remove the current parent from the list
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                
                ArrayList<String> childScars = assignScarsHelper(child, grandChildren);
                scars.addAll(childScars);
            } else {
                
                ArrayList<String> childScars = new ArrayList<String>();
                if (child.getComposition().size() > 1) {
                    if (!child.getScars().isEmpty()) {
                        childScars.addAll(child.getScars());
                    } else {
                        
                        for (int j = 0; j < child.getComposition().size() - 1; j++) {
                            childScars.add("_");
                        }
                        child.setScars(childScars);
                    }
                }
                scars.addAll(childScars);
            }
        }     
        
        parent.setScars(scars);       
        return scars;
    }
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
    
    public static String generateInstructions(ArrayList<RNode> roots, Collector coll) {
        return null;
    }
    
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
}