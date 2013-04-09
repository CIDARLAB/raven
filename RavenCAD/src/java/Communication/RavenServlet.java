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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Admin
 */
public class RavenServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String command = request.getParameter("command");
        if (command.equals("dataStatus")) {
            try {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "";
                responseString = getDataStatus();
                out.write(responseString);
            } finally {
                out.close();
            }
        } else if (command.equals("load")) {
            try {
                response.setContentType("text/html;charset=UTF-8");
                String responseString = "loaded data";
                loadData();
                out.write(responseString);
            } finally {
                out.close();
            }
        } else if (command.equals("fetch")) {
            try {
                response.setContentType("application/json");
                String responseString = "";
                responseString = fetchData();
                out.write(responseString);
            } finally {
                out.close();
            }
        } else if (command.equals("run")) {
            try {
                response.setContentType("application/json");
                //run(String method, HashMap<Part, ArrayList<Part>> goalParts, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden)
                String[] targetIDs = request.getParameter("targets").split(",");
                String[] partLibraryIDs = request.getParameter("partLibrary").split(",");
                String[] vectorLibraryIDs = request.getParameter("vectorLibrary").split(",");
                String[] recArray = request.getParameter("recommended").split(";");
                String[] reqArray = request.getParameter("required").split(";");
                String[] forbiddenArray = request.getParameter("forbidden").split(";");
                String method = request.getParameter("method");
                HashMap<Part, ArrayList<Part>> goalParts = new HashMap();
                HashSet<String> required = new HashSet();
                HashSet<String> recommended = new HashSet();
                HashSet<String> forbidden = new HashSet();
                HashSet<Vector> vectorLibrary = new HashSet();
                ArrayList<Part> partLibrary = new ArrayList();
                if (partLibraryIDs.length > 0) {
                    for (int i = 0; i < partLibraryIDs.length; i++) {
                        Part current = Collector.getPart(partLibraryIDs[i]);
                        if (current != null) {
                            partLibrary.add(current);
                        }
                    }
                }
                if (vectorLibraryIDs.length > 0) {
                    for (int i = 0; i < vectorLibraryIDs.length; i++) {
                        Vector current = Collector.getVector(vectorLibraryIDs[i]);
                        if (current != null) {
                            vectorLibrary.add(current);
                        }
                    }
                }
                for (int i = 0; i < targetIDs.length; i++) {
                    Part current = Collector.getPart(targetIDs[i]);
                    goalParts.put(current, ClothoReader.getComposition(current));
                }
                if (recArray.length > 0) {
                    for (int i = 0; i < recArray.length; i++) {
                        if (recArray[i].length() > 0) {
                            recommended.add(recArray[i]);
                        }
                    }
                }
                if (reqArray.length > 0) {
                    for (int i = 0; i < reqArray.length; i++) {
                        if (reqArray[i].length() > 0) {
                            required.add(reqArray[i]);
                        }
                    }
                }
                if (forbiddenArray.length > 0) {
                    for (int i = 0; i < forbiddenArray.length; i++) {
                        if (forbiddenArray[i].length() > 0) {
                            forbidden.add(forbiddenArray[i]);
                        }
                    }
                }
                String image = run(method, goalParts, required, recommended, forbidden, vectorLibrary, partLibrary);
                String statString = "assembly stats fromt the server!";
                String instructions = "instructions from the server!";
//                lblStats.setText("Statistics:");
//                lblStages.setText(_statistics.getStages());
//                lblSteps.setText(_statistics.getSteps());
//                lblSharing.setText(_statistics.getSharing());
//                lblEfficiency.setText(_statistics.getEfficiency());
//                lblGoalParts.setText(_statistics.getGoalParts());
//                lblRecommended.setText(_statistics.getRecommended());
//                lblModularity.setText(_statistics.getModularity());
//                lblExecutionTime.setText(_statistics.getExecutionTime());
//                lblReactions.setText(_statistics.getReactions());
                out.println("{\"image\":\""+image+"\",\"statistics\":\""+statString+"\",\"instructions\":\""+instructions+"\"}");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                out.close();
            }
        }
    }

    /**
     * @param request servlet request
     * @param response servlet response
     * @
     * throws ServletException if a servlet -specific error occurs
     * @
     * throws IOException if an I /O error occurs
     *
     */
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processGetRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processPostRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    //returns "loaded" or "not loaded" depending on whether there are objects in the collector
    private String getDataStatus() {
        String toReturn = "not loaded";
        if (Collector.getAllParts().size() > 0 || Collector.getAllVectors().size() > 0) {
            toReturn = "loaded";
        }
        return toReturn;
    }

    //parses all csv files stored in ravencache directory, and then adds parts and vectors to Collecor
    private void loadData() {
        Collector.purge();//TODO remove this, for testing purposes only
        String uploadFilePath = "C:\\Users\\Admin\\Desktop\\ravencache"; //TODO change this to a folder in the web directory matching UploadServlet
        File[] filesInDirectory = new File(uploadFilePath).listFiles();
        for (File currentFile : filesInDirectory) {
            String filePath = currentFile.getAbsolutePath();
            String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length()).toLowerCase();
            if ("csv".equals(fileExtension)) {
                parseInputFile(currentFile);
            }
        }
    }

    private void parseInputFile(File input) {
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
                        String name = tokens[0];
                        String sequence = tokens[1];
                        String leftOverhang = tokens[2];
                        String rightOverhang = tokens[3];
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
                        Boolean toBreak = !newVector.saveDefault();
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
                        String name = tokens[0];
                        String sequence = tokens[1];
                        String leftOverhang = tokens[2];
                        String rightOverhang = tokens[3];
                        String type = tokens[4];
                        Part newBasicPart = Part.generateBasic(name, sequence);
                        newBasicPart.addSearchTag("LO: " + leftOverhang);
                        newBasicPart.addSearchTag("RO: " + rightOverhang);
                        newBasicPart.addSearchTag("Type: " + type);
                        Boolean toBreak = !newBasicPart.saveDefault();
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
                        composition.add(Collector.getPartByName(partName));
                    }
                    String name = tokens[0];
                    String leftOverhang = tokens[2];
                    String rightOverhang = tokens[3];

                    Part newComposite = Part.generateComposite(composition, name);
                    newComposite.addSearchTag("LO: " + leftOverhang);
                    newComposite.addSearchTag("RO: " + rightOverhang);
                    newComposite.addSearchTag("Type: composite");
                    newComposite.saveDefault();
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

    private String fetchData() {
        String toReturn = "[";
        ArrayList<Part> allParts = Collector.getAllParts();
        for (Part p : allParts) {
            toReturn = toReturn + "{\"uuid\":\"" + p.getUUID() + "\",\"Name\":\"" + p.getName() + "\",\"Sequence\":\"" + p.getSeq() + "\",\"LO\":\"" + p.getLeftoverhang() + "\",\"RO\":\"" + p.getRightOverhang() + "\",\"Type\":\"" + p.getType() + "\",\"Composition\":\"" + p.getStringComposition() + "\",\"Resistance\":\"\",\"Level\":\"\"},";
        }

        ArrayList<Vector> allVectors = Collector.getAllVectors();
        for (Vector v : allVectors) {
            toReturn = toReturn + "{\"uuid\":\"" + v.getUUID() + "\",\"Name\":\"" + v.getName() + "\",\"Sequence\":\"" + v.getSeq() + "\",\"LO\":\"" + v.getLeftoverhang() + "\",\"RO\":\"" + v.getRightOverhang() + "\",\"Type\":\"vector\",\"Composition\":\"\"" + ",\"Resistance\":\"" + v.getResistance() + "\",\"Level\":\"" + v.getLevel() + "\"},";
        }
        toReturn = toReturn.subSequence(0, toReturn.length() - 1) + "]";
        return toReturn;
    }
//private String run() {

    private String run(String method, HashMap<Part, ArrayList<Part>> goalParts, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<Vector> vectorLibrary, ArrayList<Part> partLibrary) {
        _goalParts = goalParts;
        _required = required;
        _recommended = recommended;
        _forbidden = forbidden;
        _statistics = new Statistics();
        _vectorLibrary = vectorLibrary;
        _partLibrary = partLibrary;
        _assemblyGraphs = new ArrayList<SRSGraph>();
        method = method.toLowerCase().trim();
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

    /**
     * ************************************************************************
     *
     * RUN ASSEMBLY METHODS
     *
     *************************************************************************
     */
    /**
     * Run Binary SRS algorithm *
     */
    private void runBioBricks() {

        //Run algorithm for BioBricks assembly
        _assemblyGraphs.clear();
        ArrayList<Part> gps = new ArrayList();
        gps.addAll(_goalParts.keySet());
        SRSBioBricks biobricks = new SRSBioBricks();
        Statistics.start();
        ArrayList<SRSGraph> optimalGraphs = biobricks.bioBricksClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _partLibrary, false);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
    }

    /**
     * Run SRS algorithm for Gibson *
     */
    private void runGibson() {

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
        ArrayList<SRSGraph> optimalGraphs = gibson.gibsonClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
        gps = null;
    }

    /**
     * Run SRS algorithm for CPEC *
     */
    private void runCPEC() {

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
        ArrayList<SRSGraph> optimalGraphs = cpec.cpecClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
    }

    /**
     * Run SRS algorithm for SLIC *
     */
    private void runSLIC() {

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
        ArrayList<SRSGraph> optimalGraphs = slic.slicClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(postOrderEdges, canPigeon));
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
    private void runMoClo() {
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
        moclo.setForcedOverhangs(forcedOverhangHash);
        ArrayList<SRSGraph> optimalGraphs = moclo.mocloClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _partLibrary, false, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();

    }

    /**
     * Run SRS algorithm for Golden Gate *
     */
    private void runGoldenGate() {

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
        ArrayList<SRSGraph> optimalGraphs = gg.goldenGateClothoWrapper(gps, _vectorLibrary, _required, _recommended, _forbidden, _partLibrary, true, efficiencies);
        Statistics.stop();
        solutionStats(optimalGraphs);
        ClothoReader reader = new ClothoReader();
        ArrayList<String> graphTextFiles = new ArrayList();
        for (SRSGraph result : optimalGraphs) {
            try {
                reader.nodesToClothoPartsVectors(result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            boolean canPigeon = result.canPigeon();
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            graphTextFiles.add(result.generateWeyekinFile(postOrderEdges, canPigeon));
        }
        String mergedGraphText = SRSGraph.mergeWeyekinFiles(graphTextFiles);
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
        //Clean up data
        gps = null;
    }

    /**
     * Traverse a solution graph for statistics *
     */
    private void solutionStats(ArrayList<SRSGraph> optimalGraphs) {

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
    //FIELDS
    private HashMap<Part, ArrayList<Part>> _goalParts;//key: target part, value: composition
    private HashSet<String> _required;
    private HashSet<String> _recommended;
    private HashSet<String> _forbidden;
    private Statistics _statistics = new Statistics();
    private ArrayList<SRSGraph> _assemblyGraphs = new ArrayList<SRSGraph>();
    private HashMap<String, ArrayList<String>> forcedOverhangHash;
    private ArrayList<Part> _partLibrary;
    private HashSet<Vector> _vectorLibrary;
}
