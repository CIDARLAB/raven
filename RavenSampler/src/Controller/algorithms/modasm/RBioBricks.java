/*
 * This class contains the SDS++ algorithm
 * 
 */
package Controller.algorithms.modasm;

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
    public ArrayList<RGraph> bioBricksClothoWrapper(HashMap<Part, Vector> goalPartsVectors, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, ArrayList<Double> costs) throws Exception {

        //Try-Catch block around wrapper method
        _maxNeighbors = 2;
        ArrayList<Part> goalParts = new ArrayList<Part>(goalPartsVectors.keySet());

        //Initialize part hash and vector set
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, discouraged, recommended);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalPartsVectors, true);
        HashMap<String, RVector> keyVectors = new HashMap<String, RVector>();
        for (RNode root : gpsNodes) {
            String nodeKey = root.getNodeKey("+");
            keyVectors.put(nodeKey, root.getVector());
        }

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, null, true);
        assignBioBricksOverhangs(optimalGraphs, keyVectors);

        return optimalGraphs;
    }

    /**
     * First step of overhang assignment - enforce numeric place holders for
     * overhangs, ie no overhang redundancy in any step *
     */
    private void assignBioBricksOverhangs(ArrayList<RGraph> optimalGraphs, HashMap<String, RVector> keyVectors) {

        //Initialize fields that record information to save complexity for future steps
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        RVector vector = new RVector("EX", "SP", -1, "pSK1A2", null);

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            RVector rootVector = keyVectors.get(root.getNodeKey("+"));
            if (rootVector == null) {
                rootVector = vector;
            }

            root.setVector(rootVector);
            root.setLOverhang("EX");
            root.setROverhang("SP");
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);
            ArrayList<RNode> neighbors = root.getNeighbors();
            assignBioBricksOverhangsHelper(root, neighbors, root, rootVector);
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

    /**
     * This helper method executes the loops necessary to enforce overhangs for
     * each graph in enforceOverhangRules *
     */
    private void assignBioBricksOverhangsHelper(RNode parent, ArrayList<RNode> children, RNode root, RVector vector) {

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);

            //Give biobricks overhangs
            child.setVector(vector);
            child.setLOverhang("EX");
            child.setROverhang("SP");

            //Make recursive call
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                assignBioBricksOverhangsHelper(child, grandChildren, root, vector);

                //Or record the level zero parts
            } else {
                ArrayList<RNode> l0nodes = _rootBasicNodeHash.get(root);
                l0nodes.add(child);
                _rootBasicNodeHash.put(root, l0nodes);
            }
        }
    }

    /**
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {

        boolean valid = true;

        for (RGraph graph : graphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            RNode root = graph.getRootNode();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);

                if (!("EX".equals(current.getLOverhang()) && "SP".equals(current.getROverhang()))) {
                    return false;
                }

                ArrayList<RNode> neighbors = current.getNeighbors();
                for (RNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return valid;
    }

    /**
     * Generation of new BioBricks primers for parts *
     */
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
}