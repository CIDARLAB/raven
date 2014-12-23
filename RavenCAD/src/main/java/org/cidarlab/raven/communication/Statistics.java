/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cidarlab.raven.communication;

import java.text.DecimalFormat;

/**
 *
 * @author jenhantao,evanappleton
 */
public class Statistics {

    /**
     * Set statistics stages *
     */
    public void setStages(int stages) {
        _stages = stages;
    }

    /**
     * Set statistics steps *
     */
    public void setSteps(int steps) {
        _steps = steps;
    }

    /**
     * Set statistics sharing *
     */
    public void setSharing(int asmTime) {
        _sharing = asmTime;
    }

    /**
     * Set statistics forbidden count of solution graphs. Should == 0 *
     */
    public void setForbidden(int cost) {
        _forbidden = cost;
    }

    /**
     * Set statistics number of goal parts *
     */
    public void setGoalParts(int goalParts) {
        _goalParts = goalParts;
    }

    /**
     * Set statistics recommended intermediate count of solution graphs *
     */
    public void setRecommended(int stIntermediates) {
        _recommended = stIntermediates;
    }

    /**
     * Set statistics discouraged intermediate count of solution graphs *
     */
    public void setDiscouraged(int discouraged) {
        _discouraged = discouraged;
    }

    /**
     * Set statistics required intermediate count of solution graphs *
     */
    public void setRequired(int ftIntermediates) {
        _required = ftIntermediates;
    }

    /**
     * Set modularity *
     */
    public void setModularity(double modularity) {
        _modularity = modularity;
    }

    /**
     * Set modularity *
     */
    public void setEfficiency(double efficiency) {
        _efficiency = efficiency;
    }

    /**
     * Set algorithm execution time *
     */
    public void setExecutionTime(long executionTime) {
        _executionTime = executionTime;
    }

    /**
     * Set reactions score *
     */
    public void setReaction(int numReactions) {
        _reactions = numReactions;
    }

    /**
     * Get statistics stages *
     */
    public String getStages() {
        return new Integer(_stages).toString();
    }

    /**
     * Get statistics steps *
     */
    public String getSteps() {
        return new Integer(_steps).toString();
    }

    /**
     * Get statistics sharing *
     */
    public String getSharing() {
        return new Integer(_sharing).toString();
    }

    /**
     * Get statistics forbidden count of solution graphs. Should == 0 *
     */
    public String getForbidden() {
        return new Integer(_forbidden).toString();
    }

    /**
     * Get statistics number of goal parts *
     */
    public String getGoalParts() {
        return new Integer(_goalParts).toString();
    }

    /**
     * Get statistics recommended intermediate count of solution graphs *
     */
    public String getRecommended() {
        return new Integer(_recommended).toString();
    }

    /**
     * Get statistics discouraged intermediate count of solution graphs *
     */
    public String getDiscouraged() {
        return new Integer(_discouraged).toString();
    }

    /**
     * Get statistics required intermediate count of solution graphs *
     */
    public String getRequired() {
        return new Integer(_required).toString();
    }

    /**
     * Get modularity score *
     */
    public String getModularity() {
        float floatValue = new Double(_modularity).floatValue();
        String mod = new Float(floatValue).toString();
        if (mod.length() > 6) {
            mod = mod.substring(0, 6);
        }
        return mod;
    }

    /**
     * Get efficiency score *
     */
    public String getEfficiency() {
        float floatValue = new Double(_efficiency).floatValue();
        String eff = new Float(floatValue).toString();
        if (eff.length() > 6) {
            eff = eff.substring(0, 6);
        }
        return eff;
    }

    /**
     * Get reactions score *
     */
    public String getReactions() {
        return String.valueOf(_reactions);
    }

    /**
     * Get algorithm execution time *
     */
    public String getExecutionTime() {
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(_executionTime) + "ms";
    }

    public static void start() {
        _start = System.currentTimeMillis();
    }

    public static void stop() {
        _end = System.currentTimeMillis();
    }

    public static long getTime() {
        return _end - _start;
    }

    public void setValid(boolean b) {
        _isValid = b;
    }

    public boolean isValid() {
        return _isValid;
    }
    //FIELDS
    private int _stages;
    private int _steps;
    private int _sharing;
    private int _forbidden;
    private int _goalParts;
    private int _recommended;
    private int _discouraged;
    private int _required;
    private double _modularity;
    private double _efficiency;
    private long _executionTime;
    private static long _start;
    private static long _end;
    private int _reactions;
    private boolean _isValid = false;
}