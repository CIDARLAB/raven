/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author jenhantao, evanappleton
 */
public class RNode {

    /** SDSNode constructor, no neighbors, parent or children or meta-data specified **/
    public RNode() {
        _recommended = false;
        _discouraged = false;
        _efficiency = 0;
        _successCnt = 0;
        _failureCnt = 0;
        _neighbors = new ArrayList<RNode>();
        _composition = new ArrayList<String>();
        _direction = new ArrayList<String>();
        _scars = new ArrayList<String>();
        _uuid = null;
        _type = new ArrayList<String>();
        _lOverhang = "";
        _rOverhang = "";
        _name = "";
        _stage = 0;
        _nodeID = _nodeCount;
        _nodeCount++;
    }

    /** SDSNode constructor for intermediates with meta-data, neighbors and composition, but no part**/
    public RNode(boolean recommended, boolean discouraged, ArrayList<String> composition, ArrayList<String> direction, ArrayList<String> type, ArrayList<String> scars, String lOverhang, String rOverhang, int successCnt, int failureCnt, RVector vector) {
        _uuid = null;
        _recommended = recommended;
        _discouraged = discouraged;
        _efficiency = 0;
        _successCnt = successCnt;
        _failureCnt = failureCnt;
        _neighbors = new ArrayList<RNode>();
        _scars = scars;
        _composition = composition;
        _direction = direction;
        _type = type;
        _lOverhang = lOverhang;
        _rOverhang = rOverhang;
        _vector = vector;
        _name = "";
        _nodeID = _nodeCount;
        _nodeCount++;
    }
    
    /** Clone nodes of a graph by traversing and copying nodes **/
    @Override
    public RNode clone() {
        
        RNode clone = new RNode();
        clone._recommended = this._recommended;
        clone._discouraged = this._discouraged;
        clone._uuid = this._uuid;
        clone._type = this._type;
        clone._lOverhang = this._lOverhang;
        clone._rOverhang = this._rOverhang;
        clone._composition = this._composition;
        clone._direction = this._direction;
        clone._scars = this._scars;
        clone._name = this._name;
        clone._stage = this._stage;
        clone._vector = this._vector;
        clone._efficiency = this._efficiency;
        clone._successCnt = this._successCnt;
        clone._failureCnt = this._failureCnt;
        ArrayList<RNode> neighbors = this._neighbors;
        cloneHelper(clone, this, neighbors);
        
        return clone;
    }
    
    private void cloneHelper(RNode parentClone, RNode parent, ArrayList<RNode> children) {
        
        for (int i = 0; i < children.size(); i++) {
            
            RNode child = children.get(i);
            
            RNode childClone = new RNode();
            childClone._recommended = child._recommended;
            childClone._discouraged = child._discouraged;
            childClone._uuid = child._uuid;
            childClone._type = child._type;
            childClone._lOverhang = child._lOverhang;
            childClone._rOverhang = child._rOverhang;
            childClone._composition = child._composition;
            childClone._direction = child._direction;
            childClone._scars = child._scars;
            childClone._name = child._name;
            childClone._stage = child._stage;
            childClone._vector = child._vector;
            childClone._efficiency = child._efficiency;
            childClone._successCnt = child._successCnt;
            childClone._failureCnt = child._failureCnt;
            
            parentClone.addNeighbor(childClone);
            childClone.addNeighbor(parentClone);
            
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                cloneHelper(childClone, child, grandChildren);
            }
        }
    }
    
    /**************************************************************************
     * 
     * GETTER AND SETTER METHODS
     * 
    **************************************************************************/
    
    public int getNodeID() {
        return _nodeID;
    }
    
    /** Determine if part at node is recommended **/
    public boolean getRecommended() {
        return _recommended;
    }

    /** Determine if part at node is recommended **/
    public boolean getDiscouraged() {
        return _discouraged;
    }
    
    /** Get Clotho UUID **/
    public String getUUID() {
        return _uuid;
    }

    /** Get part feature type **/
    public ArrayList<String> getType() {
        return _type;
    }
    
    /** Get left overhang **/
    public String getLOverhang() {
        return _lOverhang;
    }
    
    /** Get right overhang **/
    public String getROverhang() {
        return _rOverhang;
    }
    
    /** Get node neighbors **/
    public ArrayList<RNode> getNeighbors() {
        return _neighbors;
    }

    /** Get node composition **/
    public ArrayList<String> getComposition() {
        return _composition;
    }
    
    /** Get vector **/
    public RVector getVector() {
        return _vector;
    }
    
    /** Get name **/
    public String getName() {
        return _name;
    }
    
    /** Get stage **/
    public int getStage() {
        return _stage;
    }
    
    /** Get efficiency **/
    public double getEfficiency() {
        return _efficiency;
    }
    
    /** Get modularity **/
    public double getModularity() {
        return _modularity;
    }
    
    /** Return success count - for debugging **/
    public int getSuccessCnt() {
        return _successCnt;
    }
    
    /** Return failure count - for debugging **/
    public int getFailureCnt() {
        return _failureCnt;
    }
    
    /** Get the direction of the node's composition **/
    public ArrayList<String> getDirection() {
        return _direction;
    }
    
    /** Get the scars of a part **/
    public ArrayList<String> getScars() {
        return _scars;
    }
    
    /** Get node keys for either forward or reverse direction **/
    public String getNodeKey(String dir) {
        
        //Forward key information
        ArrayList<String> composition = this._composition;
        ArrayList<String> direction = this._direction;
        ArrayList<String> scars = this._scars;
        String lOverhang = this._lOverhang;
        String rOverhang = this._rOverhang;
        
        if (dir.equals("+")) {           
            String aPartLOcompRO = composition + "|" + direction + "|" + scars + "|" + lOverhang + "|" + rOverhang;
            return aPartLOcompRO;
        } else {
            
            //Backward key information
            ArrayList<String> revComp = new ArrayList<String>();
            revComp.addAll(composition);
            Collections.reverse(revComp);
            
            ArrayList<String> revDir = new ArrayList<String>();
            ArrayList<String> revDirF = new ArrayList<String>();
            revDir.addAll(direction);
            Collections.reverse(revDir);
            for (String RD : revDir) {
                if (RD.equals("+")) {
                    revDirF.add("-");
                } else if (RD.equals("-")) {
                    revDirF.add("+");
                }
            }
            
            ArrayList<String> revScars = new ArrayList<String>();
            ArrayList<String> revScarsF = new ArrayList<String>();
            revScars.addAll(scars);
            Collections.reverse(revScars);
            for (String aRevScar : revScars) {
                if (aRevScar.contains("*")) {
                    aRevScar = aRevScar.replace("*", "");
                    revScarsF.add(aRevScar);
                } else {
                    aRevScar = aRevScar + "*";
                    revScarsF.add(aRevScar);
                }
            }

            String lOverhangR = rOverhang;
            String rOverhangR = lOverhang;
            if (lOverhangR.contains("*")) {
                lOverhangR = lOverhangR.replace("*", "");
            } else {
                lOverhangR = lOverhangR + "*";
            }
            if (rOverhangR.contains("*")) {
                rOverhangR = rOverhangR.replace("*", "");
            } else {
                rOverhangR = rOverhangR + "*";
            }
            
            String aPartCompDirScarLOROR = revComp + "|" + revDirF + "|" + revScarsF + "|" + lOverhangR + "|" + rOverhangR;
            return aPartCompDirScarLOROR;
        }
    }
    
    /** Set part as recommended or not required **/
    public void setRecommended(boolean recommended) {
        _recommended = recommended;
    }
    
    /** Set part as discouraged or not required **/
    public void setDiscouraged(boolean discouraged) {
        _discouraged = discouraged;
    }
    
    /** Set Clotho UUID **/
    public void setUUID(String newuuid) {
        _uuid = newuuid;
    }

    /** Set part feature type **/
    public void setType(ArrayList<String> type) {
        _type = type;
    }
    
    /** Set left overhang **/
    public void setLOverhang(String overhang) {
        _lOverhang = overhang;
    }
    
    /** Set right overhang **/
    public void setROverhang(String overhang) {
        _rOverhang = overhang;
    }
    
    /** Add neighbor node to end of the list **/
    public void addNeighbor(RNode newNeighbor) {
        _neighbors.add(newNeighbor);
    }
    
    /** Remove a node's neighbor **/
    public void removeNeighbor(RNode neighbor) {
        _neighbors.remove(neighbor);
    }

    
    /** Replace a neighbor with the same composition at an exact point in the list to conserve order **/
    public void replaceNeighbor(RNode oldNode, RNode newNode) {
        int indexOf = _neighbors.indexOf(oldNode);
        _neighbors.remove(indexOf);
        _neighbors.add(indexOf, newNode);
    }
    
    /** Set node composition **/
    public void setComposition(ArrayList<String> comp) {
        _composition = comp;
    }
    
    /** Set vector **/
    public void setVector(RVector vector) {
        _vector = vector;
    }
    
    /** Set name **/
    public void setName(String name) {
        _name = name;
    }
    
    /** Set stage of the node **/
    public void setStage(int stage) {
        _stage = stage;
    }
    
    /** Set the efficiency of a node **/
    public void setEfficiency(double eff) {
        _efficiency = eff;
    }
    
    /** Set the modularity of a node **/
    public void setModularity(double mod) {
        _modularity = mod;
    }
    
    /** Set success count **/
    public void setSuccessCnt(int success) {
        _successCnt = success;
    }
    
    /** Set failure count **/
    public void setFailureCnt(int failure) {
        _failureCnt = failure;
    }
    
    /** Set the direction of the node composition **/
    public void setDirection(ArrayList<String> direction) {
        _direction = direction;
    }
    
    /** Set the scars for a node **/
    public void setScars(ArrayList<String> scars) {
        _scars = scars;
    }
    
    //FIELDS
    private int _successCnt;
    private int _failureCnt;
    private double _efficiency;
    private double _modularity;
    private boolean _recommended;
    private boolean _discouraged;
    private ArrayList<RNode> _neighbors;
    private ArrayList<String> _direction;
    private String _uuid;
    private ArrayList<String> _composition;
    private ArrayList<String> _type;
    private ArrayList<String> _scars;
    private String _lOverhang;
    private String _rOverhang;
    private RVector _vector;
    private String _name;
    private int _nodeID;
    private int _stage;
    private static int _nodeCount = 0;
}
