/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devicesvisualizer;

import java.io.File;
import javax.swing.JFileChooser;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class DevicesVisualizer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFileChooser fc = new JFileChooser();
        fc.showOpenDialog(null);
        File selectedFile = fc.getSelectedFile();
        if(selectedFile!=null) {
            
        }
    }
}
