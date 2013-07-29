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

    public String leftOverhang = null;
    public String rightOverhang = null;
    public int level = 0;
    public ArrayList<CartesianNode> neighbors = new ArrayList();
}
