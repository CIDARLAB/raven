/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;

/**
 *
 * @author jenhantao, evanappleton
 */
public class SRSNode {

    /** SDSNode constructor, no neighbors, parent or children or meta-data specified **/
    public SRSNode() {
        _recommended = false;
        _discouraged = false;
        _efficiency = 0;
        _neighbors = new ArrayList<SRSNode>();
        _composition = new ArrayList<String>();
        _uuid = null;
        _type = new ArrayList<String>();
        _lOverhang = "";
        _rOverhang = "";
//        _vector = new SRSVector();
        _name = "";
        _stage = 0;
        _nodeID = _nodeCount;
        _nodeCount++;
    }

    /** SDSNode constructor for intermediates with meta-data, neighbors and composition, but no part**/
    public SRSNode(boolean recommended, boolean discouraged, ArrayList<SRSNode> neighbors, ArrayList<String> composition, ArrayList<String> type, boolean success) {
        _uuid = null;
        _recommended = recommended;
        _discouraged = discouraged;
        _efficiency = 0;
        _success = success;
        _neighbors = neighbors;
        if (_neighbors == null) {
            _neighbors = new ArrayList<SRSNode>();
        }
        _composition = composition;
        _type = type;
        _lOverhang = "";
        _rOverhang = "";
//        _vector = vector;
        _name = "";
        _nodeID = _nodeCount;
        _nodeCount++;
    }
    
    /** Clone nodes of a graph by traversing and copying nodes **/
    @Override
    public SRSNode clone() {
        
        SRSNode clone = new SRSNode();
        clone._recommended = this._recommended;
        clone._discouraged = this._discouraged;
        clone._uuid = this._uuid;
        clone._type = this._type;
        clone._lOverhang = this._lOverhang;
        clone._rOverhang = this._rOverhang;
        clone._composition = this._composition;
        clone._name = this._name;
        clone._stage = this._stage;
        clone._vector = this._vector;
        clone._efficiency = this._efficiency;
        clone._success = this._success;
        ArrayList<SRSNode> neighbors = this._neighbors;
        cloneHelper(clone, this, neighbors);
        
        return clone;
    }
    
    private void cloneHelper(SRSNode parentClone, SRSNode parent, ArrayList<SRSNode> neighbors) {
        
        for (int i = 0; i < neighbors.size(); i++) {
            
            SRSNode neighbor = neighbors.get(i);
            
            SRSNode neighborClone = new SRSNode();
            neighborClone._recommended = neighbor._recommended;
            neighborClone._discouraged = neighbor._discouraged;
            neighborClone._uuid = neighbor._uuid;
            neighborClone._type = neighbor._type;
            neighborClone._lOverhang = neighbor._lOverhang;
            neighborClone._rOverhang = neighbor._rOverhang;
            neighborClone._composition = neighbor._composition;
            neighborClone._name = neighbor._name;
            neighborClone._stage = neighbor._stage;
            neighborClone._vector = neighbor._vector;
            neighborClone._efficiency = neighbor._efficiency;
            neighborClone._success = neighbor._success;
            
            parentClone.addNeighbor(neighborClone);
            neighborClone.addNeighbor(parentClone);
            
            if (neighbor.getStage() > 0) {
                ArrayList<SRSNode> orderedChildren = new ArrayList<SRSNode>();
                orderedChildren.addAll(neighbor.getNeighbors());

                //Remove the current parent from the list
                if (orderedChildren.contains(parent)) {
                    orderedChildren.remove(parent);
                }
                cloneHelper(neighborClone, neighbor, orderedChildren);
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
    public ArrayList<SRSNode> getNeighbors() {
        return _neighbors;
    }

    /** Get node composition **/
    public ArrayList<String> getComposition() {
        return _composition;
    }
    
    /** Get vector **/
    public SRSVector getVector() {
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
    
    /** Return success or failure - for debugging **/
    public boolean getSuccess() {
        return _success;
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
    public void addNeighbor(SRSNode newNeighbor) {
        _neighbors.add(newNeighbor);
    }
    
    /** Remove a node's neighbor **/
    public void removeNeighbor(SRSNode neighbor) {
        _neighbors.remove(neighbor);
    }

    
    /** Replace a neighbor with the same composition at an exact point in the list to conserve order **/
    public void replaceNeighbor(SRSNode oldNode, SRSNode newNode) {
        int indexOf = _neighbors.indexOf(oldNode);
        _neighbors.remove(indexOf);
        _neighbors.add(indexOf, newNode);
    }
    
    /** Set node composition **/
    public void setComposition(ArrayList<String> comp) {
        _composition = comp;
    }
    
    /** Set vector **/
    public void setVector(SRSVector vector) {
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
    
    /** Set success **/
    public void setSuccess(boolean success) {
        _success = success;
    }
    
    //FIELDS
    private boolean _success;
    private double _efficiency;
    private double _modularity;
    private boolean _recommended;
    private boolean _discouraged;
    private ArrayList<SRSNode> _neighbors;
    private String _uuid;
    private ArrayList<String> _composition;
    private ArrayList<String> _type;
    private String _lOverhang;
    private String _rOverhang;
    private SRSVector _vector;
    private String _name;
    private int _nodeID;
    private int _stage;
    private static int _nodeCount = 0;
}