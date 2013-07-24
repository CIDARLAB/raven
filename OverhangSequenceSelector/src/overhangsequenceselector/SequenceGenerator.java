/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsequenceselector;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static int scoreAlignment(String seqA, String seqB) {

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
        return -1;

    }
}
