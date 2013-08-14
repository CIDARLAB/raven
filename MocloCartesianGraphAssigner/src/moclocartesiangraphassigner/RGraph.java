/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moclocartesiangraphassigner;

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
        _rootNode = new RNode();
        _subGraphs = new ArrayList<RGraph>();
        _stages = 0;
        _steps = 0;
        _recCnt = 0;
        _disCnt = 0;
        _sharingFactor = 0;
        _efficiencyArray = new ArrayList<Double>();
        _reactions = 0;
    }

    /**
     * SDSGraph constructor, specified root node *
     */
    public RGraph(RNode node) {
        _rootNode = node;
        _subGraphs = new ArrayList<RGraph>();
        _stages = 0;
        _steps = 0;
        _recCnt = 0;
        _disCnt = 0;
        _sharingFactor = 0;
        _efficiencyArray = new ArrayList<Double>();
        _reactions = 0;
    }

    /**
     * Clone method for an SDSGraph *
     */
    @Override
    public RGraph clone() {
        RGraph clone = new RGraph();
        clone._rootNode = this._rootNode.clone();
        clone._recCnt = this._recCnt;
        clone._disCnt = this._disCnt;
        clone._stages = this._stages;
        clone._steps = this._steps;
        clone._sharingFactor = this._sharingFactor;
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
//                            System.out.println("replacing "+current.getComposition()+" stage: "+current.getStage()+" "+current.getLOverhang()+current.getROverhang());
//                            System.out.println("with      "+finalNode.getComposition()+" stage: "+finalNode.getStage()+" "+finalNode.getLOverhang()+finalNode.getROverhang());
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
        seenUUIDs.add(this._rootNode.getUUID());

        //Start at the root node and look at all children
        for (RNode neighbor : this._rootNode.getNeighbors()) {
            seenUUIDs.add(neighbor.getUUID());
            edges = getPostOrderEdgesHelper(neighbor, this._rootNode, edges, seenUUIDs, true);
        }
        //first edge is the vector
        edges.add(0, this._rootNode.getUUID() + " -> " + this._rootNode.getVector().getUUID());
//        edges.add("node -> vector");
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
                if (current.getStage() != 0) {
                    edgesToAdd.add(current.getUUID() + " -> " + current.getVector().getUUID());
                }
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
        return _rootNode;
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
    public int getSharingFactor() {
        return _sharingFactor;
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
        _rootNode = newRoot;
    }

    /**
     * Find sharing score for a given SDSGraph *
     */
    public void setSharingFactor(int sharing) {
        _sharingFactor = sharing;
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
    
    //FIELDS
    private ArrayList<RGraph> _subGraphs;
    private RNode _rootNode;
    private int _stages;
    private int _steps;
    private int _sharedSteps;
    private ArrayList<Double> _efficiencyArray;
    private int _recCnt;
    private int _disCnt;
    private int _sharingFactor;
    private int _reactions;
    private boolean _pinned;
}