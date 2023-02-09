package com.syncapp.model;

import com.syncapp.utility.VariablesGlobales;
import java.io.Serializable;

/**
 * Esta clase representa el modelo de un bloque de bytes de un archivo. Para ello, tiene ciertos parametros, que
 * permite transferir el archivo en una secuencia de bloques, de forma sencilla. Ademas contine un identificador de
 * archivo, lo que permite que no se pierda el bloque. Se trata, en esencia, una encapsulacion para facilitar el
 * trabajo de bloques.
 */
public class BloqueBytes implements Serializable{

    /**
     * Array de bytes que constituyen el bloque de bytes. Representan una porcion del archivo.
     */
    public byte[] data;

    /**
     * Indica el tamaño del bloque, medido en numero de bytes que contiene.
     */
    public int size;

    /**
     * Representa la posicion del primer byte del bloque. Esto es, la posicion desde la cual se debe escribir el bloque.
     */
    public long position;

    /**
     * Identificador del archivo al que pertenece el bloque.
     */
    public int fileID;


    /**
     * Este metodo nos permite obtener el bloque de datos en texto, de forma que sea legible para el ser humano.
     * @return {@link String} que representa al bloque.
     */
    @Override
    public String toString() {
        float kb = (float) size / 1000; //Para saber cuantos kB
        float mb = kb / 1000;
        // no comprobamos GB ya que se sabe de antemano que los bloques son del orden de B, KB o MB (24MB normalmente)

        String sizeText = (mb < 1) ? ((kb < 1) ? (size + "B") : kb + "KB") : (mb + "MB");

        return "[file=" + VariablesGlobales.COLOR_MAGENTA + fileID + VariablesGlobales.COLOR_WHITE + ",pos=" + position +
                ",size=" + VariablesGlobales.COLOR_MAGENTA + sizeText + VariablesGlobales.COLOR_WHITE + "]";
    }





    public void keepOnlyNBytes(int numberOfBytesToKeep){
        if(numberOfBytesToKeep >= 0 && numberOfBytesToKeep < VariablesGlobales.MAX_BYTES_IN_BLOCK) {
            byte[] temp = new byte[numberOfBytesToKeep];
            System.arraycopy(data, 0, temp, 0, numberOfBytesToKeep);
            data = temp;
        }
    }
}
