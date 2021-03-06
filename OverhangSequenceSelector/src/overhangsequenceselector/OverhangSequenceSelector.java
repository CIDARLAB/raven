/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsequenceselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.biojava3.alignment.Alignments;
import org.biojava3.alignment.SimpleGapPenalty;
import org.biojava3.alignment.SubstitutionMatrixHelper;
import org.biojava3.alignment.template.SequencePair;
import org.biojava3.alignment.template.SubstitutionMatrix;
import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava3.core.sequence.compound.NucleotideCompound;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class OverhangSequenceSelector {
    //given a target length, generate all possible strings using given alphabet of target length

    public static String[] generateSequences(int length, String[] alphabet) {
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
        String[] toReturn = new String[currentSequences.size()];
        toReturn = currentSequences.toArray(toReturn);
        return toReturn;
    }

    public static int[][] buildScoreMatrix(String[] sequences) {
        int[][] toReturn = new int[sequences.length][sequences.length];
        for (int i = 0; i < sequences.length; i++) {
            for (int j = i + 1; j < sequences.length; j++) {
                int score = scoreSequences(sequences[i], sequences[j]);
//                System.out.println("scoring " + i + " against " + j + " score: " + score);
//                System.out.println(sequences[i]);
//                System.out.println(sequences[j]);
                toReturn[i][j] = score;
                toReturn[j][i] = score;//matrix is symmetrical
            }

        }
        return toReturn;
    }

    public static String[] selectSequences(int numberOfSequences, String[] sequences, int[][] scoreMatrix, String[] selectedSequences) {
        ArrayList<Integer> selectedIndices = new ArrayList();
        ArrayList<Integer> revCompIndices = new ArrayList(); //stores indices of overhangs that are the reverse complement of selectedIndices
        int currentIndex = -1;
        if (selectedSequences.length > 0) {
            for (int i = 0; i < selectedSequences.length; i++) {
                String revComp = reverseComplement(selectedSequences[i]);
                for (int j = 0; j < sequences.length; j++) {
                    if (sequences[j].equals(selectedSequences[i])) {
                        selectedIndices.add(j);
                    }
                    if (sequences[j].equals(revComp)) {
                        revCompIndices.add(j);
                    }
                }
            }
        }
        if (selectedIndices.isEmpty()) {
            while (selectedIndices.isEmpty()) {
                //due to symmetry of scoring matrix, any initial sequence is as good as the next really
                //select one without long runs at random to avoid systematic bias
                int randIndex = (int) (Math.random() * (sequences.length - 1) + 1);
                char[] currentSeqCharArray = sequences[randIndex].toCharArray();
                int score = 0; //arbitrary score that assess the length of character runs
                char prev = '*';
                for (int i = 0; i < currentSeqCharArray.length; i++) {
                    if (currentSeqCharArray[i] != prev) {
                        score = score + 1;
                    } else {
                        score = score - 1;
                    }
                    prev = currentSeqCharArray[i];
                }
                if (score > currentSeqCharArray.length / 2) {
                    selectedIndices.add(randIndex);
                    currentIndex = randIndex;
                }
            }
        } else {
            currentIndex = selectedIndices.get(selectedIndices.size() - 1);
        }
        ArrayList<Integer> potentialIndices = new ArrayList();
        while (selectedIndices.size() < numberOfSequences/2) {
            potentialIndices.clear();
            //add at least one sequence
            while (potentialIndices.isEmpty()) {
                for (int i = 0; i < sequences.length; i++) {
                    if (!selectedIndices.contains(i) && !revCompIndices.contains(i)) {
                        potentialIndices.add(i);
                    }
                }
            }
            for (int i = 0; i < sequences.length; i++) {
                if (i != currentIndex && !selectedIndices.contains(i) && !revCompIndices.contains(i)) {
                    if (scoreMatrix[currentIndex][i] < scoreMatrix[currentIndex][potentialIndices.get(0)]) {
                        potentialIndices.clear();
                        potentialIndices.add(i);
                    } else if (scoreMatrix[currentIndex][i] > scoreMatrix[potentialIndices.get(0)][i]) {
                        //do nothing; ignore selections that would give a greater score
                    } else {
                        potentialIndices.add(i);
                    }
                }
            }
            int minScore = 1000000000;
            int minIndex = potentialIndices.get(0);
            for (Integer potentialIndex : potentialIndices) {
                int currentScore = 0;
                String potentialSequence = sequences[potentialIndex];
                //don't pick sequences that are pallindromes
                if (potentialSequence.substring(0, potentialSequence.length() / 2).equals(reverseComplement(potentialSequence.substring(potentialSequence.length() / 2)))) {
                    currentScore = currentScore + potentialSequence.length();
                }
                for (Integer selectedIndex : selectedIndices) {
                    currentScore = currentScore + scoreMatrix[selectedIndex][potentialIndex];
                }
                if (currentScore < minScore) {
                    minScore = currentScore;
                    minIndex = potentialIndex;
                }
            }
            currentIndex = minIndex;
            selectedIndices.add(minIndex);
            //remove the reverseComplement
            String revComp = reverseComplement(sequences[minIndex]);
            for (int i = 0; i < sequences.length; i++) {
                if (sequences[i].equals(revComp)) {
                    revCompIndices.add(i);
                    break;
                }
            }
        }

        String[] toReturn = new String[numberOfSequences];
        for (int i = 0; i < selectedIndices.size(); i++) {
            System.out.println("selected: " + sequences[selectedIndices.get(i)]);
            toReturn[i] = sequences[selectedIndices.get(i)];
        }
        for (int i = 0; i < revCompIndices.size(); i++) {
            System.out.println("revComp: " + sequences[revCompIndices.get(i)]);
            toReturn[i+numberOfSequences/2] = sequences[revCompIndices.get(i)];
        }

        return toReturn;
    }

    public static void printMatrix(int[][] matrix, String[] sequences) {
        for (int i = 0; i < matrix.length; i++) {
            System.out.print(sequences[i] + " | ");
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

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

    //given two input sequences, return a score indicating the similarity of the two sequences
    //positive score denotes similarity, negative score inidcates dissimilarity 
    public static int scoreSequences(String seqA, String seqB) {
        int toReturn = 0;
        if (seqA.equals(reverseComplement(seqB))) {
            return Math.min(seqA.length(), seqB.length());
        }
        DNASequence target = new DNASequence(seqA,
                AmbiguityDNACompoundSet.getDNACompoundSet());

        DNASequence query = new DNASequence(seqB,
                AmbiguityDNACompoundSet.getDNACompoundSet());

        SubstitutionMatrix<NucleotideCompound> matrix = SubstitutionMatrixHelper.getNuc4_4();

        SimpleGapPenalty gapP = new SimpleGapPenalty();
        gapP.setOpenPenalty((short) 5);
        gapP.setExtensionPenalty((short) 2);

        SequencePair<DNASequence, NucleotideCompound> psa
                = Alignments.getPairwiseAlignment(query, target,
                        Alignments.PairwiseSequenceAlignerType.GLOBAL, gapP, matrix);

        String[] alignmentStrings = psa.toString().split("[\\n\\r]");
        //TODO iterate over alignment strings and count stuff
        char[] alignA = alignmentStrings[0].toCharArray();
        char[] alignB = alignmentStrings[2].toCharArray();
        HashSet<Character> charHash = new HashSet();
        for (int i = 0; i < alignmentStrings[0].length(); i++) {
            charHash.add(alignA[i]);
            charHash.add(alignB[i]);
            if (alignA[i] == '-') {
                toReturn = toReturn - 1;
            }
            if (alignB[i] == '-') {
                toReturn = toReturn - 1;
            }
            if (alignA[i] == alignB[i] && alignA[i] != '-') {
                toReturn = toReturn + 2;
            }
            if (alignA[i] != alignB[i]) {
                toReturn = toReturn - 1;
            }
        }
        toReturn = toReturn - charHash.size(); //score overhangs lower if many different characters appear to discourage runs of characters
        //ex: aaaa and tttt align poorly, but both are bad choices
        return toReturn;
    }
}
