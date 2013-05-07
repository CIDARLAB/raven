/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

/**
 *
 * @author Admin
 */
public class RavenLogger {

    public static void setPath(String s) {
        _path = s;
    }

    public static void logSessionIn(String user, String address) {
        try {
            File file = new File(_path + "userLog.txt");
            FileWriter fw = new FileWriter(file,true);
            BufferedWriter out = new BufferedWriter(fw);
            Date date= new Date();
            out.write("\nIN |"+user+" time: "+date.toString()+" location: "+address);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void logSessionOut(String user, String address, String designCount) {
        try {
            File file = new File(_path + "userLog.txt");
            FileWriter fw = new FileWriter(file,true);
            BufferedWriter out = new BufferedWriter(fw);
            Date date= new Date();
            out.write("\nOUT|"+user+" time: "+date.toString()+" location: "+ address +" designCount: "+designCount);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void logError(String error) {
    }
    static String _path = "/";
}
