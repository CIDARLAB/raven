/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.core;

import java.util.HashMap;

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
//        String lSeq = seq.toLowerCase();
        String lSeq = seq;
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
            } else if (lSeq.charAt(i) == 'n') {
                revComplement = "n" + revComplement;
            } else if (lSeq.charAt(i) == 'A') {
                revComplement = "T" + revComplement;
            } else if (lSeq.charAt(i) == 'G') {
                revComplement = "C" + revComplement;
            } else if (lSeq.charAt(i) == 'C') {
                revComplement = "G" + revComplement;
            } else if (lSeq.charAt(i) == 'T') {
                revComplement = "A" + revComplement;
            } else if (lSeq.charAt(i) == 'W') {
                revComplement = "W" + revComplement;
            } else if (lSeq.charAt(i) == 'S') {
                revComplement = "S" + revComplement;
            } else if (lSeq.charAt(i) == 'M') {
                revComplement = "K" + revComplement;
            } else if (lSeq.charAt(i) == 'K') {
                revComplement = "M" + revComplement;
            } else if (lSeq.charAt(i) == 'R') {
                revComplement = "Y" + revComplement;
            } else if (lSeq.charAt(i) == 'Y') {
                revComplement = "R" + revComplement;
            } else if (lSeq.charAt(i) == 'B') {
                revComplement = "V" + revComplement;
            } else if (lSeq.charAt(i) == 'D') {
                revComplement = "H" + revComplement;
            } else if (lSeq.charAt(i) == 'H') {
                revComplement = "D" + revComplement;
            } else if (lSeq.charAt(i) == 'V') {
                revComplement = "B" + revComplement;
            } else {
                revComplement = "N" + revComplement;
            }
        }
        return revComplement;
    }

    //calculates the length of homology required for primers based on nearest neighbor calculations
    public static int getPrimerHomologyLength(Double meltingTemp, Integer targetLength, Integer maxHomolLength, Integer minHomolLength, String sequence, boolean fivePrime) {

        //If the sequence length is under this length, return the whole sequence length
        if (sequence.length() < targetLength) {
            return sequence.length();
        }
        
        int length = targetLength;
        
        //If determining the homology of the five prime side of a part
        if (fivePrime) {

            String candidateSeq = sequence.substring(0, length);
            double candidateTemp = getMeltingTemp(candidateSeq);
            
            //Add base pairs until candidate temp reaches the desired temp if too low
            if (candidateTemp < meltingTemp) {
                while (candidateTemp < meltingTemp) {
                    length++;
                    
                    //If the sequence length is under this length, return the whole sequence length
                    if (sequence.length() < length) {
                        return sequence.length();
                    }
                    
                    //If the sequence is becoming longer than max, return this length
                    if (length >= maxHomolLength) {
                        return length;
                    }
                    
                    candidateSeq = sequence.substring(0, length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }

            //Remove base pairs until candidate temp reaches the desired temp if too high
            } else if (candidateTemp > meltingTemp) {
                while (candidateTemp > meltingTemp) {
                    
                    //If the length is less than minimum, return it
                    if (length <= minHomolLength) {
                        return targetLength;
                    }

                    length--;
                    candidateSeq = sequence.substring(0, length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }
            }

        //If determining the homology length of the three prime side
        } else {

            String candidateSeq = sequence.substring(sequence.length() - length);
            double candidateTemp = getMeltingTemp(candidateSeq);

            //Add base pairs until candidate temp reaches the desired temp if too low
            if (candidateTemp < meltingTemp) {
                
                while (candidateTemp < meltingTemp) {
                    length++;
                    
                    //If the sequence length is under this length, return the whole sequence length
                    if (sequence.length() < length) {
                        return sequence.length();
                    }
                    
                    //If the sequence is becoming longer than max, return this length
                    if (length >= maxHomolLength) {
                        return length;
                    }
                    
                    candidateSeq = sequence.substring(sequence.length() - length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }

            //Remove base pairs until candidate temp reaches the desired temp if too high
            } else if (candidateTemp > meltingTemp) {

                //Call the melting temperature function
                while (candidateTemp > meltingTemp) {
                    
                    //If the length is less than minimum, return it
                    if (length <= minHomolLength) {
                        return targetLength;
                    }
                    
                    length--;
                    candidateSeq = sequence.substring(sequence.length() - length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }
            }
        }
        
        //Extra check to make sure no indexing errors occur
        if (sequence.length() > length) {
            return length;
        } else {
            length = sequence.length();
        }
        return length;
    }

    /**
     * Logic for going from OH variable place holders to actual sequences for MoClo *
     */
    public static HashMap<String, String> getMoCloOHseqs() {

        HashMap<String, String> overhangVariableSequenceHash = new HashMap<String, String>();
        overhangVariableSequenceHash.put("0", "ggag");
        overhangVariableSequenceHash.put("1", "tact");
        overhangVariableSequenceHash.put("2", "aatg");
        overhangVariableSequenceHash.put("3", "aggt");
        overhangVariableSequenceHash.put("4", "gctt");
        overhangVariableSequenceHash.put("5", "cgct");
        overhangVariableSequenceHash.put("6", "tgcc");
        overhangVariableSequenceHash.put("7", "acta");
        overhangVariableSequenceHash.put("8", "tcta");
        overhangVariableSequenceHash.put("9", "cgac");
        overhangVariableSequenceHash.put("10", "cgtt");
        overhangVariableSequenceHash.put("11", "tgtg");
        overhangVariableSequenceHash.put("12", "atgc");
        overhangVariableSequenceHash.put("13", "gtca");
        overhangVariableSequenceHash.put("14", "gaac");
        overhangVariableSequenceHash.put("15", "ctga");
        overhangVariableSequenceHash.put("16", "acag");
        overhangVariableSequenceHash.put("17", "tagc");
        overhangVariableSequenceHash.put("18", "atcg");
        overhangVariableSequenceHash.put("19", "cagt");
        overhangVariableSequenceHash.put("20", "gcaa");
        overhangVariableSequenceHash.put("21", "cggc");
        overhangVariableSequenceHash.put("22", "aaga");
        overhangVariableSequenceHash.put("23", "tacg");
        overhangVariableSequenceHash.put("24", "ctaa");
        overhangVariableSequenceHash.put("25", "gtgc");
        overhangVariableSequenceHash.put("26", "gact");
        overhangVariableSequenceHash.put("27", "actc");
        overhangVariableSequenceHash.put("28", "ctag");
        overhangVariableSequenceHash.put("29", "tcga");
        overhangVariableSequenceHash.put("30", "aagt");
        overhangVariableSequenceHash.put("31", "gtac");
        overhangVariableSequenceHash.put("32", "tgca");
        overhangVariableSequenceHash.put("33", "cctg");
        overhangVariableSequenceHash.put("34", "agct");
        overhangVariableSequenceHash.put("35", "gatc");
        overhangVariableSequenceHash.put("36", "ccag");
        overhangVariableSequenceHash.put("37", "ttga");
        overhangVariableSequenceHash.put("38", "acgt");
        overhangVariableSequenceHash.put("39", "catg");
        overhangVariableSequenceHash.put("40", "ttac");
        overhangVariableSequenceHash.put("41", "agca");
        overhangVariableSequenceHash.put("42", "gaat");
        overhangVariableSequenceHash.put("43", "ccta");
        overhangVariableSequenceHash.put("44", "gtcc");
        overhangVariableSequenceHash.put("45", "tgag");
        overhangVariableSequenceHash.put("46", "aggc");
        overhangVariableSequenceHash.put("47", "cact");
        overhangVariableSequenceHash.put("48", "gcga");
        overhangVariableSequenceHash.put("49", "attg");
        overhangVariableSequenceHash.put("50", "taac");
        overhangVariableSequenceHash.put("51", "ggct");
        overhangVariableSequenceHash.put("52", "ccga");
        overhangVariableSequenceHash.put("53", "tatg");
        overhangVariableSequenceHash.put("54", "atac");
        overhangVariableSequenceHash.put("55", "gcgt");
        overhangVariableSequenceHash.put("56", "cgca");
        overhangVariableSequenceHash.put("57", "tctg");
        overhangVariableSequenceHash.put("58", "atag");
        overhangVariableSequenceHash.put("59", "catc");
        overhangVariableSequenceHash.put("60", "gtga");
        overhangVariableSequenceHash.put("61", "tgat");
        overhangVariableSequenceHash.put("62", "aact");
        overhangVariableSequenceHash.put("63", "ggtc");
        overhangVariableSequenceHash.put("64", "ctcg");
        overhangVariableSequenceHash.put("65", "acga");
        overhangVariableSequenceHash.put("66", "taca");
        overhangVariableSequenceHash.put("67", "gcag");
        overhangVariableSequenceHash.put("68", "cggt");
        overhangVariableSequenceHash.put("69", "tatc");
        overhangVariableSequenceHash.put("70", "atga");
        overhangVariableSequenceHash.put("71", "ggat");
        overhangVariableSequenceHash.put("72", "cttg");
        overhangVariableSequenceHash.put("73", "tacc");
        overhangVariableSequenceHash.put("74", "acgg");
        overhangVariableSequenceHash.put("75", "gctc");
        overhangVariableSequenceHash.put("76", "cgaa");
        overhangVariableSequenceHash.put("77", "atct");
        overhangVariableSequenceHash.put("78", "taag");
        overhangVariableSequenceHash.put("79", "gcca");
        overhangVariableSequenceHash.put("80", "cgtc");
        overhangVariableSequenceHash.put("81", "atgt");
        overhangVariableSequenceHash.put("82", "tcca");
        overhangVariableSequenceHash.put("83", "cagc");
        overhangVariableSequenceHash.put("84", "gtag");
        overhangVariableSequenceHash.put("85", "tgtc");
        overhangVariableSequenceHash.put("86", "ccat");
        overhangVariableSequenceHash.put("87", "gatt");
        overhangVariableSequenceHash.put("88", "ttca");
        overhangVariableSequenceHash.put("89", "agac");
        overhangVariableSequenceHash.put("90", "cacg");
        overhangVariableSequenceHash.put("91", "gggt");
        overhangVariableSequenceHash.put("92", "atta");
        overhangVariableSequenceHash.put("93", "tccg");
        overhangVariableSequenceHash.put("94", "gggc");
        overhangVariableSequenceHash.put("95", "acaa");
        overhangVariableSequenceHash.put("96", "cttt");
        overhangVariableSequenceHash.put("97", "aagg");
        overhangVariableSequenceHash.put("98", "tatt");
        overhangVariableSequenceHash.put("99", "ggcg");
        overhangVariableSequenceHash.put("100", "ccaa");
        overhangVariableSequenceHash.put("101", "ttcc");
        overhangVariableSequenceHash.put("102", "tggt");
        overhangVariableSequenceHash.put("103", "ccac");
        overhangVariableSequenceHash.put("104", "gttg");
        overhangVariableSequenceHash.put("105", "aaac");
        overhangVariableSequenceHash.put("106", "ggtg");
        overhangVariableSequenceHash.put("107", "ccca");
        overhangVariableSequenceHash.put("108", "gtgt");
        overhangVariableSequenceHash.put("109", "taaa");
        overhangVariableSequenceHash.put("110", "cggg");
        overhangVariableSequenceHash.put("111", "attt");
        overhangVariableSequenceHash.put("112", "tccc");
        overhangVariableSequenceHash.put("113", "agaa");
        overhangVariableSequenceHash.put("114", "ccct");
        overhangVariableSequenceHash.put("115", "gagg");
        overhangVariableSequenceHash.put("116", "tttc");
        overhangVariableSequenceHash.put("117", "agag");
        overhangVariableSequenceHash.put("118", "cttc");
        overhangVariableSequenceHash.put("119", "gaga");
        overhangVariableSequenceHash.put("120", "tcct");
        overhangVariableSequenceHash.put("121", "gcgg");
        overhangVariableSequenceHash.put("122", "ataa");
        overhangVariableSequenceHash.put("123", "tgtt");
        overhangVariableSequenceHash.put("124", "aacc");
        overhangVariableSequenceHash.put("125", "tttg");
        overhangVariableSequenceHash.put("126", "aatt");
        overhangVariableSequenceHash.put("127", "ggcc");
        
        overhangVariableSequenceHash.put("0*", "ctcc");
        overhangVariableSequenceHash.put("1*", "agta");
        overhangVariableSequenceHash.put("2*", "catt");
        overhangVariableSequenceHash.put("3*", "acct");
        overhangVariableSequenceHash.put("4*", "aagc");
        overhangVariableSequenceHash.put("5*", "agcg");
        overhangVariableSequenceHash.put("6*", "ggca");
        overhangVariableSequenceHash.put("7*", "tagt");
        overhangVariableSequenceHash.put("8*", "taga");
        overhangVariableSequenceHash.put("9*", "gtcg");
        overhangVariableSequenceHash.put("10*", "aacg");
        overhangVariableSequenceHash.put("11*", "caca");
        overhangVariableSequenceHash.put("12*", "gcat");
        overhangVariableSequenceHash.put("13*", "tgac");
        overhangVariableSequenceHash.put("14*", "gtcc");
        overhangVariableSequenceHash.put("15*", "tcag");
        overhangVariableSequenceHash.put("16*", "ctgt");
        overhangVariableSequenceHash.put("17*", "gcta");
        overhangVariableSequenceHash.put("18*", "cgat");
        overhangVariableSequenceHash.put("19*", "actg");
        overhangVariableSequenceHash.put("20*", "ttgc");
        overhangVariableSequenceHash.put("21*", "gccg");
        overhangVariableSequenceHash.put("22*", "tctt");
        overhangVariableSequenceHash.put("23*", "cgta");
        overhangVariableSequenceHash.put("24*", "ttag");
        overhangVariableSequenceHash.put("25*", "gcac");
        overhangVariableSequenceHash.put("26*", "agtc");
        overhangVariableSequenceHash.put("27*", "gagt");
//        overhangVariableSequenceHash.put("28*", "ctag");
//        overhangVariableSequenceHash.put("29*", "tcga");
        overhangVariableSequenceHash.put("30*", "actt");
//        overhangVariableSequenceHash.put("31*", "gtac");
//        overhangVariableSequenceHash.put("32*", "tgca");
        overhangVariableSequenceHash.put("33*", "cagg");
//        overhangVariableSequenceHash.put("34*", "agct");
//        overhangVariableSequenceHash.put("35*", "gatc");
        overhangVariableSequenceHash.put("36*", "ctgg");
        overhangVariableSequenceHash.put("37*", "tcaa");
//        overhangVariableSequenceHash.put("38*", "acgt");
//        overhangVariableSequenceHash.put("39*", "catg");
        overhangVariableSequenceHash.put("40*", "gtaa");
        overhangVariableSequenceHash.put("41*", "tgct");
        overhangVariableSequenceHash.put("42*", "attc");
        overhangVariableSequenceHash.put("43*", "tagg");
        overhangVariableSequenceHash.put("44*", "ggac");
        overhangVariableSequenceHash.put("45*", "ctca");
        overhangVariableSequenceHash.put("46*", "gcct");
        overhangVariableSequenceHash.put("47*", "agtg");
        overhangVariableSequenceHash.put("48*", "tcgc");
        overhangVariableSequenceHash.put("49*", "caat");
        overhangVariableSequenceHash.put("50*", "gtta");
        overhangVariableSequenceHash.put("51*", "agcc");
        overhangVariableSequenceHash.put("52*", "tcgg");
        overhangVariableSequenceHash.put("53*", "cata");
        overhangVariableSequenceHash.put("54*", "gtat");
        overhangVariableSequenceHash.put("55*", "acgc");
        overhangVariableSequenceHash.put("56*", "tgcg");
        overhangVariableSequenceHash.put("57*", "caga");
        overhangVariableSequenceHash.put("58*", "ctat");
        overhangVariableSequenceHash.put("59*", "gatg");
        overhangVariableSequenceHash.put("60*", "tcac");
        overhangVariableSequenceHash.put("61*", "atca");
        overhangVariableSequenceHash.put("62*", "agtt");
        overhangVariableSequenceHash.put("63*", "gacc");
        overhangVariableSequenceHash.put("64*", "cgag");
        overhangVariableSequenceHash.put("65*", "tcgt");
        overhangVariableSequenceHash.put("66*", "tgta");
        overhangVariableSequenceHash.put("67*", "ctgc");
        overhangVariableSequenceHash.put("68*", "accg");
        overhangVariableSequenceHash.put("69*", "gata");
        overhangVariableSequenceHash.put("70*", "tcat");
        overhangVariableSequenceHash.put("71*", "atcc");
        overhangVariableSequenceHash.put("72*", "caag");
        overhangVariableSequenceHash.put("73*", "ggta");
        overhangVariableSequenceHash.put("74*", "ccgt");
        overhangVariableSequenceHash.put("75*", "gagc");
        overhangVariableSequenceHash.put("76*", "ttcg");
        overhangVariableSequenceHash.put("77*", "agat");
        overhangVariableSequenceHash.put("78*", "ctta");
        overhangVariableSequenceHash.put("79*", "tggc");
        overhangVariableSequenceHash.put("80*", "gacg");
        overhangVariableSequenceHash.put("81*", "acat");
        overhangVariableSequenceHash.put("82*", "tgga");
        overhangVariableSequenceHash.put("83*", "gctg");
        overhangVariableSequenceHash.put("84*", "ctac");
        overhangVariableSequenceHash.put("85*", "gaca");
        overhangVariableSequenceHash.put("86*", "atgg");
        overhangVariableSequenceHash.put("87*", "aatc");
        overhangVariableSequenceHash.put("88*", "tgaa");
        overhangVariableSequenceHash.put("89*", "gtct");
        overhangVariableSequenceHash.put("90*", "cgtg");
        overhangVariableSequenceHash.put("91*", "accc");
        overhangVariableSequenceHash.put("92*", "taat");
        overhangVariableSequenceHash.put("93*", "cgga");
        overhangVariableSequenceHash.put("94*", "gccc");
        overhangVariableSequenceHash.put("95*", "ttgt");
        overhangVariableSequenceHash.put("96*", "aaag");
        overhangVariableSequenceHash.put("97*", "cctt");
        overhangVariableSequenceHash.put("98*", "aata");
        overhangVariableSequenceHash.put("99*", "cgcc");
        overhangVariableSequenceHash.put("100*", "ttgg");
        overhangVariableSequenceHash.put("101*", "ggaa");
        overhangVariableSequenceHash.put("102*", "acca");
        overhangVariableSequenceHash.put("103*", "gtgg");
        overhangVariableSequenceHash.put("104*", "caac");
        overhangVariableSequenceHash.put("105*", "gttt");
        overhangVariableSequenceHash.put("106*", "cacc");
        overhangVariableSequenceHash.put("107*", "tggg");
        overhangVariableSequenceHash.put("108*", "acac");
        overhangVariableSequenceHash.put("109*", "ttta");
        overhangVariableSequenceHash.put("110*", "cccg");
        overhangVariableSequenceHash.put("111*", "aaat");
        overhangVariableSequenceHash.put("112*", "ggga");
        overhangVariableSequenceHash.put("113*", "ttct");
        overhangVariableSequenceHash.put("114*", "aggg");
        overhangVariableSequenceHash.put("115*", "cctc");
        overhangVariableSequenceHash.put("116*", "gaaa");
        overhangVariableSequenceHash.put("117*", "ctct");
        overhangVariableSequenceHash.put("118*", "gaag");
        overhangVariableSequenceHash.put("119*", "tctc");
        overhangVariableSequenceHash.put("120*", "agga");
        overhangVariableSequenceHash.put("121*", "ccgc");
        overhangVariableSequenceHash.put("122*", "ttat");
        overhangVariableSequenceHash.put("123*", "aaca");
        overhangVariableSequenceHash.put("124*", "ggtt");
        overhangVariableSequenceHash.put("125*", "caaa");
        overhangVariableSequenceHash.put("126*", "aatt");
        overhangVariableSequenceHash.put("127*", "ggcc");
        return overhangVariableSequenceHash;
    }
    
    public static HashMap<String, String> getGatewayGibsonOHseqs() {
        
        HashMap<String, String> overhangVariableSequenceHash = new HashMap<String, String>();

        overhangVariableSequenceHash.put("UNS1", "ggtttaccgagctcttattggttttcaaacttcattgactgtgcc");
        overhangVariableSequenceHash.put("UNS2", "ggtgcgtttttatgcttgtagtattgtataatgtttttaagatcc");
        overhangVariableSequenceHash.put("UNS3", "ggtctaatacccaatctctcgtcttatccagatgttttatacgcc");
        overhangVariableSequenceHash.put("UNS4", "ggtgaattcccttatgtgagtgtaaaaggcaggcgagtttgtccc");
        overhangVariableSequenceHash.put("UNS5", "ggttgcttgcaaaagcagtaattggaaagcactctcaaagaatcc");
        overhangVariableSequenceHash.put("UNS6", "ggtagataagttgatttagccataaaatattgtttccgtgacccc");
        overhangVariableSequenceHash.put("UNS7", "ggttctgagtcacggcttcattggcattccgtacaacgaacgtcc");
        overhangVariableSequenceHash.put("UNS8", "ggtcctcagagagcctatagcggtaaaacaacaccatgcatcccc");
        overhangVariableSequenceHash.put("UNS9", "ggtcgcagtcgcttcctgtaaatagagcattaaattccatagtcc");
        overhangVariableSequenceHash.put("UNS10", "ggtacttaatcgaaaaaaaaacagacagccatgtgctcttcggcc");
        overhangVariableSequenceHash.put("UNS11", "ggtggacatgattatggaacacacacacgctagccgcccagttcc");
        overhangVariableSequenceHash.put("UNSX", "ggtatcactagtattacagaggtaagttataacagtcgcctaacc");
        
        overhangVariableSequenceHash.put("UNS1*", "ggcacagtcaatgaagtttgaaaaccaataagagctcggtaaacc");
        overhangVariableSequenceHash.put("UNS2*", "ggatcttaaaaacattatacaatactacaagcataaaaacgcacc");
        overhangVariableSequenceHash.put("UNS3*", "ggcgtataaaacatctggataagacgagagattgggtattagacc");
        overhangVariableSequenceHash.put("UNS4*", "gggacaaactcgcctgccttttacactcacataagggaattcacc");
        overhangVariableSequenceHash.put("UNS5*", "ggattctttgagagtgctttccaattactgcttttgcaagcaacc");
        overhangVariableSequenceHash.put("UNS6*", "ggggtcacggaaacaatattttatggctaaatcaacttatctacc");
        overhangVariableSequenceHash.put("UNS7*", "ggacgttcgttgtacggaatgccaatgaagccgtgactcagaacc");
        overhangVariableSequenceHash.put("UNS8*", "ggggatgcatggtgttgttttaccgctataggctctctgaggacc");
        overhangVariableSequenceHash.put("UNS9*", "ggactatggaatttaatgctctatttacaggaagcgactgcgacc");
        overhangVariableSequenceHash.put("UNS10*", "ggccgaagagcacatggctgtctgtttttttttcgattaagtacc");
        overhangVariableSequenceHash.put("UNS11*", "ggaactgggcggctagcgtgtgtgtgttccataatcatgtccacc");
        
        overhangVariableSequenceHash.put("attB4", "caactttgtatagaaaagttg");
        overhangVariableSequenceHash.put("attB1", "caagtttgtacaaaaaagcag");
        overhangVariableSequenceHash.put("attB2", "ccactttgtacaagaaagctg");
        overhangVariableSequenceHash.put("attR1", "tcgggccccaaataatgattttattttgactgatagtgacctgttcgttgcaacaaattgatgagcaatgcttttttataatgccaactttgtacaaaaaagcaggctga");
        overhangVariableSequenceHash.put("attR2", "tacccagctttcttgtacaaagttggcattataagaaagcattgcttatcaatttgttgcaacgaacaggtcactatcagtcaaaataaaatcattatttg");
        overhangVariableSequenceHash.put("attR4", "caaataatgattttattttgactgatagtgacctgttcgttgcaacaaattgataagcaatgcttttttataatgccaactttgtatagaaaagttg");
        overhangVariableSequenceHash.put("attL1", "caagtttgtacaaaaaagttgaacgagaaacgtaaaatgatataaatatcaatatattaaattagattttgcataaaaaacagactacataatactgtaaaacacaacatatgcagtcactatga");
        overhangVariableSequenceHash.put("attL2", "caactttgtatagaaaagttgaacgagaaacgtaaaatgatataaatatcaatatattaaattagattttgcataaaaaacagactacataatactgtaaaacacaacatatccagtcactatgg");
        overhangVariableSequenceHash.put("attL4", "catagtgactggatatgttgtgttttacagtattatgtagtctgttttttatgcaaaatctaatttaatatattgatatttatatcattttacgtttctcgttcagctttcttgtacaaagtggt");
        
        return overhangVariableSequenceHash;
        
    }

    public static double getMeltingTemp(String sequence) {

        /* Resources:
         * http://en.wikipedia.org/wiki/DNA_melting#Nearest-neighbor_method
         * http://www.basic.northwestern.edu/biotools/oligocalc.html
         * http://dna.bio.puc.cl/cardex/servers/dnaMATE/tm-pred.html
         */

        String seq = sequence;
        int length = sequence.length();
        double concP = 10 * java.lang.Math.pow(10, -9);
        double dH = 0;
        double dS = 0;
        double R = 1.987;
        double temp;
        String pair;
        seq = seq.toUpperCase();

        // Checks terminal base pairs
        char init = seq.charAt(0);
        if (init == 'G' || init == 'C') {
            dH += 0.1;
            dS += -2.8;
        } else if (init == 'A' || init == 'T') {
            dH += 2.3;
            dS += 4.1;
        }
        init = seq.charAt(length - 1);
        if (init == 'G' || init == 'C') {
            dH += 0.1;
            dS += -2.8;
        } else if (init == 'A' || init == 'T') {
            dH += 2.3;
            dS += 4.1;
        }

        // Checks nearest neighbor pairs
        for (int i = 0; i < length - 1; i++) {
            pair = seq.substring(i, i + 2);
            if (pair.equals("AA") || pair.equals("TT")) {
                dH += -7.9;
                dS += -22.2;
            } else if (pair.equals("AG") || pair.equals("CT")) {
                dH += -7.8;
                dS += -21.0;
            } else if (pair.equals("AT")) {
                dH += -7.2;
                dS += -20.4;
            } else if (pair.equals("AC") || pair.equals("GT")) {
                dH += -8.4;
                dS += -22.4;
            } else if (pair.equals("GA") || pair.equals("TC")) {
                dH += -8.2;
                dS += -22.2;
            } else if (pair.equals("GG") || pair.equals("CC")) {
                dH += -8.0;
                dS += -19.9;
            } else if (pair.equals("GC")) {
                dH += -9.8;
                dS += -24.4;
            } else if (pair.equals("TA")) {
                dH += -7.2;
                dS += -21.3;
            } else if (pair.equals("TG") || pair.equals("CA")) {
                dH += -8.5;
                dS += -22.7;
            } else if (pair.equals("CG")) {
                dH += -10.6;
                dS += -27.2;
            }
        }

        // Checks for self-complementarity
        int mid;
        if (length % 2 == 0) {
            mid = length / 2;
            if (seq.substring(0, mid).equals(reverseComplement(seq.substring(mid+1)))) {
                dS += -1.4;
            }
        } else {
            mid = (length - 1) / 2;
            if (seq.substring(0, mid).equals(reverseComplement(seq.substring(mid+1)))) {
                dS += -1.4;
            }
        }

        // dH is in kCal, dS is in Cal - equilibrating units
        dH = dH * 1000;

        double logCt = java.lang.Math.log(concP/4);
        temp = (dH / (dS + (R * logCt))) - 273.15;

        //return temp;
        return temp;
    }
}
