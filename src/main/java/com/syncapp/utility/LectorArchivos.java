package com.syncapp.utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import com.syncapp.model.Archivo;
import com.syncapp.model.BloqueBytes;


/**
 * Clase que permite leer y escribir archivos mediante {@link BloqueBytes bloques de bytes}.
 */
public class LectorArchivos {

    // Transmision de datos

    /**
     * Con este metodo podemos leer un {@link BloqueBytes} del archivo con el que se creo el {@link LectorArchivos},
     * indicando la {@link Long posicion} a partir de la cual queremos leer. Para leer bloques es necesario indicar la
     * posicion de lectura, pues esta no se almacena, ya que varios usuarios pueden leer del mismo archivo simultaneamente.
     * <br>
     * Un dato importante, es que cuando se lee el ultimo bloque de bytes, se creara un {@link BloqueBytes} de tamaño distinto
     * al indicado en {@link VariablesGlobales#MAX_BYTES_IN_BLOCK}, porque no se deben llenar archivos con bytes vacios,
     * pues esto cambiara totalmente su hash. <br>
     * Si el archivo no tiene mas bytes para leer, se devuelve null.
     * @param posicion {@link Long} indicando el primer byte que se quiere leer.
     * @return {@link BloqueBytes} del tamaño de bytes que se hayan podido leer. Si hay suficientes bytes, se leeran hasta
     * {@link VariablesGlobales#MAX_BYTES_IN_BLOCK}, en caso contrario, se leeran todos los bytes que queden.
     * @throws IOException si ocurre un problema al leer el archivo.
     */
    public static BloqueBytes leerBloqueBytes(long posicion, Archivo archivo) throws IOException {
        if(posicion < 0 || archivo == null) return null;


        BloqueBytes bb = new BloqueBytes();
        bb.data = new byte[VariablesGlobales.MAX_BYTES_IN_BLOCK];

        try (FileInputStream fis = new FileInputStream(archivo.toFile())) {

            fis.skip(posicion);

            int totalBytes = fis.read(bb.data);

            if(totalBytes < 1) return null;

            bb.keepOnlyNBytes(totalBytes);

            bb.position = posicion + totalBytes;
            bb.size = totalBytes;
            bb.fileID = archivo.globalID;
        }

        return bb;
    }




    /**
     * Con este metodo podemos escribir un {@link BloqueBytes} dentro de un archivo. El bloque debe contener en su interior
     * la posicion en la que quiere escribir.
     * <br>
     * Se escribiran hasta {@link BloqueBytes#size} bytes de datos.
     * @param bb
     * @throws IOException
     */
    public void escribirBloqueBytes(BloqueBytes bb, Archivo archivo) throws IOException {
        if(bb == null || archivo == null || bb.size < 1 || bb.position < 0) return;

        boolean beginOfFile = (bb.position == 0);
        try (FileOutputStream fos = new FileOutputStream(archivo.toFile(), !beginOfFile)) {
            // Negamos beingOfFile, ya que, este argumento sirve para indicar si se añaden bytes al archivo
            // o si se crea uno desde 0 (append si true)

            // Escribimos los datos
            fos.write(bb.data);

            // Los volcamos en el archivo
            fos.flush();

        }

        System.out.println("escribiendo el bloque"+bb); //TESTS
    }









}
