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
public class OverhangSequenceSelector {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String[] alphabet = {"a", "g", "c", "t"};
        ArrayList<String> sequences = SequenceGenerator.generateSequences(4, alphabet);
        for (int i = 0; i < sequences.size(); i++) {
            System.out.println((i + 1) + " " + sequences.get(i));
        }
        int[][] matrix = {{1, 2, 3, 4}, {2, 2, 3, 4}, {3, 2, 3, 4}, {4, 2, 3, 4}};
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j]+" ");
            }
            System.out.println();
        }


    }
}
