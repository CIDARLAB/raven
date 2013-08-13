/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.nonmodasm;

import Controller.algorithms.PrimerDesign;
import Controller.datastructures.Collector;
import Controller.datastructures.Part;
import Controller.datastructures.RNode;
import java.util.ArrayList;

/**
 *
 * @author evanappleton
 */
public class RHomologyPrimerDesign {
    
    public static ArrayList<String> homologousRecombinationPrimers(RNode node, ArrayList<RNode> l0Nodes, Collector coll, Double meltingTemp, Integer targetLength) {
        
        //initialize primer parameters
        ArrayList<String> oligos = new ArrayList<String>(2);
        String forwardOligoSequence;
        String reverseOligoSequence;
        String leftNeighborSeq;
        String rightNeighborSeq;
        Part currentPart = coll.getPart(node.getUUID(), true);
        int indexOf = l0Nodes.indexOf(node);
        
        //Get the seqeunces of the neighbors
        if (indexOf == 0) {
            RNode leftNeighborNode = l0Nodes.get(l0Nodes.size() - 1);
            leftNeighborSeq = coll.getPart(leftNeighborNode.getUUID(), true).getSeq();
            RNode rightNeighborNode = l0Nodes.get(1);
            rightNeighborSeq = coll.getPart(rightNeighborNode.getUUID(), true).getSeq();
        } else if (indexOf == l0Nodes.size() - 1) {
            RNode leftNeighborNode = l0Nodes.get(indexOf - 1);
            leftNeighborSeq = coll.getPart(leftNeighborNode.getUUID(), true).getSeq();
            RNode rightNeighborNode = l0Nodes.get(0);
            rightNeighborSeq = coll.getPart(rightNeighborNode.getUUID(), true).getSeq();
        } else {
            RNode leftNeighborNode = l0Nodes.get(indexOf - 1);
            leftNeighborSeq = coll.getPart(leftNeighborNode.getUUID(), true).getSeq();
            RNode rightNeighborNode = l0Nodes.get(indexOf + 1);
            rightNeighborSeq = coll.getPart(rightNeighborNode.getUUID(), true).getSeq();
        }
        
        int leftNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, leftNeighborSeq, false);
        int rightNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(rightNeighborSeq), false);
        int currentPartLeftHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, currentPart.getSeq(), true);
        int currentPartRightHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(currentPart.getSeq()), true);  
        
        forwardOligoSequence = leftNeighborSeq.substring(leftNeighborSeq.length() - leftNeighborHomologyLength) + currentPart.getSeq().substring(0, currentPartLeftHomologyLength);
        reverseOligoSequence = PrimerDesign.reverseComplement(currentPart.getSeq().substring(currentPart.getSeq().length() - currentPartRightHomologyLength) + rightNeighborSeq.substring(0, rightNeighborHomologyLength));

        oligos.add(forwardOligoSequence);
        oligos.add(reverseOligoSequence);
        
        return oligos;
    }
    
}
