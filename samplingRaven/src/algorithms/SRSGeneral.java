/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms;

import datastructures.SRSGraph;
import datastructures.SRSNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.Set;
import java.util.Random;

/**
 *
 * @author evanappleton
 */
public class SRSGeneral extends SRSAlgorithmCore {

    /** Find assembly graph for multiple goal parts **/
    protected ArrayList<SRSGraph> createAsmGraph_mgp(ArrayList<SRSNode> gps) {

        //First initiate results and pinned hash
        ArrayList<SRSGraph> resultGraphs = new ArrayList<SRSGraph>();
        _randomSpace = new HashMap<String, ArrayList<SRSGraph>>();

        //Call single-goal-part algorithm for each goal part and determine which of the graphs to pin
        for (int j = 0; j < gps.size(); j++) {
            SRSNode gp = gps.get(j);
            _goalComp = gp.getComposition().toString();
            createAsmGraph_sgp(gp);
        }
        
        Set<String> keySet = _randomSpace.keySet();
        ArrayList<String> keys = new ArrayList<String>(keySet);
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            ArrayList<SRSGraph> solns = _randomSpace.get(keys.get(i));
            Random rand = new Random();
            int index = rand.nextInt(solns.size());
            System.out.println("Goal part: " + keys.get(i) + " space size: " + _randomSpace.get(keys.get(i)).size() + " randomly selected index: " + index);
            resultGraphs.add(solns.get(index));
        }
        
        return resultGraphs;
    }

    /** Find assembly graph for a single goal part factoring in slack and sharing **/
    protected SRSGraph createAsmGraph_sgp(SRSNode goalPartNode) {

        SRSGraph newGraph = new SRSGraph(goalPartNode);

        //Recursive Programing to find best graph
        int gpSize = goalPartNode.getComposition().size();
        ArrayList<String> gpComp = goalPartNode.getComposition();
        ArrayList<String> gpType = goalPartNode.getType();

        //Create idexes for goal part
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        for (int i = 1; i < gpSize; i++) {
            indexes.add(i);
        }

        //If a large part has a combination of smaller parts that are all forbidden, which make it impossible make, this part must also be forbidden
        //We must record if there is any way to break it
        HashMap<Integer, ArrayList<int[]>> partitionSizes = new HashMap<Integer, ArrayList<int[]>>();
        for (int i = 1; i < _maxNeighbors; i++) {
            ArrayList<int[]> forbiddenPartitions = new ArrayList<int[]>();
            partitionSizes.put(i, forbiddenPartitions);
        }
        
        HashMap<Integer, ArrayList<int[]>> partitions = getPartitions(indexes, partitionSizes);
        
        //Iterate over all part "breaks"
        //Find best graph for all possible number of partition sizes
        Set<Integer> keySet = partitions.keySet();
        ArrayList partitionKeys = new ArrayList(keySet);
        Collections.sort(partitionKeys); 
        
        for (int p = 0; p < partitionKeys.size(); p++) {

            Integer aPartitionKey = (Integer) partitionKeys.get(p);
            ArrayList<int[]> candidatePartitions = partitions.get(aPartitionKey);
             
            //For each partition of this size
            while (!candidatePartitions.isEmpty()) {
                
                int[] thisPartition = candidatePartitions.get(0);
                candidatePartitions.remove(0);
                ArrayList<SRSNode> allSubParts = new ArrayList<SRSNode>();

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

                    //If not, make the new SRSNode
                    SRSNode aSubPart = new SRSNode(false, false, null, comp, type);
                    allSubParts.add(aSubPart);
                }
               
                ArrayList<SRSGraph> toCombine = new ArrayList<SRSGraph>();
                for (int o = 0; o < allSubParts.size(); o++) {
                    SRSGraph solution = createAsmGraph_sgp(allSubParts.get(o));
                    toCombine.add(solution);
                }

                newGraph = combineGraphsStageStep(toCombine);
                
                //If this graph is for one of the goal parts, save it in the goalpartComp - ArrayList<graph>> set
                if (newGraph.getRootNode().getComposition().toString().equals(_goalComp)) {
                    if (_randomSpace.containsKey(_goalComp)) {
                        ArrayList<SRSGraph> graphsThisComp = _randomSpace.get(_goalComp);
                        graphsThisComp.add(newGraph);
                        _randomSpace.put(_goalComp, graphsThisComp);
                    } else {
                        ArrayList<SRSGraph> graphsThisComp = new ArrayList<SRSGraph>();
                        graphsThisComp.add(newGraph);
                        _randomSpace.put(_goalComp, graphsThisComp);
                    }                    
                }
            }
        }        

        //Return best graph for the initial goal part
        return newGraph;
    }

    /** Combine multiple graphs, ignoring sharing and recommended **/
    protected SRSGraph combineGraphsStageStep(ArrayList<SRSGraph> graphs) {
        SRSNode newRoot = new SRSNode();
        ArrayList<String> mergerComposition = new ArrayList<String>();
        ArrayList<String> mergerType = new ArrayList<String>();
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

    /** For n-way assembly, must find all ways to "break" a part i.e. all possible partition of size maxNeighbors and less **/
    protected HashMap<Integer, ArrayList<int[]>> getPartitions(ArrayList<Integer> indexes, HashMap<Integer, ArrayList<int[]>> forbiddenPartitions) {

        int[] newIndexes = buildIntArray(indexes);
        HashMap<Integer, ArrayList<int[]>> partitions = new HashMap<Integer, ArrayList<int[]>>();
        Set<Integer> keySet = forbiddenPartitions.keySet();
        ArrayList<Integer> keys = new ArrayList<Integer>(keySet);
        Collections.sort(keys);

        for (Integer n : keys) {
            ArrayList<int[]> subsets = getSubsets(newIndexes, n, forbiddenPartitions.get(n));
            partitions.put(n, subsets);
        }

        return partitions;
    }
    
    //Fields
    private String _goalComp;
    private HashMap<String, ArrayList<SRSGraph>> _randomSpace;
    protected int _maxNeighbors;
}