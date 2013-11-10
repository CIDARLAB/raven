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
//        File selectedFile = new File(args[0]);
        File selectedFile = new File("/host/Users/Admin/Dropbox/Raven_Paper/data/literature_devices/complete/collins_counters_d_p.csv");
        if (selectedFile != null) {
            try {
                
                controller.parseRavenFile(selectedFile);
                //method, number of parts, number of runs, sample partitioning?, sample overhangs?
                controller.run("moclo",1,1, false, true);
//                controller.run(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),Boolean.parseBoolean(args[4]),Boolean.parseBoolean(args[5]));
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(OverhangSampler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}