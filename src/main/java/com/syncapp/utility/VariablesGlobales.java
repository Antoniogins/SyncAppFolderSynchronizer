package com.syncapp.utility;


/**
 * Esta clase describe variables globales al sistema distribuido.
 */
public class VariablesGlobales {
    public static final int UPLOAD = 1100 ;
    public static final int DOWNLOAD = 2200 ;
    public static final int FAIL = 001100 ;
    public static final int SUCESS = 11100011 ;
    public static final int MORE_INFO = 0011010001;
    public static final int MAX_BYTES_IN_BLOCK = 24000000; //Por defecto 24MB = 24 000 000B

    // ANSII escape colors
    public static final String COLOR_BLACK = "\033[90m";
    public static final String COLOR_RED = "\033[91m";
    public static final String COLOR_GREEN = "\033[92m";
    public static final String COLOR_YELLOW = "\033[93m";
    public static final String COLOR_BLUE = "\033[94m";
    public static final String COLOR_MAGENTA = "\033[95m";
    public static final String COLOR_CYAN = "\033[96m";
    public static final String COLOR_WHITE = "\033[97m";


    public static String toString(int op) {
        String res = "not recognized operation";
        switch (op) {
            case UPLOAD -> {
                res = "op_upload";
            }
            case DOWNLOAD -> {
                res = "op_download";
            }
            case MORE_INFO -> {
                res = "op_more_info";
            }
        }

        return res;
    }
}
