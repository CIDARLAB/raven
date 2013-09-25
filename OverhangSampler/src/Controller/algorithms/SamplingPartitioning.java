/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author jenhan
 */
public class SamplingPartitioning {

    protected HashMap<Integer, ArrayList<int[]>> getPartitions(ArrayList<Integer> indices, int numBreaks) {
        //key: number of breaks that can be made in a part, value: arraylist of partitions
        for (int i = 1; i <= numBreaks; i++) {
            ArrayList<int[]> allChoices = new ArrayList();
            for (int j = 0; i <= indices.size(); j++) {
                ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
                int[] choices = new int[numBreaks];
                choices[0] = available.get(j);
                available.remove(j);
                choices = getPartitionsHelper(available, choices, numBreaks - 1);
                allChoices.add(choices);
            }
            partitions.put(i, allChoices);
        }
        return partitions;
    }
    HashMap<Integer, ArrayList<int[]>> partitions = new HashMap<Integer, ArrayList<int[]>>();

    private int[] getPartitionsHelper(ArrayList<Integer> indices, int[] choices, int numBreaks) {
        if (numBreaks == 0) {
            return choices;
        } else {
            for (int i = 1; i <= numBreaks; i++) {

                choices = getPartitionsHelper(indices, choices, numBreaks - 1);
            }
        }
        return null;
    }
}
