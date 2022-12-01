package com.syncapp.model;

import java.io.Serializable;

public class BloqueBytes implements Serializable{
    public byte[] data;
    public int size;
    public long position;
    public int id;
    public int id_file;

    // cuando iniciemos a contar los bloques, que comience en 0, asi cuando se
    // pierde un bloque es facil re obtenerlos, ya que su posicion inicial sera su
    // id*Util.MAX_BYTES (cantidad de bytes ya leidos)

    @Override
    public String toString() {
        float kb = size/1000; //Para saber cuantos kB
        // float mb = size/1000000; //Para saber cuantos mB

        // DecimalFormat df = new DecimalFormat("0.00");
        
        // String sizzze = ( mb < 1.0 )? df.format(kb)+"KB" : df.format(mb)+"MB" ;
        // return "[ file_id="+", block_id="+id+", pos="+position+", size="+sizzze+"]";
        return "[file="+id_file+", block_id="+id+", pos="+position+", size="+kb+"KB]";
    }
    public void anunciate() {
        System.out.println(this.toString());
    }
}
