/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import Controller.accessibility.ClothoReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author jenhantao, evanappleton
 */
public class RGraph {

    /**
     * SDSGraph constructor, no specified root node *
     */
    public RGraph() {
        _node = new RNode();
        _subGraphs = new ArrayList<RGraph>();
        _stages = 0;
        _steps = 0;
        _recCnt = 0;
        _disCnt = 0;
        _modularityFactor = 0;
        _efficiencyArray = new ArrayList<Double>();
        _reactions = 0;
    }

    /**
     * SDSGraph constructor, specified root node *
     */
    public RGraph(RNode node) {
        _node = node;
        _subGraphs = new ArrayList<RGraph>();
        _stages = 0;
        _steps = 0;
        _recCnt = 0;
        _disCnt = 0;
        _modularityFactor = 0;
        _efficiencyArray = new ArrayList<Double>();
        _reactions = 0;
    }

    /**
     * Clone method for an SDSGraph *
     */
    @Override
    public RGraph clone() {
        RGraph clone = new RGraph();
        clone._node = this._node.clone();
        clone._recCnt = this._recCnt;
        clone._disCnt = this._disCnt;
        clone._stages = this._stages;
        clone._steps = this._steps;
        clone._modularityFactor = this._modularityFactor;
        clone._efficiencyArray = this._efficiencyArray;
        clone._reactions = this._reactions;
        return clone;
    }

    /**
     * Pin a graph - pin and set steps to 0 *
     */
    public void pin() {
        this._pinned = true;
        this._steps = 0;
    }

    public ArrayList<Part> getPartsInGraph(Collector coll) {
        ArrayList<Part> toReturn = new ArrayList<Part>();
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        queue.add(this.getRootNode());
        while (!queue.isEmpty()) {
            RNode current = queue.get(0);
            seenNodes.add(current);
            queue.remove(0);
            Part toAdd = coll.getPart(current.getUUID(), true);
            if (toAdd != null) {
                toReturn.add(toAdd);
            }
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return toReturn;
    }

    public ArrayList<Vector> getVectorsInGraph(Collector coll) {
        ArrayList<Vector> toReturn = new ArrayList<Vector>();
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        queue.add(this.getRootNode());
        while (!queue.isEmpty()) {
            RNode current = queue.get(0);
            seenNodes.add(current);
            queue.remove(0);
            if (current.getVector() != null) {
                Vector toAdd = coll.getVector(current.getVector().getUUID(), true);
                if (toAdd != null) {
                    toReturn.add(toAdd);
                }
            }
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return toReturn;
    }

    public static ArrayList<RGraph> mergeGraphs(ArrayList<RGraph> graphs) {

        ArrayList<RGraph> mergedGraphs = new ArrayList<RGraph>();
        HashMap<String, RNode> mergedNodesHash = new HashMap<String, RNode>();
        
        //Traverse and merge graphs
        for (int i = 0; i < graphs.size(); i++) {
            
            RGraph aGraph = graphs.get(i);
            boolean hasParent = true;
            
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList<RNode>();
            queue.add(aGraph.getRootNode());
            
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);
                
                String currentCompOHStage = current.getComposition().toString() + "|" + current.getLOverhang() + "|" + current.getROverhang() + "|" + current.getStage();
                
                //If a node with this composition, overhangs and stage has not been seen before
                if (mergedNodesHash.containsKey(currentCompOHStage) == false) {
                    mergedNodesHash.put(currentCompOHStage, current);
                    
                    for (RNode neighbor : current.getNeighbors()) {
                        if (!seenNodes.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                
                //If it has been seen merge the node in the hash and disconnect this node from solution
                } else {
                 
                    RNode finalNode = mergedNodesHash.get(currentCompOHStage);
                    ArrayList<RNode> neighbors = current.getNeighbors();
                    
                    //Remove parent from current node's neighbors, add it to the hashed node's nieghbors
                    hasParent = false;
                    for (int j = 0; j < neighbors.size(); j++) {
                        if (neighbors.get(j).getStage() > current.getStage()) {
                            RNode parent = neighbors.get(j);
                            hasParent = true;                           
                            parent.replaceNeighbor(current, finalNode); 
                            finalNode.addNeighbor(parent);
                            current.removeNeighbor(parent);
                        }
                    }
                    
                    //Edge case where multiple goal parts have the same composition
                    if (hasParent == false) {
                        String name = finalNode.getName();
                        name = name + " | " + current.getName();
                        finalNode.setName(name);                        
                    }                   
                }
            }
            
            if (hasParent == true) {
                mergedGraphs.add(aGraph);
            }            
        }
        
        //Remove graphs that have identical nodes to ones already seen from returned set
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        ArrayList<RGraph> remGraphs = new ArrayList<RGraph>();

        for (RGraph graph : mergedGraphs) {
            queue.add(graph.getRootNode());
            boolean newNodes = seenNodes.add(graph.getRootNode());

            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);

                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        newNodes = true;
                    }
                }
            }
            
            if (newNodes == false) {
                remGraphs.add(graph);
            }
        }
        
        mergedGraphs.removeAll(remGraphs);
        return mergedGraphs;
    }

    /** Get graph statistics **/
    public static void getGraphStats(ArrayList<RGraph> mergedGraphs, ArrayList<Part> partLib, ArrayList<Vector> vectorLib, HashMap<Part, ArrayList<Part>> goalParts, HashSet<String> recommended, HashSet<String> discouraged, boolean scarless, Double stepCost, Double stepTime, Double pcrCost, Double pcrTime) {
        
        HashSet<String> startPartsLOcompRO = new HashSet<String>();
        HashSet<String> startVectorsLOlevelRO = new HashSet<String>();
        
        //Get goal part compositions
        Set<Part> keySet = goalParts.keySet();
        HashSet<ArrayList<String>> gpComps = new HashSet<ArrayList<String>>();
        for (Part gp : keySet) {
            ArrayList<String> compStr = new ArrayList<String>();
            ArrayList<Part> compPart = goalParts.get(gp);
            for (int i = 0; i < compPart.size(); i++) {
                Part p = compPart.get(i);
                compStr.add(p.getName());
            }
            gpComps.add(compStr);
        }
        
        //Go through parts library, put all compositions into hash of things that already exist
        for (Part aPart : partLib) {
            String leftOverhang = aPart.getLeftOverhang();
            String rightOverhang = aPart.getRightOverhang();
            ArrayList<Part> partComp = aPart.getComposition();
            ArrayList<String> comp = new ArrayList<String>();
            for (int j = 0; j < partComp.size(); j++) {
                String name = partComp.get(j).getName();
                comp.add(name);
            }

            String aPartLOnameRO = leftOverhang + comp + rightOverhang;
            startPartsLOcompRO.add(aPartLOnameRO);          
        }

        //Go through vectors library, put all compositions into hash of things that already exist
        for (Vector aVec : vectorLib) {
            String leftoverhang = aVec.getLeftoverhang();
            String rightOverhang = aVec.getRightOverhang();
            int stage = aVec.getLevel();

            String aVecLOlevelRO = leftoverhang + stage + rightOverhang;
            startVectorsLOlevelRO.add(aVecLOlevelRO);          
        }

        //Will get stats for a set of graphs and assign the values to the individual graphs
        for (int i = 0; i < mergedGraphs.size(); i++) {
            
            HashSet<String> partsLOcompRO = new HashSet<String>();
            HashSet<String> vectorsLOlevelRO = new HashSet<String>();
            HashSet<ArrayList<String>> neighborHash = new HashSet<ArrayList<String>>();
            partsLOcompRO.addAll(startPartsLOcompRO);
            vectorsLOlevelRO.addAll(startVectorsLOlevelRO);

            int PCRs = 0;
            int steps = 0;
            int recCount = 0;
            int disCount = 0;
            int stages = 0;
            int shared = 0;
            ArrayList<Double> efficiency = new ArrayList<Double>();

            RGraph aGraph = mergedGraphs.get(i);
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList<RNode>();
            queue.add(aGraph.getRootNode());

            //Traverse the graph
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);
                int numParents = 0;

                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        if (!queue.contains(neighbor)) {
                            queue.add(neighbor);
                        } 
                    }
                    if (neighbor.getStage() > current.getStage()) {
                        numParents++;
                    }
                }

                ArrayList<String> composition = current.getComposition();
                String lOverhang = current.getLOverhang();
                String rOverhang = current.getROverhang();
                String aPartLOcompRO = lOverhang + composition + rOverhang;
                String aVecLOlevelRO = new String();

                //PCR Reactions for scarless assembly
                if (scarless == true) {
                    if (gpComps.contains(composition)) {
                        if (lOverhang.isEmpty() && rOverhang.isEmpty()) {

                            //Record left and right neighbors for each node... this will determine how many PCRs need to be performed                            
                            String prev = new String();
                            String next = new String();
                            for (int j = 0; j < composition.size(); j++) {
                                String currentBP = composition.get(j);
                                if (j == 0) {
                                    next = composition.get(j + 1);
                                } else if (j == composition.size() - 1) {
                                    prev = composition.get(j - 1);
                                } else {
                                    next = composition.get(j + 1);
                                    prev = composition.get(j - 1);
                                }
                                ArrayList<String> seq = new ArrayList<String>();
                                seq.add(prev);
                                seq.add(currentBP);
                                seq.add(next);
                                neighborHash.add(seq);
                            }
                        }
                    }
                }
                
                if (current.getVector() != null) {
                    int level = current.getVector().getLevel();
                    aVecLOlevelRO = lOverhang + level + rOverhang;
                }


                //If a part with this composition and overhangs doesn't exist, there must be a PCR done                
                if (current.getStage() == 0) {
                    if (partsLOcompRO.add(aPartLOcompRO) != false) {
                        PCRs++;
                    }
                } 
                
                //If a vector with this composition and overhangs doesn't exist, there must be a PCR done
                if (vectorsLOlevelRO.add(aVecLOlevelRO) != false && !aVecLOlevelRO.isEmpty()) {
                    PCRs++;
                }
                
                //If the node is grater than stage 0, it is a step and add to efficiency list
                if (current.getStage() > 0) {
                    steps++;
                    efficiency.add(current.getEfficiency());

                    //If the step is shared
                    if (numParents > 1) {
                        shared++;
                    }
                }

                //Save max stage
                if (current.getStage() > stages) {
                    stages = current.getStage();
                } 
                
                //Add it to recommended count if it's recommended
                if (recommended.contains(current.getComposition().toString())) {
                    recCount++;
                }
                
                //Add it to discouraged count if it's discouraged
                if (discouraged.contains(current.getComposition().toString())) {
                    disCount++;
                }
            }
            
            if (scarless == false) {
                aGraph.setReactions(PCRs);
            } else {
                aGraph.setReactions(neighborHash.size());
            }

            //Estimated time and cost
            double estCost = (steps*stepCost) + (pcrCost*PCRs);
            double estTime = (stages*stepTime) + pcrTime;
            
            aGraph.setSteps(steps);
            aGraph.setDiscouragedCount(disCount);
            aGraph.setReccomendedCount(recCount);
            aGraph.setStages(stages);
            aGraph.setEfficiencyArray(efficiency);
            aGraph.setSharing(shared);
            aGraph.setEstCost(estCost);
            aGraph.setEstTime(estTime);
        }
    }
    
    
    /**
     * ************************************************************************
     *
     * GRAPH EXPORT METHODS
     *
     *************************************************************************
     */
    /**
     * Get all the edges of an SDSGraph in Post Order *
     */
    public ArrayList<String> getPostOrderEdges() {
        ArrayList<String> edges = new ArrayList();
        HashSet<String> seenUUIDs = new HashSet();
        seenUUIDs.add(this._node.getUUID());
        
        //Start at the root node and look at all children
        for (RNode neighbor : this._node.getNeighbors()) {
            seenUUIDs.add(neighbor.getUUID());
            edges = getPostOrderEdgesHelper(neighbor, this._node, edges, seenUUIDs, true);
        }
        return edges;
    }

    /**
     * Return graph edges in an order specified for puppetshow *
     */
    private ArrayList<String> getPostOrderEdgesHelper(RNode current, RNode parent, ArrayList<String> edges, HashSet<String> seenUUIDs, boolean recurse) {
        ArrayList<String> edgesToAdd = new ArrayList();

        //Do a recursive call if there are unseen neighbors
        if (recurse) {

            //For all of this node's neighbors
            for (RNode neighbor : current.getNeighbors()) {

                //If the neighbor's composition is not that of the parent
                if (!parent.getUUID().equals(neighbor.getUUID())) {

                    //If this neighbor's composition hasn't been seen before, add it to the seen composition list and do a recursive call of this node, this time as this node being the parent
                    if (!seenUUIDs.contains(neighbor.getUUID())) {
                        seenUUIDs.add(neighbor.getUUID());
                        edges = getPostOrderEdgesHelper(neighbor, current, edges, seenUUIDs, true);

                        //If this neighbor has been seen, do not recursively call
                    } else {
                        edges = getPostOrderEdgesHelper(neighbor, current, edges, seenUUIDs, false);
                    }
                }
            }
        }

        //For all current neighbors... this is always done on any call
        for (RNode neighbor : current.getNeighbors()) {

            //Write arc connecting to the parent
            if (neighbor.getComposition().toString().equals(parent.getComposition().toString())) {
          
                //Make the edge going in the direction of the node with the greatest composition, whether this is parent or child
                if (current.getComposition().size() > neighbor.getComposition().size()) {
                    edgesToAdd.add(current.getUUID() + " -> " + neighbor.getUUID());
                } else if (current.getComposition().size() < neighbor.getComposition().size()) {
                    edgesToAdd.add(neighbor.getUUID() + " -> " + current.getUUID());
                }
            }
        }

        for (String s : edgesToAdd) {
            edges.add(s);
        }
        return edges;
    }

    /**
     * Print edges arc file *
     */
    public String printArcsFile(Collector coll, ArrayList<String> edges, String method) {

        //Build String for export
        //Header
        StringBuilder arcsText = new StringBuilder();
        DateFormat dateFormat = new SimpleDateFormat("MMddyyyy@HHmm");
        Date date = new Date();
        arcsText.append("# AssemblyMethod: "+method+"\n# ").append(" ").append(dateFormat.format(date)).append("\n");
        arcsText.append("# ").append(coll.getPart(this._node.getUUID(), true)).append("\n");
        arcsText.append("# ").append(this._node.getUUID()).append("\n\n");

        //Build arc file 
        HashMap<String, String> nodeMap = new HashMap<String, String>();//key is uuid, value is name
        for (String s : edges) {
            String[] tokens = s.split("->");
            Part vertex1 = coll.getPart(tokens[0].trim(), true);
            Part vertex2 = coll.getPart(tokens[1].trim(), true);
            nodeMap.put(vertex1.getUUID(), vertex1.getName());
            nodeMap.put(vertex2.getUUID(), vertex2.getName());
            arcsText.append("# ").append(vertex1.getName()).append(" -> ").append(vertex2.getName()).append("\n");
            arcsText.append(s).append("\n");
        }

        //Build key
        Stack<RNode> stack = new Stack<RNode>();
        HashSet<RNode> seenNodes = new HashSet<RNode>();
        HashMap<String, String> compositionHash = new HashMap<String, String>();
        stack.add(this._node);
        while (!stack.isEmpty()) {
            RNode current = stack.pop();
            seenNodes.add(current);
            compositionHash.put(current.getUUID(), current.getComposition().toString());
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    stack.add(neighbor);
                }
            }
        }
        arcsText.append("\n# Key\n");
        for (String key : nodeMap.keySet()) {
            arcsText.append("# ").append(nodeMap.get(key)).append("\n");
            arcsText.append("# ").append(key).append("\n");
            String compositionString = compositionHash.get(key);
            arcsText.append("# (").append(compositionString.substring(1, compositionString.length() - 1)).append(")\n");
        }
        return arcsText.toString();
    }

    /**
     * Generate a Weyekin image file for a list of edges *
     */
    public String generateWeyekinFile(Collector coll, ArrayList<String> edges, boolean pigeon) {
        //Initiate weyekin file
        StringBuilder weyekinText = new StringBuilder();
        HashMap<String, String> nodeMap = new HashMap<String, String>();//key is uuid, value is name
        weyekinText.append("digraph {\n");

        //If edges are empty (happens when no assembly is necessary)
        if (edges.isEmpty()) {
            nodeMap.put(this.getRootNode().getUUID(), this.getRootNode().getComposition().toString() + this.getRootNode().getLOverhang() + this.getRootNode().getROverhang());
        }

        //Store list of edges
        String edgeLines = "";
        for (String s : edges) {
            String[] tokens = s.split("->");
            Part vertex1 = coll.getPart(tokens[0].trim(), true);
            Part vertex2 = coll.getPart(tokens[1].trim(), true);
            nodeMap.put(vertex1.getUUID(), vertex1.getStringComposition() + vertex1.getLeftOverhang() + vertex1.getRightOverhang());
            nodeMap.put(vertex2.getUUID(), vertex2.getStringComposition() + vertex2.getLeftOverhang() + vertex2.getRightOverhang());
            edgeLines = edgeLines + "\"" + nodeMap.get(vertex2.getUUID()) + "\"" + " -> " + "\"" + nodeMap.get(vertex1.getUUID()) + "\"" + "\n";

        }

        if (pigeon) {
            for (String key : nodeMap.keySet()) {
                Part currentPart = coll.getPart(key, true);
                ArrayList<Part> composition = currentPart.getComposition();
                ArrayList<String> direction = new ArrayList<String>();
                ArrayList<String> searchTags = currentPart.getSearchTags();
                for (String tag : searchTags) {
                    if (tag.startsWith("Direction:")) {
                        direction = ClothoReader.parseTags(tag);
                    }
                }
                
                StringBuilder pigeonLine = new StringBuilder();
                pigeonLine.append("PIGEON_START\n");
                pigeonLine.append("\"").append(nodeMap.get(key)).append("\"\n");

                //Assign left overhang if it exists                
                pigeonLine.append("3 ").append(currentPart.getLeftOverhang()).append("\n");
                pigeonLine.append("5 ").append(currentPart.getLeftOverhang()).append("\n");

                for (int i = 0; i < composition.size(); i++) {

                    Part p = composition.get(i);
                    String dir = "";
                    
                    //Turn direction of glyph in reverse if reverse direction
                    if (!direction.isEmpty()) {
                        dir = direction.get(i);
                        if ("-".equals(dir)) {
                            pigeonLine.append("<");
                        } 
                    }
                    
                    //Write pigeon code line
                    if (p.getType().equalsIgnoreCase("promoter") || p.getType().equalsIgnoreCase("p")) {
                        pigeonLine.append("P ").append(p.getName()).append(" 4" + "\n");
                    } else if (p.getType().equalsIgnoreCase("RBS") || p.getType().equalsIgnoreCase("r")) {
                        pigeonLine.append("r ").append(p.getName()).append(" 5" + "\n");
                    } else if (p.getType().equalsIgnoreCase("gene") || p.getType().equalsIgnoreCase("g")) {
                        pigeonLine.append("c ").append(p.getName()).append(" 1" + "\n");
                    } else if (p.getType().equalsIgnoreCase("reporter") || p.getType().equalsIgnoreCase("gr")) {
                        pigeonLine.append("c ").append(p.getName()).append(" 2" + "\n");
                    } else if (p.getType().equalsIgnoreCase("terminator") || p.getType().equalsIgnoreCase("t")) {
                        pigeonLine.append("T ").append(p.getName()).append(" 6" + "\n");
                    } else if (p.getType().equalsIgnoreCase("invertase site") || p.getType().equalsIgnoreCase("is")) {
                        if ("-".equals(dir)) {
                            pigeonLine.append(" ").append(p.getName()).append(" 12" + "\n");
                        } else {
                            pigeonLine.append("> ").append(p.getName()).append(" 12" + "\n");  
                        }                                           
                    } else if (p.getType().equalsIgnoreCase("spacer") || p.getType().equalsIgnoreCase("s")) {
                        pigeonLine.append("s ").append(p.getName()).append(" 10" + "\n");  
                    } else if (p.getType().equalsIgnoreCase("origin") || p.getType().equalsIgnoreCase("o")) {
                        pigeonLine.append("z ").append(p.getName()).append(" 14" + "\n");
                    } else if (p.getType().equalsIgnoreCase("fusion") || p.getType().equalsIgnoreCase("fu")) {
                        pigeonLine.append("f1");
                        String[] fusionParts = p.getName().split("-");                        
                        for (int j = 1; j < fusionParts.length; j++) {
                            int color = j % 13 + 1;
                            pigeonLine.append("-").append(color);
                        }
                        pigeonLine.append(" ").append(p.getName()).append("\n");   
                    } else {
                        pigeonLine.append(key);
                    }
                }

                //Assign right overhang                
                pigeonLine.append("3 ").append(currentPart.getRightOverhang()).append("\n");
                pigeonLine.append("5 ").append(currentPart.getRightOverhang()).append("\n");
                pigeonLine.append("v").append("\n");
                pigeonLine.append("# Arcs\n");
                pigeonLine.append("PIGEON_END\n\n");
                weyekinText.append(pigeonLine.toString());
            }
        } else {

            //Write node properties - purple boxes if cannot pigeon
            for (String key : nodeMap.keySet()) {
                weyekinText.append("\"").append(coll.getPart(key, true).getStringComposition()).append("\"" + " [shape=box, color=\"#B293C9\", style=\"filled,rounded\"]" + "\n");
            }
        }

        //Write edge lines
        weyekinText.append(edgeLines);
        weyekinText.append("}");
        return weyekinText.toString();
    }

    /**
     * Merge multiple arc files into one file with one graph *
     */
    public static String mergeArcFiles(ArrayList<String> inputFiles) {
        String outFile = "";
        String header = "";

        //Grab the header from the first file; first two lines of header should be the same for all of the files
        String[] firstFileLines = inputFiles.get(0).split("\n"); //should split file into separate lines
        for (int i = 0; i < 2; i++) {
            header = header + firstFileLines[i] + "\n";
        }
        ArrayList<String> keyLines = new ArrayList<String>(); //stores the lines in all of the keys
        HashSet<String> seenArcLines = new HashSet(); //stores arc lines
        
        //Iterate through each arc file; each one is represented by a string
        for (String inputFile : inputFiles) {
            String[] lines = inputFile.split("\n"); //should split file into separate lines
            boolean seenKey = false;

            //Apend to the header
            for (int j = 2; j < 4; j++) {
                header = header + lines[j] + "\n";
            }
            
            //Apend to the key section
            for (int k = 4; k < lines.length; k++) {//first 4 lines are the header
                if (lines[k].contains("# Key")) {

                    //Once this line appears, store the following lines (which are lines of the key) into the keyLines arrayList.
                    seenKey = true;
                }
                if (seenKey) {

                    //If the key file doesnt have the current line in the current key, add it
                    if (!keyLines.contains(lines[k])) {
                        keyLines.add(lines[k]);
                    }
                } else {

                    //If the line isn't an empty line
                    if (lines[k].length() > 0 && !seenArcLines.contains(lines[k])) {
                        outFile = outFile + lines[k] + "\n";
                        seenArcLines.add(lines[k]);
                    }
                }
            }
        }

        //Apend key to toReturn
        outFile = outFile + "\n";
        for (int l = 0; l < keyLines.size(); l++) {
            outFile = outFile + keyLines.get(l) + "\n";
        }

        //Add header to toReturn
        outFile = header + "\n" + outFile;
        return outFile;
    }

    /**
     * Merge multiple graphviz files into one file with one graph *
     */
    public static String mergeWeyekinFiles(ArrayList<String> filesToMerge) {

        //Repeated edges should only appear in the same graph; an edge that appears in one graph should not appear in another
        String mergedFile = "";
        HashSet<String> seenLines = new HashSet<String>();
        HashSet<ArrayList<String>> seenEdges = new HashSet<ArrayList<String>>();
        ArrayList<String> edgeList = new ArrayList<String>();

        //For each file to merge
        for (String graphFile : filesToMerge) {
//            System.out.println("*******************************\n"+graphFile);
            String[] fileLines = graphFile.split("\n");
            HashSet<String> currentSeenLines = new HashSet<String>();
            boolean keepGoing = false;
            boolean lookAtNext = false;

            //For all the lines in each file
            for (int i = 1; i < fileLines.length - 1; i++) {

                //If this is the line directly after PIGEON_START
                if (lookAtNext) {

                    //If the line PIGEON_END is reached, include it and go on to larger if statement in next iteration
                    if (fileLines[i].equalsIgnoreCase("PIGEON_END")) {
                        if (keepGoing) {
                            mergedFile = mergedFile + "PIGEON_END\n\n";
                        }
                        lookAtNext = false;
                        keepGoing = false;
                        continue;
                    }

                    //If the name of the pigeon node hasn't been seen before, save it and all other lines until PIGEON_END
                    if (keepGoing) {
                        mergedFile = mergedFile + fileLines[i] + "\n";
                    } else if (!seenLines.contains(fileLines[i])) {
                        mergedFile = mergedFile + "PIGEON_START\n" + fileLines[i] + "\n";
                        keepGoing = true;
                    }

                    //All things needed multiple times, never stop them from being added to merged file
                } else if (!(fileLines[i].equalsIgnoreCase("digraph {") || fileLines[i].equalsIgnoreCase("}") || fileLines[i].equalsIgnoreCase("\n"))) {

                    //If the line hasn't been seen before
                    if (!seenLines.contains(fileLines[i])) {

                        //If there appears to be an edge
                        if (fileLines[i].contains("->") || fileLines[i].contains("<-")) {
                            ArrayList<String> nodePair = new ArrayList<String>();
                            String[] twoNodesA = fileLines[i].split(" <- ");
                            String[] twoNodesB = fileLines[i].split(" -> ");
                            if (twoNodesA.length == 2) {
                                nodePair.add(twoNodesA[0]);
                                nodePair.add(twoNodesA[1]);
                            } else if (twoNodesB.length == 2) {
                                nodePair.add(twoNodesB[0]);
                                nodePair.add(twoNodesB[1]);
                            }

                            //Search to see if a pair with these edges exists in another file in the merge in reverse order
                            boolean seenPair = false;
                            for (ArrayList<String> edges : seenEdges) {
                                if (edges.contains(nodePair.get(0)) && edges.contains(nodePair.get(1))) {
                                    seenPair = true;
                                }
                            }

                            //If the pair has already been seen
                            if (!seenPair) {
                                seenEdges.add(nodePair);
                                edgeList.add(fileLines[i]);
                            }
                        } else {

                            //Append this line to the merged file
                            mergedFile = mergedFile + fileLines[i] + "\n";
                        }

                        //Add this line to the lines seen in this file
                        currentSeenLines.add(fileLines[i]);
                    } else if (fileLines[i].equalsIgnoreCase("PIGEON_START")) {
                        lookAtNext = true;
                    }
                }
            }

            Iterator<String> iterator = currentSeenLines.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                seenLines.add(next);
            }
        }

        for (String edge : edgeList) {
            mergedFile = mergedFile + edge + "\n";
        }
        mergedFile = "digraph{\n" + mergedFile + "\n}";

        return mergedFile;
    }

    /**
     * Check a graph to see if all of its basic parts *
     */
    public boolean canPigeon() {
        boolean canPigeon = true;
        RNode root = this.getRootNode();
        ArrayList<String> types = root.getType();
        for (int i = 0; i < types.size(); i++) {
            if (!(types.get(i).equalsIgnoreCase("promoter") || types.get(i).equalsIgnoreCase("p") || types.get(i).equalsIgnoreCase("RBS") || types.get(i).equalsIgnoreCase("r") || types.get(i).equalsIgnoreCase("gene") || types.get(i).equalsIgnoreCase("g") || types.get(i).equalsIgnoreCase("terminator") || types.get(i).equalsIgnoreCase("t") || types.get(i).equalsIgnoreCase("reporter") || types.get(i).equalsIgnoreCase("gr") || types.get(i).equalsIgnoreCase("invertase site") || types.get(i).equalsIgnoreCase("is") || types.get(i).equalsIgnoreCase("fusion") || types.get(i).equalsIgnoreCase("fu") || types.get(i).equalsIgnoreCase("spacer") || types.get(i).equalsIgnoreCase("s") || types.get(i).equalsIgnoreCase("origin") || types.get(i).equalsIgnoreCase("o") || types.get(i).equalsIgnoreCase("promoter_r") || types.get(i).equalsIgnoreCase("p_r") || types.get(i).equalsIgnoreCase("RBS_r") || types.get(i).equalsIgnoreCase("r_r") || types.get(i).equalsIgnoreCase("gene_r") || types.get(i).equalsIgnoreCase("g_r") || types.get(i).equalsIgnoreCase("terminator_r") || types.get(i).equalsIgnoreCase("t_r") || types.get(i).equalsIgnoreCase("reporter_r") || types.get(i).equalsIgnoreCase("r_r") || types.get(i).equalsIgnoreCase("invertase site_r") || types.get(i).equalsIgnoreCase("is_r") || types.get(i).equalsIgnoreCase("fusion_r") || types.get(i).equalsIgnoreCase("fu_r") || types.get(i).equalsIgnoreCase("spacer_r") || types.get(i).equalsIgnoreCase("s_r"))) {
                canPigeon = false;
            }
        }
        return canPigeon;
    }

    /**
     * ************************************************************************
     *
     * GETTER AND SETTER METHODS
     *
     *************************************************************************
     */
    /**
     * Add a subgraph to a graph *
     */
    public void addSubgraph(RGraph graph) {
        _subGraphs.add(graph);
    }

    /**
     * Get graph root node *
     */
    public RNode getRootNode() {
        return _node;
    }

    /**
     * Find how many stages for a given SDSGraph *
     */
    public int getStages() {
        return _stages;
    }

    /**
     * Find how many steps for a given SDSGraph *
     */
    public int getSteps() {
        return _steps;
    }

    /**
     * Find how many recommended intermediates for a given SDSGraph *
     */
    public int getReccomendedCount() {
        return _recCnt;
    }

    /**
     * Find how many discouraged intermediates for a given SDSGraph *
     */
    public int getDiscouragedCount() {
        return _disCnt;
    }

    /**
     * Determine if the graph in question is pinned *
     */
    public boolean getPinned() {
        return _pinned;
    }

    /**
     * Find sharing score for a given SDSGraph *
     */
    public int getModularityFactor() {
        return _modularityFactor;
    }
    
    /**
     * Get the number of shared steps in a graph
     */
    public int getSharing() {
        return _sharedSteps;
    }

    /**
     * Get all subgraphs of this graph *
     */
    public ArrayList<RGraph> getSubGraphs() {
        return _subGraphs;
    }

    /**
     * Get the array of efficiency scores for all nodes of a graph *
     */
    public ArrayList<Double> getEfficiencyArray() {
        return _efficiencyArray;
    }
    
    /**
     * Get the average efficiency score of a graph *
     */
    public double getAveEff() {
        
        ArrayList<Double> efficiencyArray = this.getEfficiencyArray();
        double sumEff = 0;
        double aveEff;
        for (int i = 0; i < efficiencyArray.size(); i++) {
            sumEff = sumEff + efficiencyArray.get(i);
        }
        aveEff = sumEff/efficiencyArray.size();        
        return aveEff;
    }
    
    /**
     * Get the reaction score of a graph *
     */
    public int getReaction() {
        return _reactions;
    }
    
    /**
     * Get the estimated graph time *
     */
    public double getEstTime() {
        return _estTime;
    }
    
    /**
     * Get the estimated graph cost *
     */
    public double getEstCost() {
        return _estCost;
    }

    /**
     * Set the number of stages for an SDSGraph *
     */
    public void setStages(int stages) {
        _stages = stages;
    }

    /**
     * Set the number of steps for an SDSGraph *
     */
    public void setSteps(int steps) {
        _steps = steps;
    }

    /**
     * Set the number of recommended intermediates for an SDSGraph *
     */
    public void setReccomendedCount(int count) {
        _recCnt = count;
    }

    /**
     * Set the number of recommended intermediates for an SDSGraph *
     */
    public void setDiscouragedCount(int count) {
        _disCnt = count;
    }

    /**
     * Set graph root node *
     */
    public void setRootNode(RNode newRoot) {
        _node = newRoot;
    }

    /**
     * Find sharing score for a given SDSGraph *
     */
    public void setModularityFactor(int modularity) {
        _modularityFactor = modularity;
    }
    
    /**
     * Set the number of shared steps in a graph
     */
    public void setSharing(int sharing) {
        _sharedSteps = sharing;
    }

    /**
     * Get all subgraphs of this graph *
     */
    public void setSubGraphs(ArrayList<RGraph> subGraphs) {
        _subGraphs = subGraphs;
    }

    /**
     * Set the efficiency score of a graph *
     */
    public void setEfficiencyArray(ArrayList<Double> efficiency) {
        _efficiencyArray = efficiency;
    }
    
    /**
     * Set the reaction score of a graph *
     */
    public void setReactions(int numReactions) {
        _reactions = numReactions;
    }
    
    /**
     * Set the estimated graph time *
     */
    public void setEstTime(Double estTime) {
        _estTime = estTime;
    }
    
    /**
     * Set the estimated graph time *
     */
    public void setEstCost(Double estCost) {
        _estCost = estCost;
    }
    
    //FIELDS
    private ArrayList<RGraph> _subGraphs;
    private RNode _node;
    private int _stages;
    private int _steps;
    private int _sharedSteps;
    private ArrayList<Double> _efficiencyArray;
    private int _recCnt;
    private int _disCnt;
    private int _modularityFactor;
    private int _reactions;
    private double _estCost;
    private double _estTime;
    private boolean _pinned;
}