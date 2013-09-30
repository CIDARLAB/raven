/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsampler;

import Communication.RavenController;
import Controller.algorithms.SamplingPartitioning;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author jenhan
 */
public class OverhangSampler {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RavenController controller = new RavenController();
        JFileChooser fc = new JFileChooser();
        fc.showOpenDialog(null);
        File selectedFile = fc.getSelectedFile();
        if (selectedFile != null) {
            try {
                controller.parseRavenFile(selectedFile);
                controller.run("moclo",10,3, false, false);
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(OverhangSampler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void testPartitioning() {

        ArrayList indices = new ArrayList();
        int numBreaks = 2;
        indices.add(1);
        indices.add(2);
        indices.add(3);
        indices.add(4);

        int[] solution = SamplingPartitioning.getPartition(indices, numBreaks);
        for (int i = 0; i < solution.length; i++) {
            System.out.print(solution[i] + ", ");
        }
        System.out.println("\n*******************");
        HashMap<Integer, HashSet<int[]>> partitions = SamplingPartitioning.partitions;
        for (Integer key : partitions.keySet()) {
            System.out.println("numBreaks: " + key);
            for (int[] partition : partitions.get(key)) {
                for (int i = 0; i < partition.length; i++) {
                    System.out.print(partition[i] + ", ");
                }
                System.out.println();
            }
        }
    }
}