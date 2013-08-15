/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.modasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.PrimerDesign;
import Controller.algorithms.RGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jenhantao,evanappleton
 */
public class RGoldenGate extends RGeneral {

    /**
     * Clotho part wrapper for Golden Gate assembly *
     */
    public ArrayList<RGraph> goldenGateClothoWrapper(HashMap<Part, Vector> goalPartsVectors, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, HashMap<Integer, Double> efficiencies, ArrayList<Double> costs) throws Exception {

        //Designate how many parts can be efficiently ligated in one step
        int max = 0;
        Set<Integer> keySet = efficiencies.keySet();
        for (Integer key : keySet) {
            if (key > max) {
                max = key;
            }
        }
        _maxNeighbors = max;
        ArrayList<Part> goalParts = new ArrayList<Part>(goalPartsVectors.keySet());

        //Create hashMem parameter for createAsmGraph_sgp() call
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, discouraged, recommended);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalPartsVectors, true);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        optimalGraphs = assignOverhangs(optimalGraphs);

        return optimalGraphs;
    }

    /**
     * Optimize overhang assignments based on available parts and vectors with
     * overhangs *
     */
    private ArrayList<RGraph> assignOverhangs(ArrayList<RGraph> optimalGraphs) {
        return optimalGraphs;
    }

    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
    
    /**
     * Generation of new MoClo primers for parts *
     */
    public static ArrayList<String> generatePartPrimers(RNode node, Collector coll, Double meltingTemp, Integer targetLength) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getModularOHseqs();
        ArrayList<String> oligos = new ArrayList<String>(2);
        String partPrimerPrefix = "nn";
        String partPrimerSuffix = "nn";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";

        Part currentPart = coll.getPart(node.getUUID(), true);
        String forwardOligoSequence;
        String reverseOligoSequence;
        if (currentPart.getSeq().length() > 24) {
            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "nn" + overhangVariableSequenceHash.get(node.getLOverhang()) + currentPart.getSeq().substring(0, PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, currentPart.getSeq(), true, false));
            reverseOligoSequence = PrimerDesign.reverseComplement(currentPart.getSeq().substring(currentPart.getSeq().length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(currentPart.getSeq()), true, false)) + revEnzymeRecSite1 + "nn" + overhangVariableSequenceHash.get(node.getROverhang()) + partPrimerSuffix);
        } else {
            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "nn" + overhangVariableSequenceHash.get(node.getLOverhang()) + currentPart.getSeq() + overhangVariableSequenceHash.get(node.getROverhang()) + "nn" + revEnzymeRecSite1 + partPrimerSuffix;
            reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);
        }
        oligos.add(forwardOligoSequence);
        oligos.add(reverseOligoSequence);
        
        System.out.println("node.getUUID(): " + node.getUUID());
        System.out.println();

        return oligos;
    }

    /**
     * Generation of new MoClo primers for parts *
     */
    public static ArrayList<String> generateVectorPrimers(RVector vector, Collector coll) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getModularOHseqs();
        String vectorPrimerPrefix = "gttctttactagtg";
        String vectorPrimerSuffix = "tactagtagcggccgc";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";
        String fwdEnzymeRecSite2 = "ggtctc";
        String revEnzymeRecSite2 = "gagacc";

        ArrayList<String> oligos = new ArrayList<String>(2);

        //Level 0, 2, 4, 6, etc. vectors
        String forwardOligoSequence;
        String reverseOligoSequence;
        if (vector.getLevel() % 2 == 0) {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite2 + "n" + overhangVariableSequenceHash.get(vector.getLOverhang()) + "nn" + revEnzymeRecSite1 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite1 + "nn" + overhangVariableSequenceHash.get(vector.getROverhang()) + "n" + revEnzymeRecSite2 + vectorPrimerSuffix);

            //Level 1, 3, 5, 7, etc. vectors
        } else {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite1 + "n" + overhangVariableSequenceHash.get(vector.getLOverhang()) + "nn" + revEnzymeRecSite2 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite2 + "nn" + overhangVariableSequenceHash.get(vector.getROverhang()) + "n" + revEnzymeRecSite1 + vectorPrimerSuffix);
        }

        oligos.add(forwardOligoSequence);
        oligos.add(reverseOligoSequence);

        return oligos;
    }
}
