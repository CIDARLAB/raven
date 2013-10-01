/*
 * To change this template,choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import Controller.algorithms.RGeneral;
import Controller.algorithms.modasm.RBioBricks;
import Controller.algorithms.modasm.*;
import Controller.algorithms.nonmodasm.*;
import Controller.datastructures.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Jenhan
 */
public class RavenController {

    public static boolean samplePartitions = false;
    public static boolean sampleOverhangs = false;
    public static boolean error = false;

    public ArrayList<RGraph> runBioBricks() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //Run algorithm for BioBricks assembly
        _assemblyGraphs.clear();
        RBioBricks biobricks = new RBioBricks();
        ArrayList<RGraph> optimalGraphs = biobricks.bioBricksClothoWrapper(_goalParts,_required,_recommended,_forbidden,_discouraged,_partLibrary,null);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for Gibson *
     */
    public ArrayList<RGraph> runGibson() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //Run algorithm for Gibson assembly
        _assemblyGraphs.clear();
        RGibson gibson = new RGibson();
        ArrayList<RGraph> optimalGraphs = gibson.gibsonClothoWrapper(_goalParts,_required,_recommended,_forbidden,_discouraged,_partLibrary,_efficiency,null);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for CPEC *
     */
    public ArrayList<RGraph> runCPEC() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //Run algorithm for CPEC assembly
        _assemblyGraphs.clear();
        RCPEC cpec = new RCPEC();
        ArrayList<RGraph> optimalGraphs = cpec.cpecClothoWrapper(_goalParts,_required,_recommended,_forbidden,_discouraged,_partLibrary,_efficiency,null);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for SLIC *
     */
    public ArrayList<RGraph> runSLIC() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //Run algorithm for SLIC assembly
        _assemblyGraphs.clear();
        RSLIC slic = new RSLIC();
        ArrayList<RGraph> optimalGraphs = slic.slicClothoWrapper(_goalParts,_required,_recommended,_forbidden,_discouraged,_partLibrary,_efficiency,null);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for MoClo *
     */
    public ArrayList<RGraph> runMoClo() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //Run algorithm for MoClo assembly
        _assemblyGraphs.clear();
        RMoClo moclo = new RMoClo();
        moclo.setForcedOverhangs(_collector,_forcedOverhangHash);
        ArrayList<RGraph> optimalGraphs = moclo.mocloClothoWrapper(_goalParts,_vectorLibrary,_required,_recommended,_forbidden,_discouraged,_partLibrary,false,_efficiency,null);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for Golden Gate *
     */
    public ArrayList<RGraph> runGoldenGate() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //Run algorithm for Golden Gate assembly
        _assemblyGraphs.clear();
        RGoldenGate gg = new RGoldenGate();
        ArrayList<RGraph> optimalGraphs = gg.goldenGateClothoWrapper(_goalParts,_vectorLibrary,_required,_recommended,_forbidden,_discouraged,_partLibrary,_efficiency,null);
        return optimalGraphs;
    }

    /**
     * Parse an input Raven file *
     */
    public void parseRavenFile(File input) throws Exception {
        ArrayList<String> badLines = new ArrayList();
        ArrayList<String[]> compositePartTokens = new ArrayList<String[]>();
        if (_forcedOverhangHash == null) {
            _forcedOverhangHash = new HashMap<String,ArrayList<String>>();
        }
        HashSet<String> seenPartNames = new HashSet();
        BufferedReader reader = new BufferedReader(new FileReader(input.getAbsolutePath()));
        String line = reader.readLine();
        line = reader.readLine(); //skip first line

        //Read each line of the input file to parse parts
        while (line != null) {
            while (line.matches("^[\\s,]+")) {
                line = reader.readLine();
            }
            String[] tokens = line.split(",");
            int tokenCount = tokens.length; //keeps track of how many columns are filled by counting backwards
            for (int i = tokens.length - 1; i > -1; i--) {
                if (tokens[i].trim().matches("[\\s]*")) {
                    tokenCount--;
                } else {
                    break;
                }
            }

            //Composite parts - read,but do not generate
            if (tokenCount > 9) {

                try {
                    String[] trimmedTokens = new String[tokenCount];
                    System.arraycopy(tokens,0,trimmedTokens,0,tokenCount);
                    compositePartTokens.add(trimmedTokens);
                } catch (Exception e) {
                    badLines.add(line);
                }

                //Vectors - read and generate new vector
            } else if (tokenCount == 7 || tokenCount == 6) {

                try {
                    String name = tokens[0].trim();
                    String sequence = tokens[1].trim();
                    String leftOverhang = tokens[2].trim();
                    String rightOverhang = tokens[3].trim();
                    String resistance = tokens[5].toLowerCase().trim();
                    int level;
                    if (tokens.length == 7) {
                        try {
                            level = Integer.parseInt(tokens[6]);
                        } catch (NumberFormatException e) {
                            level = -1;
                        }
                    } else {
                        level = -1;
                    }

                    Vector newVector = Vector.generateVector(name,sequence);
                    newVector.addSearchTag("LO: " + leftOverhang);
                    newVector.addSearchTag("RO: " + rightOverhang);
                    newVector.addSearchTag("Level: " + level);
                    newVector.addSearchTag("Resistance: " + resistance);
                    newVector.setTransientStatus(false);
                    Vector toBreak = newVector.saveDefault(_collector);
                    //save vector with no overhangs juse in case;
                    if (toBreak == null) {
                        break;
                    }
                } catch (Exception e) {
                    badLines.add(line);
                }

                //Basic part - read and generate new part
            } else if (tokenCount == 5) {

                try {
                    String name = tokens[0].trim();
                    String sequence = tokens[1].trim();
                    String leftOverhang = tokens[2].trim();
                    String rightOverhang = tokens[3].trim();
                    String type = tokens[4].trim();
                    Part newBasicPart = Part.generateBasic(name,sequence);
                    newBasicPart.addSearchTag("LO: " + leftOverhang);
                    newBasicPart.addSearchTag("RO: " + rightOverhang);
                    newBasicPart.addSearchTag("Type: " + type);

                    Part toBreak = newBasicPart.saveDefault(_collector);
                    newBasicPart.setTransientStatus(false);
                    //save part with no scars or overhangs juse in case;
                    if (leftOverhang.length() > 0 && rightOverhang.length() > 0 && !seenPartNames.contains(name)) {
                        Part blankBasicPart = Part.generateBasic(name,sequence);
                        blankBasicPart.addSearchTag("Type: " + type);
                        blankBasicPart.saveDefault(_collector);
                        blankBasicPart.setTransientStatus(false);
                        seenPartNames.add(name);
                    }
                    if (toBreak == null) {
                        break;
                    }
                } catch (Exception e) {
                    badLines.add(line);
                }

                //Basic parts with direction and overhangs
            } else if (tokenCount == 9) {

                try {
                    String name = tokens[0].trim();
                    String sequence = tokens[1].trim();
                    String leftOverhang = tokens[2].trim();
                    String rightOverhang = tokens[3].trim();
                    String type = tokens[4].trim();
                    String vectorName = tokens[7].trim();
                    String composition = tokens[8].trim();
                    Part newBasicPart = Part.generateBasic(name,sequence);
                    newBasicPart.addSearchTag("LO: " + leftOverhang);
                    newBasicPart.addSearchTag("RO: " + rightOverhang);
                    newBasicPart.addSearchTag("Direction: [" + composition.substring(composition.length() - 1) + "]");
                    newBasicPart.addSearchTag("Type: " + type);
                    Vector vector = null;
                    ArrayList<Vector> allVectorsWithName = _collector.getAllVectorsWithName(vectorName,true);
                    if (!allVectorsWithName.isEmpty()) {
                        //TODO do i need an exact match?
                        vector = allVectorsWithName.get(0);
                    }
                    _compPartsVectors.put(newBasicPart,vector);
                    Part toBreak = newBasicPart.saveDefault(_collector);
                    newBasicPart.setTransientStatus(false);
                    if (toBreak == null) {
                        break;
                    }
                } catch (Exception e) {
                    badLines.add(line);
                }

            } else {
                //poorly formed line
                badLines.add(line);

            }
            line = reader.readLine();
        }
        reader.close();

        //Create the composite parts
        for (String[] tokens : compositePartTokens) {
            try {
                ArrayList<Part> composition = new ArrayList<Part>();

                //For all of the basic parts in the composite part composition
                String name = tokens[0].trim();
                String leftOverhang = tokens[2].trim();
                String rightOverhang = tokens[3].trim();
                String vectorName = tokens[7].trim();
                ArrayList<String> directions = new ArrayList<String>();

                //Parse composition tokens
                for (int i = 8; i < tokens.length; i++) {
                    String basicPartString = tokens[i].trim();
                    String[] partNameTokens = basicPartString.split("\\|");
                    String bpForcedLeft = " ";
                    String bpForcedRight = " ";
                    String bpDirection = "+";
                    String compositePartName = tokens[0];
                    String basicPartName = partNameTokens[0];

                    //Check for forced overhangs and direction
                    if (partNameTokens.length > 1) {
                        if (partNameTokens.length == 2) {
                            if ("+".equals(partNameTokens[1]) || "-".equals(partNameTokens[1])) {
                                bpDirection = partNameTokens[1];
                            }
                        } else if (partNameTokens.length == 3) {
                            bpForcedLeft = partNameTokens[1];
                            bpForcedRight = partNameTokens[2];
                        } else if (partNameTokens.length == 4) {
                            bpForcedLeft = partNameTokens[1];
                            bpForcedRight = partNameTokens[2];
                            bpDirection = partNameTokens[3];
                        }
                    }
                    if (_forcedOverhangHash.get(compositePartName) != null) {
                        _forcedOverhangHash.get(compositePartName).add(bpForcedLeft + "|" + bpForcedRight);
                    } else {
                        ArrayList<String> toAdd = new ArrayList();
                        toAdd.add(bpForcedLeft + "|" + bpForcedRight);
                        _forcedOverhangHash.put(compositePartName,toAdd);
                    }

                    directions.add(bpDirection);
                    composition.add(_collector.getAllPartsWithName(basicPartName,true).get(0));
                }

                Part newComposite = Part.generateComposite(composition,name);
                Vector vector = null;
                ArrayList<Vector> vectors = _collector.getAllVectorsWithName(vectorName,true);
                if (vectors.size() > 0) {
                    for (Vector vec : vectors) {
                        if (vec.getLeftoverhang().equals(leftOverhang) && vec.getRightOverhang().equals(rightOverhang)) {
                            vector = vec;
                        }
                    }
                }
                _compPartsVectors.put(newComposite,vector);
                newComposite.addSearchTag("Direction: " + directions);
                newComposite.addSearchTag("LO: " + leftOverhang);
                newComposite.addSearchTag("RO: " + rightOverhang);
                newComposite.addSearchTag("Type: composite");
                newComposite = newComposite.saveDefault(_collector);
                newComposite.setTransientStatus(false);
            } catch (NullPointerException e) {
                String badLine = "";

                for (int j = 0; j < tokens.length; j++) {
                    badLine = badLine + tokens[j] + ",";
                }
                badLines.add(badLine.substring(0,badLine.length() - 1));//trim the last comma
            }
        }

        //Print warning about bad line
        if (badLines.size() > 0) {

            String badLineMessage = "The following lines in your csv input was malformed. \nPlease check you input spreadsheet.";

            for (String bl : badLines) {
                badLineMessage = badLineMessage + "\n" + bl;
            }
            throw new Exception(badLineMessage);
        }
    }

    //traverse the graph and return a boolean indicating whether or not hte graph is valid in terms of composition
    private boolean validateGraphComposition() throws Exception {
        boolean toReturn = true;
        HashSet<String> seenRequired = new HashSet();
        for (RGraph graph : _assemblyGraphs) {
            ArrayList<RNode> queue = new ArrayList();
            HashSet<RNode> seenNodes = new HashSet();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                //handle directionality
                ArrayList<String> direction = current.getDirection();
                ArrayList<String> composition = current.getComposition();
                String currentCompositionString = composition.toString();
                if (direction.size() == composition.size()) {
                    currentCompositionString = "";
                    for (int i = 0; i < composition.size(); i++) {
                        currentCompositionString = currentCompositionString + composition.get(i) + "|" + direction.get(i) + ",";
                    }
                }
                currentCompositionString = "[" + currentCompositionString.substring(0,currentCompositionString.length() - 2) + "]";
                if (_forbidden.contains(currentCompositionString)) {
                    toReturn = false;
                    break;
                }
                if (_required.contains(currentCompositionString)) {
                    seenRequired.add(currentCompositionString);
                }
                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            if (toReturn == false) {
                break;
            }
        }

        if (toReturn && _required.size() == seenRequired.size()) {
            return true;
        } else {
            return false;
        }

    }
    //set searchPartitions to true to sample mgp space
    //set searchPartitions to true to sample overhang space

    public void run(String method,int targetSize,int numberOfRuns,boolean searchPartitions,boolean searchOverhangs) throws Exception {
        RavenController.samplePartitions = searchPartitions;
        RavenController.sampleOverhangs = searchOverhangs;
        _goalParts = new HashMap();
        _required = new HashSet();
        _recommended = new HashSet();
        _forbidden = new HashSet();
        _discouraged = new HashSet();
        _vectorLibrary = new ArrayList<Vector>();
        _partLibrary = new ArrayList<Part>();
        _assemblyGraphs = new ArrayList<RGraph>();
        _efficiency = new HashMap();
        _efficiency.put(1,1.0);
        _efficiency.put(2,1.0);
        _efficiency.put(3,1.0);
        _efficiency.put(4,1.0);

        method = method.toLowerCase().trim();
        ArrayList<Part> allParts = _collector.getAllParts(true);
        ArrayList<Part> allCompositeParts = new ArrayList();
        for(Part p:allParts) {
            if(p.isComposite()) {
                allCompositeParts.add(p);
            }
        }
        HashSet<Part> goal = new HashSet(allCompositeParts.subList(0,Math.min(allCompositeParts.size(),targetSize)));
        for (Part p : goal) {
            if (p.isComposite()) {
                _goalParts.put(p,null);
            }
        }
        System.out.println("Steps,Stages,Sharing,PCRs,numberOfDevices: "+targetSize +",numberOfRuns: "+numberOfRuns+",searchPartitions: "+searchPartitions+",searchOverhangs: "+searchOverhangs);
        for (int i = 0; i < numberOfRuns; i++) {
            boolean scarless = false;
            _assemblyGraphs.clear();
            if (method.equals("biobricks")) {
                _assemblyGraphs = runBioBricks();
            } else if (method.equals("cpec")) {
                _assemblyGraphs = runCPEC();
                scarless = true;
            } else if (method.equals("gibson")) {
                _assemblyGraphs = runGibson();
                scarless = true;
            } else if (method.equals("goldengate")) {
                _assemblyGraphs = runGoldenGate();
                scarless = true;
            } else if (method.equals("moclo")) {
                _assemblyGraphs = runMoClo();
            } else if (method.equals("slic")) {
                _assemblyGraphs = runSLIC();
                scarless = true;
            }
            RGraph.getGraphStats(_assemblyGraphs,_partLibrary,_vectorLibrary,_goalParts,_recommended,_discouraged,scarless,0.0,0.0,0.0,0.0);
            getSolutionStats();
            
            if(error) {
//            ArrayList<String> graphTextFiles = new ArrayList();
//            ArrayList<RNode> targetRootNodes = new ArrayList();
//            if (!_assemblyGraphs.isEmpty()) {
//                for (RGraph result : _assemblyGraphs) {
//                    targetRootNodes.add(result.getRootNode());
//                }
//            }
//            ArrayList<RGraph> mergeGraphs = RGraph.mergeGraphs(_assemblyGraphs);
//            if (!_assemblyGraphs.isEmpty()) {
//                for (RGraph result : _assemblyGraphs) {
//                    graphTextFiles.add(result.generateWeyekinFile(_partLibrary,_vectorLibrary,targetRootNodes,scarless));
//                }
//            }
//            String mergedGraphText = RGraph.mergeWeyekinFiles(graphTextFiles);
//            WeyekinPoster.setDotText(mergedGraphText);
//            WeyekinPoster.postMyVision();
//
//            String imageURL = "";
//            imageURL = WeyekinPoster.getmGraphVizURI().toString();
//            System.out.println(imageURL);
            }
            error = false;
        }


    }

    private void getSolutionStats() throws Exception {

        int steps = 0;
        int stages = 0;
        int recCnt = 0;
        int disCnt = 0;
        int shr = 0;
        int rxn = 0;
        ArrayList<Double> effArray = new ArrayList<Double>();
        double eff = 0;

        if (!_assemblyGraphs.isEmpty()) {

            for (RGraph graph : _assemblyGraphs) {
                if (graph.getStages() > stages) {
                    stages = graph.getStages();
                }
                steps = steps + graph.getSteps();
                recCnt = recCnt + graph.getReccomendedCount();
                disCnt = disCnt + graph.getDiscouragedCount();
                shr = shr + graph.getSharing();
                rxn = rxn + graph.getReaction();
                effArray.addAll(graph.getEfficiencyArray());
            }
            double sum = 0;

            for (Double anEff : effArray) {
                sum = sum + anEff;
            }
            eff = sum / effArray.size();
        }
        System.out.println(steps + "," + stages + "," + shr + "," + rxn);
    }
    //FIELDS
    private HashMap<Part,Vector> _goalParts = new HashMap<Part,Vector>();//key: target part,value: composition
    private HashMap<Part,Vector> _compPartsVectors = new HashMap<Part,Vector>();
    private HashMap<Integer,Double> _efficiency = new HashMap<Integer,Double>();
    private HashSet<String> _required = new HashSet<String>();
    private HashSet<String> _recommended = new HashSet<String>();
    private HashSet<String> _discouraged = new HashSet<String>();
    private HashSet<String> _forbidden = new HashSet<String>();
    private ArrayList<RGraph> _assemblyGraphs = new ArrayList<RGraph>();
    private HashMap<String,ArrayList<String>> _forcedOverhangHash = new HashMap<String,ArrayList<String>>();
    private ArrayList<Part> _partLibrary = new ArrayList<Part>();
    private ArrayList<Vector> _vectorLibrary = new ArrayList<Vector>();
    protected Collector _collector = new Collector(); //key:user,value: collector assocaited with that user
}
