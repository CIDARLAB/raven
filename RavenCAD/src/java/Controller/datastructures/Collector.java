/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JOptionPane;

/**
 *
 * @author jenhantao
 */
public class Collector {

    public Part getPart(String uuid) {
        return partUUIDHash.get(uuid);
    }

    public Vector getVector(String uuid) {
        return vectorUUIDHash.get(uuid);
    }

    public ArrayList<Vector> getAllVectors() {
        return new ArrayList(vectorUUIDHash.values());
    }

    public ArrayList<Part> getAllParts() {
        return new ArrayList(partUUIDHash.values());
    }

    public Vector getVectorByName(String name) {
        Vector toReturn = vectorUUIDHash.get(vectorNameHash.get(name));

        if (toReturn == null) {
            if (!name.contains("|")) {
                toReturn = vectorUUIDHash.get(vectorNameHash.get(name + "||"));
                if (toReturn == null && vectorNameHash.get(name) != null) {
                    String next = vectorNameRootHash.get(name).iterator().next();
                    toReturn = vectorUUIDHash.get(vectorNameHash.get(next));
                }
            } else {
                //no vector without overhangs exist, return any variant
                if (vectorNameRootHash.get(name.split("|")[0]) != null) {
                    String next = vectorNameRootHash.get(name.split("|")[0]).iterator().next();
                    toReturn = vectorUUIDHash.get(vectorNameHash.get(next));
                }
            }
        }
        return toReturn;
    }

    public Vector getVectorByExactName(String name) {
        return vectorUUIDHash.get(vectorNameHash.get(name));

    }

    public Part getPartByExactName(String name) {
        return partUUIDHash.get(partNameHash.get(name));

    }

    public Part getPartByName(String name) {
        Part toReturn = partUUIDHash.get(partNameHash.get(name));

        if (toReturn == null) {
            if (!name.contains("|")) {
                toReturn = partUUIDHash.get(partNameHash.get(name + "||"));
                if (toReturn == null && partNameRootHash.get(name) != null) {
                    String next = partNameRootHash.get(name).iterator().next();
                    toReturn = partUUIDHash.get(partNameHash.get(next));
                }
            } else {
                //no part without overhangs exist, return any variant
                if (partNameRootHash.get(name.split("|")[0]) != null) {
                    String next = partNameRootHash.get(name.split("|")[0]).iterator().next();
                    toReturn = partUUIDHash.get(partNameHash.get(next));
                }
            }
        }
        return toReturn;
    }

    public Boolean addPart(Part aPart) {
        Part sameNamePart = this.getPartByName(aPart.getName());
        if (sameNamePart != null) {
            if (sameNamePart.getLeftOverhang().equals(aPart.getLeftOverhang()) && sameNamePart.getRightOverhang().equals(aPart.getRightOverhang())) {
//                int selValue = JOptionPane.showConfirmDialog(null, "Part: " + aPart.getName() + " is already imported!\n Do you want to overrwrite it?");
                int selValue = JOptionPane.OK_OPTION;
                //TODO figure out error handling here
                if (selValue == JOptionPane.OK_OPTION) {
//                    basicPartNameHash.put(aPart.getName(), aPart.getUUID());
                    partUUIDHash.remove(partNameHash.get(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang()));
                    partNameHash.put(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang(), aPart.getUUID());
                    partUUIDHash.put(aPart.getUUID(), aPart);
                    if (partNameRootHash.get(aPart.getName()) == null) {
                        HashSet<String> toAdd = new HashSet<String>();
                        toAdd.add(aPart.getName());
                        partNameRootHash.put(aPart.getName(), toAdd);
                        partNameRootHash.get(aPart.getName()).add(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang());
                        System.out.println("before: " + partUUIDHash.keySet());
                    } else {
                        partNameRootHash.get(aPart.getName()).add(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang());
                    }
                } else if (selValue == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
            } else {
                partNameHash.put(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang(), aPart.getUUID());
                partUUIDHash.put(aPart.getUUID(), aPart);
                if (partNameRootHash.get(aPart.getName()) == null) {
                    HashSet<String> toAdd = new HashSet<String>();
                    partNameRootHash.put(aPart.getName(), toAdd);
                    partNameRootHash.get(aPart.getName()).add(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang());
                } else {
                    partNameRootHash.get(aPart.getName()).add(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang());
                }
            }
        } else {
            partNameHash.put(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang(), aPart.getUUID());
            partUUIDHash.put(aPart.getUUID(), aPart);
            if (partNameRootHash.get(aPart.getName()) == null) {
                HashSet<String> toAdd = new HashSet<String>();
                partNameRootHash.put(aPart.getName(), toAdd);
                partNameRootHash.get(aPart.getName()).add(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang());
            } else {
                partNameRootHash.get(aPart.getName()).add(aPart.getName() + "|" + aPart.getLeftOverhang() + "|" + aPart.getRightOverhang());
            }
        }
        return true;

    }

    public Boolean addVector(Vector aVector) {
        Vector sameNameVector = this.getVectorByName(aVector.getName());
        if (sameNameVector != null) {
            if (sameNameVector.getLeftoverhang().equals(aVector.getLeftoverhang()) && sameNameVector.getRightOverhang().equals(aVector.getRightOverhang())) {
//                int selValue = JOptionPane.showConfirmDialog(null, "Vector: " + aVector.getName() + " is already imported!\n Do you want to overrwrite it?");
                int selValue = JOptionPane.OK_OPTION;
                //TODO figure out error handling here
                if (selValue == JOptionPane.OK_OPTION) {
//                    basicVectorNameHash.put(aVector.getName(), aVector.getUUID());
                    vectorUUIDHash.remove(vectorNameHash.get(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang()));
                    vectorNameHash.put(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang(), aVector.getUUID());
                    vectorUUIDHash.put(aVector.getUUID(), aVector);
                    if (vectorNameRootHash.get(aVector.getName()) == null) {
                        HashSet<String> toAdd = new HashSet<String>();
                        toAdd.add(aVector.getName());
                        vectorNameRootHash.put(aVector.getName(), toAdd);
                        vectorNameRootHash.get(aVector.getName()).add(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang());
                    } else {
                        vectorNameRootHash.get(aVector.getName()).add(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang());
                    }
                } else if (selValue == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
            } else {
                vectorNameHash.put(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang(), aVector.getUUID());
                vectorUUIDHash.put(aVector.getUUID(), aVector);
                if (vectorNameRootHash.get(aVector.getName()) == null) {
                    HashSet<String> toAdd = new HashSet<String>();
                    vectorNameRootHash.put(aVector.getName(), toAdd);
                    vectorNameRootHash.get(aVector.getName()).add(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang());
                } else {
                    vectorNameRootHash.get(aVector.getName()).add(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang());
                }
            }
        } else {
            vectorNameHash.put(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang(), aVector.getUUID());
            vectorUUIDHash.put(aVector.getUUID(), aVector);
            if (vectorNameRootHash.get(aVector.getName()) == null) {
                HashSet<String> toAdd = new HashSet<String>();
                vectorNameRootHash.put(aVector.getName(), toAdd);
                vectorNameRootHash.get(aVector.getName()).add(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang());
            } else {
                vectorNameRootHash.get(aVector.getName()).add(aVector.getName() + "|" + aVector.getLeftoverhang() + "|" + aVector.getRightOverhang());
            }
        }
        return true;
    }

    public boolean removePart(String uuid) throws Exception {
        partUUIDHash.remove(uuid);
        return true;
    }

    public boolean removeVector(String uuid) throws Exception {
        vectorUUIDHash.remove(uuid);
        return true;
    }
    private HashMap<String, Vector> vectorUUIDHash = new HashMap(); //key: uuid, value: vector
    private HashMap<String, String> vectorNameHash = new HashMap(); //key name, value uuid
    private HashMap<String, String> partNameHash = new HashMap(); //key name, value uuid
    private HashMap<String, Part> partUUIDHash = new HashMap(); //key:uuid, value: part
    private HashMap<String, HashSet<String>> partNameRootHash = new HashMap(); //key: part name root, value: arrayList of part names that contains the name root
    //name root is essentially a part name without extra overhang information
    private HashMap<String, HashSet<String>> vectorNameRootHash = new HashMap(); //key: vector name root, value: arrayList of vector names that contains the name root
    //name root is essentially a vector name without extra overhang information
    public void purge() {
        vectorUUIDHash = new HashMap();
        vectorNameHash = new HashMap();
        partNameHash = new HashMap();
        partUUIDHash = new HashMap();
        partNameRootHash = new HashMap();
        vectorNameRootHash = new HashMap();
        

    }
}
