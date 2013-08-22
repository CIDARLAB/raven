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
        boolean missingLeftSequence = false;
        boolean missingSequence = false;
        boolean missingRightSequence = false;
        Part currentPart = coll.getPart(node.getUUID(), true);
        Part rootPart = coll.getPart(root.getUUID(), true);
        ArrayList<Part> composition = rootPart.getComposition();

        String currentSeq = currentPart.getSeq();
        ArrayList<String> direction = node.getDirection();
        if ("-".equals(direction.get(0))) {
            currentSeq = PrimerDesign.reverseComplement(currentSeq);
        }
        if (currentSeq.equals("")) {
            missingSequence = true;
        }
        int indexOf = composition.indexOf(currentPart);

        //Keep getting the sequences of the leftmost neighbors until the min sequence size is satisfied
        while (leftNeighborSeqLength < targetLength) {
            if (indexOf == 0) {
                Part leftNeighbor = composition.get(composition.size() - 1);
                String seq = leftNeighbor.getSeq();
                if (seq.equals("")) {
                    missingLeftSequence = true;
                    break;
                }
                ArrayList<String> leftNeighborDirection = leftNeighbor.getDirections();
                if ("-".equals(leftNeighborDirection.get(0))) {
                    seq = PrimerDesign.reverseComplement(seq);
                }

                leftNeighborSeq = leftNeighborSeq + seq;
                leftNeighborSeqLength = leftNeighborSeq.length();
                indexOf = composition.size() - 1;
            } else {
                Part leftNeighbor = composition.get(indexOf - 1);
                String seq = leftNeighbor.getSeq();
                if (seq.equals("")) {
                    missingLeftSequence = true;
                    break;
                }
                ArrayList<String> leftNeighborDirection = leftNeighbor.getDirections();
                if ("-".equals(leftNeighborDirection.get(0))) {
                    seq = PrimerDesign.reverseComplement(seq);
                }

                leftNeighborSeq = leftNeighborSeq + seq;
                leftNeighborSeqLength = leftNeighborSeq.length();
                indexOf--;
            }
        }

        //Keep getting the sequences of the righttmost neighbors until the min sequence size is satisfied
        while (rightNeighborSeqLength < targetLength) {
            if (indexOf == composition.size() - 1) {
                Part rightNeighbor = composition.get(0);
                String seq = rightNeighbor.getSeq();
                ArrayList<String> leftNeighborDirection = rightNeighbor.getDirections();
                if (seq.equals("")) {
                    missingRightSequence = true;
                    break;
                }
                if ("-".equals(leftNeighborDirection.get(0))) {
                    seq = PrimerDesign.reverseComplement(seq);
                }

                rightNeighborSeq = rightNeighborSeq + seq;
                rightNeighborSeqLength = rightNeighborSeq.length();
                indexOf = 0;
            } else {
                Part rightNeighbor = composition.get(indexOf + 1);
                String seq = rightNeighbor.getSeq();
                ArrayList<String> leftNeighborDirection = rightNeighbor.getDirections();
                if (seq.equals("")) {
                    missingRightSequence = true;
                    break;
                }
                if ("-".equals(leftNeighborDirection.get(0))) {
                    seq = PrimerDesign.reverseComplement(seq);
                }

                rightNeighborSeq = rightNeighborSeq + seq;
                rightNeighborSeqLength = rightNeighborSeq.length();
                indexOf++;
            }
        }

        int lNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, leftNeighborSeq, false, true);
        int rNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(rightNeighborSeq), false, true);
        int currentPartLHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, currentSeq, true, true);
        int currentPartRHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(currentSeq), true, true);

        //If the homology of this part is the full length of this part, return blank oligos... other longer oligos will cover this span
        if (currentPartLHomologyLength == currentPart.getSeq().length() || currentPartRHomologyLength == currentPart.getSeq().length()) {
            return oligos;
        }
        if (missingSequence || missingLeftSequence || missingRightSequence) {
            forwardOligoSequence = "[LEFT NEIGHBOR HOMOLOGY][" + currentPart.getName() + " HOMOLOGY][RIGHT NEIGHBOR HOMOLOGY]";
            reverseOligoSequence = "[RIGHT NEIGHBOR HOMOLOGY][" + currentPart.getName() + " HOMOLOGY][LEFT NEIGHBOR HOMOLOGY]";
        } else {
            forwardOligoSequence = leftNeighborSeq.substring(Math.max(0, leftNeighborSeq.length() - lNeighborHomologyLength)) + currentSeq.substring(0, Math.min(currentSeq.length(), currentPartLHomologyLength));
            reverseOligoSequence = PrimerDesign.reverseComplement(currentSeq.substring(Math.max(0, currentSeq.length() - currentPartRHomologyLength)) + rightNeighborSeq.substring(0, Math.min(rightNeighborSeq.length(), rNeighborHomologyLength)));
        }


        oligos.add(forwardOligoSequence);
        oligos.add(reverseOligoSequence);

        return oligos;
    }

    /**
     * Traverses graphs and looks for parts that are too small for homologous
     * recombination and merges them with their neighbor(s) *
     */
    public void smallPartHomologyGraphMerge(ArrayList<RGraph> optimalGraphs) {
    }
}
