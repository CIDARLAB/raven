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

//    /**
//     * @param args the command line arguments
//     */
    public static void main(String[] args) {
        // TODO code application logic here
        String[] alphabet = {"a", "g", "c", "t"};
        ArrayList<String> sequences = SequenceGenerator.generateSequences(4, alphabet);
        for (int i = 0; i < sequences.size(); i++) {
            System.out.println((i + 1) + " " + sequences.get(i));
        }
        SequenceGenerator.scoreAlignment("CACGTTTCTTGTGGCAGCTTAAGTTTGAATGTCATTTCTTCAATGGGACGGA", "ACGAGTGCGTGTTTTCCCGCCTGGTCCCCAGGCCCCCTTTCCGTCCTCAGGAA");
        
        
        
    }



}
