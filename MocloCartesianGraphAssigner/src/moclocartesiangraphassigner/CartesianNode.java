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
public class CartesianNode {

    public CartesianNode() {
        this.neighbors = new ArrayList();
        this.level = 0;
        this.rightOverhang = null;
        this.leftOverhang = null;
    }

    public String getLeftOverhang() {
        return leftOverhang;
    }

    public void setLeftOverhang(String leftOverhang) {
        this.leftOverhang = leftOverhang;
    }

    public String getRightOverhang() {
        return rightOverhang;
    }

    public void setRightOverhang(String rightOverhang) {
        this.rightOverhang = rightOverhang;
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
    public String leftOverhang; //left overhang option specified by this CartesianNode
    public String rightOverhang; //right overhang option specified by this CartesianNode
    public int level; //level of this node
    public ArrayList<CartesianNode> neighbors; //neighbors of this node
}
