/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author jenhan
 */
public class SamplingPartitioning {

    public static HashMap<Integer, HashSet<int[]>> partitions = new HashMap();

    public static HashMap<Integer, ArrayList<int[]>> getPartitions(ArrayList<Integer> indices, int maxNumBreaks) {
        //key: number of breaks that can be made in a part, value: arraylist of partitions

        int numBreaks = 1 + (int) (Math.random() * ((maxNumBreaks - 1) + 1));
        int[] partition = new int[numBreaks];
        ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
        ArrayList<Integer> chosen = new ArrayList();
        Collections.shuffle(available);
        
        for(int i=0;i<numBreaks;i++) {
            chosen.add(available.get(i));
        }
        Collections.sort(chosen);
        for(int i=0;i<chosen.size();i++) {
            partition[i]=chosen.get(i);
        }
        HashMap<Integer,ArrayList<int[]>> toReturn = new HashMap();
        toReturn.put(numBreaks, new ArrayList());
        toReturn.get(numBreaks).add(partition);
        return toReturn;
    }

    public static int[] getPartitionOld(ArrayList<Integer> indices, int maxNumBreaks) {
        //key: number of breaks that can be made in a part, value: arraylist of partitions
        for (int numBreaks = 1; numBreaks <= maxNumBreaks; numBreaks++) {
            partitions.put(numBreaks, new HashSet());
            for (int j = 0; j < indices.size(); j++) {
                ArrayList<Integer> choices = new ArrayList();
                choices.add(indices.get(j));
                ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
                available.remove(j);
                getPartitionsHelper(available, choices, numBreaks, numBreaks - 1);
            }

        }
        int index = 1 + (int) (Math.random() * ((maxNumBreaks - 1) + 1));
        int index2 = 0 + (int) (Math.random() * ((partitions.get(index).size() - 0) + 1));
        ArrayList<int[]> arrayList = new ArrayList(partitions.get(index));
        return arrayList.get(index2);
    }

    public static HashMap<Integer, ArrayList<int[]>> getPartitionsOld(ArrayList<Integer> indices, int maxNumBreaks) {
        //key: number of breaks that can be made in a part, value: arraylist of partitions
        for (int numBreaks = 1; numBreaks <= maxNumBreaks; numBreaks++) {
            partitions.put(numBreaks, new HashSet());
            for (int j = 0; j < indices.size(); j++) {
                ArrayList<Integer> choices = new ArrayList();
                choices.add(indices.get(j));
                ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
                available.remove(j);
                getPartitionsHelper(available, choices, numBreaks, numBreaks - 1);
            }

        }
        int index = 1 + (int) (Math.random() * ((maxNumBreaks - 1) + 1));
        int index2 = 0 + (int) (Math.random() * ((partitions.get(index).size() - 1) + 1));
        ArrayList<int[]> arrayList = new ArrayList(partitions.get(index));
        HashMap<Integer, ArrayList<int[]>> toReturn = new HashMap();
        toReturn.put(index, new ArrayList());
        toReturn.get(index).add(arrayList.get(index2));
        return toReturn;
    }

    private static void getPartitionsHelper(ArrayList<Integer> indices, ArrayList<Integer> choices, int maxNumBreaks, int targetSize) {
        if (targetSize == 0) {
            Collections.sort(choices);
            int[] partition = new int[choices.size()];
            for (int i = 0; i < choices.size(); i++) {
                partition[i] = choices.get(i);
            }
            partitions.get(maxNumBreaks).add(partition);
        } else {
            for (int j = 0; j < indices.size(); j++) {
                ArrayList<Integer> currentChoices = (ArrayList<Integer>) choices.clone();
                currentChoices.add(indices.get(j));
                ArrayList<Integer> available = (ArrayList<Integer>) indices.clone();
                available.remove(j);
                getPartitionsHelper(available, currentChoices, maxNumBreaks, targetSize - 1);
            }
        }
    }
}
