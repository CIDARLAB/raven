/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.nonmodasm;

import Controller.algorithms.PrimerDesign;
import Controller.datastructures.Collector;
import Controller.datastructures.Part;
import Controller.datastructures.RGraph;
import Controller.datastructures.RNode;
import java.util.ArrayList;

/**
 *
 * @author evanappleton
 */
public class RHomologyPrimerDesign {
    
    public static ArrayList<String> homologousRecombinationPrimers(RNode node, RNode root, Collector coll, Double meltingTemp, Integer targetLength) {
        
        //initialize primer parameters
        ArrayList<String> oligos = new ArrayList<String>(2);
        String forwardOligoSequence;
        String reverseOligoSequence;
        String leftNeighborSeq = "";
        String rightNeighborSeq = "";
        int rightNeighborSeqLength = 0;
        int leftNeighborSeqLength = 0;
        Part currentPart = coll.getPart(node.getUUID(), true);
        Part rootPart = coll.getPart(root.getUUID(), true);
        ArrayList<Part> composition = rootPart.getComposition();
        int indexOf = composition.indexOf(currentPart);
        
        //Keep getting the sequences of the leftmost neighbors until the min sequence size is satisfied
        while (leftNeighborSeqLength < targetLength) {
            if (indexOf == 0) {
                leftNeighborSeq = leftNeighborSeq + composition.get(composition.size() - 1).getSeq();
                leftNeighborSeqLength = leftNeighborSeq.length();
                indexOf = composition.size() - 1;
            } else {
                leftNeighborSeq = leftNeighborSeq + composition.get(indexOf - 1).getSeq();
                leftNeighborSeqLength = leftNeighborSeq.length();
                indexOf--;
            }
        }

        //Keep getting the sequences of the righttmost neighbors until the min sequence size is satisfied
        while (rightNeighborSeqLength < targetLength) {
            if (indexOf == composition.size() - 1) {
                rightNeighborSeq = rightNeighborSeq + composition.get(0).getSeq();
                rightNeighborSeqLength = rightNeighborSeq.length();
                indexOf = 0;
            } else {
                rightNeighborSeq = rightNeighborSeq + composition.get(indexOf + 1).getSeq();
                rightNeighborSeqLength = rightNeighborSeq.length();
                indexOf++;
            }
        }

        int lNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, leftNeighborSeq, false, true);
        int rNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(rightNeighborSeq), false, true);
        int currentPartLHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, currentPart.getSeq(), true, true);
        int currentPartRHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(currentPart.getSeq()), true, true);
        
        //If the homology of this part is the full length of this part, return blank oligos... other longer oligos will cover this span
        if (currentPartLHomologyLength == currentPart.getSeq().length() || currentPartRHomologyLength == currentPart.getSeq().length()) {
            return oligos;
        }
        
        forwardOligoSequence = leftNeighborSeq.substring(leftNeighborSeq.length() - lNeighborHomologyLength) + currentPart.getSeq().substring(0, currentPartLHomologyLength);
        reverseOligoSequence = PrimerDesign.reverseComplement(currentPart.getSeq().substring(currentPart.getSeq().length() - currentPartRHomologyLength) + rightNeighborSeq.substring(0, rNeighborHomologyLength));

        oligos.add(forwardOligoSequence);
        oligos.add(reverseOligoSequence);
        
        return oligos;
    }
    
    /** Traverses graphs and looks for parts that are too small for homologous recombination and merges them with their neighbor(s) **/
    public void smallPartHomologyGraphMerge (ArrayList<RGraph> optimalGraphs) {
        
    }
}
