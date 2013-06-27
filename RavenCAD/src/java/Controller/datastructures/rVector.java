/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

/**
 *
 * @author evanappleton
 */
public class rVector {

    /** rVector constructor, no explicit parameters **/
    public rVector() {
        _uuid = null;
        _lOverhang = new String();
        _rOverhang = new String();
        _resistance = new String();
        _level = -1;
        _vectorID = _vectorCount;
        _vectorCount++;
    }

//    /** rVector constructor with explicit parameters **/
//    public rVector(String lOverhang, String rOverhang, boolean recommended, int level) {
//        _uuid = null;
//        _lOverhang = lOverhang;
//        _rOverhang = rOverhang;
//        _recommended = recommended;
//        _level = level;
//        _vectorID = _vectorCount;
//        _vectorCount++;
//    }
    /**************************************************************************
     * 
     * GETTER AND SETTER METHODS
     * 
     **************************************************************************/
    /** Get vectorID **/
    public int getVectorID() {
        return _vectorID;
    }

    /** Get Clotho UUID **/
    public String getUUID() {
        return _uuid;
    }

    /** Get left overhang **/
    public String getLOverhang() {
        return _lOverhang;
    }

    /** Get right overhang **/
    public String getROverhang() {
        return _rOverhang;
    }

    /** Get a vector's resistance **/
    public String getResistance() {
        return _resistance;
    }

    /** Get vector level **/
    public int getLevel() {
        return _level;
    }

    /** Get if this vector should be used for all nodes **/
    public boolean getUseForAll() {
        return _useForAll;
    }

    /** Get vector name **/
    public String getName() {
        return _name;
    }

    /** Set Clotho UUID **/
    public void setUUID(String newuuid) {
        _uuid = newuuid;
    }

    /** Set right overhang **/
    public void setROverhang(String overhang) {
        _rOverhang = overhang;
    }

    /** Set left overhang **/
    public void setLOverhang(String overhang) {
        _lOverhang = overhang;
    }

    /** Set a vector's resistance **/
    public void setResistance(int level) {
//        if (level == 2) {
//            _resistance = "amp";
//        } else if (level == 1) {
//            _resistance = "kan";
//        } else if (level == 0) {
//            _resistance = "cam";
//        }
    }

    public void setStringResistance(String s) {
        _resistance = s;
    }

    /** Set vector level **/
    public void setLevel(int level) {
        _level = level;
    }

    /** Set this vector to be used by all intermediates of a graph **/
    public void setUseForAll(boolean useForAll) {
        _useForAll = useForAll;
    }

    /** Set vector name **/
    public void setName(String name) {
        _name = name;
    }
    //Fields
    private String _uuid;
    private String _lOverhang;
    private String _rOverhang;
    private String _resistance;
    private String _name;
    private int _level;
    private int _vectorID;
    private boolean _useForAll;
    private static int _vectorCount = 0;
}