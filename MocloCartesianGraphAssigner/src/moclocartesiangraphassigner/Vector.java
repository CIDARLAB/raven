/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moclocartesiangraphassigner;

import java.util.ArrayList;

/**
 *
 * @author jenhantao
 */
public class Vector {

    public static Vector generateVector(String name, String sequence) {
        Vector newVector = new Vector();
        newVector.sequence = sequence;
        newVector.name = name;
        newVector.searchTags = new ArrayList<String>();

        return newVector;
    }

    private Vector() {
        UUID++;
        this.uuid = "vector_"+String.valueOf(UUID);
    }

    public static Vector generateVector(String name) {
        Vector newVector = new Vector();
        newVector.name = name;
        newVector.searchTags = new ArrayList<String>();
        return newVector;
    }

    public static Vector retrieveByName(Collector coll, String name,boolean allowTransient) {
        return coll.getVectorByName(name, allowTransient);
    }

    public String getUUID() {
        return this.uuid;
    }

    public ArrayList<String> getSearchTags() {
        return this.searchTags;
    }

    public void addSearchTag(String string) {
        this.searchTags.add(string);
    }

    //adds vector to collector hash
    public Boolean saveDefault(Collector coll) {
        return coll.addVector(this);
    }

    public String getName() {
        return this.name;
    }

    public int getLevel() {
        int toReturn = -1;
        for (String tag : this.searchTags) {
            if (tag.startsWith("Level:")) {
                toReturn = Integer.parseInt(tag.substring(7).trim());
            }
        }
        return toReturn;
    }

    public String getSequence() {
        return this.sequence;
    }

    public String getResistance() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Resistance:")) {
                toReturn = tag.substring(12).trim();
            }
        }
        return toReturn;
    }

    public String getLeftoverhang() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("LO:")) {
                toReturn = tag.substring(4);
            }
        }
        return toReturn;
    }

    public String getRightOverhang() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("RO:")) {
                toReturn = tag.substring(4);
            }
        }
        return toReturn;
    }

    public String getType() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Type:")) {
                toReturn = tag.substring(6);
            }
        }
        return toReturn;
    }

    public String getSeq() {
        return this.sequence;
    }

    public Boolean isTransient() {
        return _transient;
    }

    public void setTransientStatus(Boolean b) {
        _transient = b;
    }
    //Fields
    protected static int UUID = 0;
    private String name;
    private ArrayList<String> searchTags = new ArrayList<String>();
    private String sequence;
    private String uuid;
    private Boolean _transient = true;
}
