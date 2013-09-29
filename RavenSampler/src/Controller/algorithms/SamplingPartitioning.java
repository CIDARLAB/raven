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

    public static HashMap<Integer, ArrayList<int[]>> getPartitions(ArrayList<Integer> indices, int numBreaks) {
        //key: number of breaks that can be made in a part, value: arraylist of partitions
        HashMap<Integer, ArrayList<int[]>> partitions = new HashMap<Integer, ArrayList<int[]>>();
        for (int i = 1; i <= numBreaks; i++) {
            ArrayList<int[]> allChoices = new ArrayList();
            ArrayList<Integer> choices = new ArrayList();
            while (choices != null) {
                choices = choices = new ArrayList();
                ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
                choices = getPartitionsHelper(available, choices, i );
                int[] choicesArray = new int[choices.size()];
                for (int k = 0; k < choices.size(); k++) {
                    choicesArray[k] = choices.get(k);
                }
                allChoices.add(choicesArray);
            }
            partitions.put(i, allChoices);
        }
        return partitions;
    }

    private static ArrayList<Integer> getPartitionsHelper(ArrayList<Integer> indices, ArrayList<Integer> choices, int numBreaks) {

        if (numBreaks == 0) {
            return choices;
        } else {
            for (int i = 0; i < indices.size(); i++) {
                ArrayList<Integer> toReturn = (ArrayList<Integer>) choices.clone();
                ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
                toReturn.add(indices.get(i));
                available.remove(i);
                toReturn = getPartitionsHelper(available, toReturn, numBreaks - 1);
                return toReturn;

            }

        }
        return null;
    }
}
