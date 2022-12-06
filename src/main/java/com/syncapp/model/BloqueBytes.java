package com.syncapp.model;

import com.syncapp.utility.VariablesGlobales;

import java.io.Serializable;

public class BloqueBytes implements Serializable{
    public byte[] data;
    public int size;
    public long position;
    public int fileID;

    // cuando iniciemos a contar los bloques, que comience en 0, asi cuando se
    // pierde un bloque es facil re obtenerlos, ya que su posicion inicial sera su
    // id*Util.MAX_BYTES (cantidad de bytes ya leidos)

    @Override
    public String toString() {
        float kb = (float) size/1000; //Para saber cuantos kB
        float mb = kb/1000;

        String sizeText = (mb<1) ? ( (kb<1) ? (size+"B") : kb+"KB" )  : (mb+"MB")  ;

        return "[file="+VariablesGlobales.COLOR_MAGENTA+ fileID+ VariablesGlobales.COLOR_WHITE +",pos="+position+",size="+ VariablesGlobales.COLOR_MAGENTA+sizeText+VariablesGlobales.COLOR_WHITE+"]";
    }
}
