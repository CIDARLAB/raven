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
                int score = scoreAlignment(sequences[i], sequences[j]);
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
        int currentIndex = -1;
        if (selectedSequences.length > 0) {
            for (int i = 0; i < selectedSequences.length; i++) {
                for (int j = 0; j < sequences.length; j++) {
                    if (sequences[j].equals(selectedSequences[i])) {
                        selectedIndices.add(j);
                        break;
                    }
                }
            }
        }
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
        ArrayList<Integer> potentialIndices = new ArrayList();
        while (selectedIndices.size() < numberOfSequences) {
            potentialIndices.clear();
            while (potentialIndices.isEmpty()) {
                for (int i = 0; i < sequences.length; i++) {
                    if (!selectedIndices.contains(i)) {
                        potentialIndices.add(i);
                    }
                }
            }
            for (int i = 0; i < sequences.length; i++) {
                if (i != currentIndex && !selectedIndices.contains(i)) {
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
            System.out.println(potentialIndices);
            for (Integer potentialIndex : potentialIndices) {
                int currentScore = 0;
                for (Integer selectedIndex : selectedIndices) {
                    currentScore = currentScore + scoreMatrix[selectedIndex][potentialIndex];
                }
                if (currentScore < minScore) {
                    minScore = currentScore;
                    minIndex = potentialIndex;
                }
            }
            selectedIndices.add(minIndex);
        }

        String[] toReturn = new String[numberOfSequences];
        for (int i = 0; i < numberOfSequences; i++) {
            System.out.println("selected: " + sequences[selectedIndices.get(i)]);
            toReturn[i] = sequences[selectedIndices.get(i)];
        }
        return toReturn;
    }

    public static void printMatrix(int[][] matrix, String[] sequences) {
        for (int i = 0; i < matrix.length; i++) {
            System.out.print(sequences[i]+" | ");
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static int scoreAlignment(String seqA, String seqB) {
        int toReturn = 0;
        DNASequence target = new DNASequence(seqA,
                AmbiguityDNACompoundSet.getDNACompoundSet());


        DNASequence query = new DNASequence(seqB,
                AmbiguityDNACompoundSet.getDNACompoundSet());

        SubstitutionMatrix<NucleotideCompound> matrix = SubstitutionMatrixHelper.getNuc4_4();

        SimpleGapPenalty gapP = new SimpleGapPenalty();
        gapP.setOpenPenalty((short) 5);
        gapP.setExtensionPenalty((short) 2);

        SequencePair<DNASequence, NucleotideCompound> psa =
                Alignments.getPairwiseAlignment(query, target,
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
