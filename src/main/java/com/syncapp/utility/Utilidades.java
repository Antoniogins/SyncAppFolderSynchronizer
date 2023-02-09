package com.syncapp.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import com.syncapp.model.BloqueBytes;
import com.syncapp.model.LocalRemote;
import com.syncapp.model.Archivo;

/**
 * Esta clase permite trabajar con listas de archivos, y los metadatos del archivo. Permite
 * abstraer al cliente de la comparacion de listas de archivos.
 */

public class Utilidades {


    // Metodos para iterar carpetas y obtener listas de archivos y directorios

    /**
     * Este metodo devuelve un {@link ArrayList}<{@link Archivo}> (lista de archivos, exclusivamente archivos)
     * que existe bajo el directorio {@link Path de entrada}. Usa el metodo {@link Files#walk(Path, FileVisitOption...)} para
     * iterar el directorio y subdirectorios dados, buscando los archivos que realmente son un archivo, y los añade
     * a la lista.
     *
     * <pre>
     * La lista de archivos viene relativizada al path que queremos lista.
     * Por ejemplo: p = /home/usuario/carpeta
     *              c = /home/usuario/carpeta/documentos/renta.pdf
     *              r = documentos/renta.pdf   ->   path de "c" relativo a "p"
     * </pre>
     * Esto ocurre para todos los archivos de la lista, respecto al path de entrada.
     * 
     * @param pathToList directorio sobre el que se quiere listar los archivos.
     * @return {@link ArrayList}<{@link Archivo}> dentro del directorio indicado.
     * @throws IOException si ocurre un problema al iterar los directorios.
     */
    public static ArrayList<Archivo> listFiles(Path pathToList) {

        // Comprobamos que el path indicado cumpla las condiciones para poder listar los archivos.
        // En este caso para que un directorio se pueda iterar, debe ser no nulo y debe ser absoluto
        if (  !(pathToList != null && pathToList.isAbsolute()) ){
            return null;
        }

        // Comprobamos si el directorio indicado es un archivo
        if(pathToList.toFile().isFile()) {
            System.out.println("el directorio indicado es un archivo, no se puede iterar");
            return null;
        }

        // Comprobamos si el directorio existe, en caso contrario lo creamos.
        if(!pathToList.toFile().exists()) {
            try {
                Files.createDirectory(pathToList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // Creamos la lista en la que añadiremos los archivos
        ArrayList<Archivo> lista = new ArrayList<>();

        // Por motivos de seguridad, normalizamos el path a iterar, esto se debe a que se puede indicar un directorio
        // superior indicando "..", lo cual en un servidor puede acceder a directorios que los usuarios no deberian acceder.
        Path pathNorm = pathToList.normalize();

        // Iteramos el directorio indicado
        try {
            Files.walk(pathNorm).forEach(c -> {

                // Dado que unicamente queremos poner al archivo la ruta respecto al path de original, lo relativizamos
                // Por ejemplo: pathNorm = /home/usuario/carpeta
                //                     c = /home/usuario/carpeta/documentos/renta.pdf
                //                     m = documentos/renta.pdf
                Path m = pathNorm.relativize(c);

                // Si el path c corresponde a un archivo, y su tamaño es mas de 0bytes, lo añadimos a la lista de archivos
                long fileSize = c.toFile().length();
//                System.out.println(fileSize);
                if (c.toFile().isFile() && fileSize > 0){
//                    System.out.println("tiene mas de 0bytes asi que añado");
                    Archivo toAdd = new Archivo(m, pathNorm);
                    toAdd.sizeInBytes = fileSize;
                    lista.add(toAdd);
                }
                
            });
        } catch (IOException ioe) {
            System.out.println("excepcion en utils al listar archivos");
            ioe.printStackTrace();
        }

        return lista;

    }



    /**
     * Devuelve un {@link ArrayList}<{@link Path}> con los directiorios dentro del path de entrada. <br>
     * Itera el path de entrada, hasta que no encuentra mas archivos/directorios.
     * @param pathToList {@link Path} del que queremos conocer los subdirectorios.
     * @return {@link ArrayList}<{@link Path}> con los subdirectorios dentro del path de entrada.
     */
    public static ArrayList<Path> listFolders(Path pathToList) {

        // Comprobamos si cumple los requisistos para ser iterado, en caso contrario devolvemos null
        if (  !(pathToList != null && pathToList.isAbsolute())  ) {
            return null;
        }

        // Creamos la lista donde añadiremos los directorios encontrados
        ArrayList<Path> lista = new ArrayList<>();

        // Iteramos el path de entrada, buscando los paths que sean directorios
        try {
            Files.walk(pathToList).forEach(c -> {

                // Si el path actual es un directorio, lo añadimos a la lista
                if (c.toFile().isDirectory()) {
                    lista.add(c);
                }

            });
        } catch (IOException ioe) {
            System.out.println("excepcion en utils al listar carpetas");
            ioe.printStackTrace();
        }

        return lista;

    }





















    // Metodos para obtener metadatos de una lista de archivos

    /**
     * Este metodo nos permite obtener metadatos de archivos de la maquina que lo ejecuta. Estos metadatos se
     * obtienen para cada uno de los archivos de la lista de entrada. Los metadatos que se obtienen son: hashmd5
     * ({@link String}) y la ultima fecha/hora de modificacion ({@link Long})
     * <br>
     * Dado que los archivos contienen rutas relativas (a la carpeta de sincronizacion), es necesario que se indique
     * dicha carpeta para acceder poder acceder al archivo real.
     *
     * @param lista {@link ArrayList}<{@link Archivo}> lista de los que se quiere obtener metadatos.
     * @param containerFolder carpeta contenedora de esos archivos (directorio de la carpeta de sincronizacion)
     * @return {@link ArrayList}<{@link Archivo}> lista de los archivos pedidos, pero contienen los metadatos.
     */
    public static ArrayList<Archivo> obtenerMultiplesMetadatos(ArrayList<Archivo> lista, Path containerFolder) {

        // Comprobamos que la lista contenga elementos
        if(lista == null || lista.size() == 0) {
            return null;
        }

        // Creamos una lista temporal en la que añadiremos los archivos conforme vayamos leyendo
        ArrayList<Archivo> listaFinal = new ArrayList<>();

        // Para cada uno de los archivos de entrada, calculamos el hash y obtenemos la ultima fecha de modificacion.
        lista.forEach(c -> {
            try {

                listaFinal.add( obtenerMetadatos( c, containerFolder) );
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return listaFinal;
    }






















    // Metodos para obtener metadatos sobre un archivo

    /**
     * Este metodo nos permite obtener los metadatos de un archivo en concreto.
     * <br>
     * Se genera un checksum tipo md5, realizado mediante la clase {@link MessageDigest}.<br>
     * Este MessageDigest consume todos los bytes que tiene un archivo (los vamos introduciendo por bloques) y
     * tras consumir todos, genera un array de bytes representando el hash. Se necesita transformar ese array de bytes
     * en un String, para ello usamos un {@link StringBuilder}, que convertira cada uno de los bytes archivo un caracter y
     * construira la cadena de texto.
     * <br>
     * Para la ultima fecha de modificacion, el sistema de archivos almacena automaticamente la ultima vez que se ha
     * modificado ese archivo, asi que se obtiene usando una clase, y se obtiene en forma de milisegundos. Estos
     * milisegundos, son una cuenta en milisegundos desde 1970, y que nos permite sincronizarnos a una hora concreta.
     * <br>
     * Para comparar las horas de modificacion, se tiene en cuenta el offset de tiempo que el cliente tiene respecto
     * al servidor, para ello, inicialmente, se ha ejecutado el Algoritmo de Cristian para obtener nuestro offset
     * respecto al servidor.
     * @param archivo {@link Archivo} sobre el que se quiere consultar los metadatos.
     * @param workingFolder carpeta de trabajo del cliente, ya que las rutas de los archivos son relativas a esa carpeta.
     * @return {@link Archivo} con los metadatos en su interior.
     * @throws IOException si ocurre un problema al leer los metadatos del fichero.
     */
    public static Archivo obtenerMetadatos(Archivo archivo, Path workingFolder) throws IOException {

        // Creamos un nuevo archivo, que sera donde incluiremos los metadatos
        Archivo deVuelta = new Archivo(  archivo.toRelativePath(), workingFolder);

        // Obtenemos el hash del fichero y se lo añadimos al fichero de vuelta
        deVuelta.hash = checkSumhash( deVuelta );

        // Obtenemos la hora de ultima modificacion
        FileTime ft = Files.getLastModifiedTime( deVuelta.toPath() );

        // Le añadimos la hora al archivo de vuelta
        deVuelta.timeMilisLastModified = ft.toMillis();

        return deVuelta;
    }



    /**
     * Este metodo nos permite crear un checksum a un File de entrada, usango el
     * algoritmo md5.<br>
     * Para ello, el {@link File archivo} de entrada se separa en bloques de 800KB (para mejorar la rapidez del algoritmo)
     * y lo pasamos al obtejo {@link MessageDigest} para que lo consuma y vaya produciendo el hash. Una vez que se
     * ha consumido el total de bytes del archivo, se invoca el metodo {@link MessageDigest#digest()} que nos devolvera
     * un array de bytes representativo del hash. Este array de bytes lo tenemos que convertir a un String, dado que
     * es como se representa un hashmd5. Para ello usamos un {@link StringBuilder}, al que le pasaremos el array de bytes,
     * y le indicaremos una funcion que convierte esos bytes en texto.
     * 
     * @param archivo {@link Archivo} al que le realizaremos el md5sum.
     * @return un String que contiene el resultado del algoritmo md5.
     * @throws FileNotFoundException si existe el archivo indicado
     * @throws IOException           cuando se produce un error al leer el archivo
     */
    public static String checkSumhash(Archivo archivo) {

        // Comprobamos errores antes de intentar realizar ninguna operacion
        if (archivo == null || archivo.toFile().exists()){
            return  null;
        }


        // Usamos un StringBuilder para formatear el resultado del md5
        StringBuilder sb = new StringBuilder();

        // Algoritmo md5
        try {
            long posicion = 0;

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BloqueBytes data;

            while (   (data = LectorArchivos.leerBloqueBytes(posicion, archivo))  != null   ){
                md5.update(data.data);
            }

            // Finalizamos el resultado del algoritmo, y lo almacenamos en un array
            byte[] result = md5.digest();

            // Construimos el String del hash
            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }








        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

        // Devolvemos el hash md5 del archivo
        return sb.toString();


    }
























    // Metodos para obtener hashes sobre objetos (no archivos)

    /**
     * Analogo a {@link #checkSumhash(Archivo)} pero, en vez de trabajar sobre un fichero, se trabaja con un array de bytes,
     * que pueden ser un String convertido a bytes[], un entero, un Objeto, etc.
     * <br>
     * Este metodo nos permite generar identificadores unicos seguros para el manejo de constraseñas e informacion
     * sensible (no implementado)
     * @param bytes {@link Byte[]} array de bytes sobre el que calcular el md5.
     * @return {@link String} que representa el hash md5.
     */
    public String hashmd5(byte[] bytes) {
        try {

            // Analogo a la funcion anterior, pero mas sencilla, consulte la funcion checksumhash()

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            byte[] result = md5.digest();

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }

            return  sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }




}
