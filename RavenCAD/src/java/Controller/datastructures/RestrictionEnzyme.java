/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import Controller.algorithms.PrimerDesign;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author evanappleton
 */
public class RestrictionEnzyme {

    /** Restriction enzyme constructor **/
    //Convention for cut sites: integer index refers to index of recogition sequence where cut occurs directly before
    //The recognition sequence must include forward sequence extended out to end of forward or reverse cut, whichever is furthest
    public RestrictionEnzyme(String name, String recSeq, ArrayList<Integer> cutSites, String buffer, Double temp, ArrayList<Double> HI) {
        
        _name = name;
        _buffer = buffer;
        _incubationTemp = temp;
        _heatInactivation = HI;
        
        //Convert recogition site indexes from forward to reverse
        ArrayList<Integer> revCutSites = new ArrayList<Integer>(2);
        int rCut0 = cutSites.get(1) - recSeq.length();
        int rCut1 = cutSites.get(0) - recSeq.length();
        revCutSites.add(rCut0);
        revCutSites.add(rCut1);
        
        //Convert the recognition to a regular expression
        String lRecSeq = recSeq.toLowerCase();
        String lRevRecSeq = PrimerDesign.reverseComplement(recSeq);        
        
        ArrayList<String> recSeqs = new ArrayList<String>(2);
        ArrayList<String> regExRecSeqs = new ArrayList<String>();
        recSeqs.add(lRecSeq);
        recSeqs.add(lRevRecSeq);
        
        //For the forward and reverse recognition sequences
        for (int i = 0; i < recSeqs.size(); i++) {
            String seq = recSeqs.get(i);
            String regExSeq = new String();
            
            //Scan through the sequence and translate to a regular expression
            for (int j = 0; j < seq.length(); j++) {
     
                char nuc = seq.charAt(i);
                if (nuc == 'a' || nuc == 'c' || nuc == 'g' || nuc == 't' || nuc == 'u') {
                    regExSeq = regExSeq + nuc;
                } else if (nuc == 'w') {
                    regExSeq = regExSeq + "[at]{1}";
                } else if (nuc == 's') {
                    regExSeq = regExSeq + "[cg]{1}";
                } else if (nuc == 'm') {
                    regExSeq = regExSeq + "[ac]{1}";
                } else if (nuc == 'k') {
                    regExSeq = regExSeq + "[tg]{1}";
                } else if (nuc == 'r') {
                    regExSeq = regExSeq + "[ag]{1}";
                } else if (nuc == 'y') {
                    regExSeq = regExSeq + "[ct]{1}";
                } else if (nuc == 'b') {
                    regExSeq = regExSeq + "[ctg]{1}";
                } else if (nuc == 'd') {
                    regExSeq = regExSeq + "[atg]{1}";
                } else if (nuc == 'h') {
                    regExSeq = regExSeq + "[atc]{1}";
                } else if (nuc == 'v') {
                    regExSeq = regExSeq + "[acg]{1}";
                } else if (nuc == 'n') {
                    regExSeq = regExSeq + "[actg]{1}";
                }
            }
            
            regExRecSeqs.add(regExSeq);
        }
        
        _fwdRecSeq = regExRecSeqs.get(0);
        _revRecSeq = regExRecSeqs.get(0);
        _fwdCutSites = cutSites;
        _revCutSites = revCutSites;
        _reID = _reCount;
        _reCount++;
        _uuid = null;
    }
    
    //THIS NEXT METHOD USES RESTRICTION ENZYMES WHICH ARE OUTSIDE THE CLOTHO DATA MODEL, UNCLEAR WHERE THIS METHOD SHOULD GO
    
    /** Scan a set of parts for restriction sites **/
    //HashMap<Part, HashMap<Restriction Enzyme name, ArrayList<ArrayList<Start site, End site>>>>
    public static HashMap<Part, HashMap<String, ArrayList<ArrayList<Integer>>>> reSeqScan(ArrayList<Part> parts, ArrayList<RestrictionEnzyme> enzymes) {
        
        HashMap<Part, HashMap<String, ArrayList<ArrayList<Integer>>>> partEnzResSeqs = new HashMap<Part, HashMap<String, ArrayList<ArrayList<Integer>>>>();
        
        //For all parts
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            String name = part.getName();
            String seq = part.getSeq();
            HashMap<String, ArrayList<ArrayList<Integer>>> detectedResSeqs = new HashMap<String, ArrayList<ArrayList<Integer>>>();
            
            //Look at each enzyme's cut sites
            for (int j = 0; j < enzymes.size(); j++) {
                ArrayList<ArrayList<Integer>> matchSites = new ArrayList<ArrayList<Integer>>();
                RestrictionEnzyme enzyme = enzymes.get(j);
                String enzName = enzyme.getName();
                String fwdRec = enzyme.getFwdRecSeq();
                String revRec = enzyme.getRevRecSeq();
                
                //Compile regular expressions
                Pattern compileFwdRec = Pattern.compile(fwdRec, Pattern.CASE_INSENSITIVE);
                Pattern compileRevRec = Pattern.compile(revRec, Pattern.CASE_INSENSITIVE);
                Matcher matcherFwdRec = compileFwdRec.matcher(seq);
                Matcher matcherRevRec = compileRevRec.matcher(seq);
                
                //Find matches of forward sequence
                while (matcherFwdRec.find()) {
                    ArrayList<Integer> matchIndexes = new ArrayList<Integer>(2);
                    int start = matcherFwdRec.start();
                    int end = matcherFwdRec.end();
                    matchIndexes.add(start);
                    matchIndexes.add(end);
                    matchSites.add(matchIndexes);
                }
                
                //Find matches of reverse sequence
                while (matcherRevRec.find()) {
                    ArrayList<Integer> matchIndexes = new ArrayList<Integer>(2);
                    int start = matcherRevRec.start();
                    int end = matcherRevRec.end();
                    matchIndexes.add(start);
                    matchIndexes.add(end);
                    matchSites.add(matchIndexes);
                }
                
                detectedResSeqs.put(enzName, matchSites);
            }
            partEnzResSeqs.put(part, detectedResSeqs);
        }
        
        return partEnzResSeqs;
    }

    /** Get name **/
    public String getName() {
        return _name;
    }

    /** Get forward recognition site **/
    public String getFwdRecSeq() {
        return _fwdRecSeq;
    }

    /** Get reverse recognition site **/
    public String getRevRecSeq() {
        return _revRecSeq;
    }
    
    /** Get the forward cut sites **/
    //Convention for cut sites: integer index refers to index of recogition sequence where cut occurs directly before
    public ArrayList<Integer> getFwdCutSites() {
        return _fwdCutSites;
    }
    
    /** Get the forward cut sites **/
    //Convention for cut sites: integer index refers to index of recogition sequence where cut occurs directly before
    public ArrayList<Integer> getRevCutSites() {
        return _revCutSites;
    }
    
    /** Get the buffer **/
    public String getBuffer() {
        return _buffer;
    }
    
    /** Get the incubation time **/
    public Double getIncubationTemp() {
        return _incubationTemp;
    }

    /** Get the heat inactivation parameters... temp first, then time **/
    public ArrayList<Double> getHeatInactivation() {
        return _heatInactivation;
    }
    
    /** Get UUID **/
    public String getUUID() {
        return _uuid;
    }
    
    /** Get reID **/
    public int getREID() {
        return _reID;
    }
    
    /** Set UUID **/
    public void setUUID (String uuid) {
        _uuid = uuid;
    }
    
    //FIELDS
    private final String _name;
    private final String _buffer;
    private final String _fwdRecSeq;
    private final Double _incubationTemp; 
    private final ArrayList<Double> _heatInactivation;
    private final ArrayList<Integer> _fwdCutSites;
    private final String _revRecSeq;
    private final ArrayList<Integer> _revCutSites;
    private int _reID;
    private String _uuid;
    private static int _reCount;
}
