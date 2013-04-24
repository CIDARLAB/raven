/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import Controller.datastructures.SRSGraph;
import Controller.datastructures.SRSNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 *
 * @author evanappleton
 */
public class SRSGeneral extends SRSAlgorithmCore {

    /** Find assembly graph for multiple goal parts **/
    protected ArrayList<SRSGraph> createAsmGraph_mgp(ArrayList<SRSNode> gps, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, HashMap<String, SRSGraph> partHash, HashMap<Integer, HashMap<String, Double>> modularityHash, HashMap<Integer, Double> efficiencies) {

        //Search all goal parts for potential conflicts with requried parts, return a blank graph and error message if there is a conflict
        for (int i = 0; i < gps.size(); i++) {
            SRSNode gp = gps.get(i);
            try {
                conflictSearchRequired(gp, required);
            } catch (Exception ex) {

                //Return a new graph if there is an exception
                ArrayList<SRSGraph> blank = new ArrayList<SRSGraph>();
                return blank;
            }
        }

        //Run algorithm for all goal parts separately, find the max stages
        HashMap<String, SRSGraph> slackLibrary = new HashMap<String, SRSGraph>();
        slackLibrary.putAll(partHash);
        int slack = determineSlack(gps, required, recommended, forbidden, slackLibrary);
        slackLibrary = null;
        System.gc();

        //Compute sharing scores for all goal parts
        HashMap<String, Integer> sharingHash = computeIntermediateSharing(gps);

        //First initiate results and pinned hash
        ArrayList<SRSGraph> resultGraphs = new ArrayList<SRSGraph>();
        HashMap<String, SRSGraph> pinnedPartHash = new HashMap<String, SRSGraph>();

        //Iterate across all goal parts until we have a result graph for each goal part
        //Start finding graphs for each goal part until all goal parts are formed  
        while (!gps.isEmpty()) {

            //Reinitialize memoization hash with part library and pinned graphs each with zero cost
            HashMap<String, SRSGraph> hashMem = new HashMap<String, SRSGraph>();
            if (partHash != null && pinnedPartHash != null) {
                hashMem.putAll(partHash);
                hashMem.putAll(pinnedPartHash);
            }

            //Call single-goal-part algorithm for each goal part and determine which of the graphs to pin
            int index = 0;
            SRSGraph pinnedGraph = null;
            for (int j = 0; j < gps.size(); j++) {
                SRSNode gp = gps.get(j);
                SRSGraph newGraph = createAsmGraph_sgp(gp, hashMem, required, recommended, forbidden, null, slack, sharingHash, modularityHash, efficiencies);
                newGraph.getRootNode().setUUID(gp.getUUID());

                //Pin graph if no existing pinned graph
                if (pinnedGraph == null) {
                    pinnedGraph = newGraph;
                    index = j;
                }

                //If there are any discouraged parts, pin the graph with the fewest discouraged parts
                if (!discouraged.isEmpty()) {
                    if (newGraph.getDiscouragedCount() < pinnedGraph.getDiscouragedCount()) {
                        pinnedGraph = newGraph;
                        index = j;
                    }
                }
                
                //If there are any recommended parts, pin the graph with greatest recommended parts
                if (!recommended.isEmpty()) {
                    if (newGraph.getReccomendedCount() > pinnedGraph.getReccomendedCount()) {
                        pinnedGraph = newGraph;
                        index = j;
                    }

                //If no recommended parts, pin the graph with the most sharing
                } else {
                    if (newGraph.getSharing() > pinnedGraph.getSharing()) {
                        pinnedGraph = newGraph;
                        index = j;
                    }
                }
            }

            //Add pinned graph and graph for each intermediate part to our hash of pinned graphs
            //Also search through the subgraphs of the bestGraph to see if it has any basic parts
            boolean cantMake = true;
            pinnedPartHash.put(pinnedGraph.getRootNode().getComposition().toString(), pinnedGraph.clone());
            if (pinnedGraph.getSubGraphs().isEmpty()) {
                cantMake = false;
            }

            for (int k = 0; k < pinnedGraph.getSubGraphs().size(); k++) {
                SRSGraph subGraph = pinnedGraph.getSubGraphs().get(k);
                SRSGraph subGraphClone = subGraph.clone();
                subGraphClone.pin();
                pinnedPartHash.put(subGraph.getRootNode().getComposition().toString(), subGraphClone);

                //If a basic part is seen in the solution graph
                if (subGraph.getRootNode().getUUID() != null) {
                    cantMake = false;
                }
            }

//            //If it does not have any parts except from goal parts with a 
//            if (cantMake) {
//                JOptionPane.showMessageDialog(null, "Forbidden part conflict discovered! Forbidden set is too restrictive. Please select compatible set of forbidden parts so that part can be constructed.");
//            }

            //Remove pinned graph from goal part list and add to result list
            gps.remove(gps.get(index));
            resultGraphs.add(pinnedGraph);
        }

        return resultGraphs;
    }

    /** Find assembly graph for a single goal part factoring in slack and sharing **/
    protected SRSGraph createAsmGraph_sgp(SRSNode goalPartNode, HashMap<String, SRSGraph> partsHash, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, int slack, HashMap<String, Integer> sharingHash, HashMap<Integer, HashMap<String, Double>> modularityHash, HashMap<Integer, Double> efficiencies) {

        //If any of the parameters is null, must be set to a new object to avoid null pointer issues
        if (partsHash == null) {
            partsHash = new HashMap<String, SRSGraph>();
        }
        if (sharingHash == null) {
            sharingHash = new HashMap<String, Integer>();
        }
        if (required == null) {
            required = new HashSet<String>();
        }
        if (forbidden == null) {
            forbidden = new HashSet<String>();
        }
        if (recommended == null) {
            recommended = new HashSet<String>();
        }
        if (discouraged == null) {
            discouraged = new HashSet<String>();
        }
        if (efficiencies == null) {
            efficiencies = new HashMap<Integer, Double>();
        }
        if (modularityHash == null) {
            modularityHash = new HashMap<Integer, HashMap<String, Double>>();
        }

        //Memoization Case - If graph already exists for this composition. This is the case for all basic parts and library parts
        if (partsHash.containsKey(goalPartNode.getComposition().toString())) {
            return partsHash.get(goalPartNode.getComposition().toString());
        }
        SRSGraph bestGraph = new SRSGraph(goalPartNode);

        //Recursive Programing to find best graph
        int gpSize = goalPartNode.getComposition().size();
        ArrayList<String> gpComp = goalPartNode.getComposition();
        ArrayList<String> gpType = goalPartNode.getType();

        //Create idexes for goal part
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        for (int i = 1; i < gpSize; i++) {
            indexes.add(i);
        }

        //Remove indexes of required parts
        for (int start = 0; start < gpSize; start++) {
            for (int end = start + 2; end < gpSize + 1; end++) {
                if (start == 0 && end == gpSize) {
                    continue;
                }
                ArrayList<String> gpSub = new ArrayList<String>();
                gpSub.addAll(gpComp.subList(start, end));
                if (required.contains(gpSub.toString())) {
                    for (int j = start + 1; j < end; j++) {
                        indexes.remove(new Integer(j));
                    }
                }
            }
        }

        //If a large part has a combination of smaller parts that are all forbidden, which make it impossible make, this part must also be forbidden
        //We must record if there is any way to break it
        forbidden = conflictSearchForbidden(gpComp, forbidden);
        ArrayList<int[]> partitions = getPartitions(indexes);

        //Iterate over all part "breaks"
        //Find best graph for n partitions        
        for (int k = 0; k < partitions.size(); k++) {
            ArrayList<SRSNode> allSubParts = new ArrayList<SRSNode>();
            boolean canPartition = true;
            int[] thisPartition = partitions.get(k);

            //For each partition, create a new SRSNode and add it to the list
            for (int n = 0; n < (thisPartition.length + 1); n++) {
                ArrayList<String> type = new ArrayList<String>();
                ArrayList<String> comp = new ArrayList<String>();
                if (n == 0) {
                    type.addAll(gpType.subList(0, thisPartition[n]));
                    comp.addAll(gpComp.subList(0, thisPartition[n]));
                } else if (n == thisPartition.length) {
                    type.addAll(gpType.subList(thisPartition[n - 1], gpSize));
                    comp.addAll(gpComp.subList(thisPartition[n - 1], gpSize));
                } else {
                    type.addAll(gpType.subList(thisPartition[n - 1], thisPartition[n]));
                    comp.addAll(gpComp.subList(thisPartition[n - 1], thisPartition[n]));
                }

                //If any of the compositions is in the forbidden hash, this partition will not work
                if (forbidden.contains(comp.toString())) {
                    canPartition = false;
                    continue;
                }

                //If not, make the new SRSNode
                boolean rec = recommended.contains(comp.toString());
                boolean dis = discouraged.contains(comp.toString());
                SRSNode aSubPart = new SRSNode(rec, dis, null, comp, type, null, null, null, null, 0);
                allSubParts.add(aSubPart);
            }

            //If there are no forbidden parts amongst subparts
            if (canPartition) {
                ArrayList<SRSGraph> toCombine = new ArrayList<SRSGraph>();
                for (int o = 0; o < allSubParts.size(); o++) {
                    SRSGraph solution = createAsmGraph_sgp(allSubParts.get(o), partsHash, required, recommended, forbidden, discouraged, slack - 1, sharingHash, modularityHash, efficiencies);
                    toCombine.add(solution);
                }

                SRSGraph newGraph = combineGraphsModEff(toCombine, recommended, discouraged, sharingHash, modularityHash, efficiencies);

                //Edge case: best graph does not exist yet
                if (bestGraph.getRootNode().getNeighbors().isEmpty()) {
                    bestGraph = newGraph;
                } else {

                    // If cost of new graph is the best so far save it
                    bestGraph = minCostSlack(bestGraph, newGraph, slack);
                }
            }
        }

        //Save best graph for this intermediate
        partsHash.put(bestGraph.getRootNode().getComposition().toString(), bestGraph);

        //Return best graph for the initial goal part
        return bestGraph;
    }

    /** Combine multiple graphs, including efficiency and modularity scoring **/
    //Currently, it is assumed that efficiencies are additive and not multiplicative
    protected SRSGraph combineGraphsModEff(ArrayList<SRSGraph> graphs, HashSet<String> recommended, HashSet<String> discouraged, HashMap<String, Integer> sharing, HashMap<Integer, HashMap<String, Double>> modularityHash, HashMap<Integer, Double> efficiencies) {
        //Call method without efficiency and modularity first
        SRSGraph combineGraphsME = combineGraphsShareRecDis(graphs, recommended, discouraged, sharing);
        SRSNode root = combineGraphsME.getRootNode();

        //Get effiency and modularity of subgraphs
        double subModularity = 0; //modularity of subgraphs
        double modularity = 0; //modularity of new root
        double efficiency = 0;
        int numCombine = graphs.size();
        if (efficiencies.containsKey(numCombine)) {
            efficiency = efficiencies.get(numCombine);
        }
        ArrayList<Double> allEfficiencies = new ArrayList<Double>();
        for (int i = 0; i < graphs.size(); i++) {
            subModularity = graphs.get(i).getModularity() + subModularity;
            allEfficiencies.addAll(graphs.get(i).getEfficiency());
        }
        allEfficiencies.add(efficiency);
        if (numCombine == 0) {
            subModularity = 0;
        } else {
            subModularity = subModularity / numCombine;
        }
        double max = 0;
        //Get the modularity of the root node
        ArrayList<String> type = root.getType();
        for (int j = 0; j < type.size(); j++) {
            String part = type.get(j);
            if (!modularityHash.isEmpty()) {
                HashMap<String, Double> aPos = modularityHash.get(j);
                Set<String> keySet = aPos.keySet();
                for (String partType : keySet) {
                    if (part.equals(partType)) {
                        modularity = modularity + aPos.get(partType);
                        max = max + aPos.get("maximum");
                    }
                }
            }
        }
        modularity = modularity / max;
        if (max == 0) {
            modularity = 0;
        }

        //Set the new graph's modularity and efficiency scores
        combineGraphsME.setEfficiency(allEfficiencies);
        combineGraphsME.setModularity((subModularity + modularity) / 2);

        return combineGraphsME;
    }

    /** Combine multiple graphs, including sharing and recommended **/
    protected SRSGraph combineGraphsShareRecDis(ArrayList<SRSGraph> graphs, HashSet<String> recommended, HashSet<String> discouraged, HashMap<String, Integer> sharing) {

        //Call method without sharing first
        SRSGraph combineGraphsSRD = combineGraphsStageStep(graphs);
        SRSNode root = combineGraphsSRD.getRootNode();

        //Look in sharing and recommended hash to set sharing and recommended
        int graphSharing = 0;
        int recCount = 0;
        int disCount = 0;
        for (int i = 0; i < graphs.size(); i++) {
            graphSharing = graphs.get(i).getSharing() + graphSharing;
            recCount = graphs.get(i).getReccomendedCount() + recCount;
            disCount = graphs.get(i).getDiscouragedCount() + disCount;
        }

        //If sharing hash contains the root's composition
        if (sharing.containsKey(root.getComposition().toString())) {
            combineGraphsSRD.setSharing(graphSharing + sharing.get(root.getComposition().toString()));
        } else {
            combineGraphsSRD.setSharing(graphSharing);
        }

        //If recommended hash contains the root's composition
        if (recommended.contains(combineGraphsSRD.getRootNode().getComposition().toString())) {
            combineGraphsSRD.setReccomendedCount(recCount + 1);
        } else {
            combineGraphsSRD.setReccomendedCount(recCount);
        }
        
        //If recommended hash contains the root's composition
        if (discouraged.contains(combineGraphsSRD.getRootNode().getComposition().toString())) {
            combineGraphsSRD.setDiscouragedCount(disCount + 1);
        } else {
            combineGraphsSRD.setDiscouragedCount(disCount);
        }
        return combineGraphsSRD;

    }

    /** Combine multiple graphs, ignoring sharing and recommended **/
    protected SRSGraph combineGraphsStageStep(ArrayList<SRSGraph> graphs) {
        SRSNode newRoot = new SRSNode();
        ArrayList<String> mergerComposition = newRoot.getComposition();
        ArrayList<String> mergerType = newRoot.getType();
        for (int i = 0; i < graphs.size(); i++) {
            SRSNode currentNeighbor = graphs.get(i).getRootNode();
            mergerComposition.addAll(currentNeighbor.getComposition());
            mergerType.addAll(currentNeighbor.getType());
        }
        newRoot.setComposition(mergerComposition);
        newRoot.setType(mergerType);

        //Clone all the nodes from graphs being combined and then add to new root node
        for (int j = 0; j < graphs.size(); j++) {
            SRSNode toAdd = graphs.get(j).getRootNode().clone();
            toAdd.addNeighbor(newRoot);
            newRoot.addNeighbor(toAdd);
        }
        SRSGraph newGraph = new SRSGraph(newRoot);

        //Determine stages, steps, recommended for the new graph
        int steps = 0;
        int maxStage = 0;

        //Stages and steps determined
        for (int k = 0; k < graphs.size(); k++) {
            int currentGraphStage = graphs.get(k).getStages();
            if (currentGraphStage > maxStage) {
                maxStage = currentGraphStage;
            }
            int currentGraphSteps = graphs.get(k).getSteps();
            steps = currentGraphSteps + steps;
            newGraph.addSubgraph(graphs.get(k));
            for (int l = 0; l < graphs.get(k).getSubGraphs().size(); l++) {
                newGraph.addSubgraph(graphs.get(k).getSubGraphs().get(l));
            }
        }
        newGraph.setSteps(steps + 1);
        newGraph.setStages(maxStage + 1);
        newRoot.setStage(maxStage + 1);

        return newGraph;
    }

    /** Find which of two graphs has a lower cost, including slack. Supports multi-goal-part assembly **/
    protected SRSGraph minCostSlack(SRSGraph g0, SRSGraph g1, int slack) {

        //Return pinned graphs
        if (g0.getPinned()) {
            return g0;
        } else if (g1.getPinned()) {
            return g1;
        }

        //If either graph has more stages than slack, ignore slack
        if (g0.getStages() > slack || g1.getStages() > slack) {
            return minCost(g0, g1);
        }

        //Efficiency
        ArrayList<Double> g0effs = g0.getEfficiency();
        double sum = 0;
        for (int i = 0; i < g0effs.size(); i++) {
            sum = sum + g0effs.get(i);
        }
        double aveEffg0 = sum / g0effs.size();

        ArrayList<Double> g1effs = g1.getEfficiency();
        sum = 0;
        for (int i = 0; i < g1effs.size(); i++) {
            sum = sum + g1effs.get(i);
        }
        double aveEffg1 = sum / g1effs.size();

        if (aveEffg0 > aveEffg1) {
            return g0;
        } else if (aveEffg1 > aveEffg0) {
            return g1;
        }

        //Discouraged
        if (g0.getDiscouragedCount() > g1.getDiscouragedCount()) {
            return g1;
        } else if (g1.getDiscouragedCount() > g0.getDiscouragedCount()) {
            return g0;
        }
        
        //Recommended
        if (g0.getReccomendedCount() > g1.getReccomendedCount()) {
            return g0;
        } else if (g1.getReccomendedCount() > g0.getReccomendedCount()) {
            return g1;
        }

        //Modularity
        if (g0.getModularity() > g1.getModularity()) {
            return g0;
        } else if (g1.getModularity() > g0.getModularity()) {
            return g1;
        }

        //Adjusted steps
        int stepsAdjg0 = g0.getSteps() - g0.getSharing();
        int stepsAdjg1 = g1.getSteps() - g1.getSharing();
        if (stepsAdjg0 < stepsAdjg1) {
            return g0;
        } else if (stepsAdjg1 < stepsAdjg0) {
            return g1;
        }

        //If equal, return the original
        return g0;
    }

    /** Find which of two graphs has a lower cost, ignoring slack **/
    protected SRSGraph minCost(SRSGraph g0, SRSGraph g1) {

        //Stages
        if (g0.getStages() < g1.getStages()) {
            return g0;
        } else if (g1.getStages() < g0.getStages()) {
            return g1;
        }

        //Steps
        if (g0.getSteps() < g1.getSteps()) {
            return g0;
        } else if (g1.getSteps() < g0.getSteps()) {
            return g1;
        }

        //Discouraged
        if (g0.getDiscouragedCount() > g1.getDiscouragedCount()) {
            return g1;
        } else if (g1.getDiscouragedCount() > g0.getDiscouragedCount()) {
            return g0;
        }
        
        //Recommended
        if (g0.getReccomendedCount() > g1.getReccomendedCount()) {
            return g0;
        } else if (g1.getReccomendedCount() > g0.getReccomendedCount()) {
            return g1;
        }

        //If all else has failed, return the original graph
        return g0;
    }

    /** Find the maximum amount of stages for a set of goal parts with a library. This determines a mgp assembly slack factor **/
    protected int determineSlack(ArrayList<SRSNode> gps, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashMap<String, SRSGraph> library) {
        int slack = 0;
        for (int i = 0; i < gps.size(); i++) {
            SRSNode gp = gps.get(i);
            SRSGraph graph = createAsmGraph_sgp(gp, library, required, null, forbidden, null, 0, null, null, null);
            if (graph.getStages() > slack) {
                slack = graph.getStages();
            }
        }

        return slack;
    }

    /** For n-way assembly, must find all ways to "break" a part i.e. all possible partition of size maxNeighbors and less **/
    protected ArrayList<int[]> getPartitions(ArrayList<Integer> indexes) {

        int[] newIndexes = buildIntArray(indexes);
        ArrayList<int[]> partitions = new ArrayList<int[]>();

        for (int l = 1; l < _maxNeighbors; l++) {
            ArrayList<int[]> subsets = getSubsets(newIndexes, l);
            partitions.addAll(subsets);
        }

        return partitions;
    }
    //Fields
    protected int _maxNeighbors;
}