/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import Controller.accessibility.ClothoReader;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 *
 * @author jenhantao, evanappleton
 */
public class SRSAlgorithmCore {

     /**
     * ************************************************************************
     *
     * THIS CLASS HAS A LOT OF METHODS THAT ARE USED BY THE GENERAL ALGORITHM WITH NO REAL HOME
     *
     *************************************************************************
     */
    
    /**
     * Given goal parts and library, create hashMem, key: composition with
     * overhangs concatenated at the end, value: corresponding graph *
     */
    
     /**
     * ************************************************************************
     *
     * DATA IMPORT FROM CLOTHO DATA STRUCTURE
     *
     *************************************************************************
     */
    
    protected HashMap<String, SRSGraph> partImportClotho(ArrayList<Part> goalParts, ArrayList<Part> partLibrary, HashSet<String> discouraged, HashSet<String> recommended) throws Exception {

        //Create library to initialize hashMem
        HashMap<String, SRSGraph> library = new HashMap<String, SRSGraph>();

        //Add goal parts to memoization hash, making new nodes with only type and composition from library
        for (Part goalPart : goalParts) {
            try {
                ArrayList<Part> basicParts = ClothoReader.getComposition(goalPart);
                for (int i = 0; i < basicParts.size(); i++) {

                    //Initialize new graph for a basic part
                    SRSGraph newBasicGraph = new SRSGraph();
                    newBasicGraph.getRootNode().setUUID(basicParts.get(i).getUUID());

                    //Get basic part compositions and search tags relating to feature type, overhangs ignored for this step
                    ArrayList<String> composition = new ArrayList<String>();
                    composition.add(basicParts.get(i).getName());
                    ArrayList<String> sTags = basicParts.get(i).getSearchTags();
                    ArrayList<String> type = new ArrayList<String>();

                    for (int k = 0; k < sTags.size(); k++) {
                        if (sTags.get(k).startsWith("Type:")) {
                            String typeTag = sTags.get(k);
                            ArrayList<String> types = parseTypeTags(typeTag);
                            type.addAll(types);
                        }
                    }

                    //Set type and composition
                    SRSNode root = newBasicGraph.getRootNode();
                    root.setName(basicParts.get(i).getName());
                    root.setComposition(composition);
                    root.setType(type);
                    library.put(root.getComposition().toString(), newBasicGraph);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //If there is an input Clotho part library, make a new node with only type and composition from library
        if (partLibrary != null) {
            if (partLibrary.size() > 0) {
                for (Part libraryPart : partLibrary) {

                    //Check if basic part or not and assign composition 
                    ArrayList<Part> libPartComposition = new ArrayList<Part>();
                    if (!libraryPart.isBasic()) {
                        libPartComposition = ClothoReader.getComposition(libraryPart);
                    } else {
                        libPartComposition.add(libraryPart);
                    }

                    //For all of this library part's components make new basic graph
                    ArrayList<String> type = new ArrayList<String>();
                    ArrayList<String> composition = new ArrayList<String>();

                    for (Part libPartComponent : libPartComposition) {
                        ArrayList<String> sTags = libPartComponent.getSearchTags();
                        composition.add(libPartComponent.getName());

                        //If the part has search tags
                        if (libPartComponent.getSearchTags() != null) {
                            for (int j = 0; j < sTags.size(); j++) {
                                if (sTags.get(j).startsWith("Type:")) {
                                    String typeTag = sTags.get(j);
                                    ArrayList<String> types = parseTypeTags(typeTag);
                                    type.addAll(types);
                                }
                            }
                        }
                    }

                    //Initialize new graph for library part
                    SRSGraph libraryPartGraph = new SRSGraph();
                    libraryPartGraph.getRootNode().setUUID(libraryPart.getUUID());
                    libraryPartGraph.getRootNode().setComposition(composition);
                    libraryPartGraph.getRootNode().setType(type);
                    libraryPartGraph.getRootNode().setName(libraryPart.getName());


                    //If recommended, give graph a recommended score of 1, make root node recommended
                    if (recommended.contains(composition.toString())) {
                        libraryPartGraph.setReccomendedCount(libraryPartGraph.getReccomendedCount() + 1);
                        libraryPartGraph.getRootNode().setRecommended(true);
                    }
                    
                    //If discouraged, give graph a recommended score of 1, make root node recommended
                    if (discouraged.contains(composition.toString())) {
                        libraryPartGraph.setDiscouragedCount(libraryPartGraph.getDiscouragedCount() + 1);
                        libraryPartGraph.getRootNode().setDiscouraged(true);
                    }

                    //Put library part into library for assembly
                    library.put(libraryPartGraph.getRootNode().getComposition().toString(), libraryPartGraph);
                }
            }
        }
        return library;
    }

    /**
     * Given a vector library, create vectorHash *
     */
    protected ArrayList<SRSVector> vectorImportClotho(ArrayList<Vector> vectorLibrary) {

        //Initialize vector library
        ArrayList<SRSVector> library = new ArrayList<SRSVector>();

        //Provided there is an input vector library
        if (vectorLibrary != null) {
            if (vectorLibrary.size() > 0) {
                for (Vector aVector : vectorLibrary) {

                    //Initialize a new vector
                    SRSVector vector = new SRSVector();

                    //If there's search tags, find overhangs
                    if (aVector.getSearchTags() != null) {
                        ArrayList<String> sTags = aVector.getSearchTags();
                        String LO = new String();
                        String RO = new String();
                        String resistance = new String();
                        int level = -1;
                        for (int i = 0; i < sTags.size(); i++) {
                            if (sTags.get(i).startsWith("LO:")) {
                                LO = sTags.get(i).substring(4);
                            } else if (sTags.get(i).startsWith("RO:")) {
                                RO = sTags.get(i).substring(4);
                            } else if (sTags.get(i).startsWith("Level:")) {
                                String aLevel = sTags.get(i).substring(7);
                                level = Integer.parseInt(aLevel);
                            } else if (sTags.get(i).startsWith("Resistance:")) {
                                resistance = sTags.get(i).substring(12);
                            }
                        }
                        vector.setLOverhang(LO);
                        vector.setROverhang(RO);
                        vector.setStringResistance(resistance);
                        vector.setLevel(level);
                    }

                    vector.setName(aVector.getName());
                    vector.setUUID(aVector.getUUID());

                    library.add(vector);
                }
            }
        }
        return library;
    }

    /**
     * Convert goal parts into SRS nodes for the algorithm *
     */
    protected ArrayList<SRSNode> gpsToNodesClotho(ArrayList<Part> goalParts) throws Exception {
        ArrayList<SRSNode> gpsNodes = new ArrayList<SRSNode>();
        for (int i = 0; i < goalParts.size(); i++) {

            //Get goal part's composition and type (part description type)
            ArrayList<Part> basicParts = ClothoReader.getComposition(goalParts.get(i));
            ArrayList<String> composition = new ArrayList<String>();
            ArrayList<String> type = new ArrayList<String>();
            for (int j = 0; j < basicParts.size(); j++) {
                composition.add(basicParts.get(j).getName());
                ArrayList<String> sTags = basicParts.get(j).getSearchTags();
                for (int k = 0; k < sTags.size(); k++) {
                    if (sTags.get(k).startsWith("Type:")) {
                        String typeTag = sTags.get(k);
                        ArrayList<String> types = parseTypeTags(typeTag);
                        type.addAll(types);

                    }
                }
            }

            //Create a new node with the specified composition, add it to goal parts, required intermediates and recommended intermediates for algorithm
            SRSNode gp = new SRSNode(false, false, null, composition, type, false);
            gp.setUUID(goalParts.get(i).getUUID());
            gpsNodes.add(gp);
        }
        return gpsNodes;
    }

    /**
     * Parse type search tags from a string into an ArrayList *
     */
    public static ArrayList<String> parseTypeTags(String typeTag) {
        ArrayList<String> types = new ArrayList<String>();
        int length = typeTag.length();
        if (typeTag.startsWith("Type:")) {
            typeTag = typeTag.substring(6, length);
        }
        if (typeTag.startsWith("[")) {
            typeTag = typeTag.substring(1, (length - 1));
        }
        String[] tokens = typeTag.split(",");
        types.addAll(Arrays.asList(tokens));
        return types;
    }

    
    /**
     * ************************************************************************
     *
     * SHARING, REQUIRED, FORBIDDEN
     *
     *************************************************************************
     */
    
    /**
     * Find sharing score for all possible intermediates for a set of goal parts
     * *
     */
    protected HashMap<String, Integer> computeIntermediateSharing(ArrayList<SRSNode> goalParts) {
        HashMap<String, Integer> sharing = new HashMap<String, Integer>();

        //For each goal part
        for (int i = 0; i < goalParts.size(); i++) {
            SRSNode gp = goalParts.get(i);
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
     * For all goal parts, search for conflicts with required parts *
     */
    protected void conflictSearchRequired(SRSNode gp, HashSet<String> required) throws Exception {

        //Initialize hash to keep track of potential intermediate conflicts
        ArrayList<String> gpComp = gp.getComposition();
        int gpSize = gp.getComposition().size();
        HashMap<ArrayList<Integer>, String> indexes = new HashMap<ArrayList<Integer>, String>();

        //Scan through all intermediates to see if any are required
        for (int start = 0; start < gpSize; start++) {
            for (int end = start + 2; end < gpSize + 1; end++) {
                ArrayList<String> gpSub = new ArrayList<String>();
                gpSub.addAll(gpComp.subList(start, end));

                //If an intermediate matches a composition in the required part hash
                if (required.contains(gpSub.toString())) {

                    //Record required indices
                    ArrayList<Integer> foundIndices = new ArrayList<Integer>();
                    int endCheck = end - 1;
                    foundIndices.add(start);
                    foundIndices.add(endCheck);
                    indexes.put(foundIndices, gpSub.toString());

                    //Detect required part conflicts
                    Collection<ArrayList<Integer>> values = indexes.keySet();
                    for (ArrayList<Integer> index : values) {

                        //If in between the indices being checked
                        if ((start >= index.get(0) && endCheck <= index.get(1)) || index.get(0) >= start && index.get(1) <= endCheck) {
                            //If completely outside indices
                        } else if (endCheck < index.get(0) || start > index.get(1)) {
                            //If conflicts, return an error and stop the program
                        } else {
                            JOptionPane.showMessageDialog(null, "Required part conflict discovered with \"" + gpSub.toString() + "\"...\nThis intermediate overlaps with another required intermediate and both cannot appear in one assmebly graph.\nPlease remove this required part or the part(s) that conflict.\nIf using MoClo, beware that all basic transcriptional units are required without manual selection");
                            throw new Exception("Required part conflict discovered with \"" + gpSub.toString() + "\"...\nThis intermediate overlaps with another required intermediate and both cannot appear in one assmebly graph.\nPlease remove this required part or the part(s) that conflict.\nIf using MoClo, beware that all basic transcriptional units are required without manual selection");
                        }
                    }
                }
            }
        }
    }

    /**
     * For a given goal part, search for conflicts with forbidden parts *
     */
    protected HashSet<String> conflictSearchForbidden(ArrayList<String> gpComp, HashSet<String> forbidden) {
        int gpSize = gpComp.size();

        //Create idexes for goal part
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (int i = 1; i < gpSize; i++) {
            indices.add(i);
        }
        boolean canBreak = false;
        for (int k : indices) {

            //Find right and left composition
            ArrayList<String> leftComp = new ArrayList<String>();
            leftComp.addAll(gpComp.subList(0, k));
            ArrayList<String> rightComp = new ArrayList<String>();
            rightComp.addAll(gpComp.subList(k, gpSize));

            //If either the left or right part is forbidden
            if (!forbidden.contains(leftComp.toString()) && !forbidden.contains(rightComp.toString())) {

                //It can be broken
                canBreak = true;
            }
        }

        //Provided that this part cannot be broken, add this part to the forbidden set
        if (!canBreak) {
            forbidden.add(gpComp.toString());
            return forbidden;
        } else {
            return forbidden;
        }
    }

    
    /**
     * ************************************************************************
     *
     * PARTITIONING
     *
     *************************************************************************
     */
    
    /**
     * Get all subsets of a set for a specific sized subset *
     */
    protected ArrayList<int[]> getSubsets(int[] set, int k, ArrayList<int[]> forbiddenPartitions) {
//        int[] subset = new int[k];
//        ArrayList<int[]> bestIndexes = new ArrayList<int[]>();
//        _subsetScore = -1;
//        getSubsetsHelper(set, subset, 0, 0, bestIndexes);
//        return bestIndexes;

        int numel = (int) Math.ceil((double) set.length / k);
        int[][] subsets = new int[k][numel];
        double space = (double) set.length / k;

        int start = 0;
        int row = 0;
        int col = 0;

        //Divide the set into equal peices
        for (int i = 0; i < set.length; i++) {
            int end = (int) (i / space);
            if (end > start) {
                start = end;
                row++;
                col = 0;
                subsets[row][col] = set[i];
                col++;
            } else {
                subsets[row][col] = set[i];
                col++;
            }

        }

        ArrayList<int[]> allSets = new ArrayList<int[]>();
        int[] solution = new int[subsets.length];
        _subsetScore = -1;
        getSets(set, subsets, solution, 0, allSets, forbiddenPartitions);

        return allSets;
    }

    /**
     * Search permutations for the best score *
     */
    private void getSets(int[] set, int[][] subsets, int[] subset, int subsetSize, ArrayList<int[]> optimal, ArrayList<int[]> forbiddenSets) {

        if (subsetSize == subset.length) {

            boolean noZeros = true;
            for (int i = 0; i < subset.length; i++) {
                if (subset[i] == 0) {
                    noZeros = false;
                }
            }

            //If this set is forbidden
            boolean forbidden = false;
            for (int j = 0; j < forbiddenSets.size(); j++) {
                if (Arrays.equals(subset, forbiddenSets.get(j))) {
                    forbidden = true;
                }
            }

            //If this set has no zeros and is not forbidden
            if (noZeros && !forbidden) {
                int score = 0;

                //Determine score of subset
                int[] setCopy = set.clone();
                for (int k = 0; k < subset.length; k++) {
                    for (int l = 0; l < setCopy.length; l++) {
                        if (setCopy[l] == subset[k]) {
                            setCopy[l] = 0;
                        }
                    }
                }
                int start = 0;
                for (int i = 0; i < setCopy.length; i++) {
                    if (setCopy[i] == 0) {
                        score = score + (i - start) * (i - start);
                        start = i + 1;
                    }
                    if (i == (setCopy.length - 1)) {
                        score = score + (i + 1 - start) * (i + 1 - start);
                    }
                }

                //If this is the best score, put it into the optimal solutions
                if (_subsetScore == -1) {
                    _subsetScore = score;
                    optimal.add(subset);

                    //If this is the new best score
                } else if (score < _subsetScore) {
                    _subsetScore = score;
                    optimal.clear();
                    optimal.add(subset);

                    //If current score equal to the best score
                } else if (score == _subsetScore) {
                    optimal.add(subset);
                }
            }
        } else {

            //Get all the subsets of interest
            int col = subsets[0].length;
            for (int k = 0; k < col; k++) {
                int[] sub = subset.clone();
                sub[subsetSize] = subsets[subsetSize][k];
                getSets(set, subsets, sub, subsetSize + 1, optimal, forbiddenSets);
            }
        }
    }

//    private void getSubsetsHelper(int[] set, int[] subset, int subsetSize, int nextIndex, ArrayList<int[]> bestIndexes) { 
//        
//        //Subset optimization
//        if (subsetSize == subset.length) {
//            int score = 0;
//            int[] setCopy = set.clone();
//            
//            //Set all indexes of the regular set that match the subsets to 0 for splitting
//            for (int k = 0; k < subset.length; k++) {
//                for (int l = 0; l < setCopy.length; l++) {
//                    if (setCopy[l] == subset[k]) {
//                        setCopy[l] = 0;
//                    }
//                }
//            }
//            int start = 0;
//            
//            //Calculate spacing scores
//            for (int i = 0; i < setCopy.length; i++) {
//                if (setCopy[i] == 0) {
//                    score = score + (i-start)*(i-start);
//                    start = i + 1;
//                }
//                if (i == (setCopy.length - 1)) {
//                    score = score + (i+1-start)*(i+1-start);
//                }
//            }     
//            
//            //If the first pass through the algorithm
//            if (_subsetScore == -1) {      
//                _subsetScore = score;
//                bestIndexes.add(subset);          
//            
//            //If the current score is least squares
//            } else if (score < _subsetScore) {
//                _subsetScore = score;
//                bestIndexes.clear();
//                bestIndexes.add(subset);             
//            
//            //If current score equal to the best score
//            } else if (score == _subsetScore) {
//                bestIndexes.add(subset);
//            }
//        } else {
//            
//            //Recursive call to search more index combinations
//            for (int j = nextIndex; j < set.length; j++) {
//                int[] sub = subset.clone();
//                sub[subsetSize] = set[j];
//                getSubsetsHelper(set, sub, subsetSize + 1, j + 1, bestIndexes);
//            }
//        }
//    }
    /**
     * Convert an ArrayList of integers to an integer array *
     */
    protected int[] buildIntArray(ArrayList<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }

    
    /**
     * ************************************************************************
     *
     * MODULARITY CALCULATION METHODS
     *
     *************************************************************************
     */
    
    /**
     * Get all subgraphs of a graph by traversing a graph *
     */
    protected ArrayList<SRSGraph> getSubgraphs(SRSGraph graph) {

        ArrayList<SRSNode> queue = new ArrayList<SRSNode>();
        HashSet<SRSNode> seenNodes = new HashSet<SRSNode>();
        SRSGraph clone = graph.clone();
        queue.add(clone.getRootNode());
        while (!queue.isEmpty()) {
            SRSNode current = queue.get(0);
            queue.remove(0);
            seenNodes.add(current);
            for (SRSNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
            seenNodes.add(current);
        }

        return null;
    }

    /**
     * For each node, decompose into transcriptional units, return either part
     * types (1) or compositions (2). << Assumes part starts with a promoter and
     * ends with a terminator >> *
     */
    // type- 1 for name 2 for composition
    protected ArrayList<ArrayList<String>> getTranscriptionalUnits(ArrayList<SRSNode> goalParts, int type) {

        ArrayList<ArrayList<String>> TUs = new ArrayList<ArrayList<String>>();

        //For each goal part get TUs
        for (int i = 0; i < goalParts.size(); i++) {
            SRSNode gp = goalParts.get(i);

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

    protected ArrayList<ArrayList<String>> getSingleTranscriptionalUnits(ArrayList<SRSNode> goalParts, int type) {

        ArrayList<ArrayList<String>> TUs = new ArrayList<ArrayList<String>>();

        //For each goal part get TUs
        for (int i = 0; i < goalParts.size(); i++) {
            SRSNode gp = goalParts.get(i);

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
    protected HashMap<Integer, ArrayList<SRSNode>> getStageStepHash(ArrayList<SRSGraph> optimalGraphs) {

        HashMap<Integer, ArrayList<SRSNode>> stageHash = new HashMap<Integer, ArrayList<SRSNode>>();
        ArrayList<SRSNode> stageNodes = new ArrayList<SRSNode>();

        for (int i = 0; i < optimalGraphs.size(); i++) {

            //Traverse one graph and store the nodes
            SRSGraph graph = optimalGraphs.get(i);
            SRSNode rootNode = graph.getRootNode();
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
                    ArrayList<SRSNode> finalStageNodes = new ArrayList<SRSNode>();
                    finalStageNodes.addAll(stageHash.get(stage));
                    SRSNode spacer = new SRSNode();
                    finalStageNodes.add(spacer);
                    stageHash.put(stage, finalStageNodes);
                }
            }
        }

        return stageHash;
    }

    private HashMap<Integer, ArrayList<SRSNode>> getStageHashHelper(SRSNode parent, ArrayList<SRSNode> neighbors, HashMap<Integer, ArrayList<SRSNode>> stageHash) {

        //Check the current stageHash to get nodes that are already in there
        ArrayList<SRSNode> stageNodes = new ArrayList<SRSNode>();
        if (stageHash.containsKey(parent.getStage() - 1)) {
            stageNodes = stageHash.get(parent.getStage() - 1);
        }

        for (int i = 0; i < neighbors.size(); i++) {
            SRSNode neighbor = neighbors.get(i);
            stageNodes.add(neighbor);
            if (neighbor.getStage() > 0) {
                ArrayList<SRSNode> orderedChildren = new ArrayList<SRSNode>();
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
    
    //Fields
    private int _subsetScore;
}