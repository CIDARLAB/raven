/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

/**
 *
 * @author evanappleton
 */
public class PrimerDesign {
    
    /**
     * ************************************************************************
     *
     * PRIMER DESIGN
     *
     *************************************************************************
     */
    
    public static String reverseComplement(String seq) {
        String revComplement = "";
        for (int i = 0; i < seq.length(); i++) {
            if (seq.charAt(i) == 'A') {
                revComplement = "T" + revComplement;
            } else if (seq.charAt(i) == 'a') {
                revComplement = "t" + revComplement;
            } else if (seq.charAt(i) == 'G') {
                revComplement = "C" + revComplement;
            } else if (seq.charAt(i) == 'g') {
                revComplement = "c" + revComplement;
            } else if (seq.charAt(i) == 'C') {
                revComplement = "G" + revComplement;
            } else if (seq.charAt(i) == 'c') {
                revComplement = "g" + revComplement;
            } else if (seq.charAt(i) == 'T') {
                revComplement = "A" + revComplement;
            } else if (seq.charAt(i) == 't') {
                revComplement = "a" + revComplement;
            } else {
                revComplement = "N" + revComplement;
            }
        }
        return revComplement;
    }
    
    //calculates the length of homology required for primers based on nearest neighbor calculations
    public static int getPrimerHomologyLength(Double meltingTemp, String sequence) {
        //TODO write actual code
        return 20;
    }
    
    //generates a random DNA sequence with input length
    public static String generateRandomSequence(int length) {
        //TODO write actual code
        return "agac";
    }
    
    /** Logic for going from OH variable place holders to actual sequences **/
    private void selectOHseqs() {
        
    }
    
}
