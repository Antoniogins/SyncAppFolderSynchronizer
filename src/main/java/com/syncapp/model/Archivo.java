package com.syncapp.model;

import com.syncapp.utility.VariablesGlobales;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Esta clase abstrae el concepto de archivo dentro del sistema distribuido. Nos permite trabajar de forma mas sencilla,
 * pues contiene toda la informacion necesaria de un archivo para el sistema distribuido, evitando hacer uso extra de
 * invocaciones al servidor.
 */
public class Archivo implements Serializable, Comparable {

    /**
     * Contiene la ruta de un archivo, respecto a la carpeta de sincronizacion.
     */
    public final String childFolder; //Ruta a un archivo de forma realitva a la carpeta que queremos sincronizar

    /**
     * Contiene la ruta de la carpeta de sincronizacion.
     */
    public String parentFolder;

    /**
     * Contiene un hash md5 en formato texto.
     */
    public String hash;

    /**
     * Contiene la ultima fecha de modificacion en milisegundos. Esta cuenta de milisegundos viene dada respecto a una
     * fecha de 1970, viene calculada por Java. Nos permite saber la ultima fecha de modificacion de un archivo de forma
     * simple.
     */
    public long timeMilisLastModified;

    /**
     * Contiene el tamaño en bytes de un archivo. Esta informacion es unicamente ilustrativa para el usuario, nunca
     * comparar si un archivo ha cambiado por su tamaño, para ello se dispone de hash y hora de modificacion.
     */
    public long sizeInBytes;

    /**
     * Contiene el identificador unico del archivo dentro del servidor.
     */
    public String remoteID;


    /**
     * Identificador global dentro de nuestro contexto de transmisiones.
     */
    public int globalID;




    // Constructores

    /**
     * Nos permite crear un child a partir de su ruta relativa y la ruta de la carpeta de sincronizacion.
     * @param child {@link Path ruta} del child respecto a la carpeta de sincronizacion.
     * @param parent {@link Path ruta} de la carpeta de sincronizacion.
     */
    public Archivo(Path child, Path parent) {
        // Comprobamos si la ruta indicada es absoluta, en cuyo caso la relativizamos respecto a la carpeta de sincronizacion
        if(  child.isAbsolute()  ) {
            childFolder = (parent.relativize(child)).toString();
        } else {
            childFolder = child.toString();
        }

        // Guardamos los valores
        parentFolder = parent.toString();

        File archivoReal = Paths.get( parent.toString() , childFolder).toFile();

        // Introducimos los metadatos
        sizeInBytes = archivoReal.length();
        timeMilisLastModified = archivoReal.lastModified();
//        hash = Utilidades.checkSumhash(archivoReal); // hash no lo obtenemos de primeras, ya que es una funcion pesada


    }


    /**
     * Permite crear un archivo a partir de su ruta relativa. Es util cuando no necesitamos la ruta completa del archivo.
     * @param childFolder {@link String ruta} relativa del archivo.
     */
    public Archivo(String childFolder) {
        this.childFolder = childFolder;
    }























    // Conversores

    /**
     * Con este metodo podemos obtener un archivo en forma de texto, de misma forma para todos los archivos.
     * @return {@link String} texto representativo del archivo.
     */
    @Override
    public String toString() {

        // Creamos el string
        String toRet = "[";

        // Añadimos la ruta, en color CYAN
        toRet = toRet.concat(VariablesGlobales.COLOR_CYAN + childFolder + VariablesGlobales.COLOR_WHITE + " , ");

        // Añadimos el hash, si no hay hash añadimos "no_hash"
        toRet = toRet.concat((hash == null) ? "no_hash , " : (hash + " , "));

        // Obtenemos la fecha de modificacion en formato año/mes/dia-hora:minutos:segundos
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss");
        Date fecha = new Date(timeMilisLastModified);

        // Añadimos la fecha de modificacion
        toRet = toRet.concat((timeMilisLastModified < 0) ? "no_time , " : (df.format(fecha) + " , "));

        // Calculamos el tamaño para que sea legible por el usuario
        float kb = (float) sizeInBytes / 1000; //Para saber cuantos kB
        float mb = kb / 1000;
        float gb = mb / 1000;

        // Comprobamos cual es la forma mas adecuada para representar el tamaño
        String sizeText = (gb < 1) ? ((mb < 1) ? ((kb < 1) ? (sizeInBytes + "B") : kb + "KB") : (mb + "MB")) : (gb + "GB");

        // Finalmente, añadimos el texto del tamaño y devolvemos el texto
        return toRet +  VariablesGlobales.COLOR_MAGENTA + sizeText + VariablesGlobales.COLOR_WHITE + " ]";
    }

    /**
     * Con este metodo podemos convertir el archivo en un {@link Path} absoluto, uniendo la ruta de trabajo del archivo
     * mas su propia ruta relativa.
     * @return {@link Path} absoluto del archivo (workingFloder + ruta).
     */
    public Path toPath() {
        return Paths.get(parentFolder, childFolder);
    }

    /**
     * Con este metodo podemos convertir el archivo en un {@link Path}, pero unicamente con la ruta relativa del archivo.
     * @return {@link Path} relativo del archivo.
     */
    public Path toRelativePath() {
        return Paths.get(childFolder);
    }


    /**
     * Con este metodo podemos convertir el archivo en un {@link File}, para ello se usa la ruta completa del archivo (
     * ruta de trabajo + ruta del archivo).
     * @return
     */
    public File toFile() {
        return Paths.get(parentFolder, childFolder).toFile();
    }










    /**
     * Con este metodo definimos como se compara un objeto tipo Archivo. Esto es util para poder implementar mayor
     * facilidad de comparacion en el codigo (para que java lo haga por nosotros.
     * @param o the object to be compared (must be Archivo).
     * @return -1 if the object is not of the type Archivo. Elsewhere, it will return the comparison of both their
     * childFolder strings, that is, -1 if given object string is lower (alphabetically), 0 if its the same, and 1 if
     * higher.
     */
    @Override
    public int compareTo(Object o) {
        if(o instanceof Archivo) {
            Archivo oo = (Archivo) o;
            return oo.childFolder.compareTo(this.childFolder);
        }
        return -1;
    }
}
