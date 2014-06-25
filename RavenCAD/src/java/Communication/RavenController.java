/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import Controller.accessibility.RInstructions;
import Controller.algorithms.modasm.RBioBricks;
import Controller.accessibility.ClothoWriter;
import Controller.accessibility.ClothoReader;
import Controller.algorithms.PrimerDesign;
import Controller.algorithms.modasm.*;
import Controller.algorithms.nonmodasm.*;
import Controller.datastructures.*;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
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

    public ArrayList<RGraph> runBioBricks() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //If stageVectors are empty, fill with defaults
        if (_stageVectors.get(0) == null) {

            ArrayList<String> defaultTags0 = new ArrayList<String>();
            defaultTags0.add("LO: ");
            defaultTags0.add("RO: ");
            defaultTags0.add("Type: vector");
            defaultTags0.add("Resistance: ampicilin");
            Vector st0Vec = _collector.getExactVector("pSK1A2", _pSK1A2, defaultTags0, false);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSK1A2", _pSK1A2);
                for (String tag : defaultTags0) {
                    st0Vec.addSearchTag(tag);
                }
            }

            _stageVectors.put(0, st0Vec);
        }

        //Run algorithm for BioBricks assembly
        _assemblyGraphs.clear();
        RBioBricks biobricks = new RBioBricks();
        ArrayList<RGraph> optimalGraphs = biobricks.bioBricksClothoWrapper(_goalParts, _required, _recommended, _forbidden, _discouraged, _partLibrary, _stageVectors, null);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for Gibson *
     */
    public ArrayList<RGraph> runGibson(Integer minCloneLength) throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //If stageVectors are empty, fill with defaults
        if (_stageVectors.get(0) == null) {

            ArrayList<String> defaultTags0 = new ArrayList<String>();
            defaultTags0.add("LO: ");
            defaultTags0.add("RO: ");
            defaultTags0.add("Type: vector");
            defaultTags0.add("Resistance: ampicilin");
            Vector st0Vec = _collector.getExactVector("pSK1A2", _pSK1A2, defaultTags0, false);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSK1A2", _pSK1A2);
                for (String tag : defaultTags0) {
                    st0Vec.addSearchTag(tag);
                }
            }

            _stageVectors.put(0, st0Vec);
        }

        //Run algorithm for Gibson assembly
        _assemblyGraphs.clear();
        RGibson gibson = new RGibson();
        ArrayList<RGraph> optimalGraphs = gibson.gibsonClothoWrapper(_goalParts, _required, _recommended, _forbidden, _discouraged, _partLibrary, _efficiency, _stageVectors, null, minCloneLength, _collector);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for CPEC *
     */
    public ArrayList<RGraph> runCPEC(Integer minCloneLength) throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //If stageVectors are empty, fill with defaults
        if (_stageVectors.get(0) == null) {

            ArrayList<String> defaultTags0 = new ArrayList<String>();
            defaultTags0.add("LO: ");
            defaultTags0.add("RO: ");
            defaultTags0.add("Type: vector");
            defaultTags0.add("Resistance: ampicilin");
            Vector st0Vec = _collector.getExactVector("pSK1A2", _pSK1A2, defaultTags0, false);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSK1A2", _pSK1A2);
                for (String tag : defaultTags0) {
                    st0Vec.addSearchTag(tag);
                }
            }

            _stageVectors.put(0, st0Vec);
        }

        //Run algorithm for CPEC assembly
        _assemblyGraphs.clear();
        RCPEC cpec = new RCPEC();
        ArrayList<RGraph> optimalGraphs = cpec.cpecClothoWrapper(_goalParts, _required, _recommended, _forbidden, _discouraged, _partLibrary, _efficiency, _stageVectors, null, minCloneLength, _collector);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for SLIC *
     */
    public ArrayList<RGraph> runSLIC(Integer minCloneLength) throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //If stageVectors are empty, fill with defaults
        if (_stageVectors.get(0) == null) {

            ArrayList<String> defaultTags0 = new ArrayList<String>();
            defaultTags0.add("LO: ");
            defaultTags0.add("RO: ");
            defaultTags0.add("Type: vector");
            defaultTags0.add("Resistance: ampicilin");
            Vector st0Vec = _collector.getExactVector("pSK1A2", _pSK1A2, defaultTags0, false);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSK1A2", _pSK1A2);
                for (String tag : defaultTags0) {
                    st0Vec.addSearchTag(tag);
                }
            }

            _stageVectors.put(0, st0Vec);
        }

        //Run algorithm for SLIC assembly
        _assemblyGraphs.clear();
        RSLIC slic = new RSLIC();
        ArrayList<RGraph> optimalGraphs = slic.slicClothoWrapper(_goalParts, _required, _recommended, _forbidden, _discouraged, _partLibrary, _efficiency, _stageVectors, null, minCloneLength, _collector);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for MoClo *
     */
    public ArrayList<RGraph> runMoClo() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //If stageVectors are empty, fill with defaults
        if (_stageVectors.get(0) == null) {

            ArrayList<String> defaultTags0 = new ArrayList<String>();
            defaultTags0.add("LO: ");
            defaultTags0.add("RO: ");
            defaultTags0.add("Type: vector");
            defaultTags0.add("Resistance: kanamycin");
            Vector st0Vec = _collector.getExactVector("pSB1K3", _pSB1K3, defaultTags0, false);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSB1K3", _pSB1K3);
                for (String tag : defaultTags0) {
                    st0Vec.addSearchTag(tag);
                }
            }

            ArrayList<String> defaultTags1 = new ArrayList<String>();
            defaultTags1.add("LO: ");
            defaultTags1.add("RO: ");
            defaultTags1.add("Type: vector");
            defaultTags1.add("Resistance: ampicilin");
            Vector st1Vec = _collector.getExactVector("pSB1A2", _pSB1A2, defaultTags1, false);
            if (st1Vec == null) {
                st1Vec = Vector.generateVector("pSB1A2", _pSB1A2);
                for (String tag : defaultTags1) {
                    st1Vec.addSearchTag(tag);
                }
            }

            _stageVectors.put(0, st1Vec);
            _stageVectors.put(1, st0Vec);
        }

        //Run algorithm for MoClo assembly
        _assemblyGraphs.clear();
        RMoClo moclo = new RMoClo();
        moclo.setForcedOverhangs(_collector, _forcedOverhangHash);
        ArrayList<RGraph> optimalGraphs = moclo.mocloClothoWrapper(_goalParts, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, false, _efficiency, _stageVectors, null, _libraryOHHash);
        return optimalGraphs;
    }

    /**
     * Run SRS algorithm for Golden Gate *
     */
    public ArrayList<RGraph> runGoldenGate() throws Exception {
        if (_goalParts == null) {
            return null;
        }

        //If stageVectors are empty, fill with defaults
        if (_stageVectors.get(0) == null) {

            ArrayList<String> defaultTags0 = new ArrayList<String>();
            defaultTags0.add("LO: ");
            defaultTags0.add("RO: ");
            defaultTags0.add("Type: vector");
            defaultTags0.add("Resistance: kanamycin");
            Vector st0Vec = _collector.getExactVector("pSB1K3", _pSB1K3, defaultTags0, false);
            if (st0Vec == null) {
                st0Vec = Vector.generateVector("pSB1K3", _pSB1K3);
                for (String tag : defaultTags0) {
                    st0Vec.addSearchTag(tag);
                }
            }

            ArrayList<String> defaultTags1 = new ArrayList<String>();
            defaultTags1.add("LO: ");
            defaultTags1.add("RO: ");
            defaultTags1.add("Type: vector");
            defaultTags1.add("Resistance: ampicilin");
            Vector st1Vec = _collector.getExactVector("pSB1A2", _pSB1A2, defaultTags1, false);
            if (st1Vec == null) {
                st1Vec = Vector.generateVector("pSB1A2", _pSB1A2);
                for (String tag : defaultTags1) {
                    st1Vec.addSearchTag(tag);
                }
            }

            _stageVectors.put(0, st1Vec);
            _stageVectors.put(1, st0Vec);
        }

        //Run algorithm for Golden Gate assembly
        _assemblyGraphs.clear();
        RGoldenGate gg = new RGoldenGate();
        ArrayList<RGraph> optimalGraphs = gg.goldenGateClothoWrapper(_goalParts, _vectorLibrary, _required, _recommended, _forbidden, _discouraged, _partLibrary, _efficiency, _stageVectors, null);
        return optimalGraphs;
    }

    //returns json array containing all objects in parts list; generates parts list file
    //input: design number refers to the design number on the client
    public JSONArray generatePartsList(String designNumber, String params) throws Exception {
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
            if (!"plasmid".equals(libPart.getType())) {
                if (!libPart.getLeftOverhang().isEmpty() || !libPart.getRightOverhang().isEmpty()) {
                    toRemove.add(libPart);
                } else if ("composite".equals(libPart.getType())) {
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
            String type = p.getType();
            ArrayList<String> direction = p.getDirections();
            String composition = "";
            String vectorName = "";

            if (p.isBasic()) {
                
                //Edge case for merged parts
                String bpDirection;
                if (type.startsWith("[") && type.endsWith("]")) {
                    type = "multitype";
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
                type = "plasmid";
                Vector v = partVectorHash.get(p);
                if (v != null) {
                    vectorName = v.getName();
                }

                for (int i = 0; i < p.getComposition().size(); i++) {
                    Part subpart = p.getComposition().get(i);
                    String cRO = subpart.getLeftOverhang();
                    String cLO = subpart.getRightOverhang();

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

                    if (!cLO.isEmpty() && !cRO.isEmpty()) {
                        composition = composition + ", " + subpart.getName() + "|" + cLO + "|" + cRO + "|" + direction.get(i).trim();
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
        _goalParts = new HashSet<Part>();//key: target part, value: vector
        _efficiency = new HashMap<Integer, Double>();
        _required = new HashSet<String>();
        _recommended = new HashSet<String>();
        _discouraged = new HashSet<String>();
        _forbidden = new HashSet<String>();
        _statistics = new Statistics();
        _assemblyGraphs = new ArrayList<RGraph>();
        _forcedOverhangHash = new HashMap<String, ArrayList<String>>();
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
                String bpLO = new String();
                String bpRO = new String();
                String type = p.getType();
                ArrayList<String> tags = p.getSearchTags();
                ArrayList<String> direction = ClothoReader.parseTags(tags, "Direction:");

                //Displaying plasmids in data upload tab
                if (composition.size() > 1) {
                    for (int i = 0; i < composition.size(); i++) {
                        Part part = composition.get(i);

                        if (!direction.isEmpty()) {
                            bpDir = direction.get(i);
                        }

                        ArrayList<String> searchTags = part.getSearchTags();
                        String aPartComp;

                        //Look at all of the search tags
                        for (String tag : searchTags) {
                            if (tag.startsWith("LO:")) {
                                bpLO = tag.substring(4);
                            } else if (tag.startsWith("RO:")) {
                                bpRO = tag.substring(4);
                            }
                        }

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

                    ArrayList<String> searchTags = part.getSearchTags();
                    String aPartComp;

                    //Look at all of the search tags
                    for (String tag : searchTags) {
                        if (tag.startsWith("LO:")) {
                            bpLO = tag.substring(4);
                        } else if (tag.startsWith("RO:")) {
                            bpRO = tag.substring(4);
                        }
                    }

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

        // generate default vectors
        //default for biobricks, CPEC, Gibson, SLIC
        ArrayList<String> defaultTags0 = new ArrayList<String>();
        defaultTags0.add("LO: ");
        defaultTags0.add("RO: ");
        defaultTags0.add("Type: vector");
        defaultTags0.add("Resistance: ampicilin");
        Vector st0Vec = Vector.generateVector("pSK1A2", _pSK1A2);
        for (String tag : defaultTags0) {
            st0Vec.addSearchTag(tag);
        }
        st0Vec.saveDefault(_collector);
        st0Vec.setTransientStatus(false);

        //default for Golden Gate
        defaultTags0 = new ArrayList<String>();
        defaultTags0.add("LO: ");
        defaultTags0.add("RO: ");
        defaultTags0.add("Type: vector");
        defaultTags0.add("Resistance: kanamycin");
        st0Vec = Vector.generateVector("pSB1K3", _pSB1K3);
        for (String tag : defaultTags0) {
            st0Vec.addSearchTag(tag);
        }
        st0Vec.saveDefault(_collector);
        st0Vec.setTransientStatus(false);

        ArrayList<String> defaultTags1 = new ArrayList<String>();
        defaultTags1.add("LO: ");
        defaultTags1.add("RO: ");
        defaultTags1.add("Type: vector");
        defaultTags1.add("Resistance: ampicilin");
        Vector st1Vec = Vector.generateVector("pSB1A2", _pSB1A2);
        for (String tag : defaultTags1) {
            st1Vec.addSearchTag(tag);
        }
        st1Vec.saveDefault(_collector);
        st1Vec.setTransientStatus(false);
        
        _vectorLibrary = new ArrayList<Vector>();
        _partLibrary = new ArrayList<Part>();
        ArrayList<String> badLines = new ArrayList();
        ArrayList<String[]> compositePartTokens = new ArrayList<String[]>();
        if (_forcedOverhangHash == null) {
            _forcedOverhangHash = new HashMap<String, ArrayList<String>>();
        }
        if (_libraryOHHash == null) {
            _libraryOHHash = new HashMap<String, String>();
        }
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
                    String resistance = tokens[6].toLowerCase().trim();

                    Vector newVector = Vector.generateVector(name, sequence);
                    newVector.addSearchTag("LO: " + leftOverhang);
                    newVector.addSearchTag("RO: " + rightOverhang);
                    newVector.addSearchTag("Type: " + type);
                    newVector.addSearchTag("Resistance: " + resistance);

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

                        Vector newVector = Vector.generateVector(name, sequence);
                        newVector.addSearchTag("LO: " + leftOverhang);
                        newVector.addSearchTag("RO: " + rightOverhang);
                        newVector.addSearchTag("Level: " + level);
                        newVector.addSearchTag("Type: " + type);
                        newVector.addSearchTag("Resistance: " + resistance);
                        newVector.addSearchTag("Vector: " + name);
                        newVector.addSearchTag("Composition: " + composition);

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
                    String sequence = tokens[2].trim().replaceAll(" ","");

                    Part newBasicPart = Part.generateBasic(name, sequence, null);
                    newBasicPart.addSearchTag("LO: ");
                    newBasicPart.addSearchTag("RO: ");
                    newBasicPart.addSearchTag("Type: " + type);
                    newBasicPart.addSearchTag("Direction: [+]");
                    newBasicPart.addSearchTag("Scars: []");
                    
                    Part newReverseBasicPart = Part.generateBasic(name, PrimerDesign.reverseComplement(sequence), null);
                    newReverseBasicPart.addSearchTag("LO: ");
                    newReverseBasicPart.addSearchTag("RO: ");
                    newReverseBasicPart.addSearchTag("Type: " + type);
                    newReverseBasicPart.addSearchTag("Direction: [-]");
                    newReverseBasicPart.addSearchTag("Scars: []");

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

        //Create the composite parts
        for (String[] tokens : compositePartTokens) {
            try {
                ArrayList<Part> composition = new ArrayList<Part>();

                //For all of the basic parts in the composite part composition
                String name = tokens[1].trim();
                String leftOverhang = tokens[3].trim();
                String rightOverhang = tokens[4].trim();
                String vectorName = tokens[8].trim();
                ArrayList<String> directions = new ArrayList<String>();
                ArrayList<String> scars = new ArrayList<String>();

                //Parse composition tokens
                for (int i = 9; i < tokens.length; i++) {
                    String basicPartString = tokens[i].trim();
                    String[] partNameTokens = basicPartString.split("\\|");
                    String bpForcedLeft = " ";
                    String bpForcedRight = " ";
                    String bpDirection = "+";
                    String scar = "_";
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
                            scar = bpForcedRight;
                        } else if (partNameTokens.length == 4) {
                            bpForcedLeft = partNameTokens[1];
                            bpForcedRight = partNameTokens[2];
                            bpDirection = partNameTokens[3];
                            scar = bpForcedRight;
                        }
                    }

                    //Add scars from the right side... maybe not perfect, but ok
                    if (i != (tokens.length - 1)) {
                        scars.add(scar);
                    }

                    //Basic part plasmids - add as new basic parts with overhang for re-use
                    if (tokens.length == 10) {
                        if (i == 9) {

                            Part basic = null;
                            ArrayList<Part> allPartsWithName = _collector.getAllPartsWithName(basicPartName, false);
                            for (Part aPart : allPartsWithName) {
                                if (!aPart.getType().equals("plasmid") && bpDirection.equals(aPart.getDirections().get(0))) {
                                    basic = aPart;
                                }
                            }
                            String sequence = basic.getSeq().replaceAll(" ","");
                            String type = basic.getType();
                            Part newBasicPart = Part.generateBasic(basicPartName, sequence, null);
                            newBasicPart.addSearchTag("Type: " + type);
                            newBasicPart.addSearchTag("Direction: [" + bpDirection + "]");
                            newBasicPart.addSearchTag("LO: " + leftOverhang);
                            newBasicPart.addSearchTag("RO: " + rightOverhang);
                            newBasicPart.addSearchTag("Scars: []");

                            //Library logic
                            if (!tokens[0].trim().isEmpty()) {
                                _partLibrary.add(newBasicPart);
                                newBasicPart.saveDefault(_collector);
                                newBasicPart.setTransientStatus(false);
                            }
                        }
                    }

                    //Forced overhangs
//                    if (_forcedOverhangHash.get(compositePartName) != null) {
//                        _forcedOverhangHash.get(compositePartName).add(bpForcedLeft + "|" + bpForcedRight);
//                    } else {
//                        ArrayList<String> toAdd = new ArrayList();
//                        toAdd.add(bpForcedLeft + "|" + bpForcedRight);
//                        _forcedOverhangHash.put(compositePartName, toAdd);
//                    }
                    directions.add(bpDirection);

                    //Forming the composite part composition
                    ArrayList<Part> allPartsWithName = _collector.getAllPartsWithName(basicPartName, false);
                    Part bp = null;

                    //First pick the part with no overhangs, i.e. basic part
                    for (Part partWithName : allPartsWithName) {
                        String LO = partWithName.getLeftOverhang();
                        String RO = partWithName.getRightOverhang();
                        if (LO.isEmpty() && RO.isEmpty() && bpDirection.equals(partWithName.getDirections().get(0))) {
                            if (!partWithName.getType().equals("plasmid")) {
                                bp = partWithName;
                            }
                        }
                    }

                    //Then try to find a match
                    for (Part partWithName : allPartsWithName) {
                        String LO = partWithName.getLeftOverhang();
                        String RO = partWithName.getRightOverhang();
                        if (LO.equals(bpForcedLeft) && RO.equals(bpForcedRight) && bpDirection.equals(partWithName.getDirections().get(0))) {
                            if (!partWithName.getType().equals("plasmid")) {
                                bp = partWithName;
                            }
                        }
                    }

                    composition.add(bp);
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
                }

                //Library logic - make new plasmids whether or not they are in the library
                Part newPlasmid;
                if (composition.size() > 1) {
                    newPlasmid = Part.generateComposite(composition, name);
                } else {
                    newPlasmid = Part.generateBasic(name, composition.get(0).getSeq(), composition);
                }
                newPlasmid.addSearchTag("Direction: " + directions);
                newPlasmid.addSearchTag("LO: " + leftOverhang);
                newPlasmid.addSearchTag("RO: " + rightOverhang);
                newPlasmid.addSearchTag("Type: plasmid");
                newPlasmid.addSearchTag("Scars: " + scars);
                newPlasmid = newPlasmid.saveDefault(_collector);
                newPlasmid.setTransientStatus(false);
                _libraryOHHash.put(newPlasmid.getUUID(), leftOverhang + "|" + rightOverhang);

                //Library logic - if the pasmid is in the library, add a composite part, which is different from a plasmid
                if (!tokens[0].trim().isEmpty()) {
                    _libraryPartsVectors.put(newPlasmid, vector);

                    if (composition.size() > 1) {
                        Part newComposite = Part.generateComposite(composition, name);
                        newComposite.addSearchTag("Direction: " + directions);
                        newComposite.addSearchTag("LO: " + leftOverhang);
                        newComposite.addSearchTag("RO: " + rightOverhang);
                        newComposite.addSearchTag("Type: composite");
                        newComposite.addSearchTag("Scars: " + scars);
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
    public String save(String[] partIDs, String[] vectorIDs, boolean writeSQL) {
        ArrayList<Part> toSaveParts = new ArrayList();
        ArrayList<Vector> toSaveVectors = new ArrayList();
        if (partIDs.length > 0) {
            for (int i = 0; i < partIDs.length; i++) {
                Part p = _collector.getPart(partIDs[i], true);
                if (p != null) {

                    if (p.isTransient()) {
                        
                        ArrayList<Part> allPartsWithName = _collector.getAllPartsWithName(p.getName(), true);
                        for (Part partWithName : allPartsWithName) {
                            if (p.getLeftOverhang().equalsIgnoreCase(partWithName.getLeftOverhang()) && p.getRightOverhang().equalsIgnoreCase(partWithName.getRightOverhang()) && p.getStringComposition().equals(partWithName.getStringComposition()) && p.getScars().equals(partWithName.getScars()) && p.getDirections().equals(partWithName.getDirections()) && !partWithName.getType().equalsIgnoreCase("plasmid")) {
                                if (p != partWithName) {
                                    partWithName.setTransientStatus(false);
                                    _partLibrary.add(partWithName);
                                }
                            }
                        }

                        p.setTransientStatus(false);
                        _libraryOHHash.put(p.getUUID(), p.getLeftOverhang() + "|" + p.getRightOverhang());
                        _partLibrary.add(p);
                        
                        //Extra logic to determine part-vector pairs... probably not perfect logic here, but works some times... hack
                        if (vectorIDs.length > 0) {
                            for (int k = 0; k < vectorIDs.length; k++) {
                                Vector v = _collector.getVector(vectorIDs[k], true);
                                if (v != null) {
                                   if (p.getLeftOverhang().equals(v.getLeftOverhang()) && p.getRightOverhang().equals(v.getRightOverhang())) {
                                       _libraryPartsVectors.put(p, v);
                                   } 
                                }
                            }
                        }
                        
                        toSaveParts.add(p);
                    }
                }
            }
        }
        if (vectorIDs.length > 0) {
            for (int i = 0; i < vectorIDs.length; i++) {
                Vector v = _collector.getVector(vectorIDs[i], true);
                if (v != null) {
                    v.setTransientStatus(false);
                    _vectorLibrary.add(v);
                    toSaveVectors.add(v);
                }
            }
        }
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
    private void getSolutionStats(String method) throws Exception {

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
        _statistics.setGoalParts(_goalParts.size());
        _statistics.setExecutionTime(Statistics.getTime());
        _statistics.setReaction(rxn);
        _statistics.setValid(_valid);
        if (method.equalsIgnoreCase("biobricks")) {
            rxn = rxn + 2; //correction for additional biobrick vectors
        }
        System.out.println("Steps: " + steps + " Stages: " + stages + " Shared: " + shr + " PCRs: " + rxn + " Time: " + Statistics.getTime() + " valid: " + _valid);
    }

    //Using parameters from the client, run the algorithm
    public JSONObject run(String designCount, String method, String[] targetIDs, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, String[] partLibraryIDs, String[] vectorLibraryIDs, HashMap<Integer, Double> efficiencyHash, ArrayList<String> primerParameters, HashMap<String, String> stageVectors) throws Exception {
        _goalParts = new HashSet<Part>();
        _required = required;
        _recommended = recommended;
        _forbidden = forbidden;
        _discouraged = discouraged;
        _stageVectors = new HashMap<Integer, Vector>();
        _statistics = new Statistics();
        _assemblyGraphs = new ArrayList<RGraph>();
        _efficiency = efficiencyHash;
        _valid = false;
        method = method.trim();
        
        int minCloneLength;
        try {
            minCloneLength = Integer.valueOf(primerParameters.get(4));
        } catch (Exception e) {
            minCloneLength = 250;
        }

        for (int i = 0; i < targetIDs.length; i++) {
            Part current = _collector.getPart(targetIDs[i], false);
            _goalParts.add(current);
        }

        //Set up stage vector hash
        Set<String> keySet = stageVectors.keySet();
        for (String strStage : keySet) {

            Integer stage = Integer.parseInt(strStage);
            Vector vector = _collector.getVector(stageVectors.get(strStage), false);
            _stageVectors.put(stage, vector);
        }

        Statistics.start();
        if (method.equalsIgnoreCase("biobricks")) {
            _assemblyGraphs = runBioBricks();
        } else if (method.equalsIgnoreCase("cpec")) {
            _assemblyGraphs = runCPEC(minCloneLength);
        } else if (method.equalsIgnoreCase("gibson")) {
            _assemblyGraphs = runGibson(minCloneLength);
        } else if (method.equalsIgnoreCase("goldengate")) {
            _assemblyGraphs = runGoldenGate();
        } else if (method.equalsIgnoreCase("moclo")) {
            _assemblyGraphs = runMoClo();
        } else if (method.equalsIgnoreCase("slic")) {
            _assemblyGraphs = runSLIC(minCloneLength);
        }

        Statistics.stop();
        ClothoWriter writer = new ClothoWriter();
        ArrayList<String> graphTextFiles = new ArrayList();
        ArrayList<String> arcTextFiles = new ArrayList<String>();
        ArrayList<RNode> targetRootNodes = new ArrayList();
        HashSet<String> targetRootNodeKeys = new HashSet();

        //Get target root node list for instructions and picture generation
        for (RGraph result : _assemblyGraphs) {
            if (!targetRootNodeKeys.contains(result.getRootNode().getNodeKey("+")) || !targetRootNodeKeys.contains(result.getRootNode().getNodeKey("-"))) {
                targetRootNodes.add(result.getRootNode());
                targetRootNodeKeys.add(result.getRootNode().getNodeKey("+"));
                targetRootNodeKeys.add(result.getRootNode().getNodeKey("-"));
            }
        }

        //Initialize statistics
        boolean overhangValid = false;
        if (method.equalsIgnoreCase("biobricks")) {
            overhangValid = RBioBricks.validateOverhangs(_assemblyGraphs);
        } else if (method.equalsIgnoreCase("cpec")) {
            overhangValid = RCPEC.validateOverhangs(_assemblyGraphs);
        } else if (method.equalsIgnoreCase("gibson")) {
            overhangValid = RGibson.validateOverhangs(_assemblyGraphs);
        } else if (method.equalsIgnoreCase("goldengate")) {
            overhangValid = RGoldenGate.validateOverhangs(_assemblyGraphs);
        } else if (method.equalsIgnoreCase("moclo")) {
            overhangValid = RMoClo.validateOverhangs(_assemblyGraphs);
        } else if (method.equalsIgnoreCase("slic")) {
            overhangValid = RSLIC.validateOverhangs(_assemblyGraphs);
        }
        boolean valid = validateGraphComposition();
        _valid = valid && overhangValid;
        
        //Merge graphs and make new clotho parts where appropriate
        _assemblyGraphs = RGraph.mergeGraphs(_assemblyGraphs);
        for (RGraph result : _assemblyGraphs) {
            writer.nodesToClothoPartsVectors(_collector, result, _libraryPartsVectors, _stageVectors, method, _user);
        }

        //Get graph stats
        RGraph.getGraphStats(_assemblyGraphs, _partLibrary, _vectorLibrary, _recommended, _discouraged, 0.0, 0.0, 0.0, 0.0);
        getSolutionStats(method);       

        //Generate Instructions
        _instructions = RInstructions.generateInstructions(targetRootNodes, _collector, _partLibrary, _vectorLibrary, primerParameters, true, method);
        if (_instructions == null) {
            _instructions = "Assembly instructions for RavenCAD are coming soon! Please stay tuned.";
        }

        //Generate graph and arc files
        for (RGraph result : _assemblyGraphs) {
            ArrayList<String> postOrderEdges = result.getPostOrderEdges();
            arcTextFiles.add(result.printArcsFile(_collector, postOrderEdges, method));
            graphTextFiles.add(result.generateWeyekinFile(_partLibrary, _vectorLibrary, _libraryPartsVectors, targetRootNodes, method));
        }
        
        System.out.println("GRAPH AND ARCS FILES CREATED");
//        JSONObject d3Graph = new JSONObject();
//        JSONObject d3Graph = RGraph.generateD3Graph(_assemblyGraphs, _partLibrary, _vectorLibrary);
        String mergedArcText = RGraph.mergeArcFiles(arcTextFiles);       
        String mergedGraphText = RGraph.mergeWeyekinFiles(graphTextFiles);
        File file = new File(_path + _user + "/instructions" + designCount + ".txt");
        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        out.write(_instructions);
        out.close();

        //write graph text file
        file = new File(_path + _user + "/pigeon" + designCount + ".txt");
        fw = new FileWriter(file);
        out = new BufferedWriter(fw);
        out.write(mergedGraphText);
        out.close();

        //write arcs text file
        file = new File(_path + _user + "/arcs" + designCount + ".txt");
        fw = new FileWriter(file);
        out = new BufferedWriter(fw);
        out.write(mergedArcText);
        out.close();

        //post request to graphviz
        WeyekinPoster.setDotText(mergedGraphText);
        WeyekinPoster.postMyVision();
        String imageURL = "";
        imageURL = WeyekinPoster.getmGraphVizURI().toString();
//        String imageURL = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT2Du60RHLtNV9OG-ww7HG3srhWq3VDa3RXmA4pB0SyorO8dTrBUg";
        JSONObject toReturn = new JSONObject();
        toReturn.put("images", imageURL);
        return toReturn;
//        return d3Graph;
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
                        currentCompositionString = currentCompositionString + composition.get(i) + "|" + direction.get(i) + ", ";
                    }
                }
                currentCompositionString = "[" + currentCompositionString.substring(0, currentCompositionString.length() - 2) + "]";
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

//        if (toReturn && _required.size() == seenRequired.size()) {
//            return true;
//        } else {
            return true;
//        }

    }

    //getter for accessing the instructions from RavenServlet
    public String getInstructions() {
        return _instructions;
    }

    public String importClotho(JSONArray toImport) {
        String toReturn = "";
        try {
            for (int i = 0; i < toImport.length(); i++) {
                JSONObject currentPart = toImport.getJSONObject(i);
                if (currentPart.getString("schema").equals("BasicPart")) {
                    Part newBasicPart = Part.generateBasic(currentPart.getString("name"), currentPart.getJSONObject("sequence").getString("sequence"), null);
                    String type = "gene";
                    if (currentPart.has("type")) {
                        type = currentPart.getString("type");
                    }
                    newBasicPart.addSearchTag("Type: " + type);
                    newBasicPart.setUuid(currentPart.getString("id"));
                    newBasicPart = newBasicPart.saveDefault(_collector);
                    newBasicPart.setTransientStatus(false);
                } else if (currentPart.getString("schema").equals("CompositePart")) {
                    JSONArray compositionArray = currentPart.getJSONArray("composition");
                    ArrayList<Part> composition = new ArrayList();
                    ArrayList<String> direction = new ArrayList();
                    for (int j = 0; j < compositionArray.length(); j++) {
                        composition.add(_collector.getPart(compositionArray.getString(j), false));
                        direction.add("+");
                    }
                    Part newComposite = Part.generateComposite(composition, currentPart.getString("name"));
                    newComposite.setUuid(currentPart.getString("id"));
                    newComposite.addSearchTag("Type: composite");
                    newComposite.addSearchTag("Direction: " + direction);
                    newComposite = newComposite.saveDefault(_collector);
                    newComposite.setTransientStatus(false);
                }

            }
        } catch (JSONException ex) {
            Logger.getLogger(RavenController.class.getName()).log(Level.SEVERE, null, ex);
            return "bad";
        }

        return "good";
    }

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
    //FIELDS
    private HashSet<Part> _goalParts = new HashSet<Part>();//key: target part, value: composition
    private HashMap<Part, Vector> _libraryPartsVectors = new HashMap<Part, Vector>();
    private HashMap<Integer, Double> _efficiency = new HashMap<Integer, Double>();
    private HashSet<String> _required = new HashSet<String>();
    private HashSet<String> _recommended = new HashSet<String>();
    private HashSet<String> _discouraged = new HashSet<String>();
    private HashSet<String> _forbidden = new HashSet<String>();
    private Statistics _statistics = new Statistics();
    private ArrayList<RGraph> _assemblyGraphs = new ArrayList<RGraph>();
    private HashMap<String, ArrayList<String>> _forcedOverhangHash = new HashMap<String, ArrayList<String>>();
    private HashMap<String, String> _libraryOHHash = new HashMap<String, String>();
    private ArrayList<Part> _partLibrary = new ArrayList<Part>();
    private ArrayList<Vector> _vectorLibrary = new ArrayList<Vector>();
    private HashMap<Integer, Vector> _stageVectors = new HashMap<Integer, Vector>();
    private String _instructions = "";
    protected Collector _collector = new Collector(); //key:user, value: collector assocaited with that user
    private String _path;
    private String _user;
    private String _error = "";
    private boolean _valid = false;
    private String _preloadedParams = null;
    private ArrayList<String> _databaseConfig = new ArrayList(); //0:database url, 1:database schema, 2:user, 3:password
    private ArrayList<RestrictionEnzyme> _restrictionEnzymes = RestrictionEnzyme.getBBGGMoCloEnzymes();
    private String _pSB1K3 = "tactagtagcggccgctgcagtccggcaaaaaagggcaaggtgtcaccaccctgccctttttctttaaaaccgaaaagattacttcgcgttatgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccacaggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaagaacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagctcgagtcccgtcaagtcagcgtaatgctctgccagtgttacaaccaattaaccaattctgattagaaaaactcatcgagcatcaaatgaaactgcaatttattcatatcaggattatcaataccatatttttgaaaaagccgtttctgtaatgaaggagaaaactcaccgaggcagttccataggatggcaagatcctggtatcggtctgcgattccgactcgtccaacatcaatacaacctattaatttcccctcgtcaaaaataaggttatcaagtgagaaatcaccatgagtgacgactgaatccggtgagaatggcaaaagcttatgcatttctttccagacttgttcaacaggccagccattacgctcgtcatcaaaatcactcgcatcaaccaaaccgttattcattcgtgattgcgcctgagcgagacgaaatacgcgatcgctgttaaaaggacaattacaaacaggaatcgaatgcaaccggcgcaggaacactgccagcgcatcaacaatattttcacctgaatcaggatattcttctaatacctggaatgctgttttcccggggatcgcagtggtgagtaaccatgcatcatcaggagtacggataaaatgcttgatggtcggaagaggcataaattccgtcagccagtttagtctgaccatctcatctgtaacatcattggcaacgctacctttgccatgtttcagaaacaactctggcgcatcgggcttcccatacaatcgatagattgtcgcacctgattgcccgacattatcgcgagcccatttatacccatataaatcagcatccatgttggaatttaatcgcggcctggagcaagacgtttcccgttgaatatggctcataacaccccttgtattactgtttatgtaagcagacagttttattgttcatgatgatatatttttatcttgtgcaatgtaacatcagagattttgagacacaacgtggctttgttgaataaatcgaacttttgctgagttgaaggatcagctcgagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
    private String _pSB1A2 = "tactagtagcggccgctgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccataggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaaggacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagttaccaatgcttaatcagtgaggcacctatctcagcgatctgtctatttcgttcatccatagttgcctgactccccgtcgtgtagataactacgatacgggagggcttaccatctggccccagtgctgcaatgataccgcgagacccacgctcaccggctccagatttatcagcaataaaccagccagccggaagggccgagcgcagaagtggtcctgcaactttatccgcctccatccagtctattaattgttgccgggaagctagagtaagtagttcgccagttaatagtttgcgcaacgttgttgccattgctacaggcatcgtggtgtcacgctcgtcgtttggtatggcttcattcagctccggttcccaacgatcaaggcgagttacatgatcccccatgttgtgcaaaaaagcggttagctccttcggtcctccgatcgttgtcagaagtaagttggccgcagtgttatcactcatggttatggcagcactgcataattctcttactgtcatgccatccgtaagatgcttttctgtgactggtgagtactcaaccaagtcattctgagaatagtgtatgcggcgaccgagttgctcttgcccggcgtcaatacgggataataccgcgccacatagcagaactttaaaagtgctcatcattggaaaacgttcttcggggcgaaaactctcaaggatcttaccgctgttgagatccagttcgatgtaacccactcgtgcacccaactgatcttcagcatcttttactttcaccagcgtttctgggtgagcaaaaacaggaaggcaaaatgccgcaaaaaagggaataagggcgacacggaaatgttgaatactcatactcttcctttttcaatattattgaagcatttatcagggttattgtctcatgagcggatacatatttgaatgtatttagaaaaataaacaaataggggttccgcgcacatttccccgaaaagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
    private String _pSK1A2 = "tactagtagcggccgctgcaggcttcctcgctcactgactcgctgcgctcggtcgttcggctgcggcgagcggtatcagctcactcaaaggcggtaatacggttatccacagaatcaggggataacgcaggaaagaacatgtgagcaaaaggccagcaaaaggccaggaaccgtaaaaaggccgcgttgctggcgtttttccataggctccgcccccctgacgagcatcacaaaaatcgacgctcaagtcagaggtggcgaaacccgacaggactataaagataccaggcgtttccccctggaagctccctcgtgcgctctcctgttccgaccctgccgcttaccggatacctgtccgcctttctcccttcgggaagcgtggcgctttctcatagctcacgctgtaggtatctcagttcggtgtaggtcgttcgctccaagctgggctgtgtgcacgaaccccccgttcagcccgaccgctgcgccttatccggtaactatcgtcttgagtccaacccggtaagacacgacttatcgccactggcagcagccactggtaacaggattagcagagcgaggtatgtaggcggtgctacagagttcttgaagtggtggcctaactacggctacactagaaggacagtatttggtatctgcgctctgctgaagccagttaccttcggaaaaagagttggtagctcttgatccggcaaacaaaccaccgctggtagcggtggtttttttgtttgcaagcagcagattacgcgcagaaaaaaaggatctcaagaagatcctttgatcttttctacggggtctgacgctcagtggaacgaaaactcacgttaagggattttggtcatgagattatcaaaaaggatcttcacctagatccttttaaattaaaaatgaagttttaaatcaatctaaagtatatatgagtaaacttggtctgacagttaccaatgcttaatcagtgaggcacctatctcagcgatctgtctatttcgttcatccatagttgcctgactccccgtcgtgtagataactacgatacgggagggcttaccatctggccccagtgctgcaatgataccgcgagacccacgctcaccggctccagatttatcagcaataaaccagccagccggaagggccgagcgcagaagtggtcctgcaactttatccgcctccatccagtctattaattgttgccgggaagctagagtaagtagttcgccagttaatagtttgcgcaacgttgttgccattgctacaggcatcgtggtgtcacgctcgtcgtttggtatggcttcattcagctccggttcccaacgatcaaggcgagttacatgatcccccatgttgtgcaaaaaagcggttagctccttcggtcctccgatcgttgtcagaagtaagttggccgcagtgttatcactcatggttatggcagcactgcataattctcttactgtcatgccatccgtaagatgcttttctgtgactggtgagtactcaaccaagtcattctgagaatagtgtatgcggcgaccgagttgctcttgcccggcgtcaatacgggataataccgcgccacatagcagaactttaaaagtgctcatcattggaaaacgttcttcggggcgaaaactctcaaggatcttaccgctgttgagatccagttcgatgtaacccactcgtgcacccaactgatcttcagcatcttttactttcaccagcgtttctgggtgagcaaaaacaggaaggcaaaatgccgcaaaaaagggaataagggcgacacggaaatgttgaatactcatactcttcctttttcaatattattgaagcatttatcagggttattgtctcatgagcggatacatatttgaatgtatttagaaaaataaacaaataggggttccgcgcacatttccccgaaaagtgccacctgacgtctaagaaaccattattatcatgacattaacctataaaaataggcgtatcacgaggcagaatttcagataaaaaaaatccttagctttcgctaaggatgatttctggaattcgcggccgcttctagag";
}
