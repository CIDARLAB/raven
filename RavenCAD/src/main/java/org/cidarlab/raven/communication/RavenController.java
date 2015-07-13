/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.communication;

import org.cidarlab.raven.algorithms.core.*;
import org.cidarlab.raven.algorithms.modasm.*;
import org.cidarlab.raven.algorithms.nonmodasm.*;
import org.cidarlab.raven.datastructures.*;
import org.cidarlab.raven.accessibility.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Jenhan
 */
public class RavenController {

    public RavenController(String path, String user) {
        _path = path;
        _user = user;
        File file = new File(_path + _user);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        //temporary default values
        _databaseConfig.add("jdbc:mysql://128.197.164.27");
        _databaseConfig.add("Puppeteerv0 ");
        _databaseConfig.add("cidar.rwdu");
        _databaseConfig.add("cidar");
    }
    
    public RavenController(String filePath) {
        _path = "raven";
        _user = "raven";
    }

    /*
     * Check stage vectors for assembly method, add stage vectors if there aren't any
     */
    public HashMap<Integer, Vector> checkStageVectors(HashMap<Integer, Vector> stageVectors, Collector collector, String method) {
        
        if (stageVectors.get(0) == null) {

            Vector st0Vec = _collector.getExactVector("pSB1A2", _pSB1A2, false, "", "", "vector", "", "", "ampicilin", -1);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSB1A2", _pSB1A2, "", "", "vector", "", "", "ampicilin", -1);
                collector.addVector(st0Vec);
            }
            stageVectors.put(0, st0Vec); 
            
            if (method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("goldengate") || method.equalsIgnoreCase("gatewaygibson")) {

                Vector st1Vec = collector.getExactVector("pSB1K3", _pSB1K3, false, "", "", "vector", "", "", "kanamycin", -1);
                if (st1Vec == null) {
                    st1Vec = Vector.generateVector("pSB1K3", _pSB1K3, "", "", "vector", "", "", "kanamycin", -1);
                    collector.addVector(st1Vec);
                }
                stageVectors.put(1, st1Vec);
            }       
        }
        return stageVectors;
    }

    //returns json array containing all objects in parts list; generates parts list file
    //input: design number refers to the design number on the client
    public JSONArray generatePartsList(String designNumber, String params, String method) throws Exception {
        File partsListFile = new File(_path + _user + "/partsList" + designNumber + ".csv");
        File configFile = new File(_path + _user + "/config" + designNumber + ".csv");

        //traverse graphs to get uuids
        ArrayList<Part> usedParts = new ArrayList<Part>();
        ArrayList<Vector> usedVectors = new ArrayList<Vector>();
        HashMap<Part, Vector> partVectorHash = new HashMap();
        ArrayList<Part> existingLibraryParts = _collector.getAllParts(false);

        //Remove anything that is not a basic part without overhangs or a plasmid in the library
        ArrayList<Part> toRemove = new ArrayList<Part>();
        for (Part libPart : existingLibraryParts) {
            if (!(libPart.getType()).contains("plasmid")) {
                if (!libPart.getLeftOverhang().isEmpty() || !libPart.getRightOverhang().isEmpty()) {
                    toRemove.add(libPart);
                } else if ((libPart.getType()).contains("composite")) {
                    toRemove.add(libPart);
                }
            } else {
                if (!_libraryPartsVectors.containsKey(libPart)) {
                    toRemove.add(libPart);
                }
            }
        }
        existingLibraryParts.removeAll(toRemove);

        ArrayList<Vector> existingLibraryVectors = _collector.getAllVectors(false);

        for (RGraph result : _assemblyGraphs) {
            HashMap<Part, Vector> partVectorsInGraph = result.getPartVectorsInGraph(_collector);
            partVectorHash.putAll(partVectorsInGraph);
            for (Part p : partVectorsInGraph.keySet()) {
                if (!usedParts.contains(p)) {
                    if (!existingLibraryParts.contains(p)) {
                        usedParts.add(p);
                    } else if (existingLibraryParts.contains(p) && !_libraryPartsVectors.containsKey(p) && p.getType().equals("plasmid")) {
                        usedParts.add(p);
                    }
                }
            }
            for (Vector v : partVectorsInGraph.values()) {
                if (!usedVectors.contains(v)) {
                    if (!existingLibraryVectors.contains(v)) {
                        usedVectors.add(v);
                    }
                }
            }
        }

        //extract information from parts and write file
        String partList = "[";
        FileWriter partsListFileWriter = new FileWriter(partsListFile);
        BufferedWriter partsListBufferedWriter = new BufferedWriter(partsListFileWriter);
        partsListBufferedWriter.write("Library,Name,Sequence,Left Overhang,Right Overhang,Type,Resistance,Level,Vector,Composition");

        FileWriter configFileWriter = new FileWriter(configFile);
        BufferedWriter configBufferedWriter = new BufferedWriter(configFileWriter);
        configBufferedWriter.write("Library,Name,Sequence,Left Overhang,Right Overhang,Type,Resistance,Level,Vector,Composition");

        //Add existing parts to output files        
        for (Part libPart : existingLibraryParts) {
            String composition = "";
            String vectorName = "";
            ArrayList<String> direction = libPart.getDirections();

            //For basic parts
            if (libPart.isBasic()) {
                if (!libPart.getLeftOverhang().isEmpty() && !libPart.getRightOverhang().isEmpty()) {
                    composition = libPart.getComposition().get(0).getName() + "|" + libPart.getLeftOverhang() + "|" + libPart.getRightOverhang() + "|" + direction.get(0).trim();
                } else {
                    composition = libPart.getComposition().get(0).getName() + "|" + direction.get(0).trim();
                }
                Vector v = _libraryPartsVectors.get(libPart);
                if (v != null) {
                    vectorName = v.getName();
                }

                if (!"-".equals(direction.get(0))) {
                    partsListBufferedWriter.write("\nx," + libPart.getName() + "," + libPart.getSeq() + "," + libPart.getLeftOverhang() + "," + libPart.getRightOverhang() + "," + libPart.getType() + ",,," + vectorName + "," + composition);
                    configBufferedWriter.write("\nx," + libPart.getName() + "," + libPart.getSeq() + "," + libPart.getLeftOverhang() + "," + libPart.getRightOverhang() + "," + libPart.getType() + ",,," + vectorName + "," + composition);
                }
            } else {

                //For composite parts
                Vector v = _libraryPartsVectors.get(libPart);
                if (v != null) {
                    vectorName = v.getName();
                }

                for (int i = 0; i < libPart.getComposition().size(); i++) {
                    Part subpart = libPart.getComposition().get(i);
                    if (!subpart.getLeftOverhang().isEmpty() && !subpart.getRightOverhang().isEmpty()) {
                        composition = composition + ", " + subpart.getName() + "|" + subpart.getLeftOverhang() + "|" + subpart.getRightOverhang() + "|" + direction.get(i).trim();
                    } else {
                        composition = composition + ", " + subpart.getName() + "|" + direction.get(i).trim();
                    }
                }

                composition = composition.substring(2);
                partsListBufferedWriter.write("\nx," + libPart.getName() + "," + libPart.getSeq() + "," + libPart.getLeftOverhang() + "," + libPart.getRightOverhang() + "," + libPart.getType() + ",,," + vectorName + "," + composition);
                configBufferedWriter.write("\nx," + libPart.getName() + "," + libPart.getSeq() + "," + libPart.getLeftOverhang() + "," + libPart.getRightOverhang() + "," + libPart.getType() + ",,," + vectorName + "," + composition);
            }
        }

        //Add exiting vectors to output also
        for (Vector libVec : existingLibraryVectors) {

            String RO = libVec.getRightOverhang();
            String LO = libVec.getLeftOverhang();
            String level = "";
            if (libVec.getLevel() != -1) {
                Integer.toString(libVec.getLevel());
            }
            String type = libVec.getType();
            String resistance = libVec.getResistance();

            partsListBufferedWriter.write("\nx," + libVec.getName() + "," + libVec.getSeq() + "," + LO + "," + RO + "," + type + "," + resistance + "," + level + "," + libVec.getVector() + "," + libVec.getComposition());
            configBufferedWriter.write("\nx," + libVec.getName() + "," + libVec.getSeq() + "," + LO + "," + RO + "," + type + "," + resistance + "," + level + "," + libVec.getVector() + "," + libVec.getComposition());
        }

        //Add new parts in graph to the parts list
        for (Part p : usedParts) {

            String RO = p.getRightOverhang();
            String LO = p.getLeftOverhang();
            ArrayList<String> type = p.getType();
            ArrayList<String> direction = p.getDirections();
            String composition = "";
            String vectorName = "";

            if (p.isBasic()) {
                
                //Edge case for merged parts
                String bpDirection;
                if (type.size() > 1) {
                    type.clear();
                    type.add("multitype");
                    bpDirection = "+";
                } else {
                    bpDirection = direction.get(0).trim();
                }
                
                if (!p.getLeftOverhang().isEmpty() && !p.getRightOverhang().isEmpty()) {
                    composition = p.getName() + "|" + p.getLeftOverhang() + "|" + p.getRightOverhang() + "|" + bpDirection;
                } else {
                    composition = p.getName() + "|" + bpDirection;
                }
                Vector v = partVectorHash.get(p);
                if (v != null) {
                    vectorName = v.getName();
                }
                
                partsListBufferedWriter.write("\nx," + p.getName() + "," + p.getSeq() + "," + LO + "," + RO + "," + type + ",,," + vectorName + "," + composition);
                configBufferedWriter.write("\nx," + p.getName() + "," + p.getSeq() + "," + LO + "," + RO + "," + type + ",,," + vectorName + "," + composition);
            } else {
                
                type.clear();
                type.add("plasmid");
                Vector v = partVectorHash.get(p);
                if (v != null) {
                    vectorName = v.getName();
                }

                for (int i = 0; i < p.getComposition().size(); i++) {
                    Part subpart = p.getComposition().get(i);
                    String cRO = subpart.getRightOverhang();
                    String cLO = subpart.getLeftOverhang();

                    //Edge case with new composite part from a PCR of existing composite part
                    if (cLO.isEmpty()) {
                        if (i == 0) {
                            cLO = p.getLeftOverhang();
                        } else {
                            cLO = p.getComposition().get(i - 1).getRightOverhang();
                        }
                    }
                    if (cRO.isEmpty()) {
                        if (i == p.getComposition().size() - 1) {
                            cRO = p.getRightOverhang();
                        } else {
                            cRO = p.getComposition().get(i + 1).getLeftOverhang();
                        }
                    }

                    if (method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("biobricks")) {
                        if (!cLO.isEmpty() && !cRO.isEmpty()) {
                            composition = composition + ", " + subpart.getName() + "|" + cLO + "|" + cRO + "|" + direction.get(i).trim();
                        } else {
                            composition = composition + ", " + subpart.getName() + "|" + direction.get(i).trim();
                        }
                    } else {
                        composition = composition + ", " + subpart.getName() + "|" + direction.get(i).trim();
                    }
                }

                composition = composition.substring(2);
                partsListBufferedWriter.write("\nx," + p.getName() + "," + p.getSeq() + "," + LO + "," + RO + "," + type + ",,," + vectorName + "," + composition);
                configBufferedWriter.write("\nx," + p.getName() + "," + p.getSeq() + "," + LO + "," + RO + "," + type + ",,," + vectorName + "," + composition);
            }
            partList = partList
                    + "{\"uuid\":\"" + p.getUUID()
                    + "\",\"Name\":\"" + p.getName()
                    + "\",\"Sequence\":\"" + p.getSeq()
                    + "\",\"LO\":\"" + p.getLeftOverhang()
                    + "\",\"RO\":\"" + p.getRightOverhang()
                    + "\",\"Type\":\"" + p.getType()
                    + "\",\"Vector\":\"" + vectorName
                    + "\",\"Composition\":\"" + composition
                    + "\",\"Resistance\":\"\",\"Level\":\"\"},";
        }

        for (Vector v : usedVectors) {
            if (v != null) {

                String RO = v.getRightOverhang();
                String LO = v.getLeftOverhang();
                String level = Integer.toString(v.getLevel());
                String type = v.getType();
                String resistance = v.getResistance();

                partsListBufferedWriter.write("\nx," + v.getName() + "," + v.getSeq() + "," + LO + "," + RO + "," + type + "," + resistance + "," + level + "," + v.getVector() + "," + v.getComposition());
                configBufferedWriter.write("\nx," + v.getName() + "," + v.getSeq() + "," + LO + "," + RO + "," + type + "," + resistance + "," + level + "," + v.getVector() + "," + v.getComposition());
                partList = partList + "{\"uuid\":\"" + v.getUUID()
                        + "\",\"Name\":\"" + v.getName()
                        + "\",\"Sequence\":\"" + v.getSeq()
                        + "\",\"LO\":\"" + v.getLeftOverhang()
                        + "\",\"RO\":\"" + v.getRightOverhang()
                        + "\",\"Type\":\"" + v.getType()
                        + "\",\"Composition\":\"" + v.getComposition()
                        + "\",\"Vector\":\"" + v.getVector()
                        + "\",\"Resistance\":\"" + v.getResistance()
                        + "\",\"Level\":\"" + v.getLevel() + "\"},";
            }
        }
        // add run parameters to config file
        configBufferedWriter.write("\n####\n");
        configBufferedWriter.write(params);

        configBufferedWriter.close();
        partsListBufferedWriter.close();
        if (partList.length() > 1) {
            partList = partList.substring(0, partList.length() - 1);
        }
        partList = partList + "]";
        return new JSONArray(partList);
    }

    //reset collector, all field variales, deletes all files in user's directory
    public void clearData() throws Exception {
        _collector.purge();
        _statistics = new Statistics();
        _assemblyGraphs = new ArrayList<RGraph>();
//        _forcedOverhangHash = new HashMap<String, ArrayList<String>>();
        _libraryOHHash = new HashMap<String, String>();
        _partLibrary = new ArrayList<Part>();
        _vectorLibrary = new ArrayList<Vector>();
        _instructions = "";
        _error = "";

        String uploadFilePath = _path + _user + "/";
        File[] filesInDirectory = new File(uploadFilePath).listFiles();
        for (File currentFile : filesInDirectory) {
            currentFile.delete();
        }
    }

    //returns all saved parts in the collector as a json array
    public String fetchData(boolean scanRestrictionSites) throws Exception {
        String toReturn = "[";
        ArrayList<Part> allParts = _collector.getAllParts(false);
        for (Part p : allParts) {
            if (!p.isTransient()) {

                //Get the composition's overhangs and directions
                ArrayList<Part> composition = p.getComposition();
                ArrayList<String> compositions = new ArrayList<String>();
                String bpDir = new String();
                String bpLO = p.getLeftOverhang();
                String bpRO = p.getRightOverhang();
                ArrayList<String> type = p.getType();
                ArrayList<String> direction = p.getDirections();

                //Displaying plasmids in data upload tab
                if (composition.size() > 1) {
                    for (int i = 0; i < composition.size(); i++) {
                        Part part = composition.get(i);

                        if (!direction.isEmpty()) {
                            bpDir = direction.get(i);
                        }

                        String aPartComp;

                        if (!bpDir.isEmpty()) {
                            if (!bpLO.isEmpty() || !bpRO.isEmpty()) {
                                aPartComp = part.getName() + "|" + bpLO + "|" + bpRO + "|" + bpDir;
                            } else {
                                aPartComp = part.getName() + "|" + bpDir;
                            }
                        } else {
                            if (!bpLO.isEmpty() || !bpRO.isEmpty()) {
                                aPartComp = part.getName() + "|" + bpLO + "|" + bpRO;
                            } else {
                                aPartComp = part.getName();
                            }
                        }
                        compositions.add(aPartComp);
                    }

                    //Displaying basic parts in data upload tab
                } else {

                    Part part = composition.get(0);

                    if (!direction.isEmpty()) {
                        bpDir = direction.get(0);
                    }

                    String aPartComp;

                    if (!bpDir.isEmpty()) {
                        if (!bpLO.isEmpty() || !bpRO.isEmpty()) {
                            aPartComp = part.getName() + "|" + bpLO + "|" + bpRO + "|" + bpDir;
                        } else {
                            aPartComp = part.getName() + "|" + bpDir;
                        }
                    } else {
                        if (!bpLO.isEmpty() || !bpRO.isEmpty()) {
                            aPartComp = part.getName() + "|" + bpLO + "|" + bpRO;
                        } else {
                            aPartComp = part.getName();
                        }
                    }

                    compositions.add(aPartComp);

                }
                String vectorName = "";
                Vector v = _libraryPartsVectors.get(p);
                if (v != null) {
                    vectorName = v.getName();
                }

                toReturn = toReturn
                        + "{\"uuid\":\"" + p.getUUID()
                        + "\",\"Name\":\"" + p.getName()
                        + "\",\"Sequence\":\"" + p.getSeq()
                        + "\",\"LO\":\"" + p.getLeftOverhang()
                        + "\",\"RO\":\"" + p.getRightOverhang()
                        + "\",\"Type\":\"" + type
                        + "\",\"Vector\":\"" + vectorName
                        + "\",\"Composition\":\"" + compositions
                        + "\",\"Resistance\":\"\",\"Level\":\"\"},";
            }
        }

        ArrayList<Vector> allVectors = _collector.getAllVectors(false);
        for (Vector v : allVectors) {

            //Return blank string for level if it is -1 in the code
            int level = v.getLevel();
            String strLevel = Integer.toString(level);
            if (level == -1) {
                strLevel = "";
            }

            toReturn = toReturn + "{\"uuid\":\"" + v.getUUID()
                    + "\",\"Name\":\"" + v.getName()
                    + "\",\"Sequence\":\"" + v.getSeq()
                    + "\",\"LO\":\"" + v.getLeftOverhang()
                    + "\",\"RO\":\"" + v.getRightOverhang()
                    + "\",\"Type\":\"" + v.getType()
                    + "\",\"Vector\":\"" + v.getVector()
                    + "\",\"Composition\":\"" + v.getComposition()
                    + "\",\"Resistance\":\"" + v.getResistance()
                    + "\",\"Level\":\"" + strLevel + "\"},";
        }
        toReturn = toReturn.subSequence(0, toReturn.length() - 1) + "]";

        String restrictionScanMessage = "";
        boolean appendScanMessage = false;
        if (scanRestrictionSites) {
            restrictionScanMessage = "<strong>Warning! The following parts contain restriction sites!<br/></strong><hr/>";
            HashMap<Part, HashMap<String, ArrayList<int[]>>> reSeqScan = RestrictionEnzyme.reSeqScan(allParts, _restrictionEnzymes);
            HashMap<String, Part> namePartHash = new HashMap();
            ArrayList<String> sortedPartNames = new ArrayList();
            for (Part p : reSeqScan.keySet()) {
                sortedPartNames.add(p.getName());
                namePartHash.put(p.getName(), p);
            }

            for (String partName : sortedPartNames) {
                HashMap<String, ArrayList<int[]>> enzymeSites = reSeqScan.get(namePartHash.get(partName));
                ArrayList<String> sortedEnzymes = new ArrayList(enzymeSites.keySet());
                if (sortedEnzymes.size() > 0) {
                    restrictionScanMessage = restrictionScanMessage + partName + "<ul>";
                    appendScanMessage = true;
                    Collections.sort(sortedEnzymes);
                    for (String enzyme : sortedEnzymes) {
                        ArrayList<int[]> foundIndices = enzymeSites.get(enzyme);
                        if (foundIndices.size() > 0) {
                            restrictionScanMessage = restrictionScanMessage + "<li>" + enzyme + "<ul>";

                            for (int[] indices : foundIndices) {
                                restrictionScanMessage = restrictionScanMessage + "<li>Start: " + indices[0] + " end: " + indices[1] + "</li>";

                            }
                            restrictionScanMessage = restrictionScanMessage + "</ul></li>";
                        }
                    }
                    restrictionScanMessage = restrictionScanMessage + "</ul><hr/>";
                }
            }
        }
        if (!appendScanMessage) {
            restrictionScanMessage = "";
        }
        if (_preloadedParams != null) {
            toReturn = toReturn + ",\"params\":" + _preloadedParams;
//            _preloadedParams = null;
        } else {
            toReturn = toReturn + ",\"params\":\"none\"";
        }
        if (_error.length() > 0) {
            _error = _error.replaceAll("[\r\n\t]+", "<br/>");
            toReturn = "{\"result\":" + toReturn + ",\"status\":\"bad\",\"message\":\"" + _error + "\"}";
        } else {
            toReturn = "{\"result\":" + toReturn + ",\"status\":\"good\",\"message\":\"" + restrictionScanMessage + "\"}";
        }
        return toReturn;
    }

    //handles multiple file upload from the client
    //parses each file for parts and saves them
    public void loadUploadedFiles(ArrayList<File> filesToRead) {
        _error = "";
//        String uploadFilePath = _path + _user + "/";
//        File[] filesInDirectory = new File(uploadFilePath).listFiles();
        try {
            if (filesToRead != null) {
                for (File currentFile : filesToRead) {
                    String filePath = currentFile.getAbsolutePath();
                    String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length()).toLowerCase();
                    if ("csv".equals(fileExtension) || "txt".equals(fileExtension)) {
                        parseRavenFile(currentFile);
                    }
                }
            }
        } catch (Exception e) {
            String exceptionAsString = e.getMessage().replaceAll("[\r\n\t]+", "<br/>");
            _error = exceptionAsString;
        }
    }

    //parses a newly generated parts list from a new design and saves the parts in that parts list
    public void saveNewDesign(String designCount) throws Exception {
        _error = "";
        String filePath = _path + _user + "/partsList" + designCount + ".csv";
        File toLoad = new File(filePath);
        parseRavenFile(toLoad);
    }

    /**
     * Parse an input Raven file *
     */
    private void parseRavenFile(File input) throws Exception {
        
        if (_vectorLibrary == null) {
            _vectorLibrary = new ArrayList();
        }
        if (_partLibrary == null) {
            _partLibrary = new ArrayList();
        }
        if (_libraryOHHash == null) {
            _libraryOHHash = new HashMap();
        }
        
        ArrayList<String> badLines = new ArrayList();
        ArrayList<String[]> compositePartTokens = new ArrayList<String[]>();
        
        BufferedReader reader = new BufferedReader(new FileReader(input.getAbsolutePath()));
        String line = reader.readLine();
        line = reader.readLine(); //skip first line

        //Read each line of the input file to parse parts
        while (line != null) {
            while (line.matches("^[\\s,]+")) {
                line = reader.readLine();
            }
            if (line.contains("####")) {
                //reached end of parts list
                //break and read in params
                break;
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

            String type = tokens[5].trim();
            ArrayList<String> typeL = new ArrayList();
            typeL.add(type);

            //Composite parts - read, but do not generate
            if (type.equalsIgnoreCase("plasmid")) {

                try {
                    String[] trimmedTokens = new String[tokenCount];
                    System.arraycopy(tokens, 0, trimmedTokens, 0, tokenCount);

                    //Add plasmids with basic parts to top of plasmid token list
                    if (trimmedTokens.length == 10) {
                        compositePartTokens.add(0, trimmedTokens);
                    } else {
                        compositePartTokens.add(trimmedTokens);
                    }
                } catch (Exception e) {
                    badLines.add(line);
                }

            //Vectors - read and generate new vector
            } else if (type.equalsIgnoreCase("vector")) {

                try {
                    String name = tokens[1].trim();
                    String sequence = tokens[2].trim().replaceAll(" ","");
                    String leftOverhang = tokens[3].trim();
                    String rightOverhang = tokens[4].trim();
                    String resistance = "";
                    if (tokens.length > 6) {
                        resistance = tokens[6].toLowerCase().trim();
                    }

                    Vector newVector = Vector.generateVector(name, sequence, leftOverhang, rightOverhang, type, "", "", resistance, -1);

                    _vectorLibrary.add(newVector);
                    newVector.saveDefault(_collector);
                    newVector.setTransientStatus(false);

                } catch (Exception e) {
                    badLines.add(line);
                }

            //Destinaiton Vectors - read and generate new vector if in library
            } else if (type.equalsIgnoreCase("destination vector")) {

                try {
                    if (!tokens[0].trim().isEmpty()) {
                        String sequence = tokens[2].trim().replaceAll(" ","");
                        String leftOverhang = tokens[3].trim();
                        String rightOverhang = tokens[4].trim();
                        String resistance = tokens[6].toLowerCase().trim();
                        String name = tokens[8].trim();
                        String composition = tokens[9];

                        int level;
                        try {
                            level = Integer.parseInt(tokens[7]);
                        } catch (NumberFormatException e) {
                            level = -1;
                        }

                        Vector newVector = Vector.generateVector(name, sequence, leftOverhang, rightOverhang, type, name, composition, resistance, level);

                        _vectorLibrary.add(newVector);
                        newVector.saveDefault(_collector);
                        newVector.setTransientStatus(false);
                    }

                } catch (Exception e) {
                    badLines.add(line);
                }

            //If there is no type field, the row is malformed
            } else if (type.isEmpty()) {

                //poorly formed line
                badLines.add(line);

            //Basic part - read and generate new part
            } else {

                try {
                    String name = tokens[1].trim();
                    String sequence = tokens[2].trim().replaceAll(" ","").replaceAll("\n", "");
                    ArrayList<String> directions = new ArrayList();
                    directions.add("+");
                    ArrayList<String> rDirections = new ArrayList();
                    rDirections.add("-");
                    
                    Part newBasicPart = Part.generateBasic(name, sequence, null, typeL, directions, "", "");                    
                    Part newReverseBasicPart = Part.generateBasic(name, PrimerDesign.reverseComplement(sequence), null, typeL, rDirections, "", "");

                    //Library logic
                    newBasicPart = newBasicPart.saveDefault(_collector);
                    _partLibrary.add(newBasicPart);                    
                    newBasicPart.setTransientStatus(false);
                    
                    newReverseBasicPart = newReverseBasicPart.saveDefault(_collector);
                    _partLibrary.add(newReverseBasicPart);
                    newReverseBasicPart.setTransientStatus(false);

                } catch (Exception e) {
                    badLines.add(line);
                }
            }
            line = reader.readLine();
        }
        _preloadedParams = reader.readLine();

        reader.close();

        //Create the composite parts and plasmids
        for (String[] tokens : compositePartTokens) {
            try {
                ArrayList<Part> composition = new ArrayList();

                //For all of the basic parts in the composite part composition
                String name = tokens[1].trim();
                String leftOverhang = tokens[3].trim();
                String rightOverhang = tokens[4].trim();
                String vectorName = tokens[8].trim();
                ArrayList<String> directions = new ArrayList();
                ArrayList<String> scars = new ArrayList();
                ArrayList<String> linkers = new ArrayList();
                boolean previousIsLinker = false;

                //Parse composition tokens
                for (int i = 9; i < tokens.length; i++) {
                    String basicPartString = tokens[i].trim();
                    String[] partNameTokens = basicPartString.split("\\|");
                    String bpForcedLeft = " ";
                    String bpForcedRight = " ";
                    String bpDirection = "+";
                    String scar = "_";
                    String linker = "_";
                    boolean isLinker = false;                    
                    String basicPartName = partNameTokens[0];                    
                    
                    //Scar upload
                    if (partNameTokens.length > 1) {
                        if (partNameTokens.length == 2) {
                            if ("+".equals(partNameTokens[1]) || "-".equals(partNameTokens[1])) {
                                bpDirection = partNameTokens[1];
                            }
                        } else if (partNameTokens.length == 3) {
                            bpForcedLeft = partNameTokens[1];
                            bpForcedRight = partNameTokens[2];
                            
                            //Scar upload for BBricks and MoClo/GatewayGibson is differnt
                            if ("EX".equals(bpForcedLeft)) {
                                scar = "BB";
                            } else {
                                scar = bpForcedLeft;
                            }
                        } else if (partNameTokens.length == 4) {
                            bpForcedLeft = partNameTokens[1];
                            bpForcedRight = partNameTokens[2];
                            bpDirection = partNameTokens[3];
                            
                            //Scar upload for BBricks and MoClo/GatewayGibson is differnt
                            if ("EX".equals(bpForcedLeft)) {
                                scar = "BB";
                            } else {
                                scar = bpForcedLeft;
                            }
                        }
                    }
                    ArrayList<String> bpDirections = new ArrayList();
                    bpDirections.add(bpDirection);

                    //Fusion coding sequences 
                    //In this case, it gets added to the part composition, but will not get converted into a node
                    if (basicPartName.startsWith("(") && basicPartName.endsWith(")")) {     
                        
                        //Consecutive linkers edge case
                        if (!linkers.get(linkers.size()-1).equals("_")) {
                            linker = linkers.get(linkers.size()-1) + "|" + basicPartName.substring(1, basicPartName.length() - 1); 
                            linkers.remove(linkers.size()-1);
                        } else {
                            linker = basicPartName.substring(1, basicPartName.length() - 1);
                        }
                        
                        if (i > 9) {
                            isLinker = true;
                            previousIsLinker = true;
                            linkers.add(linker);
                        }

                    } else {
                        if (i > 9) {
                            if (!previousIsLinker) {
                                    linkers.add(linker);
                            } else {
                                previousIsLinker = false;
                            }
                        }
                    }
                    
                    //Basic part plasmids - add as new basic parts with overhang for re-use
                    if (tokens.length == 10) {
                        if (i == 9) {

                            //Multiplexed parts
                            if (basicPartName.contains("?")) {
                                
                                String type = basicPartName.replaceAll("\\?", "") + "_multiplex";
                                ArrayList<String> typeL = new ArrayList();
                                typeL.add(type);
                                Part newBasicPart = Part.generateBasic(basicPartName, "", null, typeL, bpDirections, leftOverhang, rightOverhang);
                                
                                _partLibrary.add(newBasicPart);
                                newBasicPart.saveDefault(_collector);
                                newBasicPart.setTransientStatus(false);
        
                            } else {

                                //Fusion coding sequences 
                                //This should would be an accident by the user, but would be interpretted as just a basic part
                                if (basicPartName.startsWith("(") && basicPartName.endsWith("(")) {
                                    basicPartName = basicPartName.substring(1, basicPartName.length() - 1);
                                }
                                
                                Part basic = null;
                                ArrayList<Part> allPartsWithName = _collector.getAllPartsWithName(basicPartName, false);
                                for (Part aPart : allPartsWithName) {
                                    if (!aPart.getType().contains("plasmid") && bpDirections.equals(aPart.getDirections())) {
                                        basic = aPart;
                                    }
                                }

                                //Assumed that a basic part already exists, so this possible null pointer is on purpose
                                String sequence = basic.getSeq().replaceAll(" ", "");
                                ArrayList<String> type = basic.getType();
                                Part newBasicPart = Part.generateBasic(basicPartName, sequence, null, type, bpDirections, leftOverhang, rightOverhang);

                                //Library logic
                                if (!tokens[0].trim().isEmpty()) {
                                    _partLibrary.add(newBasicPart);
                                    newBasicPart.saveDefault(_collector);
                                    newBasicPart.setTransientStatus(false);
                                }
                            }
                        }
                    } else {
                        
                        //Multiplexed parts
                        if (basicPartName.contains("?")) {

                            String type = basicPartName.replaceAll("\\?", "") + "_multiplex";
                            ArrayList<String> typeL = new ArrayList();
                            typeL.add(type);
                            Part newBasicPart = Part.generateBasic(basicPartName, "", null, typeL, bpDirections, "", "");

                            _partLibrary.add(newBasicPart);
                            newBasicPart.saveDefault(_collector);
                            newBasicPart.setTransientStatus(false);
                        }
                    }                    
                    
                    

                    //Forming the composite part composition
                    if (!isLinker) {
                        
                        directions.add(bpDirection);
                        
                        ArrayList<Part> allPartsWithName = _collector.getAllPartsWithName(basicPartName, false);
                        Part bp = null;

                        //First pick the part with no overhangs, i.e. basic part
                        for (Part partWithName : allPartsWithName) {
                            String LO = partWithName.getLeftOverhang();
                            String RO = partWithName.getRightOverhang();
                            if (LO.isEmpty() && RO.isEmpty() && bpDirections.equals(partWithName.getDirections())) {
                                if (!partWithName.getType().contains("plasmid")) {
                                    bp = partWithName;
                                }
                            }
                        }

                        //Then try to find a match
                        for (Part partWithName : allPartsWithName) {
                            String LO = partWithName.getLeftOverhang();
                            String RO = partWithName.getRightOverhang();
                            if (LO.equals(bpForcedLeft) && RO.equals(bpForcedRight) && bpDirections.equals(partWithName.getDirections())) {
                                if (!partWithName.getType().contains("plasmid")) {
                                    bp = partWithName;
                                }
                            }
                        }

                        composition.add(bp);

                        //Add scar to scar set and fix if this is a gene or reporter with biobricks
                        if (i > 9) {
                            if (scar.equals("BB") && (bp.getType().get(0).equalsIgnoreCase("gene") || bp.getType().get(0).equalsIgnoreCase("reporter"))) {
                                scar = "BBm";
                            }
                            scars.add(scar);
                        }
                    }
                }

                //Add vector pair if it is in the library
                Vector vector = null;
                ArrayList<Vector> vectors = _collector.getAllVectorsWithName(vectorName, false);
                if (vectors.size() > 0) {
                    for (Vector vec : vectors) {
                        if (vec.getLeftOverhang().equals(leftOverhang) && vec.getRightOverhang().equals(rightOverhang)) {
                            vector = vec;
                        }
                    }
                    
                    //This is a patch for the vector counting problem on data upload - God this parsing code and data management blows asshole
                    if (vector == null) {
                        int level = -1;
                        if (!tokens[7].isEmpty()) {
                            level = Integer.valueOf(tokens[7].trim());
                        }
                        
                        Vector newVector = Vector.generateVector(vectorName, vectors.get(0).getSeq(), leftOverhang, rightOverhang, "destination vector", vectorName, "", vectors.get(0).getResistance(), level);
                        
                        _vectorLibrary.add(newVector);
                        newVector.saveDefault(_collector);
                        newVector.setTransientStatus(true);
                        vector = newVector;
                    }
                }

                //Get scar sequences
                ArrayList<String> scarSeqs = ClothoWriter.scarsToSeqs(scars, _collector);
                
                //Library logic - make new plasmids whether or not they are in the library
                Part newPlasmid;
                ArrayList<String> typeP = new ArrayList();
                typeP.add("plasmid");
                if (composition.size() > 1) {
                    newPlasmid = Part.generateComposite(name, composition, scarSeqs, scars, linkers, directions, leftOverhang, rightOverhang, typeP);
                } else {
                    newPlasmid = Part.generateBasic(name, composition.get(0).getSeq(), composition, typeP, directions, leftOverhang, rightOverhang);
                }
                newPlasmid = newPlasmid.saveDefault(_collector);
                newPlasmid.setTransientStatus(false);
                _libraryOHHash.put(newPlasmid.getUUID(), leftOverhang + "|" + rightOverhang);

                //Library logic - if the pasmid is in the library, add a composite part, which is different from a plasmid
                if (!tokens[0].trim().isEmpty()) {
                    _libraryPartsVectors.put(newPlasmid, vector);

                    if (composition.size() > 1) {
                        ArrayList<String> typeC = new ArrayList();
                        typeC.add("composite");
                        Part newComposite = Part.generateComposite(name, composition, scarSeqs, scars, linkers, directions, leftOverhang, rightOverhang, typeC);
                        newComposite = newComposite.saveDefault(_collector);
                        newComposite.setTransientStatus(false);
                        _partLibrary.add(newComposite);
                    }
                }

            } catch (NullPointerException e) {
                String badLine = "";

                for (int j = 0; j < tokens.length; j++) {
                    badLine = badLine + tokens[j] + ",";
                }
                badLines.add(badLine.substring(0, badLine.length() - 1));//trim the last comma
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

    //given an array of part and vector uuids, save each of them and set their transient status to false
    //saved parts and vectors are typically associated with a new design on the client
    //writeSQL indicates whether or not parts will be saved in an sql database; url hardcoded for puppeteer team testing
    public String save(HashSet<Part> parts, HashSet<Vector> vectors, boolean writeSQL) {
        ArrayList<Part> toSaveParts = new ArrayList();
        ArrayList<Vector> toSaveVectors = new ArrayList();
        boolean multiplexed = false;

        //Save parts
        for (Part p : parts) {
            if (p != null) {

                //Extra check to make sure multiplex parts are not saved
                for (String bp : p.getStringComposition()) {
                    if (bp.contains("?")) {
                        multiplexed = true;
                    }
                }
                
                if (p.isTransient() && !multiplexed) {

                    ArrayList<Part> allPartsWithName = _collector.getAllPartsWithName(p.getName(), true);
                    for (Part partWithName : allPartsWithName) {
                        if (p.getLeftOverhang().equalsIgnoreCase(partWithName.getLeftOverhang()) && p.getRightOverhang().equalsIgnoreCase(partWithName.getRightOverhang()) && p.getStringComposition().equals(partWithName.getStringComposition()) && p.getScars().equals(partWithName.getScars()) && p.getDirections().equals(partWithName.getDirections()) && !partWithName.getType().contains("plasmid")) {
                            if (p != partWithName) {
                                partWithName.setTransientStatus(false);
                                _partLibrary.add(partWithName);
                            }
                        }
                    }

                    p.setTransientStatus(false);
                    _libraryOHHash.put(p.getUUID(), p.getLeftOverhang() + "|" + p.getRightOverhang());
                    _partLibrary.add(p);

                    for (Vector v : vectors) {
                        if (v != null) {
                            if (p.getLeftOverhang().equals(v.getLeftOverhang()) && p.getRightOverhang().equals(v.getRightOverhang())) {
                                _libraryPartsVectors.put(p, v);
                            }
                        }
                    }
                    toSaveParts.add(p);
                }
            }
        }
        
        //Save vectors
        for (Vector v : vectors) {
            if (v != null) {
                v.setTransientStatus(false);
                _vectorLibrary.add(v);
                toSaveVectors.add(v);
            }
        }
        
        //Save to Puppeteer DB
        if (writeSQL) {
            PuppeteerWriter.saveParts(toSaveParts, _databaseConfig);
            PuppeteerWriter.saveVectors(toSaveVectors, _databaseConfig);
        }
        return "saved data";
    }

    //returns "loaded" or "not loaded" depending on whether there are objects in the collector
    public String getDataStatus() throws Exception {
        String toReturn = "not loaded";
        if (_collector.getAllParts(false).size() > 0 || _collector.getAllVectors(false).size() > 0) {
            toReturn = "loaded";
        }
        return toReturn;
    }

    /**
     * Traverse a solution graph for statistics *
     */
    private void getSolutionStats(String method, HashSet<Part> gps) throws Exception {

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

        _statistics.setEfficiency(eff);
        _statistics.setRecommended(recCnt);
        _statistics.setDiscouraged(disCnt);
        _statistics.setStages(stages);
        _statistics.setSteps(steps);
        _statistics.setSharing(shr);
        _statistics.setGoalParts(gps.size());
        _statistics.setExecutionTime(Statistics.getTime());
        _statistics.setReaction(rxn);
        _statistics.setValid(_valid);
        if (method.equalsIgnoreCase("biobricks")) {
            rxn = rxn + 2; //correction for additional biobrick vectors
        }
        System.out.println("Steps: " + steps + " Stages: " + stages + " Shared: " + shr + " PCRs: " + rxn + " Time: " + Statistics.getTime() + " valid: " + _valid);
    }

    //Get parts from part IDs
    public HashSet<Part> IDsToParts(String[] partIDs) {
        
        HashSet<Part> parts = new HashSet();
        for (String ID : partIDs) {
            Part current = _collector.getPart(ID, true);
            parts.add(current);
        }
        return parts;
    }
    
    //Get vectors from part IDs
    public HashSet<Vector> IDsToVectors(String[] vectorIDs) {
        
        HashSet<Vector> vectors = new HashSet();
        for (String ID : vectorIDs) {
            Vector current = _collector.getVector(ID, true);
            vectors.add(current);
        }
        return vectors;
    }
    
    //Get parts from part IDs
    public HashMap<Integer, Vector> IDsToStageVectors(HashMap<String, String> stageVectorsString) {
        
        //Set up stage vector hash
        HashMap<Integer, Vector> stageVectors = new HashMap<Integer, Vector>();
        Set<String> keySet = stageVectorsString.keySet();
        for (String strStage : keySet) {

            Integer stage = Integer.parseInt(strStage);
            Vector vector = _collector.getVector(stageVectorsString.get(strStage), false);
            stageVectors.put(stage, vector);
        }
        return stageVectors;
    }
    
    //Make libraryOH map
    public HashMap<String, String> makeLibraryOHHash(HashSet<Part> partsLib) {
        
        HashMap<String,String> libraryOH = new HashMap();
        for (Part p : partsLib) {
            libraryOH.put(p.getUUID(), p.getLeftOverhang() + "|" + p.getRightOverhang());
        }
        return libraryOH;
    }
    
    //Traverse a graph and look for multiplex basic nodes
    public void addMultiplexParts (ArrayList<RGraph> graphs, Collector coll) {
        
        //Traverse each graph
        for (RGraph graph : graphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            RNode root = graph.getRootNode();
            queue.add(root);
            
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);

                //Extra multiplex nodes only apply to basic parts
                if (current.getUUID() != null) {
                    Part p = coll.getPart(current.getUUID(), false);
                    
                    //Special edge case of 
                    
                    if (p.isBasic()) {
                        String type = p.getType().get(0);
                        if (p.getType().contains("_multiplex")) {
                            
                            //Cycle through all parts of this type in the collector and make new nodes with each part's UUID
                            
                            ArrayList<Part> allParts = coll.getAllParts(false);
                            type = type.substring(0, type.length()-10);
                            for (Part lP : allParts) {
                                
                                //Find only basic parts with no overhangs and the same direction
                                if (lP.getType().contains(type) && lP.getDirections().equals(p.getDirections())) {
                                    if (lP.getLeftOverhang().isEmpty() && lP.getRightOverhang().isEmpty()) {
                                    
                                    //Make a new node, fix neighbors, set its UUID
                                        RNode clone = current.clone(false);
                                        for (RNode neighbor : current.getNeighbors()) {
                                            neighbor.removeNeighbor(current);
                                            neighbor.addNeighbor(clone);
                                            clone.addNeighbor(neighbor);
                                        }
                                        clone.setUUID(lP.getUUID());
                                        clone.setName(lP.getName());
                                        ArrayList<String> pComp = new ArrayList();
                                        pComp.add(lP.getName());
                                        clone.setComposition(pComp);
                                        ArrayList<String> pType = new ArrayList();
                                        pType.add(type);
                                        clone.setType(pType);
                                    }
                                }
                            }
                            
                        }
                    }
                }
                
                //Add unseen neighbors to traversal
                ArrayList<RNode> neighbors = current.getNeighbors();
                for (RNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }
    
    //Run only a specific method with full parameters defined
    public ArrayList<RGraph> solve (HashSet<Part> gps, ArrayList<Part> partLibrary, ArrayList<Vector> vectorLibrary, JSONObject parameters, HashMap<Integer, Vector> stageVectors, HashMap<String, String> libraryOHHash, Collector collector) throws Exception {
        
        ArrayList<RGraph> assemblyGraphs = new ArrayList();
        HashSet<String> required = new HashSet();
        HashSet<String> recommended = new HashSet();
        HashSet<String> forbidden = new HashSet();
        HashSet<String> discouraged = new HashSet();
        
        String method = "gibson";
        if (parameters.has("method")) {
            method = parameters.get("method").toString().trim();
        }
        
        if (parameters.has("recommended")) {
            String[] recArray = parameters.get("recommended").toString().split(";");
            if (recArray.length > 0) {
                for (int i = 0; i < recArray.length; i++) {
                    if (recArray[i].length() > 0) {
                        String rcA = recArray[i];
                        rcA = rcA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                        recommended.add(rcA);
                    }
                }
            }
        }

        if (parameters.has("required")) {
            String[] reqArray = parameters.get("required").toString().split(";");
            if (reqArray.length > 0) {
                for (int i = 0; i < reqArray.length; i++) {
                    if (reqArray[i].length() > 0) {
                        String rqA = reqArray[i];
                        rqA = rqA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                        required.add(rqA);
                    }
                }
            }
        }

        if (parameters.has("forbidden")) {
            String[] forbiddenArray = parameters.get("forbidden").toString().split(";");
            if (forbiddenArray.length > 0) {
                for (int i = 0; i < forbiddenArray.length; i++) {
                    if (forbiddenArray[i].length() > 0) {
                        String fA = forbiddenArray[i];
                        fA = fA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                        forbidden.add(fA);
                    }
                }
            }
        }

        if (parameters.has("discouraged")) {
            String[] discouragedArray = parameters.get("discouraged").toString().split(";");
            if (discouragedArray.length > 0) {
                for (int i = 0; i < discouragedArray.length; i++) {
                    if (discouragedArray[i].length() > 0) {
                        String dA = discouragedArray[i];
                        dA = dA.replaceAll("\\|[^|]\\|[^|]\\|", "|||");
                        discouraged.add(dA);
                    }
                }
            }
        }

        //Generate efficiency hash and make default has if empty
        HashMap<Integer, Double> efficiency = new HashMap();
        if (parameters.has("efficiency")) {
            String[] efficiencyArray = parameters.get("efficiency").toString().split(",");

            if (efficiencyArray.length > 0) {
                for (int i = 0; i < efficiencyArray.length; i++) {                    
                    String effVal = efficiencyArray[i].replaceAll("\"", "");
                    efficiency.put(i + 2, Double.parseDouble(effVal));
                }
            }
        } else {
            if (method.equalsIgnoreCase("gatewaygibson")) {
                for (int i = 0; i < 7; i++) {
                    efficiency.put(i + 2, Double.parseDouble("1.0"));
                }
            } else if (method.equalsIgnoreCase("biobricks")) {
                for (int i = 0; i < 1; i++) {
                    efficiency.put(i + 2, Double.parseDouble("1.0"));
                }
            } else {
                for (int i = 0; i < 5; i++) {
                    efficiency.put(i + 2, Double.parseDouble("1.0"));
                }
            }
        }
        
        //Initiate minimum cloning length
        int minCloneLength;
        if (parameters.has("minCloneLength") && parameters.get("minCloneLength") != "null") {
            if (!parameters.get("minCloneLength").toString().equals("null")) {
                minCloneLength = Integer.valueOf(parameters.get("minCloneLength").toString());
            } else {
                minCloneLength = 250;
            }        
        } else {
            minCloneLength = 250;
        }
        
        boolean overhangValid = false;
        
        stageVectors = checkStageVectors(stageVectors, collector, method);        
        
        //Run BioBricks
        if (method.equalsIgnoreCase("biobricks")) {
            RBioBricks biobricks = new RBioBricks();
            assemblyGraphs = biobricks.bioBricksClothoWrapper(gps, required, recommended, forbidden, discouraged, partLibrary, stageVectors, null);
            overhangValid = RBioBricks.validateOverhangs(assemblyGraphs);
        
        //Run CPEC    
        } else if (method.equalsIgnoreCase("cpec")) {
            RCPEC cpec = new RCPEC();
            assemblyGraphs = cpec.cpecClothoWrapper(gps, required, recommended, forbidden, discouraged, partLibrary, efficiency, stageVectors, null, minCloneLength, collector);
            overhangValid = RGibson.validateOverhangs(assemblyGraphs);
        
        //Run Gibson    
        } else if (method.equalsIgnoreCase("gibson")) {
            RGibson gibson = new RGibson();
            assemblyGraphs = gibson.gibsonClothoWrapper(gps, required, recommended, forbidden, discouraged, partLibrary, efficiency, stageVectors, null, minCloneLength, collector);
            overhangValid = RGibson.validateOverhangs(assemblyGraphs);
        
        //Run GoldenGate    
        } else if (method.equalsIgnoreCase("goldengate")) {          
            RGoldenGate gg = new RGoldenGate();
            assemblyGraphs = gg.goldenGateClothoWrapper(gps, vectorLibrary, required, recommended, forbidden, discouraged, partLibrary, efficiency, stageVectors, null);
            overhangValid = RGoldenGate.validateOverhangs(assemblyGraphs);
        
        //Run GatewayGibson    
        } else if (method.equalsIgnoreCase("gatewaygibson")) {
            RGatewayGibson gwgib = new RGatewayGibson();
            assemblyGraphs = gwgib.gatewayGibsonWrapper(gps, vectorLibrary, required, recommended, forbidden, discouraged, partLibrary, false, efficiency, stageVectors, null, libraryOHHash, collector);
            overhangValid = RGatewayGibson.validateOverhangs(assemblyGraphs);
        
        //Run MoClo     
        } else if (method.equalsIgnoreCase("moclo")) {
            RMoClo moclo = new RMoClo();
            assemblyGraphs = moclo.mocloClothoWrapper(gps, vectorLibrary, required, recommended, forbidden, discouraged, partLibrary, false, efficiency, stageVectors, null, libraryOHHash, collector);
            overhangValid = RMoClo.validateOverhangs(assemblyGraphs);
        
        //Run SLIC    
        } else if (method.equalsIgnoreCase("slic")) {
            RSLIC slic = new RSLIC();
            assemblyGraphs = slic.slicClothoWrapper(gps, required, recommended, forbidden, discouraged, partLibrary, efficiency, stageVectors, null, minCloneLength, collector);
            overhangValid = RGibson.validateOverhangs(assemblyGraphs);
        }
        
        //Add extra nodes for multiplexing
        addMultiplexParts(assemblyGraphs, collector);
        
        boolean valid = validateReqForb(assemblyGraphs, required, forbidden);
        boolean allValid = valid && overhangValid;
        _valid = allValid;
        if (allValid == false) {
            throw new Exception();
        }
        
        return assemblyGraphs;
    }
    
    //Using parameters from the client, run the algorithm
    //Gets solution graph, 
    public JSONObject run(String designCount, JSONObject parameters, HashSet<Part> gps, HashMap<Integer, Vector> stageVectors, String instructionsFilePath) throws Exception {
        
        //Check to make sure there is a method and efficieny, otherwise default to Gibson
        String method = "gibson";
        if (parameters.has("method")) {
            method = parameters.get("method").toString().trim();
        }
        
        //If stageVectors are empty, intialize
        if (stageVectors == null) {
            stageVectors = new HashMap();
        }
        
        //Solve the assembly
        _statistics = new Statistics();
        Statistics.start();        
        _assemblyGraphs = solve(gps, _partLibrary, _vectorLibrary, parameters, stageVectors, _libraryOHHash, _collector);        
        Statistics.stop();

        //Get target root node list for instructions and picture generation
        ArrayList<RNode> targetRootNodes = new ArrayList();
        HashSet<String> targetRootNodeKeys = new HashSet();
        for (RGraph result : _assemblyGraphs) {
            if (!targetRootNodeKeys.contains(result.getRootNode().getNodeKey("+")) || !targetRootNodeKeys.contains(result.getRootNode().getNodeKey("-"))) {
                targetRootNodes.add(result.getRootNode());
                targetRootNodeKeys.add(result.getRootNode().getNodeKey("+"));
                targetRootNodeKeys.add(result.getRootNode().getNodeKey("-"));
            }
        }
        
        //Merge graphs and make new clotho parts where appropriate
        _assemblyGraphs = RGraph.mergeGraphs(_assemblyGraphs);
        ClothoWriter writer = new ClothoWriter();
        for (RGraph result : _assemblyGraphs) {
            writer.nodesToClothoPartsVectors(_collector, result, _libraryPartsVectors, stageVectors, method, _user);
        }

        //Get graph stats
        RGraph.getGraphStats(_assemblyGraphs, _partLibrary, _vectorLibrary, parameters, 0.0, 0.0, 0.0, 0.0);
        getSolutionStats(method, gps);

        _instructions = RInstructions.generateInstructions(targetRootNodes, _collector, _partLibrary, _vectorLibrary, parameters, true, method);
        
        //Create export files if a design number is specified
        JSONObject toReturn = new JSONObject();
        
        //If instructionFilePath is specified
        if (designCount == null && instructionsFilePath != null) {
            
            File file = new File(instructionsFilePath + "/instructionsRavenTest.txt");
            FileWriter fw = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(fw);
            out.write(_instructions);
            out.close();            
            
            _instructionsFile = file;
        }
        
        if (designCount != null) {

            //Generate graph and arc files
            ArrayList<String> graphTextFiles = new ArrayList();
            ArrayList<String> arcTextFiles = new ArrayList();
            for (RGraph result : _assemblyGraphs) {
                ArrayList<String> postOrderEdges = result.getPostOrderEdges();
                arcTextFiles.add(result.printArcsFile(_collector, postOrderEdges, method));
                graphTextFiles.add(result.generateWeyekinFile(_partLibrary, _vectorLibrary, _libraryPartsVectors, targetRootNodes, method));
            }
            String mergedArcText = RGraph.mergeArcFiles(arcTextFiles);
            String mergedGraphText = RGraph.mergeWeyekinFiles(graphTextFiles);

            //Instruction file
            File file = new File(_path + _user + "/instructions" + designCount + ".txt");
            FileWriter fw = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(fw);
            out.write(_instructions);
            out.close();

//            _instructionsFile = file;
            
            //Pigeon text file
            file = new File(_path + _user + "/pigeon" + designCount + ".txt");
            fw = new FileWriter(file);
            out = new BufferedWriter(fw);
            out.write(mergedGraphText);
            out.close();

            //Puppeteer file
            file = new File(_path + _user + "/arcs" + designCount + ".txt");
            fw = new FileWriter(file);
            out = new BufferedWriter(fw);
            out.write(mergedArcText);
            out.close();

            //Post request to graphviz
            WeyekinPoster.setDotText(mergedGraphText);
            WeyekinPoster.postMyVision();
            String imageURL = "";
            imageURL = WeyekinPoster.getmGraphVizURI().toString();
            toReturn.put("images", imageURL);
        }
        return toReturn;
    }

    //traverse the graph and return a boolean indicating whether or not hte graph is valid in terms of composition
    private boolean validateReqForb(ArrayList<RGraph> optimalGraphs, HashSet<String> required, HashSet<String> forbidden) throws Exception {
        boolean toReturn = true;
        HashSet<String> seenRequired = new HashSet();
        
        //Traverse each graph to make sure all required compositions are there and no forbidden are there
        for (RGraph graph : optimalGraphs) {
            ArrayList<RNode> queue = new ArrayList();
            HashSet<RNode> seenNodes = new HashSet();
            queue.add(graph.getRootNode());
            while (!queue.isEmpty()) {
                
                //Traversing mechanism
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);
                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
                
                //Get compositon/direction string for the node
                ArrayList<String> direction = current.getDirection();
                ArrayList<String> composition = current.getComposition();
                String currentCompositionString = composition.toString();               
                if (direction.size() == composition.size()) {
                    currentCompositionString = "";
                    for (int i = 0; i < composition.size(); i++) {
                        currentCompositionString = currentCompositionString + composition.get(i) + "|" + direction.get(i) + ", ";
                    }
                }
                currentCompositionString = "[" + currentCompositionString.substring(0, currentCompositionString.length() - 2) + "]";
                
                //Check for this composition in forbidden set - if forbidden seen, graph invalid
                if (forbidden.contains(currentCompositionString)) {
                    toReturn = false;
                    break;
                }
                
                //Check for this composition in reqiured set
                if (required.contains(currentCompositionString)) {
                    seenRequired.add(currentCompositionString);
                }
            }
            if (toReturn == false) {
                break;
            }
        }

        if (toReturn && required.size() == seenRequired.size()) {
            return true;
        } else {
            return true;
        }

    }

    //getter for accessing the instructions from RavenServlet
    public String getInstructions() {
        return _instructions;
    }
    
    //getter for accessing the instructions from RavenServlet
    public File getInstructionsFile() {
        return _instructionsFile;
    }

//    public String importClotho(JSONArray toImport) {
//        String toReturn = "";
//        try {
//            for (int i = 0; i < toImport.length(); i++) {
//                JSONObject currentPart = toImport.getJSONObject(i);
//                if (currentPart.getString("schema").equals("BasicPart")) {
//                    Part newBasicPart = Part.generateBasic(currentPart.getString("name"), currentPart.getJSONObject("sequence").getString("sequence"), null, new ArrayList(), new ArrayList(), "", "", "");
//                    String type = "gene";
//                    if (currentPart.has("type")) {
//                        type = currentPart.getString("type");
//                    }
//                    newBasicPart.addSearchTag("Type: " + type);
//                    newBasicPart.setUuid(currentPart.getString("id"));
//                    newBasicPart = newBasicPart.saveDefault(_collector);
//                    newBasicPart.setTransientStatus(false);
//                } else if (currentPart.getString("schema").equals("CompositePart")) {
//                    JSONArray compositionArray = currentPart.getJSONArray("composition");
//                    ArrayList<Part> composition = new ArrayList();
//                    ArrayList<String> direction = new ArrayList();
//                    for (int j = 0; j < compositionArray.length(); j++) {
//                        composition.add(_collector.getPart(compositionArray.getString(j), false));
//                        direction.add("+");
//                    }
//                    Part newComposite = Part.generateComposite(currentPart.getString("name"), composition, null, null, null, null, null, null);
//                    newComposite.setUuid(currentPart.getString("id"));
//                    newComposite.addSearchTag("Type: composite");
//                    newComposite.addSearchTag("Direction: " + direction);
//                    newComposite = newComposite.saveDefault(_collector);
//                    newComposite.setTransientStatus(false);
//                }
//
//            }
//        } catch (JSONException ex) {
//            Logger.getLogger(RavenController.class.getName()).log(Level.SEVERE, null, ex);
//            return "bad";
//        }
//
//        return "good";
//    }

    public JSONObject generateStats() throws Exception {
        String statString = "{\"goalParts\":\"" + _statistics.getGoalParts()
                + "\",\"steps\":\"" + _statistics.getSteps()
                + "\",\"stages\":\"" + _statistics.getStages()
                + "\",\"reactions\":\"" + _statistics.getReactions()
                + "\",\"recommended\":\"" + _statistics.getRecommended()
                + "\",\"discouraged\":\"" + _statistics.getDiscouraged()
                + "\",\"efficiency\":\"" + _statistics.getEfficiency()
                + "\",\"sharing\":\"" + _statistics.getSharing()
                + "\",\"time\":\"" + _statistics.getExecutionTime()
                + "\",\"valid\":\"" + _statistics.isValid() + "\"}";
        return new JSONObject(statString);
    }
    
    //Get parameters
    public String getParameters() {
        return _preloadedParams;
    }
    
    //Get collector
    public Collector getCollector() {
        return _collector;
    }
    
    //Get assembly graphs
    public ArrayList<RGraph> getAssemblyGraphs() {
        return _assemblyGraphs;
    }
    
    //Get assembly stats
    public Statistics getStatistics() {
        return _statistics;
    }
    
    //Add part to library
    public void addToPartLibrary(Part p) {
        _partLibrary.add(p);
    }
    
    //Add part to library
    public void addToVectorLibrary(Vector v) {
        _vectorLibrary.add(v);
    }
    
    //Set the partVector library
    public void setPartVectorPairs (HashMap<Part, Vector> partVec) {
        _libraryPartsVectors = partVec;
    }
    
    //FIELDS
    private File _instructionsFile;
    private HashMap<Part, Vector> _libraryPartsVectors = new HashMap<Part, Vector>();
    private Statistics _statistics = new Statistics();
    private ArrayList<RGraph> _assemblyGraphs = new ArrayList<RGraph>();
    private HashMap<String, String> _libraryOHHash = new HashMap<String, String>();
    private ArrayList<Part> _partLibrary = new ArrayList<Part>();
    private ArrayList<Vector> _vectorLibrary = new ArrayList<Vector>();
    
    private String _instructions = "";
    protected Collector _collector = new Collector();
    private String _path;
    private String _user;
    private String _error = "";
    private boolean _valid = false;
    private String _preloadedParams = null;
    private ArrayList<String> _databaseConfig = new ArrayList(); //0:database url, 1:database schema, 2:user, 3:password
    private ArrayList<RestrictionEnzyme> _restrictionEnzymes = RestrictionEnzyme.getBBGGMoCloEnzymes();
    private String _pSB1K3 = "tactagtagcggccgctgcagtccggcaaaaaagggcaaggtgtcaccaccctgccctttttctttaaaaccgaaaagattacttcgcgttatgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccacaggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaagaacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagctcgagtcccgtcaagtcagcgtaatgctctgccagtgttacaaccaattaaccaattctgattagaaaaactcatcgagcatcaaatgaaactgcaatttattcatatcaggattatcaataccatatttttgaaaaagccgtttctgtaatgaaggagaaaactcaccgaggcagttccataggatggcaagatcctggtatcggtctgcgattccgactcgtccaacatcaatacaacctattaatttcccctcgtcaaaaataaggttatcaagtgagaaatcaccatgagtgacgactgaatccggtgagaatggcaaaagcttatgcatttctttccagacttgttcaacaggccagccattacgctcgtcatcaaaatcactcgcatcaaccaaaccgttattcattcgtgattgcgcctgagcgagacgaaatacgcgatcgctgttaaaaggacaattacaaacaggaatcgaatgcaaccggcgcaggaacactgccagcgcatcaacaatattttcacctgaatcaggatattcttctaatacctggaatgctgttttcccggggatcgcagtggtgagtaaccatgcatcatcaggagtacggataaaatgcttgatggtcggaagaggcataaattccgtcagccagtttagtctgaccatctcatctgtaacatcattggcaacgctacctttgccatgtttcagaaacaactctggcgcatcgggcttcccatacaatcgatagattgtcgcacctgattgcccgacattatcgcgagcccatttatacccatataaatcagcatccatgttggaatttaatcgcggcctggagcaagacgtttcccgttgaatatggctcataacaccccttgtattactgtttatgtaagcagacagttttattgttcatgatgatatatttttatcttgtgcaatgtaacatcagagattttgagacacaacgtggctttgttgaataaatcgaacttttgctgagttgaaggatcagctcgagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
    private String _pSB1A2 = "tactagtagcggccgctgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccataggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaaggacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagttaccaatgcttaatcagtgaggcacctatctcagcgatctgtctatttcgttcatccatagttgcctgactccccgtcgtgtagataactacgatacgggagggcttaccatctggccccagtgctgcaatgataccgcgagacccacgctcaccggctccagatttatcagcaataaaccagccagccggaagggccgagcgcagaagtggtcctgcaactttatccgcctccatccagtctattaattgttgccgggaagctagagtaagtagttcgccagttaatagtttgcgcaacgttgttgccattgctacaggcatcgtggtgtcacgctcgtcgtttggtatggcttcattcagctccggttcccaacgatcaaggcgagttacatgatcccccatgttgtgcaaaaaagcggttagctccttcggtcctccgatcgttgtcagaagtaagttggccgcagtgttatcactcatggttatggcagcactgcataattctcttactgtcatgccatccgtaagatgcttttctgtgactggtgagtactcaaccaagtcattctgagaatagtgtatgcggcgaccgagttgctcttgcccggcgtcaatacgggataataccgcgccacatagcagaactttaaaagtgctcatcattggaaaacgttcttcggggcgaaaactctcaaggatcttaccgctgttgagatccagttcgatgtaacccactcgtgcacccaactgatcttcagcatcttttactttcaccagcgtttctgggtgagcaaaaacaggaaggcaaaatgccgcaaaaaagggaataagggcgacacggaaatgttgaatactcatactcttcctttttcaatattattgaagcatttatcagggttattgtctcatgagcggatacatatttgaatgtatttagaaaaataaacaaataggggttccgcgcacatttccccgaaaagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
}
