/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;

/**
 *
 * @author Admin
 */
public class Part {

    public static Part generateComposite(ArrayList<Part> newComposition, String name) {
        Part newComposite = new Part();
        String sequence = "";
        for (Part p : newComposition) {
            sequence = sequence + p.getSeq();
        }
        newComposite.sequence = sequence;
        newComposite.composition = newComposition;
        newComposite.name = name;
        newComposite.uuid = String.valueOf(UUID);
        newComposite.isComposite = true;
        return newComposite;
    }

    private Part() {
        UUID++;
        this.uuid = String.valueOf(UUID);
    }

    public static Part retrieveByExactName(String name) {
        return Collector.getPartByName(name);
    }

    public static Part generateBasic(String name, String sequence) {
        Part newBasic = new Part();
        newBasic.name = name;
        newBasic.sequence = sequence;
        newBasic.isComposite = false;
        newBasic.composition= new ArrayList<Part>();
        newBasic.composition.add(newBasic);
        return newBasic;
    }

    public String getName() {
        return this.name;
    }

    public String getUUID() {
        return this.uuid;
    }

    public boolean isComposite() {
        return this.isComposite;
    }

    public ArrayList<String> getSearchTags() {
        return this.searchTags;
    }

    public void addSearchTag(String string) {
        this.searchTags.add(string);
    }

    public String getSeq() {
        return this.sequence;
    }

    public boolean isBasic() {
        return !isComposite;
    }

    //for a composite part, returns an ordered list of parts that describes its composition
    public ArrayList<Part> getComposition() {
        return this.composition;
    }

    public ArrayList<String> getStringComposition() {
        ArrayList<String> toReturn = new ArrayList();
        for (Part p : this.composition) {
            toReturn.add(p.getName());
        }
        return toReturn;
    }

    public Boolean saveDefault() {
        return Collector.addPart(this);
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
    //Fields
    private ArrayList<Part> composition;
    private String name;
    private String sequence;
    private Boolean isComposite = false;
    private String uuid;
    private ArrayList<String> searchTags = new ArrayList();
    protected static int UUID = 0;
}
