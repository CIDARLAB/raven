/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author evanappleton
 */
public class RestrictionEnzyme {

    /** Restriction enzyme constructor **/
    public RestrictionEnzyme(String name, HashMap<String, ArrayList<Integer>> recSeq) {
        _name = name;
        _recSeq = recSeq;
        _reID = _reCount;
        _reCount++;
        _uuid = null;
    }
    
    /** Get name **/
    public String getName() {
        return _name;
    }
    
    /** Get forward recognition site **/
    public HashMap<String, ArrayList<Integer>> getRecSeq() {
        return _recSeq;
    }
    
    /** Get uuid **/
    public String getUUID() {
        return _uuid;
    }
    
    /** Get reID **/
    public int getREID() {
        return _reID;
    }
    
    /** Set restriction enzyme name **/
    public void setName (String name) {
        _name = name;
    }
    
    /** Set forward recognition site **/
    public void setFwdRecSeq (HashMap<String, ArrayList<Integer>> recSeq) {
        _recSeq =  recSeq;
    }
    
    /** Set uuid **/
    public void setUUID (String uuid) {
        _uuid = uuid;
    }
    
    //FIELDS
    private String _name;
    private HashMap<String, ArrayList<Integer>> _recSeq;
    private int _reID;
    private String _uuid;
    private static int _reCount;
}
