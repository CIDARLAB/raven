/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moclocartesiangraphassigner;

import java.util.ArrayList;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class CartesianMetaNode {

    public CartesianMetaNode(String overhang) {
        this._nodes = new ArrayList();
        this._neighbors = new ArrayList();
        this._relationshipTypes = new ArrayList(); //two types; left matches right of neighbor, or left does not match right of neighbor
        this._overhang = overhang;
    }

    public ArrayList<CartesianNode> getNodes() {
        return _nodes;
    }

    public void setNodes(ArrayList<CartesianNode> _nodes) {
        this._nodes = _nodes;
    }

    public ArrayList<CartesianMetaNode> getNeighbors() {
        return _neighbors;
    }

    public void setNeighbors(ArrayList<CartesianMetaNode> _neighbors) {
        this._neighbors = _neighbors;
    }

    public ArrayList<Boolean> getRelationshipTypes() {
        return _relationshipTypes;
    }

    public void setRelationshipTypes(ArrayList<Boolean> _relationshipTypes) {
        this._relationshipTypes = _relationshipTypes;
    }
    
    //performs a cartesian product between two meta nodes 
    //returns one node that represents both of them
    public String _overhang;

    public String getOverhang() {
        return _overhang;
    }

    public void setOverhang(String _overhang) {
        this._overhang = _overhang;
    }
    public ArrayList<CartesianNode> _nodes;
    public ArrayList<CartesianMetaNode> _neighbors;
    public ArrayList<Boolean> _relationshipTypes;
}
