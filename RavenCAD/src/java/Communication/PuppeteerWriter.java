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

    public static void saveParts(ArrayList<Part> parts, ArrayList<String> databaseConfig) {


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

            ArrayList<String> toExecute = new ArrayList(); //save insert commands so they can be executed after delete commands
            ArrayList<Part> compositeParts = new ArrayList(); //composite parts have to be processed after basic parts
            ArrayList<Part> basicParts = new ArrayList();
            for (Part p : parts) {
                statement = con.createStatement();
                if (p.isComposite()) {
                    //write composite parts later
                    compositeParts.add(p);
                    //remove from collectionTable
                    statement.executeUpdate("DELETE FROM " + schema + ".CollectionXref WHERE objectId='" + p.getUUID() + "'");
                    //remove from CompositeXref
                    statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE childPart='" + p.getUUID() + "'");
                    statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE parentPart='" + p.getUUID() + "'");
                    //delete composite part from PartTable
                    statement.executeUpdate("DELETE FROM " + schema + ".PartTable WHERE idPart='" + p.getUUID() + "'");
                } else {
                    basicParts.add(p);
                }
            }

            for (Part basic : basicParts) {
                //handle basic parts
                //remove from compositeXref
                statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE parentPart='" + basic.getUUID() + "'");
                statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE childPart='" + basic.getUUID() + "'");

                //remove from collectionTable
                statement.executeUpdate("DELETE FROM " + schema + ".CollectionXref WHERE objectId='" + basic.getUUID() + "'");
                //remove from partTable
                statement.executeUpdate("DELETE FROM " + schema + ".PartTable WHERE idPart='" + basic.getUUID() + "'");
                //remove nucSeq
                statement.executeUpdate("DELETE FROM " + schema + ".NucseqTable WHERE idNucseq='" + basic.getUUID().replace("part", "seq") + "'");

                //insert row for nucseq
//                statement.executeUpdate("INSERT INTO " + schema + ".NucseqTable (idNucSeq,sequence,isLocked,dateCreated,lastmodified) VALUES ("
//                        + "'" + basic.getUUID().replace("part", "seq") + "',"
//                        + "'" + basic.getSeq() + "',"
//                        + "0,"
//                        + "'" + getDate() + "',"
//                        + "'" + getDate() + "'"
//                        + ")");
                toExecute.add("INSERT INTO " + schema + ".NucseqTable (idNucSeq,sequence,isLocked,dateCreated,lastmodified) VALUES ("
                        + "'" + basic.getUUID().replace("part", "seq") + "',"
                        + "'" + basic.getSeq() + "',"
                        + "0,"
                        + "'" + getDate() + "',"
                        + "'" + getDate() + "'"
                        + ")");

                //insert row for part
//                statement.executeUpdate("INSERT INTO " + schema + ".PartTable (idPart,formatId,nucseqId,description,authorId,name,riskGroup,dateCreated,lastModified,isBasic) VALUES("
//                        + "'" + basic.getUUID() + "',"
//                        + "'org-clothocad-format-freeform-connect',"
//                        + "'" + basic.getUUID().replace("part", "seq") + "',"
//                        + "'made by Raven',"
//                        + "'" + authorId + "',"
//                        + "'" + basic.getName() + "',"
//                        + "-1,"
//                        + "'" + getDate() + "',"
//                        + "'" + getDate() + "',"
//                        + "0"
//                        + ")");
                toExecute.add("INSERT INTO " + schema + ".PartTable (idPart,formatId,nucseqId,description,authorId,name,riskGroup,dateCreated,lastModified,isBasic) VALUES("
                        + "'" + basic.getUUID() + "',"
                        + "'org-clothocad-format-freeform-connect',"
                        + "'" + basic.getUUID().replace("part", "seq") + "',"
                        + "'made by Raven',"
                        + "'" + authorId + "',"
                        + "'" + basic.getName() + "',"
                        + "-1,"
                        + "'" + getDate() + "',"
                        + "'" + getDate() + "',"
                        + "0"
                        + ")");
            }
            //after basic parts have been added, add composite parts
            int compositeCount = 0;
            for (Part compositePart : compositeParts) {
//                statement.executeUpdate("INSERT INTO " + schema + ".PartTable (idPart,formatId,description,authorId,name,riskGroup,dateCreated,lastModified,isBasic) VALUES("
//                        + "'" + compositePart.getUUID() + "',"
//                        + "'org-clothocad-format-freeform-connect',"
//                        + "'made by Raven',"
//                        + "'" + authorId + "',"
//                        + "'" + compositePart.getName() + "',"
//                        + "-1,"
//                        + "'" + getDate() + "',"
//                        + "'" + getDate() + "',"
//                        + "1"
//                        + ")");
                toExecute.add("INSERT INTO " + schema + ".PartTable (idPart,formatId,description,authorId,name,riskGroup,dateCreated,lastModified,isBasic) VALUES("
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
                    //write compoite part
                    statement.executeUpdate("DELETE FROM " + schema + ".CompositeXref WHERE idComposite='composite_" + compositeCount + "'");
//                    statement.executeUpdate("INSERT INTO " + schema + ".CompositeXref (idComposite,childPart,parentPart,position) VALUES ("
//                            + "'" + "composite_" + compositeCount + "',"
//                            + "'" + compositePart.getUUID() + "',"
//                            + "'" + p.getUUID() + "',"
//                            + i
//                            + ")");
                    toExecute.add("INSERT INTO " + schema + ".CompositeXref (idComposite,childPart,parentPart,position) VALUES ("
                            + "'" + "composite_" + compositeCount + "',"
                            + "'" + compositePart.getUUID() + "',"
                            + "'" + p.getUUID() + "',"
                            + i
                            + ")");
                    i++;
                    compositeCount++;
                }
            }
            for(String stmt:toExecute) {
                statement.executeUpdate(stmt);
//                System.out.println(stmt);
            }



        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    //writes each vector to database; if vector with same id already exists, overwrite it

    public static void saveVectors(ArrayList<Vector> vectors, ArrayList<String> databaseConfig) {
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
            for (Vector v : vectors) {
                //remove from vectorTable
                statement.executeUpdate("DELETE FROM " + schema + ".VectorTable WHERE idVector='" + v.getUUID() + "'");
                //remove nucSeq
                statement.executeUpdate("DELETE FROM " + schema + ".NucseqTable WHERE idNucseq='" + v.getUUID().replace("vector", "vector_seq") + "'");

                //insert row for nucseq
                statement.executeUpdate("INSERT INTO " + schema + ".NucseqTable (idNucSeq,sequence,isLocked,dateCreated,lastmodified) VALUES ("
                        + "'" + v.getUUID().replace("vector", "vector_seq") + "',"
                        + "'" + v.getSeq() + "',"
                        + "0,"
                        + "'" + getDate() + "',"
                        + "'" + getDate() + "'"
                        + ")");
                //insert row for part
                statement.executeUpdate("INSERT INTO " + schema + ".VectorTable (idVector,formatId,nucseqId,description,isCircular,isGenomic,authorId,name,riskGroup,dateCreated,lastModified) VALUES("
                        + "'" + v.getUUID() + "',"
                        + "'org-clothocad-format-freeform-connect',"
                        + "'" + v.getUUID().replace("vector", "vector_seq") + "',"
                        + "'made by Raven',"
                        + "1,"
                        + "1,"
                        + "'" + authorId + "',"
                        + "'" + v.getName() + "',"
                        + "-1,"
                        + "'" + getDate() + "',"
                        + "'" + getDate() + "'"
                        + ")");
            }






        } catch (Exception ex) {
            ex.printStackTrace();
        }

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