/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pigeontoraven;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFileChooser;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class PigeonToRaven {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFileChooser fc = new JFileChooser();
        fc.showOpenDialog(null);
        File selectedFile = fc.getSelectedFile();
        if (selectedFile != null) {
            parseRavenFile(selectedFile);
        }
    }

    private static void parseRavenFile(File input) {
        String outputPath = input.getParent() + "/output";
        File outDir = new File(outputPath);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        BufferedReader reader = null;
        HashMap<String, String> partTypeHash = new HashMap();
        ArrayList<String> compositePartLines = new ArrayList();
        ArrayList<HashMap<String, String>> positionPart = new ArrayList();
        try {
            ArrayList<String> badLines = new ArrayList();
            reader = new BufferedReader(new FileReader(input.getAbsolutePath()));
            String line = reader.readLine();
            line = reader.readLine(); //skip first line
            //Read each line of the input file to parse parts
            while (line != null) {
                while (line.matches("^[\\s,]+")) {
                    line = reader.readLine();
                }
                String[] tokens = line.split(",");
                int tokenCount = tokens.length; //keeps track of how many columns are filled by counting backwards
                for (int i = tokens.length - 1; i > -1; i--) {
                    if (tokens[i].trim().matches("[\\s]*")) {
                        tokenCount--;
                    } else {
                        break;
                    }
                }

                if (tokenCount == 5) {
                    //basic part
                    try {
                        String name = tokens[0].trim();
                        String type = tokens[4].trim();
                        partTypeHash.put(name, type);
                        //dont visualize basic parts by themselves
                    } catch (Exception e) {
                        badLines.add(line);
                    }

                    //Basic parts with direction and overhangs
                } else if (tokenCount == 9) {

                    try {
                        String name = tokens[0].trim();
                        String type = tokens[4].trim();
                        partTypeHash.put(name, type);
                        //dont visualize basic parts by themselves  }
                    } catch (Exception e) {
                        badLines.add(line);
                    }

                } else if (tokenCount > 9) {
                    compositePartLines.add(line);

                }

                line = reader.readLine();
            }
            int counter = 0;
            for (String compositeLine : compositePartLines) {
                String[] tokens = compositeLine.split(",");
                try {
                    String pigeonString = "";
                    //Parse composition tokens
                    for (int i = 8; i < tokens.length; i++) {
                        String basicPartString = tokens[i].trim();
                        String[] partNameTokens = basicPartString.split("\\|");
                        String bpDirection = "+";
                        String basicPartName = partNameTokens[0];
                        String type = partTypeHash.get(basicPartName);

                        //Check for forced overhangs and direction
                        if (partNameTokens.length > 1) {
                            if (partNameTokens.length == 2) {
                                if ("+".equals(partNameTokens[1]) || "-".equals(partNameTokens[1])) {
                                    bpDirection = partNameTokens[1];
                                }
                            } else if (partNameTokens.length == 4) {
                                bpDirection = partNameTokens[3];
                            }
                        }

                        pigeonString = pigeonString + generatePigeonImage(basicPartName, type, bpDirection);
                        if (positionPart.size() > i - 8) {
                            positionPart.get(i - 8).put(basicPartName + "_" + type + "_" + (i - 8), pigeonString);
                        } else {
                            HashMap<String, String> toAdd = new HashMap();
                            toAdd.put(basicPartName + "_" + type + "_" + (i - 8), pigeonString);
                            positionPart.add(toAdd);
                        }

                    }
                    pigeonString = pigeonString + "# Arcs";
                    WeyekinPoster.setPigeonText(pigeonString);
                    WeyekinPoster.setShouldILaunchBrowserPage(true);
                    WeyekinPoster.postMyBird();
                    System.out.println(WeyekinPoster.getmPigeonURI());

                    //download image
                    URL url = WeyekinPoster.getmPigeonURI().toURL();

                    BufferedInputStream in = null;
                    FileOutputStream fout = null;
                    try {
                        in = new BufferedInputStream(url.openStream());
                        fout = new FileOutputStream(outputPath+"/counter_"+counter+".png");

                        byte data[] = new byte[1024];
                        int count;
                        while ((count = in.read(data, 0, 1024)) != -1) {
                            fout.write(data, 0, count);
                        }
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                        if (fout != null) {
                            fout.close();
                        }
                    }

                } catch (NullPointerException e) {
                    String badLine = "";

                    for (int j = 0; j < tokens.length; j++) {
                        badLine = badLine + tokens[j] + ",";
                    }
                    badLines.add(badLine.substring(0, badLine.length() - 1));//trim the last comma
                }
                counter++;
            }


            //post to pigeon
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String generatePigeonImage(String partName, String type, String direction) {

        StringBuilder pigeonLine = new StringBuilder();
        //Assign left overhang if it exists                
        if (partName != null) {
            String name = partName;
            String dir = "";

            //Turn direction of glyph in reverse if reverse direction

            if ("-".equals(dir)) {
                pigeonLine.append("<");
            }

            //Write pigeon code for a recognized regular part type
            if (type.equalsIgnoreCase("promoter") || type.equalsIgnoreCase("p")) {
                pigeonLine.append("P ").append(name).append(" 4" + "\n");
            } else if (type.equalsIgnoreCase("RBS") || type.equalsIgnoreCase("r")) {
                pigeonLine.append("r ").append(name).append(" 5" + "\n");
            } else if (type.equalsIgnoreCase("gene") || type.equalsIgnoreCase("g")) {
                pigeonLine.append("c ").append(name).append(" 1" + "\n");
            } else if (type.equalsIgnoreCase("reporter") || type.equalsIgnoreCase("gr")) {
                pigeonLine.append("c ").append(name).append(" 2" + "\n");
            } else if (type.equalsIgnoreCase("terminator") || type.equalsIgnoreCase("t")) {
                pigeonLine.append("T ").append(name).append(" 6" + "\n");
            } else if (type.equalsIgnoreCase("invertase site") || type.equalsIgnoreCase("is")) {
                if ("-".equals(dir)) {
                    pigeonLine.append(" ").append(name).append(" 12" + "\n");
                } else {
                    pigeonLine.append("> ").append(name).append(" 12" + "\n");
                }
            } else if (type.equalsIgnoreCase("spacer") || type.equalsIgnoreCase("s")) {
                pigeonLine.append("s ").append(name).append(" 10" + "\n");
            } else if (type.equalsIgnoreCase("origin") || type.equalsIgnoreCase("o")) {
                pigeonLine.append("z ").append(name).append(" 14" + "\n");
            } else if (type.equalsIgnoreCase("fusion") || type.equalsIgnoreCase("fu")) {
                pigeonLine.append("f1");
                String[] fusionParts = name.split("-");
                for (int j = 1; j < fusionParts.length; j++) {
                    int color = j % 13 + 1;
                    pigeonLine.append("-").append(color);
                }
                pigeonLine.append(" ").append(name).append("\n");
            } else {
                pigeonLine.append("c ").append(name).append(" 13" + "\n");
            }

        }


        return pigeonLine.toString();
    }
}
