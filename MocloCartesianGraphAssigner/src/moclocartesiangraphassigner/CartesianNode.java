/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moclocartesiangraphassigner;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class CartesianNode {

    public CartesianNode() {
        this.neighbors = new ArrayList();
        this.level = 0;
        this.abstractOverhang = null;
        this.concreteOverhang = null;
        this.usedOverhangs = new HashSet();
    }

    public String getConcreteOverhang() {
        return concreteOverhang;
    }

    public void setConcreteOverhang(String overhang) {
        this.concreteOverhang = overhang;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public ArrayList<CartesianNode> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<CartesianNode> neighbors) {
        this.neighbors = neighbors;
    }

    public HashSet getUsedOverhangs() {
        return usedOverhangs;
    }

    public void setUsedOverhangs(HashSet usedOverhangs) {
        this.usedOverhangs = usedOverhangs;
    }

    void addNeighbor(CartesianNode node) {
        this.neighbors.add(node);
    }

    public String getAbstractOverhang() {
        return abstractOverhang;
    }

    public void setAbstractOverhang(String abstractOverhang) {
        this.abstractOverhang = abstractOverhang;
    }
    //fields
    public String concreteOverhang; //right concreteOverhang option specified by this CartesianNode
    public int level; //level of this node
    public ArrayList<CartesianNode> neighbors; //neighbors of this node
    public HashSet usedOverhangs;
    public String abstractOverhang;
}
