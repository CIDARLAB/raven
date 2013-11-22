/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 *
 * @author jenhan
 */
public class ImageDownloader {

    public static String downloadImage(String urlString, String path, String outputFileName) throws IOException {
        URL imageURL = new URL(urlString);
        InputStream inputStream = imageURL.openStream();
        OutputStream out = new FileOutputStream(path + outputFileName);

        byte[] b = new byte[2048];
        int length;

        while ((length = inputStream.read(b)) != -1) {
            out.write(b, 0, length);
        }

        inputStream.close();
        out.close();
        return outputFileName;
    }
}
