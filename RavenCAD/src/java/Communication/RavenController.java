/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.modasm.SRSGoldenGate;
import Controller.algorithms.modasm.SRSMoClo;
import Controller.algorithms.nonmodasm.SRSBioBricks;
import Controller.algorithms.nonmodasm.SRSCPEC;
import Controller.algorithms.nonmodasm.SRSGibson;
import Controller.algorithms.nonmodasm.SRSSLIC;
import Controller.datastructures.Collector;
import Controller.datastructures.Part;
import Controller.datastructures.SRSGraph;
import Controller.datastructures.SRSNode;
import Controller.datastructures.Vector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Jenhan
 */
public class RavenController {

    public RavenController(String path, String user) {
        _path = path;
        _user = user;
        System.out.println("user: " + _user);
        System.out.println("path: " + _path);
    }

    public void runBioBricks() throws Exception {

        //Run algorithm for BioBricks assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSBioBricks biobricks = new SRSBioBricks();
        Statistics.start();
        ArrayList<SRSGraph> optimalGraphs = biobricks.bioBricksClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, false);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(_collector, result);
            } catch (Exception ex) {
                throw ex;
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(_collector, postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
    }

    /**
     * Run SRS algorithm for Gibson *
     */
    public void runGibson() throws Exception {

        //Run algorithm for Gibson assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSGibson gibson = new SRSGibson();

        HashMap<Integer, Double> efficiencies = new HashMap<Integer, Double>();
        efficiencies.put(2, 1.0);
        efficiencies.put(3, 1.0);
        efficiencies.put(4, 1.0);
        efficiencies.put(5, 1.0);
        efficiencies.put(6, 1.0);
        efficiencies.put(7, 1.0);
        efficiencies.put(8, 1.0);
        efficiencies.put(9, 1.0);
        efficiencies.put(10, 1.0);

        Statistics.start();
        ArrayList<SRSGraph> optimalGraphs = gibson.gibsonClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(_collector, result);
            } catch (Exception ex) {
                throw ex;
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(_collector, postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
        gps = null;
    }

    /**
     * Run SRS algorithm for CPEC *
     */
    public void runCPEC() throws Exception {

        //Run algorithm for CPEC assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSCPEC cpec = new SRSCPEC();

        HashMap<Integer, Double> efficiencies = new HashMap<Integer, Double>();
        efficiencies.put(2, 1.0);
        efficiencies.put(3, 1.0);
        efficiencies.put(4, 1.0);

        Statistics.start();
        ArrayList<SRSGraph> optimalGraphs = cpec.cpecClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(_collector, result);
            } catch (Exception ex) {
                throw ex;
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(_collector, postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
    }

    /**
     * Run SRS algorithm for SLIC *
     */
    public void runSLIC() throws Exception {

        //Run algorithm for SLIC assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSSLIC slic = new SRSSLIC();

        HashMap<Integer, Double> efficiencies = new HashMap<Integer, Double>();
        efficiencies.put(2, 1.0);
        efficiencies.put(3, 1.0);
        efficiencies.put(4, 1.0);

        Statistics.start();
        ArrayList<SRSGraph> optimalGraphs = slic.slicClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            reader.nodesToClothoPartsVectors(_collector, result);
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(_collector, postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
        //Clean up data
        gps = null;
    }

    /**
     * Run SRS algorithm for MoClo *
     */
    public void runMoClo() throws Exception {
        if (_goalParts == null) {
            return;
        }
        //Run algorithm for MoClo assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSMoClo moclo = new SRSMoClo();

        HashMap<Integer, Double> efficiencies = new HashMap<Integer, Double>();
        efficiencies.put(2, 1.0);
        efficiencies.put(3, 1.0);
        efficiencies.put(4, 1.0);
        efficiencies.put(5, 1.0);
        efficiencies.put(6, 1.0);

        Statistics.start();
        moclo.setForcedOverhangs(_collector, forcedOverhangHash);
        ArrayList<SRSGraph> optimalGraphs = moclo.mocloClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            reader.nodesToClothoPartsVectors(_collector, result);
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(_collector, postOrderEdges, canPigeon));
        }
        _instructions = "";
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();

    }

    /**
     * Run SRS algorithm for Golden Gate *
     */
    public void runGoldenGate() throws Exception {

        //  Run algorithm for Golden Gate assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSGoldenGate gg = new SRSGoldenGate();

        HashMap<Integer, Double> efficiencies = new HashMap<Integer, Double>();
        efficiencies.put(2, 1.0);
        efficiencies.put(3, 1.0);
        efficiencies.put(4, 1.0);
        efficiencies.put(5, 1.0);
        efficiencies.put(6, 1.0);

        Statistics.start();
        ArrayList<SRSGraph> optimalGraphs = gg.goldenGateClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, true, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            reader.nodesToClothoPartsVectors(_collector, result);
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(_collector, postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
        //Clean up /data
        gps = null;
    }

    public boolean generatePartsListFile(String designNumber) throws Exception {
        File file = new File(_path + _user + "/partsList" + designNumber + ".csv");
        //traverse graphs to get uuids
        ArrayList<Part> usedPartsHash = new ArrayList<Part>();
        ArrayList<Vector> usedVectorsHash = new ArrayList<Vector>();
        for (SRSGraph result : _assemblyGraphs) {
            for (Part p : result.getPartsInGraph(_collector)) {
                if (!usedPartsHash.contains(p)) {
                    usedPartsHash.add(p);
                }
            }
            for (Vector v : result.getVectorsInGraph(_collector)) {
                if (!usedVectorsHash.contains(v)) {
                    usedVectorsHash.add(v);
                }
            }
        }
        //extract information from parts and write file
        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        out.write("Name,Sequence,Left Overhang,Right Overhang,Type,Resistance,Level,Composition");

        for (Part p : usedPartsHash) {
            ArrayList<String> tags = p.getSearchTags();
            String RO = "";
            String LO = "";
            String type = "";
            for (int k = 0; k < tags.size(); k++) {
                if (tags.get(k).startsWith("LO:")) {
                    LO = tags.get(k).substring(4);
                } else if (tags.get(k).startsWith("RO:")) {
                    RO = tags.get(k).substring(4);
                } else if (tags.get(k).startsWith("Type:")) {
                    type = tags.get(k).substring(6);
                }
            }

            if (p.isBasic()) {
                out.write("\n" + p.getName() + "," + p.getSeq() + "," + LO + "," + RO + "," + type);
            } else {
                String composition = "";
                type = "composite";
                for (Part subpart : p.getComposition()) {
                    composition = composition + "," + subpart.getName();
                }
                out.write("\n" + p.getName() + "," + p.getSeq() + "," + LO + "," + RO + "," + type + ",," + composition);
            }
        }

        for (Vector v : usedVectorsHash) {
            ArrayList<String> tags = v.getSearchTags();
            String RO = "";
            String LO = "";
            String level = "";
            String resistance = "";
            for (int k = 0; k < tags.size(); k++) {
                if (tags.get(k).startsWith("LO:")) {
                    LO = tags.get(k).substring(4);
                } else if (tags.get(k).startsWith("RO:")) {
                    RO = tags.get(k).substring(4);
                } else if (tags.get(k).startsWith("Level:")) {
                    level = tags.get(k).substring(7);
                } else if (tags.get(k).startsWith("Resistance:")) {
                    resistance = tags.get(k).substring(12);
                }
            }
            out.write("\n" + v.getName() + "," + v.getSeq() + "," + LO + "," + RO + ",vector," + resistance + "," + level);
        }
        out.close();
        return true;
    }

    public String generateInstructionsFile(String designNumber) throws Exception {
        File file = new File(_path + _user + "/instructions" + designNumber + ".txt");
        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        out.write(_instructions);
        out.close();
        return _instructions;
    }

    public void clearData() throws Exception {
        _collector.purge();
        String uploadFilePath = _path + _user + "/";
        File[] filesInDirectory = new File(uploadFilePath).listFiles();
        for (File currentFile : filesInDirectory) {
            currentFile.delete();
        }
    }

    public String fetchData() throws Exception {
        String toReturn = "[";
        ArrayList<Part> allParts = _collector.getAllParts();
        for (Part p : allParts) {
            toReturn = toReturn + "{\"uuid\":\"" + p.getUUID() + "\",\"Name\":\"" + p.getName() + "\",\"Sequence\":\"" + p.getSeq() + "\",\"LO\":\"" + p.getLeftoverhang() + "\",\"RO\":\"" + p.getRightOverhang() + "\",\"Type\":\"" + p.getType() + "\",\"Composition\":\"" + p.getStringComposition() + "\",\"Resistance\":\"\",\"Level\":\"\"},";
        }

        ArrayList<Vector> allVectors = _collector.getAllVectors();
        for (Vector v : allVectors) {
            toReturn = toReturn + "{\"uuid\":\"" + v.getUUID() + "\",\"Name\":\"" + v.getName() + "\",\"Sequence\":\"" + v.getSeq() + "\",\"LO\":\"" + v.getLeftoverhang() + "\",\"RO\":\"" + v.getRightOverhang() + "\",\"Type\":\"vector\",\"Composition\":\"\"" + ",\"Resistance\":\"" + v.getResistance() + "\",\"Level\":\"" + v.getLevel() + "\"},";
        }
        toReturn = toReturn.subSequence(0, toReturn.length() - 1) + "]";
        return toReturn;
    }

    //parses all csv files stored in ravencache directory, and then adds parts and vectors to Collecor
    public void loadData() throws Exception {
        String uploadFilePath = _path + _user + "/";
        File[] filesInDirectory = new File(uploadFilePath).listFiles();
        for (File currentFile : filesInDirectory) {
            String filePath = currentFile.getAbsolutePath();
            String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length()).toLowerCase();
            if ("csv".equals(fileExtension)) {
                parseInputFile(currentFile);
            }
        }
    }

    private void parseInputFile(File input) throws Exception {
        ArrayList<String> badLines = new ArrayList();
        ArrayList<String[]> compositePartTokens = new ArrayList<String[]>();
        if (forcedOverhangHash == null) {
            forcedOverhangHash = new HashMap();
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input.getAbsolutePath()));
            String line = reader.readLine();
            line = reader.readLine(); //skip first line
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
                if (tokenCount > 7) {
                    // store line for making composite part
                    try {
                        String[] trimmedTokens = new String[tokenCount];
                        System.arraycopy(tokens, 0, trimmedTokens, 0, tokenCount);
                        compositePartTokens.add(trimmedTokens);
                    } catch (Exception e) {
                        e.printStackTrace();
                        badLines.add(line);
                    }
                } else if (tokenCount == 7) {
                    //create vector
                    try {
                        String name = tokens[0].trim();
                        String sequence = tokens[1].trim();
                        String leftOverhang = tokens[2].trim();
                        String rightOverhang = tokens[3].trim();
                        String resistance = tokens[5].toLowerCase().trim();
                        int level = -1;
                        try {
                            level = Integer.parseInt(tokens[6]);
                        } catch (NumberFormatException e) {
                            level = -1;
                        }
                        Vector newVector = Vector.generateVector(name, sequence);
//                            System.out.println("creating vector: " + name + " resistance: " + resistance + " LO: " + leftOverhang + " RO: " + rightOverhang + " level: " + String.valueOf(level) + " seq: " + sequence);
                        newVector.addSearchTag("LO: " + leftOverhang);
                        newVector.addSearchTag("RO: " + rightOverhang);
                        newVector.addSearchTag("Level: " + level);
                        newVector.addSearchTag("Resistance: " + resistance);
                        Boolean toBreak = !newVector.saveDefault(_collector);
                        if (toBreak) {
                            break;
                        }
                    } catch (Exception e) {
                        badLines.add(line);
                        e.printStackTrace();
                    }
                } else if (tokenCount == 5) {
                    try {
                        //create basic part 
                        String name = tokens[0].trim();
                        String sequence = tokens[1].trim();
                        String leftOverhang = tokens[2].trim();
                        String rightOverhang = tokens[3].trim();
                        String type = tokens[4].trim();
                        Part newBasicPart = Part.generateBasic(name, sequence);
                        newBasicPart.addSearchTag("LO: " + leftOverhang);
                        newBasicPart.addSearchTag("RO: " + rightOverhang);
                        newBasicPart.addSearchTag("Type: " + type);
                        Boolean toBreak = !newBasicPart.saveDefault(_collector);
                        if (toBreak) {
                            break;
                        }
                    } catch (Exception e) {
                        badLines.add(line);
                        e.printStackTrace();
                    }
                } else {
                    //poorly formed line
                    badLines.add(line);

                }
                line = reader.readLine();
            }
            reader.close();
            //create the composite parts
            for (String[] tokens : compositePartTokens) {
                try {
                    ArrayList<Part> composition = new ArrayList<Part>();
                    for (int i = 7; i < tokens.length; i++) {
                        String partName = tokens[i].trim();
                        if (partName.contains("|")) {
                            String[] partNameTokens = partName.split("\\|");
                            if (forcedOverhangHash.get(tokens[0]) != null) {
                                forcedOverhangHash.get(tokens[0]).add((i - 7) + "|" + partNameTokens[1] + "|" + partNameTokens[2]);
                            } else {
                                ArrayList<String> toAdd = new ArrayList();
                                toAdd.add((i - 7) + "|" + partNameTokens[1] + "|" + partNameTokens[2]);
                                forcedOverhangHash.put(tokens[0], toAdd);
                            }
                            partName = partNameTokens[0];
                        }
                        composition.add(_collector.getPartByName(partName));
                    }
                    String name = tokens[0].trim();
                    String leftOverhang = tokens[2].trim();
                    String rightOverhang = tokens[3].trim();

                    Part newComposite = Part.generateComposite(composition, name);
                    newComposite.addSearchTag("LO: " + leftOverhang);
                    newComposite.addSearchTag("RO: " + rightOverhang);
                    newComposite.addSearchTag("Type: composite");
                    newComposite.saveDefault(_collector);
                } catch (NullPointerException e) {
                    String badLine = "";
                    for (int j = 0; j < tokens.length; j++) {
                        badLine = badLine + tokens[j] + ",";
                    }
                    badLines.add(badLine.substring(0, badLine.length() - 1));//trim the last period
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        if (badLines.size() > 0) {
            //print warning about bad line
            String badLineMessage = "The following lines in your csv input was malformed. \nPlease check you input spreadsheet.";
            for (String bl : badLines) {
                badLineMessage = badLineMessage + "\n" + bl;
            }
            System.out.println(badLineMessage);

        }
    }

    //returns "loaded" or "not loaded" depending on whether there are objects in the collector
    public String getDataStatus() throws Exception {
        String toReturn = "not loaded";
        if (_collector.getAllParts().size() > 0 || _collector.getAllVectors().size() > 0) {
            toReturn = "loaded";
        }
        return toReturn;
    }

    /**
     * Traverse a solution graph for statistics *
     */
    public void solutionStats(ArrayList<SRSGraph> optimalGraphs) throws Exception {

        //Initialize statistics
        HashSet<String> recd = new HashSet<String>();
//        HashSet<String> reqd = new HashSet<String>();
        HashSet<String> steps = new HashSet<String>();
        HashSet<String> sharing = new HashSet<String>();
        int stages = 0;
        int reactions = 0;
        double modularity = 0;
        double efficiency = 0;
        ArrayList<Double> effArray = new ArrayList<Double>();

        for (SRSGraph graph : optimalGraphs) {
            _assemblyGraphs.add(graph);
            reactions = reactions + graph.getReaction();
            //Get stages of this graph, if largest, set as assembly stages
            int currentStages = graph.getStages();
            if (currentStages > stages) {
                stages = currentStages;
            }

            //Tabulate efficiency and modularity
            modularity = modularity + graph.getModularity();
            effArray.addAll(graph.getEfficiency());

            //Traverse nodes to get scores for steps, recommended and sharing
            HashSet<SRSNode> seenNodes = new HashSet();
            seenNodes.add(graph.getRootNode());
            ArrayList<SRSNode> queue = new ArrayList();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                SRSNode current = queue.get(0);
                queue.remove(0);

                if (current.getComposition().size() > 1) {
                    if (steps.contains(current.getComposition().toString())) {
                        sharing.add(current.getComposition().toString());
                    }
                    if (!current.getNeighbors().isEmpty()) {
                        steps.add(current.getComposition().toString());
                    }
                }
//                if (_required.contains(current.getPartComposition().toString())) {
//                    reqd.add(current.getPartComposition().toString());
//                }
                if (_recommended.contains(current.getComposition().toString())) {
                    recd.add(current.getComposition().toString());
                }
                for (SRSNode node : current.getNeighbors()) {
                    if (!seenNodes.contains(node)) {
                        seenNodes.add(node);
                        queue.add(node);
                    }
                }
            }

            //Get the average efficiency of all steps in this assembly
            double sum = 0;
            for (int i = 0; i < effArray.size(); i++) {
                sum = sum + effArray.get(i);
            }
            efficiency = sum / effArray.size();

            //Warn if no steps or stages are required to build part - i.e. it already exists in a library
            if (steps.isEmpty()) {
                System.out.println("Warning! All goal part(s) already exist! No assembly required");
            }

        }
        if (reactions == 0) {
            reactions = steps.size() * 2;
        }
        modularity = modularity / optimalGraphs.size();
        //Statistics determined
        _statistics.setModularity(modularity);
        _statistics.setEfficiency(efficiency);
        _statistics.setRecommended(recd.size());
        _statistics.setStages(stages);
        _statistics.setSteps(steps.size());
        _statistics.setSharing(sharing.size());
        _statistics.setGoalParts(optimalGraphs.size());
        _statistics.setExecutionTime(Statistics.getTime());
        _statistics.setReaction(reactions);
    }

    public String run(String method, String[] targetIDs, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, String[] vectorLibraryIDs, String[] partLibraryIDs) throws Exception {
        _goalParts = new HashMap();
        _required = required;
        _recommended = recommended;
        _forbidden = forbidden;
        _discouraged = discouraged;
        _statistics = new Statistics();
        _vectorLibrary = new ArrayList();
        _partLibrary = new ArrayList();
        _assemblyGraphs = new ArrayList<SRSGraph>();
        method = method.toLowerCase().trim();
        if (partLibraryIDs.length > 0) {
            for (int i = 0; i < partLibraryIDs.length; i++) {
                Part current = _collector.getPart(partLibraryIDs[i]);
                if (current != null) {
                    _partLibrary.add(current);
                }
            }
        }
        if (vectorLibraryIDs.length > 0) {
            for (int i = 0; i < vectorLibraryIDs.length; i++) {
                Vector current = _collector.getVector(vectorLibraryIDs[i]);
                if (current != null) {
                    _vectorLibrary.add(current);
                }
            }
        }
        for (int i = 0; i < targetIDs.length; i++) {
            Part current = _collector.getPart(targetIDs[i]);
            _goalParts.put(current, ClothoReader.getComposition(current));
        }
        if (method.equals("biobrick")) {
            runBioBricks();
        } else if (method.equals("cpec")) {
            runCPEC();
        } else if (method.equals("gibson")) {
            runGibson();
        } else if (method.equals("golden gate")) {
            runGoldenGate();
        } else if (method.equals("moclo")) {
            runMoClo();
        } else if (method.equals("slic")) {
            runSLIC();
        }
        String toReturn = "";
        try {
            toReturn = WeyekinPoster.getmGraphVizURI().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public String generateStats() throws Exception {
        String statString = "{\"goalParts\":\"" + _statistics.getGoalParts()
                + "\",\"steps\":\"" + _statistics.getSteps()
                + "\",\"stages\":\"" + _statistics.getStages()
                + "\",\"reactions\":\"" + _statistics.getReactions()
                + "\",\"recommended\":\"" + _statistics.getRecommended()
                + "\",\"efficiency\":\"" + _statistics.getEfficiency()
                + "\",\"modularity\":\"" + _statistics.getModularity()
                + "\",\"time\":\"" + _statistics.getExecutionTime() + "\"}";
        return statString;
    }
    public HashMap<Part, ArrayList<Part>> _goalParts = new HashMap();//key: target part, value: composition
    public HashSet<String> _required = new HashSet();
    public HashSet<String> _recommended = new HashSet();
    public HashSet<String> _discouraged = new HashSet();
    public HashSet<String> _forbidden = new HashSet();
    public Statistics _statistics = new Statistics();
    public ArrayList<SRSGraph> _assemblyGraphs = new ArrayList<SRSGraph>();
    public HashMap<String, ArrayList<String>> forcedOverhangHash = new HashMap();
    public ArrayList<Part> _partLibrary = new ArrayList();
    public ArrayList<Vector> _vectorLibrary = new ArrayList();
    public String _instructions = "";
    public Collector _collector = new Collector(); //key:user, value: collector assocaited with that user
    public String _path;
    public String _user;
}