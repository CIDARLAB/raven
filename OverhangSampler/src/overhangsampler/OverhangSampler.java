/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package overhangsampler;

import Communication.RavenController;
import java.io.File;
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
                controller.run("moclo",10, false, false);
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(OverhangSampler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
