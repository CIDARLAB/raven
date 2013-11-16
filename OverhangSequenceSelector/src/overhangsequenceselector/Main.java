/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsequenceselector;

import java.util.ArrayList;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class Main {

//    /**
//     * @param args the command line arguments
//     */
    public static void main(String[] args) {
        String[] alphabet = {"a", "g", "c", "t"};
        String[] sequences = OverhangSequenceSelector.generateSequences(4, alphabet);
        int[][] scoreMatrix = OverhangSequenceSelector.buildScoreMatrix(sequences);
        System.out.print("    ");
        for (int i = 0; i < sequences.length; i++) {
            System.out.print(sequences[i] + " ");
        }
        System.out.println();
        String[] selected = {"ggag", "tact", "aatg", "aggt", "gctt", "cgct", "tgcc", "acta", "tcta", "cgac", "cgtt", "tgtg"};
        OverhangSequenceSelector.printMatrix(scoreMatrix, sequences);
        String[] selectSequences = OverhangSequenceSelector.selectSequences(256, sequences, scoreMatrix, selected);
        }
}
