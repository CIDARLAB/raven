/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import Controller.datastructures.Part;
import Controller.datastructures.Vector;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class PuppeteerWriter {
    //writes each part to sql database; if part with same id already exists, overwrite it

    public static void savePart(ArrayList<Part> parts, ArrayList<String> databaseConfig) {


        try {
            //configure sql connection
            Class.forName("com.mysql.jdbc.Driver");
            //hard code connection information for now
            String url = databaseConfig.get(0);
            String schema = databaseConfig.get(1);
            String user = databaseConfig.get(2);
            String password = databaseConfig.get(3);
            Connection con = DriverManager.getConnection(url, user, password);
            Statement statement = con.createStatement();

            //get a person id to use
            ResultSet result = statement.executeQuery("SELECT idPerson FROM " + schema + ".PersonTable");
            result.next();
            String authorId = result.getString("idPerson");


            ArrayList<Part> compositeParts = new ArrayList(); //composite parts have to be processed after basic parts
            for (Part p : parts) {
                statement = con.createStatement();
                if (p.isComposite()) {
                    //write composite parts later
                    compositeParts.add(p);
                    //remove from collectionTable
                    statement.executeUpdate("DELETE FROM " + schema + ".CollectionXref WHERE objectId='" + p.getUUID() + "'");
                    //remove from CompositeXref
                    statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE childPart='" + p.getUUID() + "'");
                    //delete composite part from PartTable
                    statement.executeUpdate("DELETE FROM " + schema + ".PartTable WHERE idPart='" + p.getUUID() + "'");
                } else {
                    //handle basic parts

                    //remove from collectionTable
                    statement.executeUpdate("DELETE FROM " + schema + ".CollectionXref WHERE objectId='" + p.getUUID() + "'");
                    //remove from partTable
                    statement.executeUpdate("DELETE FROM " + schema + ".PartTable WHERE idPart='" + p.getUUID() + "'");
                    //remove nucSeq
                    statement.executeUpdate("DELETE FROM " + schema + ".NucseqTable WHERE idNucseq='" + p.getUUID().replace("part", "seq") + "'");

                    //insert row for nucseq
                    statement.executeUpdate("INSERT INTO " + schema + ".NucseqTable (idNucSeq,sequence,isLocked,dateCreated,lastmodified) VALUES ("
                            + "'" + p.getUUID().replace("part", "seq") + "',"
                            + "'" + p.getSeq() + "',"
                            + "0,"
                            + "'" + getDate() + "',"
                            + "'" + getDate() + "'"
                            + ")");
                    //insert row for part
                    statement.executeUpdate("INSERT INTO " + schema + ".PartTable (idPart,formatId,nucseqId,description,authorId,name,riskGroup,dateCreated,lastModified,isBasic) VALUES("
                            + "'" + p.getUUID() + "',"
                            + "'org-clothocad-format-freeform-connect',"
                            + "'" + p.getUUID().replace("part", "seq") + "',"
                            + "'made by Raven',"
                            + "'" + authorId + "',"
                            + "'" + p.getName() + "',"
                            + "-1,"
                            + "'" + getDate() + "',"
                            + "'" + getDate() + "',"
                            + "0"
                            + ")");

                }
            }
            //after basic parts have been added, add composite parts
            int compositeCount = 0;
            for (Part compositePart : compositeParts) {
                statement.executeUpdate("INSERT INTO " + schema + ".PartTable (idPart,formatId,description,authorId,name,riskGroup,dateCreated,lastModified,isBasic) VALUES("
                        + "'" + compositePart.getUUID() + "',"
                        + "'org-clothocad-format-freeform-connect',"
                        + "'made by Raven',"
                        + "'" + authorId + "',"
                        + "'" + compositePart.getName() + "',"
                        + "-1,"
                        + "'" + getDate() + "',"
                        + "'" + getDate() + "',"
                        + "1"
                        + ")");
                int i = 0;
                for (Part p : compositePart.getComposition()) {
                    statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE idComposite='compoiste_" +compositeCount + "'");
                    statement.executeUpdate("INSERT INTO " + schema + ".CompositeXref (idComposite,childPart,parentPart,position) VALUES ("
                            + "'" + "composite+" + compositeCount + "',"
                            + "'" + compositePart.getUUID() + "',"
                            + "'" + p.getUUID() + "',"
                            + i
                            + ")");
                    i++;
                    compositeCount++;
                }

            }



        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    //writes each vector to database; if vector with same id already exists, overwrite it

    public static void saveVector(ArrayList<Vector> vectors, ArrayList<String> databaseConfig) {
    }

    private static String getDate() {
        Calendar rightNow = Calendar.getInstance();
        String date = rightNow.get(Calendar.YEAR) + "-";
        if (Integer.toString(rightNow.get(Calendar.MONTH)).length() < 2) {
            date = date + "0" + (rightNow.get(Calendar.MONTH) + 1) + "-";
        } else {
            date = date + (rightNow.get(Calendar.MONTH) + 1) + "-";
        }
        if (Integer.toString(rightNow.get(Calendar.DAY_OF_MONTH)).length() < 2) {
            date = date + "0" + (rightNow.get(Calendar.DAY_OF_MONTH)) + " ";
        } else {
            date = date + (rightNow.get(Calendar.DAY_OF_MONTH)) + " ";
        }
        if (Integer.toString(rightNow.get(Calendar.HOUR_OF_DAY)).length() < 2) {
            date = date + "0" + rightNow.get(Calendar.HOUR_OF_DAY) + ":";
        } else {
            date = date + rightNow.get(Calendar.HOUR_OF_DAY) + ":";
        }
        if (Integer.toString(rightNow.get(Calendar.MINUTE)).length() < 2) {
            date = date + "0" + rightNow.get(Calendar.MINUTE) + ":";
        } else {
            date = date + rightNow.get(Calendar.MINUTE) + ":";
        }
        if (Integer.toString(rightNow.get(Calendar.SECOND)).length() < 2) {
            date = date + "0" + rightNow.get(Calendar.SECOND);
        } else {
            date = date + rightNow.get(Calendar.SECOND);
        }
        return date;
    }
}
