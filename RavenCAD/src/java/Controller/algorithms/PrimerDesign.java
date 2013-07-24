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
        String lSeq = seq.toLowerCase();
        String revComplement = "";
        for (int i = 0; i < lSeq.length(); i++) {
            if (lSeq.charAt(i) == 'a') {
                revComplement = "t" + revComplement;
            } else if (lSeq.charAt(i) == 'g') {
                revComplement = "c" + revComplement;
            } else if (lSeq.charAt(i) == 'c') {
                revComplement = "g" + revComplement;
            } else if (lSeq.charAt(i) == 't') {
                revComplement = "a" + revComplement;
            } else if (lSeq.charAt(i) == 'w') {
                revComplement = "w" + revComplement;
            } else if (lSeq.charAt(i) == 's') {
                revComplement = "s" + revComplement;
            } else if (lSeq.charAt(i) == 'm') {
                revComplement = "k" + revComplement;
            } else if (lSeq.charAt(i) == 'k') {
                revComplement = "m" + revComplement;
            } else if (lSeq.charAt(i) == 'r') {
                revComplement = "y" + revComplement;
            } else if (lSeq.charAt(i) == 'y') {
                revComplement = "r" + revComplement;
            } else if (lSeq.charAt(i) == 'b') {
                revComplement = "v" + revComplement;
            } else if (lSeq.charAt(i) == 'd') {
                revComplement = "h" + revComplement;
            } else if (lSeq.charAt(i) == 'h') {
                revComplement = "d" + revComplement;
            } else if (lSeq.charAt(i) == 'v') {
                revComplement = "b" + revComplement;
            } else {
                revComplement = "n" + revComplement;
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
