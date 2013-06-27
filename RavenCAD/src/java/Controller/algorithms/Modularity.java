/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author jenhantao, evanappleton
 */
public class Modularity extends Partitioning {

     /**
     * ************************************************************************
     *
     * THIS CLASS HAS MOSTLY SHARING AND HELPER METHODS FOR MAIN ALGORITHM
     *
     *************************************************************************
     */
    
    /** Find sharing score for all possible intermediates for a set of goal parts **/
    protected HashMap<String, Integer> computeIntermediateSharing(ArrayList<rNode> goalParts) {
        HashMap<String, Integer> sharing = new HashMap<String, Integer>();

        //For each goal part
        for (int i = 0; i < goalParts.size(); i++) {
            rNode gp = goalParts.get(i);
            ArrayList<String> gpComposition = gp.getComposition();
            int gpSize = gp.getComposition().size();

            //For all possible intermediates within each goal part
            for (int j = 0; j < gpSize; j++) {
                for (int k = j + 2; k < gpSize + 1; k++) {
                    ArrayList<String> intermediateComposition = new ArrayList<String>();
                    intermediateComposition.addAll(gpComposition.subList(j, k));

                    //See if it has been seen before or not
                    if (sharing.containsKey(intermediateComposition.toString())) {

                        //If seen increment the sharing factor
                        sharing.put(intermediateComposition.toString(), sharing.get(intermediateComposition.toString()) + 1);
                    } else {

                        //If it has not been seen, initialize a place in the hashmap with value 0
                        sharing.put(intermediateComposition.toString(), 0);
                    }
                }
            }
        }
        return sharing;
    }
    
    /**
     * ************************************************************************
     *
     * MODULARITY CALCULATION METHODS
     *
     *************************************************************************
     */

    /**
     * For each node, decompose into transcriptional units, return either part
     * types (1) or compositions (2). << Assumes part starts with a promoter and
     * ends with a terminator >> *
     */
    // type- 1 for name 2 for composition
    protected ArrayList<ArrayList<String>> getTranscriptionalUnits(ArrayList<rNode> goalParts, int type) {

        ArrayList<ArrayList<String>> TUs = new ArrayList<ArrayList<String>>();

        //For each goal part get TUs
        for (int i = 0; i < goalParts.size(); i++) {
            rNode gp = goalParts.get(i);

            ArrayList<String> types = gp.getType();
            ArrayList<String> comps = gp.getComposition();
            ArrayList<Integer> starts = new ArrayList<Integer>();
            starts.add(0);

            //For all the elements of this part's types
            for (int j = 0; j < types.size(); j++) {


                //If the element is a terminator and either it's the last element or there is a promoter directly after it
                if (j < (types.size() - 1)) {
                    if ((types.get(j).equalsIgnoreCase("terminator") || types.get(j).equalsIgnoreCase("t")) && (types.get(j + 1).equalsIgnoreCase("promoter") || types.get(j + 1).equalsIgnoreCase("p"))) {
                        if (type == 1) {
                            for (Integer aStart : starts) {
                                ArrayList<String> aTU = new ArrayList<String>();
                                aTU.addAll(types.subList(aStart, j + 1));
                                TUs.add(aTU);
                            }
                            starts.add(j + 1);
                        } else if (type == 2) {
                            for (Integer aStart : starts) {
                                ArrayList<String> aTU = new ArrayList<String>();
                                aTU.addAll(comps.subList(aStart, j + 1));
                                TUs.add(aTU);
                            }
                            starts.add(j + 1);
                        }
                    }
                } else {
                    if (type == 1) {
                        for (Integer aStart : starts) {
                            ArrayList<String> aTU = new ArrayList<String>();
                            aTU.addAll(types.subList(aStart, j + 1));
                            TUs.add(aTU);
                        }
                    } else if (type == 2) {
                        for (Integer aStart : starts) {
                            ArrayList<String> aTU = new ArrayList<String>();
                            aTU.addAll(comps.subList(aStart, j + 1));
                            TUs.add(aTU);
                        }
                    }
                }
            }
        }

        return TUs;
    }

    protected ArrayList<ArrayList<String>> getSingleTranscriptionalUnits(ArrayList<rNode> goalParts, int type) {

        ArrayList<ArrayList<String>> TUs = new ArrayList<ArrayList<String>>();

        //For each goal part get TUs
        for (int i = 0; i < goalParts.size(); i++) {
            rNode gp = goalParts.get(i);

            ArrayList<String> types = gp.getType();
            ArrayList<String> comps = gp.getComposition();
            int start = 0;

            //For all the elements of this part's types
            for (int j = 0; j < types.size(); j++) {


                //If the element is a terminator and either it's the last element or there is a promoter directly after it
                if (j < (types.size() - 1)) {
                    if ((types.get(j).equalsIgnoreCase("terminator") || types.get(j).equalsIgnoreCase("t")) && (types.get(j + 1).equalsIgnoreCase("promoter") || types.get(j + 1).equalsIgnoreCase("p"))) {
                        if (type == 1) {
                            ArrayList<String> aTU = new ArrayList<String>();
                            aTU.addAll(types.subList(start, j + 1));
                            TUs.add(aTU);
                            start = j + 1;
                        } else if (type == 2) {
                            ArrayList<String> aTU = new ArrayList<String>();
                            aTU.addAll(comps.subList(start, j + 1));
                            TUs.add(aTU);
                            start = j + 1;
                        }
                    }
                } else {
                    if (type == 1) {
                        ArrayList<String> aTU = new ArrayList<String>();
                        aTU.addAll(types.subList(start, j + 1));
                        TUs.add(aTU);
                    } else if (type == 2) {
                        ArrayList<String> aTU = new ArrayList<String>();
                        aTU.addAll(comps.subList(start, j + 1));
                        TUs.add(aTU);
                    }
                }
            }
        }

        return TUs;
    }

    /**
     * Positional Scoring *
     */
    protected HashMap<Integer, HashMap<String, Double>> getPositionalScoring(ArrayList<ArrayList<String>> positionParts) {
        HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
        HashMap<Integer, ArrayList<String>> partsByPosition = new HashMap<Integer, ArrayList<String>>();

        //For each part
        for (int i = 0; i < positionParts.size(); i++) {
            ArrayList<String> part = positionParts.get(i);

            //Add it's part composition to the scoring matrix at each position
            for (int j = 0; j < part.size(); j++) {
                if (partsByPosition.get(j) == null) {
                    ArrayList<String> partList = new ArrayList<String>();
                    partList.add(part.get(j));
                    partsByPosition.put(j, partList);
                } else {
                    partsByPosition.get(j).add(part.get(j));
                }
            }
        }

        //Now score each position
        for (int k = 0; k < partsByPosition.size(); k++) {
//            System.out.println("for pos: "+k);
            HashMap<String, Double> scoreThisPos = new HashMap<String, Double>();
            ArrayList<String> partsAtAPosition = partsByPosition.get(k);
            double size = partsAtAPosition.size();

            //Get score of each part type
            for (String part : partsAtAPosition) {
                if (scoreThisPos.containsKey(part)) {
                    double score = scoreThisPos.get(part);
                    scoreThisPos.put(part, score + 1);
                } else {
                    double score = 1.0;
                    scoreThisPos.put(part, score);
                }
            }

            //Normalize scores at this position
            Set<String> keySet = scoreThisPos.keySet();
            double maxVal = 0;
            for (String part : keySet) {
                double val = scoreThisPos.get(part);
                val = val / size;
                if (val > maxVal) {
                    maxVal = val;
                }
                scoreThisPos.put(part, val);
            }
            scoreThisPos.put("maximum", maxVal);

            //Add this position's score to the scoring matrix
            positionScores.put(k, scoreThisPos);
        }
        return positionScores;
    }

    /**
     * Get the stage hash for a set of optimal graphs *
     */
    public static HashMap<Integer, ArrayList<rNode>> getStageHash(ArrayList<rGraph> optimalGraphs) {

        HashMap<Integer, ArrayList<rNode>> stageHash = new HashMap<Integer, ArrayList<rNode>>();
        ArrayList<rNode> stageNodes = new ArrayList<rNode>();

        for (int i = 0; i < optimalGraphs.size(); i++) {

            //Traverse one graph and store the nodes
            rGraph graph = optimalGraphs.get(i);
            rNode rootNode = graph.getRootNode();
            if (stageHash.containsKey(rootNode.getStage())) {
                stageNodes = stageHash.get(rootNode.getStage());
            }
            stageNodes.add(rootNode);
            stageHash.put(rootNode.getStage(), stageNodes);
            stageHash = getStageHashHelper(rootNode, rootNode.getNeighbors(), stageHash);

            //Put an extra node on the end of all stage of each optimal graph... nodes from adjacent graphs are not neighbors
            if (i != (optimalGraphs.size() - 1)) {
                Set<Integer> keySet = stageHash.keySet();
                for (Integer stage : keySet) {
                    ArrayList<rNode> finalStageNodes = new ArrayList<rNode>();
                    finalStageNodes.addAll(stageHash.get(stage));
                    rNode spacer = new rNode();
                    finalStageNodes.add(spacer);
                    stageHash.put(stage, finalStageNodes);
                }
            }
        }

        return stageHash;
    }

    private static HashMap<Integer, ArrayList<rNode>> getStageHashHelper(rNode parent, ArrayList<rNode> neighbors, HashMap<Integer, ArrayList<rNode>> stageHash) {

        //Check the current stageHash to get nodes that are already in there
        ArrayList<rNode> stageNodes = new ArrayList<rNode>();
        if (stageHash.containsKey(parent.getStage() - 1)) {
            stageNodes = stageHash.get(parent.getStage() - 1);
        }

        for (int i = 0; i < neighbors.size(); i++) {
            rNode neighbor = neighbors.get(i);
            stageNodes.add(neighbor);
            if (neighbor.getStage() > 0) {
                ArrayList<rNode> orderedChildren = new ArrayList<rNode>();
                orderedChildren.addAll(neighbor.getNeighbors());

                //Remove the current parent from the list
                if (orderedChildren.contains(parent)) {
                    orderedChildren.remove(parent);
                }
                stageHash = getStageHashHelper(neighbor, orderedChildren, stageHash);
            }
        }
        stageHash.put(parent.getStage() - 1, stageNodes);
        return stageHash;
    }
}