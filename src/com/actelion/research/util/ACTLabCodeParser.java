/*
 * Created on Oct 15, 2004
 *
 */
package com.actelion.research.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Korff, CxR
 */
public class ACTLabCodeParser
{

    //public static final int ELN_LENGTH = 11;                                  // 123456789012345678
    public static final int ELN_LENGTH = 11;                                    // ELN123-1234
    public static final int ACT_NO_LENGTH_PARENT_OLD = 10;                      // ACT-012345
    public static final int ACT_NO_LENGTH_OLD = ACT_NO_LENGTH_PARENT_OLD + 1;  // ACT-012345
    public static final int ACT_NO_LENGTH_PARENT = 13;                          // ACT-1234-5678
    public static final int ACT_NO_LENGTH = ACT_NO_LENGTH_PARENT + 1;           // ACT-1234-5678A

    public static final String SEP_ACTNO = ",; \t";

    // Biol Lab Journal
    public static final String LAB_JOURNAL = "LJ[^C]";
    public static final String LAB_JOURNAL_NUMBER = "[0-9]{3}\\-[0-9]{3}";
    //    public static final String LAB_JOURNAL_OPTIONAL = "(\\.[0-9][0-9]?)?";
    public static final String LAB_JOURNAL_OPTIONAL = "(\\.[A-Z0-9][A-Z0-9]?)?";

    // Actelion Number
    public static final String ACT_PREFIX = "ACT\\-";
    public static final String ACT_NUMBER_OLD = "[0-9]{6}[A-Z]?";
    public static final String ACT_NUMBER = "(([0-9]{6})|([0-9]{4}-[0-9]{4}))[A-Z]?";
    public static final String ACT_NUMBER_NEW = "[0-9]{4}-[0-9]{4}[A-Z]?";

    // Chem Lab Journal
    public static final String LAB_JOURNAL_CHEM = "LJC";
    public static final String LAB_JOURNAL_CHEM_NUMBER = "[0-9]{3}\\-[0-9]{3}";
    //    public static final String LAB_JOURNAL_CHEM_OPTIONAL = "(([(\\.[A-Z])][0-9])[0-9]?)?";
    public static final String LAB_JOURNAL_CHEM_OPTIONAL = "([A-Z][0-9]{1,3}|\\.[0-9]{1,3}|)";

    // ELN No.
    // 15.03.2005 Added by CxR
    public static final String ELN_PREFIX = "ELN";
    public static final String ELN_NUMBER = "[0-9]{3}\\-[0-9]{4}";
    public static final String ELN_OPTIONAL = "(\\.[A-Z][0-9]{1,2}|\\.[0-9]{1,3}|)";

    // ELB/R No.
    // 13.02.2007 Added by CxR
    // 06/02/2009 SAP Support
    public static final String ELB_PREFIX = "((EL(B|R|M))|(SAP))";
    public static final String ELB_NUMBER = "[0-9]{4}\\-[0-9]{4}";
    public static final String ELB_OPTIONAL = "(\\.[0-9]{1,3}|)";


    /**
     * Checks whether the format of the Bio Lab Journal is correct:
     * Format is given as regular expression
     *
     * @param str String to be checked
     * @return false if format is invalid
     */
    @Deprecated
    public static boolean isValidLJB(String str)
    {
        return str.matches(LAB_JOURNAL + LAB_JOURNAL_NUMBER + LAB_JOURNAL_OPTIONAL);
    }

//    public static boolean isNewActNo(String act)
//    {
//        return act != null && act.matches(ACT_PREFIX + ACT_NUMBER_NEW);
//    }

    /**
     * Checks whether the format of the Actelion number is correct:
     * This includes the old and the new format
     * Format is given as regular expression
     *
     * @param str String to be checked
     * @return false if format is invalid
     */
    public static boolean isValidACTNo(String str)
    {
        return str != null && str.matches(ACT_PREFIX + ACT_NUMBER);
    }

    /**
     * Checks whether the act no is a valid NEW ACT No.
     * @param str
     * @return
     */
    public static boolean isValidNewActNo(String str)
    {
        return str != null && str.matches(ACT_PREFIX + ACT_NUMBER_NEW);
    }

    /**
     * Checks whether the format of the Chem Lab Journal is correct:
     * This check only the old LJC format
     * Format is given as regular expression
     *
     * @param str String to be checked
     * @return false if format is invalid
     */
    @Deprecated
    public static boolean isValidLJC(String str)
    {
        return (str != null && str.matches(LAB_JOURNAL_CHEM + LAB_JOURNAL_CHEM_NUMBER + LAB_JOURNAL_CHEM_OPTIONAL))
            || isValidELN(str);
    }


    /**
     * Checks whether the format of the Chem Lab Journal is correct:
     * This check only the new ELN format
     * Format is given as regular expression
     *
     * @param str String to be checked
     * @return false if format is invalid
     */
    public static boolean isValidELN(String str)
    {
        return str != null && str.matches(ELN_PREFIX + ELN_NUMBER + ELN_OPTIONAL);
    }

    /**
     * Checks whether the format of the Bio Lab Journal is correct:
     * This does not include LJB and the like: Only ELMxxx, ELBxxx and ELRxxx are considered to be valid
     * @param str
     * @return
     */
    public static boolean isValidELB(String str)
    {
        return str != null && str.matches(ELB_PREFIX + ELB_NUMBER + ELB_OPTIONAL);
    }

    public static boolean isPrefix(String str)
    {
        String pattern = LAB_JOURNAL_CHEM + ".*|" + ELN_PREFIX + ".*";
        if (str != null)
            return str.matches(pattern);
        return false;

    }

    /**
     * Checks and normalized the format for the Bio Lab Journal.
     * Lower case is converted to upper case. If only the number is given, then
     * "LBJ" is added.
     *
     * @param str String to be checked and normalized.
     * @return normalized String.
     */
    public static String getNormalizedAndCheckedLJB(String str)
    {
        String sNorm = null;
        str = str.toUpperCase();
        if (str.matches(LAB_JOURNAL + LAB_JOURNAL_NUMBER + LAB_JOURNAL_OPTIONAL)) {
            sNorm = str;
        } else if (str.matches(LAB_JOURNAL_NUMBER + LAB_JOURNAL_OPTIONAL)) {
            sNorm = "LJB" + str;
        } else if (isValidELB(str))
            sNorm = str;
        return sNorm;
    }

    /**
     * Checks and normalized the format for the Chem Lab Journal.
     * Lower case is converted to upper case. If only the number is given, than
     * "LJC" is added.
     *
     * @param str String to be checked and normalized.
     * @return normalized String.
     */
    public static String getNormalizedAndCheckedLJC(String str)
    {
        String sNorm = null;
        str = str.toUpperCase();
        if (str.matches(LAB_JOURNAL_CHEM + LAB_JOURNAL_CHEM_NUMBER + LAB_JOURNAL_CHEM_OPTIONAL)) {
            sNorm = str;
        } else if (str.matches(LAB_JOURNAL_CHEM_NUMBER + LAB_JOURNAL_CHEM_OPTIONAL)) {
            sNorm = "LJC" + str;
        } else if (isValidELN(str)) {
            sNorm = str;
        }
        return sNorm;
    }

    /**
     * Check whether a lab jornal number starts with LJC than it is checked and normalized, otherwise not
     *
     * @param str String to be checked.
     * @return checked String.
     */
    public static String getNormalizedAndCheckedLJCAllowOthers(String str)
    {
        String sNorm = getNormalizedAndCheckedLJC(str);
        if (sNorm == null) {
            sNorm = str;
        }
        return sNorm;
    }


    /**
     * Checks and normalized the format for the Actelion number.
     * Lower case is converted to upper case. If only the number is given, than
     * "ACT-" is added.
     *
     * @param str String to be checked and normalized.
     * @return normalized Actelion No.
     */
    public static String getNormalizedAndCheckedACT(String str)
    {
        String sNorm = null;
        str = str.toUpperCase();
        if (isValidACTNo(str)) {
            sNorm = str;
        } else if (str.matches(ACT_NUMBER_OLD) || str.matches(ACT_NUMBER)) {
            sNorm = "ACT-" + str;
        }
        return sNorm;
    }

    /**
     * Checks and normalized the format for the Actelion number.
     * Lower case is converted to upper case. If only the number is given, than
     * "ACT-" is added. Saltletters are stripped off.
     *
     * @param str String to be checked and normalized.
     * @return normalized parent Actelion No.
     */
    public static String getParentACT(String str)
    {
        // Normalize first
        String parentActNo = getNormalizedAndCheckedACT(str);
        if (parentActNo != null && parentActNo.length() == ACT_NO_LENGTH)
            parentActNo = parentActNo.substring(0, ACT_NO_LENGTH_PARENT);
        else if (parentActNo != null && parentActNo.length() == ACT_NO_LENGTH_OLD)
            parentActNo = parentActNo.substring(0, ACT_NO_LENGTH_PARENT_OLD);
        return parentActNo;
    }

    public static String getParentELN(String eln)
    {
        if (isValidELN(eln)) {
            // 12345678901
            // ELN000-0000
            return eln.substring(0, ELN_LENGTH);
        }
        return null;
    }


    /**
     * Converts an Actelion into a unique id value.
     *
     * @param actno
     * @return MvK 02.04.2009
     */
    @Deprecated
    public static int getIdFromActNo(String actno)
    {
        int actId = -1;

        String post = null;

        int valLetter = 0;
        if (Character.isLetter(actno.charAt(actno.length() - 1))) {

            int letter = actno.charAt(actno.length() - 1);

            // A is 1
            valLetter = letter - 64;

            post = actno.substring(4, actno.length() - 1);
        } else {
            post = actno.substring(4, actno.length());
        }

        actId = Integer.parseInt(post);

        actId *= 100;

        actId += valLetter;

        return actId;
    }

    public static List<String> parse(String line)
    {
        List<String> liActNo = new ArrayList<String>();

        StringTokenizer st = new StringTokenizer(line, SEP_ACTNO);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            String sActNo = getNormalizedAndCheckedACT(s);
            if (sActNo == null) {
                throw new RuntimeException("Format error " + s + " is no valid ActNo.");
            }
            liActNo.add(sActNo);
        }
        return liActNo;
    }


//    private static String[] testPatternsLabJBiol()
//    {
//        String[] str = {
//            "123-456",
//            "ljP123-456",
//            "LJB123-456.1",
//            "LJB123-456.12",
//            "LJB123-456.",
//            "LJB123456",
//            "ELB0000-1234",
//            "ELB0000-1234.1",
//            "ELB0000-1234.1",
//            "ELB0000-1234.12",
//            "ELB0000-1234.123",
//            "ELR0000-1234",
//            "ELR0000-1234.1",
//
//            "ELB000-1234.",
//            "ELB0000-1234.1234",
//            "LJC123-456"
//        };
//        return str;
//    }

/*
    private static String[] testPatternsACTNumbers()
    {
        String[] str = {
            "ACT-123456",
            "act-123456",
            "ACT-123456A",
            "ACT123-456"
        };
        return str;
    }

    private static String[] testPatternsLJCNumbers()
    {
        String[] str = {
            "LJC123-456",
            "ljc123-456.1",
            "LJC123-456.12",
            "LJC123-456A12",
            "LJC123-456.",
            "LJC123-456.A",
            "LJC123-4561"
        };
        return str;
    }

    private static String[] testPatternsELNNumbers()
    {
        String[] str = {
            "ELN12345-123456",
            "ELN12345-123456.1",
            "ELN12345-123456.12",
            "ELN12345-123456.123",
            "ELN12345-123456.1234",
            "ELN12345-123456.A01",
            "ELN12345-123456.A02",
            "ELN12345-123456.A1",
            "ELN12345-123456.AA1",
            "ELN12345-123456.A123",
            "ELN1234-123456",
            "ELN123-123456",
            "ELN12-123456",
            "ELN1-123456",
            "ELN12345-123",
            "ELN12345-1234",
            "ELN12345-12345",
            "ELN12345-123456",
            "ELN12345-1234567",
            "ELN123-1234",
            "ELN123-1234.1",
            "ELN123-1234.A01",
        };
        return str;
    }
*/

/*
    public static void main(String[] args)
    {

       test1();
        test2();
    }
*/


/*
    // This was moved to test class
    private static void test2()
    {
        String[] TEST1 = {
            "ACT-012345",
            "ACT-012345A",
            "012345",
            "012345A",
            "123456Z",

            "ACT-1234-1234",
            "ACT-1234-1234Z",
            "1234-1234",
            "1234-1234Z",

        };

        String[] TEST2 = {
            "ACT-012345",
            "ACT-012345A",
        };

        String[] TEST3 = {
            "ACT-012345",
            "ACT-012345A",
            "ACT-1234-1234",
            "ACT-1234-1234Z",
        };

        String[] TEST4 = {
            "ACT-012345",
            "ACT-012345A",
            "ACT-1234-1234",
            "ACT-1234-1234Z",
            "012345",
            "012345A",
            "1234-1234",
            "1234-1234Z",
        };

        for (int i = 0; i < TEST2.length; i++) {
            String s = TEST2[i];
            System.out.printf("%s %s %s\n", s, !isValidNewActNo(s), isValidACTNo(s));
        }
        for (int ii = 0; ii < TEST3.length; ii++) {
            System.out.printf("%s %s\n", TEST3[ii], isValidACTNo(TEST3[ii]));
        }


        for (int i = 0; i < TEST1.length; i++) {
            String s = TEST1[i];
            String n = getNormalizedAndCheckedACT(s);
            System.out.printf("%s %s\n", n, isValidACTNo(n));
        }

        for (int i = 0; i < TEST4.length; i++) {
            String s = TEST4[i];
            String n = getParentACT(s);
            System.out.printf("%s %s\n", n, isValidACTNo(n));
        }
    }
*/


/*
    private static void test1()
    {
        String[] str = {
            "123-456",
            "ljP123-456",
            "LJB123-456.1",
            "LJB123-456.12",
            "LJB123-456.",
            "LJB123456",
            "ELB000-1234.",
            "ELB0000-1234.1234",
            "LJC123-456"
        };


        String[] str2 = {
            "ELB0000-1234",
            "ELB0000-1234.1",
            "ELB0000-1234.2",
            "ELB0000-1234.12",
            "ELB0000-1234.123",
            "ELR0000-1234",
            "ELR0000-1234.1",
            "ELM0000-1234",
            "ELM0000-1234.1",
            "ELM0000-1234.2",
            "ELM0000-1234.12",
            "ELM0000-1234.123",
            "ELM0000-1234",
            "ELM0000-1234.1",
            "ELR0000-1234",
            "ELR0000-1234.1",
            "ELR0000-1234.2",
            "ELR0000-1234.12",
            "ELR0000-1234.123",
            "ELR0000-1234",
            "ELR0000-1234.1",
        };


        String[] arr = str;
        System.out.println("Invalid Lab Journal Biol");
        for (int ii = 0; ii < arr.length; ii++) {
            String s = arr[ii];
            String n = getNormalizedAndCheckedLJB(s);
            System.out.printf("%s %s %s\n", s, n, !isValidELB(n));
        }

        arr = str2;
        System.out.println("Valid Lab Journal Biol");
        for (int ii = 0; ii < arr.length; ii++) {
            String s = arr[ii];
            String n = getNormalizedAndCheckedLJB(s);
            System.out.printf("%s %s %s\n", s, n, isValidELB(n));
        }

        arr = testPatternsACTNumbers();
        System.out.println("\nActelion Numbers");
        for (int ii = 0; ii < arr.length; ii++) {
            System.out.println(arr[ii] + " " + getNormalizedAndCheckedACT(arr[ii]));
        }
        arr = testPatternsLJCNumbers();
        System.out.println("\nChem Lab Journal");
        for (int ii = 0; ii < arr.length; ii++) {
            System.out.println(arr[ii] + " " + getNormalizedAndCheckedLJC(arr[ii]));
        }

        arr = testPatternsELNNumbers();
        System.out.println("\nELN Numbers");
        for (int ii = 0; ii < arr.length; ii++) {
            System.out.println(arr[ii] + " " + isValidELN(arr[ii]));
        }
    }
*/
}


