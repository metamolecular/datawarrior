package com.actelion.research.util;

/**
 * <p>Title: Mercury</p>
 * <p>Description: Actelion Electronic Lab Notebook</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Christian Rufener
 * @version 1.0
 */
//import org.apache.regexp.*;

public class FormatHelper
{


    /**
     * Checks whether the format of the Chem Lab Journal is correct:
     * # = DIGIT
     * @ = CHARACTER (A-Z)
     * {x-y} = Repeat occurance min x max y times
     * Format may be LJC###-###, LJC###-###@[#]{1-3},LJC###-###.[#]{1-3}
     * @param s String to be checked
     * @return false if format is invalid
     */
    public static boolean isValidLabJournalNo(String s)
    {
        if (s != null) {
            return isValidLabJournalNo(s,s.length());   
        }
        return false;
    }

    /**
     * Checks whether the format of the Chem Lab Journal is correct up to max characters.
     * Note, that if max < 10 the function will return false since the minimal valid 
     * Format is 'LJC###-###'
     * @param s String to be checked
     * @param max   check will be performed up to max characters. Needs to be >= 10
     * @return false if format is invalid
     */
    public static boolean isValidLabJournalNo(String s, int min, int max)
    {
        
        boolean ok = false;
        if (s != null && s.length() >= min && s.length() <= max) {
            try {
                String p = s.substring(0,min);
                ok = com.actelion.research.util.ACTLabCodeParser.isValidLJC(p);
/*                
                RE re = new RE("LJC[0-9]{3}-[0-9]{3}([A-Z][0-9]{1,3}|\\.[0-9]{1,3}|)");
                if (re.match(p)) {
                    ok = p.equals(re.getParen(0));
                }
 */
            } catch (Exception e) {
                System.err.println("FormatHelper.isValidLabJournal Error " + e);   
//                e.printStackTrace();
            }
        }
        return ok;
    }

    /**
     * Checks whether the format of the Chem Lab Journal is correct up to max characters.
     * Note, that if max < 10 the function will return false since the minimal valid 
     * Format is 'LJC###-###'
     * @param s String to be checked
     * @param max   check will be performed up to max characters. Needs to be >= 10
     * @return false if format is invalid
     */
    public static boolean isValidLabJournalNo(String s, int max)
    {
        
        boolean ok = false;
        if (s != null && s.length() >= max) {
            try {
                String p = s.substring(0,max);
                ok = com.actelion.research.util.ACTLabCodeParser.isValidLJC(p);
/*                
                RE re = new RE("LJC[0-9]{3}-[0-9]{3}([A-Z][0-9]{1,3}|\\.[0-9]{1,3}|)");
                if (re.match(p)) {
                    ok = p.equals(re.getParen(0));
                }
 */
            } catch (Exception e) {
                System.err.println("FormatHelper.isValidLabJournal Error " + e);   
//                e.printStackTrace();
            }
        }
        return ok;
    }


    /**
     * Checks the format of the ACT-Number: Needs to be of the form:
     * ACT-###### or ACT-######@
     * @param s Actelion No to be checked
     * @return false if format is invalid
     */
    public static boolean isValidActelionNo(String s)
    {
        boolean ok = false;
        if (s != null) {
            try {
                ok = com.actelion.research.util.ACTLabCodeParser.isValidACTNo(s);

/*                
                RE re = new RE("ACT-[0-9]{6}[A-Z]?");
                if (re.match(s)) {
                    ok = s.equals(re.getParen(0));
                }
 */
            } catch (Exception e) {
                System.err.println("FormatHelper.isValidActelionNo Error " + e);   
//                e.printStackTrace();
            }
        }
        return ok;
    }


    /**
     * Checks whether the format of the Chem Lab Journal is correct up to max characters.
     * Note, that if max < 10 the function will return false since the minimal valid 
     * Format is 'LJC###-###'
     * @param s String to be checked
     * @param max   check will be performed up to max characters. Needs to be >= 10
     * @return false if format is invalid
     */
    public static boolean isValidBioLabJournalNo(String s, int max)
    {
        
        boolean ok = false;
        if (s != null && s.length() >= max) {
            try {
                String p = s.substring(0,max);
                ok = com.actelion.research.util.ACTLabCodeParser.isValidELB(p);
            } catch (Exception e) {
                System.err.println("FormatHelper.isValidBioLabJournalNo Error " + e);   
            }
        }
        return ok;
    }

    public static boolean isValidBioLabJournalNo(String s)
    {
        return isValidBioLabJournalNo(s,s != null ? s.length() : 0);
    }

    
    /**
     * Checks the format of the plate-position: Needs to be of the form:
     * A@-#######:@/##
     * @param s plate position to be checked
     * @return false if format is invalid
     */
    public static boolean isValidPlatePosition(String s)
    {
        return (s == null) ? false : s.matches("A[A-Z]\\-[0-9]{7}:[A-P]/[0-9]{2}");
    }

    /**
     * Checks the format of the inventory barcode: Needs to be of the form:
     * AI######
     * @param s Inventory barcode no to be checked
     * @return false if format is invalid
     */
    public static boolean isValidInventoryBarcode(String s)
    {
        return (s == null) ? false : s.matches("AI[0-9]{6}");
    }
}
