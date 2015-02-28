/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.datastructures;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author jenhantao
 */
public class Vector {

    public static Vector generateVector(String name, String sequence, String leftOverhang, String rightOverhang, String type, String vector, String composition, String resistance, int level) {
        Vector newVector = new Vector();
        newVector.sequence = sequence;
        newVector.name = name;
        newVector.leftOverhang = leftOverhang;
        newVector.rightOverhang = rightOverhang;
        newVector.type = type;
        newVector.vector = vector;
        newVector.composition = composition;
        newVector.resistance = resistance;
        newVector.level = level;

        return newVector;
    }

    private Vector() {
        UUID++;
        this.uuid = "vector_"+String.valueOf(UUID);
    }

    public static Vector generateVector(String name) {
        Vector newVector = new Vector();
        newVector.name = name;
        return newVector;
    }


    public String getUUID() {
        return this.uuid;
    }

    //returns this vector or an exact match
    public Vector saveDefault(Collector coll) {
        Vector toReturn =  coll.addVector(this);
        if(!this.equals(toReturn)) {
            UUID--;
        }
        return toReturn;
    }

    public String getName() {
        return this.name;
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
    
    //Left overhang
    @Getter
    @Setter
    private String leftOverhang;
    
    //Right overhang
    @Getter
    @Setter
    private String rightOverhang;
    
    //Type
    @Getter
    @Setter
    private String type;
    
    //Vector name
    @Getter
    @Setter
    private String vector;

    //Composition
    @Getter
    @Setter
    private String composition;
    
    //Resistance
    @Getter
    @Setter
    private String resistance;
    
    //Resistance
    @Getter
    @Setter
    private int level = -1;
    
    //Fields
    protected static int UUID = 0;
    private String name;
    private String sequence;
    private String uuid;
    private Boolean _transient = true;
}
