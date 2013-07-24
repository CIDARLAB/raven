/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsequenceselector;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class SequenceGenerator {
    //given a target length, generate all possible strings using given alphabet of target length

    public static ArrayList<String> generateSequences(int length, String[] alphabet) {
        if (length < 1) {
            return null;
        }
        int currentLength = 1;
        ArrayList<String> currentSequences = new ArrayList(Arrays.asList(alphabet));
        while (currentLength < length) {
            ArrayList<String> newSequences = new ArrayList();
            for (String seq : currentSequences) {
                for (int i = 0; i < alphabet.length; i++) {
                    newSequences.add(seq + alphabet[i]);
                }
            }
            currentSequences = newSequences;
            currentLength++;
        }
        return currentSequences;
    }

    public static int[][] buildScoreMatrix(ArrayList<String> sequences) {


        return null;
    }

    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }
}
