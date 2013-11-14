/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsampler;

import Communication.RavenController;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        File selectedFile = new File(args[0]);
//        File selectedFile = new File("/host/Users/Admin/Desktop/publication_dataset.csv");
        if (selectedFile != null) {
            try {
                
                controller.parseRavenFile(selectedFile);
                //method, number of parts, number of runs, sample partitioning?, sample overhangs?
//                controller.run("moclo",20,10, false,true);
                controller.run(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),Boolean.parseBoolean(args[4]),Boolean.parseBoolean(args[5]));
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(OverhangSampler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
//    public static void main(String[] args) {
//        int[] counts =new int[6];
//        for (int i=0;i<100;i++) {
//            int rand = SamplingPartitioning.getNumBreaks(6);
//            counts[rand-1]=counts[rand-1]+1;
//        }
//        for (int i=0;i<counts.length;i++) {
//            System.out.println(counts[i]);
//        }
//    }
//    
}