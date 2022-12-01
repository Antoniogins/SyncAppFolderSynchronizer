package com.syncapp.utility;


/**
 * Esta enumeracion nos permite representar ciertas acciones al establecer el servicio SyncApp
 * 
 * @author Antonio Gines Buendia Lopez
 */
public class Ops {
    public static final int UPLOAD = 1100 ;
    public static final int DOWNLOAD = 2200 ;
    public static final int FAIL = 001100 ;
    public static final int SUCESS = 11100011 ;
    public static final int MORE_INFO = 0011010001;


    // public static final int  =  ;
    public static String toString(int op) {
        String res = "not recognized operation";
        switch(op) {
            case UPLOAD : {
                res = "op_upload";
                break;
            }
            case DOWNLOAD : {
                res = "op_download";
                break;
            }
            case MORE_INFO : {
                res = "op_more_info";
                break;
            }
        }

        return res;
    }
}
